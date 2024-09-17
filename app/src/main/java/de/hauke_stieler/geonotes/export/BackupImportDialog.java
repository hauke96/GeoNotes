package de.hauke_stieler.geonotes.export;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import de.hauke_stieler.geonotes.BuildConfig;
import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.map.Map;
import de.hauke_stieler.geonotes.map.MarkerFragment;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;

public class BackupImportDialog extends DialogFragment {
    private ActivityResultLauncher<String> resultLauncher;
    private Uri selectedInputFileUri;
    private Database database;
    private Map map;
    private NoteIconProvider noteIconProvider;
    private MarkerFragment markerFragment;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = Injector.get(Database.class);
        map = Injector.get(Map.class);
        noteIconProvider = Injector.get(NoteIconProvider.class);
        markerFragment = Injector.get(MarkerFragment.class);
        sharedPreferences = Injector.get(SharedPreferences.class);

        resultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    cursor.moveToFirst();
                    ((TextView) getView().findViewById(R.id.backup_import_filename_label)).setText(cursor.getString(nameIndex));
                    selectedInputFileUri = uri;

                    getView().findViewById(R.id.backup_import_start_button).setEnabled(true);
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_import_dialog, container);

        view.findViewById(R.id.backup_import_file_select_button).setOnClickListener(v -> {
            resultLauncher.launch("application/zip");
        });

        ((Switch) view.findViewById(R.id.backup_import_append_overwrite_switch))
                .setOnCheckedChangeListener((buttonView, isAppendSelected) -> {
                    // Append=true -> No notice visible / Overwrite=false -> Notice visible
                    view.findViewById(R.id.backup_import_overwrite_warning_label).setVisibility(isAppendSelected ? View.GONE : View.VISIBLE);
                });

        ((Switch) view.findViewById(R.id.backup_import_notes_switch))
                .setOnCheckedChangeListener((buttonView, isSelected) -> {
                    Switch photoSwitch = view.findViewById(R.id.backup_import_photos_switch);
                    if (isSelected) {
                        photoSwitch.setEnabled(true);
                    } else {
                        photoSwitch.setChecked(false);
                        photoSwitch.setEnabled(false);
                    }
                });

        ((Switch) view.findViewById(R.id.backup_import_categories_switch))
                .setOnCheckedChangeListener((buttonView, isSelected) -> {
                    boolean shouldAppend = ((Switch) view.findViewById(R.id.backup_import_append_overwrite_switch)).isChecked();
                    if (!shouldAppend) {
                        // Deleting all existing data and then importing notes without categories
                        // doesn't make any sense. So we prohibit overwriting imports with notes
                        // but without categories.
                        Switch noteSwitch = view.findViewById(R.id.backup_import_notes_switch);
                        Switch photoSwitch = view.findViewById(R.id.backup_import_photos_switch);
                        if (isSelected) {
                            noteSwitch.setEnabled(true);
                            // Do not activate photo switch because note switch is unselected and
                            // therefore importing photos should not be possible (s. click handler above).
                        } else {
                            noteSwitch.setChecked(false);
                            noteSwitch.setEnabled(false);
                            photoSwitch.setChecked(false);
                            photoSwitch.setEnabled(false);
                        }
                    }
                });

        view.findViewById(R.id.backup_import_start_button).setOnClickListener(v -> {
            hideAllBottomControls();
            view.findViewById(R.id.backup_import_wait_layout).setVisibility(View.VISIBLE);

            new Handler().post(() -> startImport(view));
        });

        view.findViewById(R.id.backup_import_close_button).setOnClickListener(v -> dismiss());

        return view;
    }

    private void startImport(View view) {
        boolean shouldAppend = ((Switch) view.findViewById(R.id.backup_import_append_overwrite_switch)).isChecked();
        boolean shouldImportNotes = ((Switch) view.findViewById(R.id.backup_import_notes_switch)).isChecked();
        boolean shouldImportPhotos = ((Switch) view.findViewById(R.id.backup_import_photos_switch)).isChecked();
        boolean shouldImportCategories = ((Switch) view.findViewById(R.id.backup_import_categories_switch)).isChecked();
        boolean shouldImportSettings = ((Switch) view.findViewById(R.id.backup_import_settings_switch)).isChecked();

        File externalFilesDir = getContext().getExternalFilesDir(FileHelper.GEONOTES_EXTERNAL_DIR_NAME);

        Log.i("import", "1. Extract ZIP");
        File backupExtractDir = extractSelectedBackupFile(view, externalFilesDir);
        if (backupExtractDir == null) {
            Log.e("import", "Abort due to error during backup file extraction");
            return;
        }

        Log.i("import", "2. Create model");
        NoteBackupModel noteBackupModel = getBackupModelFromBackupFiles(backupExtractDir);
        if (noteBackupModel == null) {
            Log.e("import", "Abort due to error during model creation of backup");
            return;
        }

        Log.i("import", "3. Check backup version");
        if (!isVersionCompatible(noteBackupModel.geonotesVersion, BuildConfig.VERSION_CODE)) {
            Log.e("import", "Version of backup file incompatible (backup=" + noteBackupModel.geonotesVersion + ", current=" + BuildConfig.VERSION_CODE + ")");
            Toast.makeText(getContext(), "Version " + noteBackupModel.geonotesVersion + " of backup not compatible with app version " + BuildConfig.VERSION_CODE + ". Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return;
        }

        Log.i("import", "4. Check append setting");
        if (!shouldAppend) {
            database.removeAllNotes(externalFilesDir);
            database.removeAllCategories();
        }

        if (shouldImportPhotos) {
            Log.i("import", "5. Import photos");
            Arrays.stream(backupExtractDir.listFiles()).filter(f -> f.getName().toLowerCase().endsWith(".jpg"))
                    .forEach(photoFile -> {
                        boolean movedSuccessfully = photoFile.renameTo(new File(externalFilesDir, photoFile.getName()));
                        if (!movedSuccessfully) {
                            Toast.makeText(getContext(), "Could not move file " + photoFile.getName() + " to the app folder.", Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            Log.i("import", "5. Reset photos in notes to import");
            noteBackupModel.notes.forEach(note -> note.photosFileNames = new ArrayList<>());
        }

        Log.i("import", "6. Import categories (or create default mapping)");
        HashMap<Long, Long> categoryIdMap = importCategories(shouldImportCategories, noteBackupModel);
        if (categoryIdMap == null) {
            Log.e("import", "Abort due to error during note-to-category mapping creation");
            return;
        }

        Log.i("import", "7. Import notes (if selected)");
        if (shouldImportNotes) {
            importNotes(noteBackupModel, categoryIdMap, externalFilesDir);
        }

        Log.i("import", "8. Import settings (if selected)");
        if (shouldImportSettings) {
            importSettings(noteBackupModel);
        }

        Log.i("import", "9. Delete backup dir " + backupExtractDir.getName());
        boolean deletionOfBackupExtractDirSucceeded = deleteRecursive(backupExtractDir);
        if (!deletionOfBackupExtractDirSucceeded) {
            Log.w("import", "Backup extraction directory deletion failed.");
        }

        Log.i("import", "10. Update and reload everything");
        noteIconProvider.updateIcons();
        map.reloadAllNotes();
        markerFragment.reloadCategories();

        hideAllBottomControls();
        view.findViewById(R.id.backup_import_done_layout).setVisibility(View.VISIBLE);
        view.findViewById(R.id.backup_import_done_label).setVisibility(View.VISIBLE);
    }

    boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                boolean deleted = deleteRecursive(child);
                if (!deleted) {
                    return deleted;
                }
            }
        }

        return fileOrDirectory.delete();
    }

    private @Nullable File extractSelectedBackupFile(View view, File externalFilesDir) {
        InputStream backupFileInputStream;
        try {
            backupFileInputStream = getContext().getContentResolver().openInputStream(selectedInputFileUri);
        } catch (FileNotFoundException e) {
            Log.e("import", "Error creating backup file input stream", e);
            CharSequence filename = ((TextView) view.findViewById(R.id.backup_import_filename_label)).getText();
            Toast.makeText(getContext(), "File " + filename + " not found. Abort import.", Toast.LENGTH_SHORT).show();
            showDoneWithErrorMessage();
            return null;
        }

        File backupExtractDir;
        try {
            backupExtractDir = File.createTempFile("backup-extract-" + System.currentTimeMillis(), "", externalFilesDir);
            backupExtractDir.deleteOnExit();
        } catch (IOException e) {
            Log.e("import", "Error creating temp dir for zip extract", e);
            Toast.makeText(getContext(), "Error creating temporary directory to extract ZIP file: " + e.getMessage() + ". Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        }

        // Delete the temporary file and create it as folder below.
        if (backupExtractDir.exists()) {
            backupExtractDir.delete();
        }

        boolean successful = backupExtractDir.mkdirs();
        if (!successful) {
            Log.e("import", "Could not create temporary extract dir");
            Toast.makeText(getContext(), "Temporary directory " + backupExtractDir.getName() + " could not be created. Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        }

        try {
            Zip.unzipFlatZip(backupFileInputStream, backupExtractDir);
        } catch (IOException e) {
            Log.e("import", "Error unzipping backup file", e);
            Toast.makeText(getContext(), "Error unzipping backup file: " + e.getMessage() + ". Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        }
        return backupExtractDir;
    }

    private @Nullable NoteBackupModel getBackupModelFromBackupFiles(File backupExtractDir) {
        List<File> backupJsonFiles = Arrays.stream(backupExtractDir.listFiles()).filter(f -> f.getName().endsWith(".json")).collect(Collectors.toList());
        if (backupJsonFiles.isEmpty()) {
            Toast.makeText(getContext(), "Could not find JSON file in unzipped backup. Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        } else if (backupJsonFiles.size() > 1) {
            Toast.makeText(getContext(), "Found multiple JSON files in unzipped backup, only one is expected. Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        }

        NoteBackupModel noteBackupModel;
        FileReader reader;
        try {
            reader = new FileReader(backupJsonFiles.get(0));
        } catch (FileNotFoundException e) {
            Log.e("import", "Error turning JSON file to Java object", e);
            Toast.makeText(getContext(), "Could not open backup JSON file: " + e.getMessage() + ". Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        }

        noteBackupModel = new GsonBuilder().create().fromJson(reader, NoteBackupModel.class);

        try {
            reader.close();
        } catch (IOException e) {
            Log.e("import", "Error closing reader to backup JSON file", e);
            Toast.makeText(getContext(), "Error reading backup JSON file: " + e.getMessage() + ". Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        }
        return noteBackupModel;
    }

    private @Nullable HashMap<Long, Long> importCategories(boolean shouldImportCategories, NoteBackupModel noteBackupModel) {
        // Take the first category (in terms of the sorting of categories) as default
        // category for all imported notes.
        Category defaultCategory = database.getAllCategories()
                .stream()
                .min((c1, c2) -> (int) (c1.getSortKey() - c2.getSortKey()))
                .orElse(null);
        if (!shouldImportCategories && defaultCategory == null) {
            Log.e("import", "No default category found but needed because categories should not be imported.");
            Toast.makeText(getContext(), "No category found, which needed as default value because categories should not be imported. Abort import.", Toast.LENGTH_LONG).show();
            showDoneWithErrorMessage();
            return null;
        }

        // Old ID to new ID
        HashMap<Long, Long> categoryIdMap = new HashMap<>();
        for (int i = 0; i < noteBackupModel.categories.size(); i++) {
            CategoryModel category = noteBackupModel.categories.get(i);
            if (shouldImportCategories) {
                long newId = database.addCategory(category.color, category.name, i);
                categoryIdMap.put(category.id, newId);
            } else {
                categoryIdMap.put(category.id, defaultCategory.getId());
            }
        }
        return categoryIdMap;
    }

    private void importNotes(NoteBackupModel noteBackupModel, HashMap<Long, Long> categoryIdMap, File externalFilesDir) {
        noteBackupModel.notes.forEach(note -> {
            long noteId = database.addNote(
                    note.description,
                    note.lat,
                    note.lon,
                    categoryIdMap.get(note.categoryId),
                    note.createdAt
            );
            note.photosFileNames.forEach(photoFilename -> {
                database.addPhoto(noteId, new File(externalFilesDir, photoFilename));
            });
        });
    }

    private void importSettings(NoteBackupModel noteBackupModel) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String key = getContext().getString(R.string.pref_zoom_buttons);
        editor.putBoolean(key, (Boolean) noteBackupModel.preferences.getOrDefault(key, false));

        key = getContext().getString(R.string.pref_map_scaling);
        editor.putFloat(key, new Float((Double) noteBackupModel.preferences.getOrDefault(key, 1.0f)));

        key = getContext().getString(R.string.pref_snap_note_gps);
        editor.putBoolean(key, (Boolean) noteBackupModel.preferences.getOrDefault(key, false));

        key = getContext().getString(R.string.pref_enable_rotating_map);
        editor.putBoolean(key, (Boolean) noteBackupModel.preferences.getOrDefault(key, false));

        key = getContext().getString(R.string.pref_tap_duration);
        editor.putBoolean(key, (Boolean) noteBackupModel.preferences.getOrDefault(key, false));

        key = getContext().getString(R.string.pref_keep_camera_open);
        editor.putBoolean(key, (Boolean) noteBackupModel.preferences.getOrDefault(key, false));

        editor.commit();
    }

    static boolean isVersionCompatible(int backupVersion, int currentVersion) {
        // Example: 1006002 -> major=1, minor=6, patch=2
        int backupMajor = backupVersion / 1000 / 1000;
        int backupMinor = (backupVersion - backupMajor * 1000 * 1000) / 1000;
        // int backupPatch = (backupVersion - backupMajor * 1000 * 1000 - backupMinor * 1000);

        int currentMajor = currentVersion / 1000 / 1000;
        int currentMinor = (currentVersion - currentMajor * 1000 * 1000) / 1000;
        // int currentPatch = (backupVersion - currentMajor * 1000 * 1000 - currentMinor * 1000);


        if (backupMajor <= 1 && backupMinor < 7) {
            // Backups older than the version where backups were introduces (1.7.0) are considered
            // invalid since this should not happen!
            return false;
        }

        // We don't accept backups from significantly newer GeoNotes versions. This is strange and
        // to not break anything, this is not allowed. Also other major versions are considered
        // incompatible because a major version a) doesn't exist yet and b) means breaking changes.
        return currentMajor == backupMajor && currentMinor >= backupMinor;
    }

    private void showDoneWithErrorMessage() {
        hideAllBottomControls();
        getView().findViewById(R.id.backup_import_done_layout).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.backup_import_done_with_error_label).setVisibility(View.VISIBLE);
    }

    private void hideAllBottomControls() {
        getView().findViewById(R.id.backup_import_start_layout).setVisibility(View.GONE);
        getView().findViewById(R.id.backup_import_done_layout).setVisibility(View.GONE);
        getView().findViewById(R.id.backup_import_done_label).setVisibility(View.GONE);
        getView().findViewById(R.id.backup_import_done_with_error_label).setVisibility(View.GONE);
        getView().findViewById(R.id.backup_import_wait_layout).setVisibility(View.GONE);
    }
}
