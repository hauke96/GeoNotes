package de.hauke_stieler.geonotes.export;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = Injector.get(Database.class);
        map = Injector.get(Map.class);
        noteIconProvider = Injector.get(NoteIconProvider.class);
        markerFragment = Injector.get(MarkerFragment.class);

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

        view.findViewById(R.id.backup_import_start_button).setOnClickListener(v -> {
            hideAllBottomControls();
            view.findViewById(R.id.backup_import_wait_layout).setVisibility(View.VISIBLE);
            view.requestLayout();
            view.invalidate();

            new Handler().post(() -> {
                boolean shouldAppend = ((Switch) view.findViewById(R.id.backup_import_append_overwrite_switch)).isChecked();
                boolean shouldImportNotes = ((Switch) view.findViewById(R.id.backup_import_notes_switch)).isChecked();
                boolean shouldImportPhotos = ((Switch) view.findViewById(R.id.backup_import_photos_switch)).isChecked();
                boolean shouldImportCategories = ((Switch) view.findViewById(R.id.backup_import_categories_switch)).isChecked();
                boolean shouldImportSettings = ((Switch) view.findViewById(R.id.backup_import_settings_switch)).isChecked();

                File externalFilesDir = getContext().getExternalFilesDir(FileHelper.GEONOTES_EXTERNAL_DIR_NAME);

                InputStream backupFileInputStream;
                try {
                    backupFileInputStream = getContext().getContentResolver().openInputStream(selectedInputFileUri);
                } catch (FileNotFoundException e) {
                    Log.e("import", "Error creating backup file input stream", e);
                    CharSequence filename = ((TextView) view.findViewById(R.id.backup_import_filename_label)).getText();
                    Toast.makeText(getContext(), "File " + filename + " not found. Abort import.", Toast.LENGTH_SHORT).show();
                    showDoneWithErrorMessage();
                    return;
                }

                File backupExtractDir;
                try {
                    backupExtractDir = File.createTempFile("backup-extract-" + System.currentTimeMillis(), "", externalFilesDir);
                    backupExtractDir.deleteOnExit();
                } catch (IOException e) {
                    Log.e("import", "Error creating temp dir for zip extract", e);
                    Toast.makeText(getContext(), "Error creating temporary directory to extract ZIP file: " + e.getMessage() + ". Abort import.", Toast.LENGTH_LONG).show();
                    showDoneWithErrorMessage();
                    return;
                }

                if (backupExtractDir.exists()) {
                    backupExtractDir.delete();
                }

                boolean successful = backupExtractDir.mkdirs();
                if (!successful) {
                    Toast.makeText(getContext(), "Temporary directory " + backupExtractDir.getName() + " could not be created. Abort import.", Toast.LENGTH_LONG).show();
                    showDoneWithErrorMessage();
                    return;
                }

                try {
                    Zip.unzipFlatZip(backupFileInputStream, backupExtractDir);
                } catch (IOException e) {
                    Log.e("import", "Error unzipping backup file", e);
                    Toast.makeText(getContext(), "Error unzipping backup file: " + e.getMessage() + ". Abort import.", Toast.LENGTH_LONG).show();
                    showDoneWithErrorMessage();
                    return;
                }

                List<File> backupJsonFiles = Arrays.stream(backupExtractDir.listFiles()).filter(f -> f.getName().endsWith(".json")).collect(Collectors.toList());
                if (backupJsonFiles.isEmpty()) {
                    Toast.makeText(getContext(), "Could not find JSON file in unzipped backup. Abort import.", Toast.LENGTH_LONG).show();
                    showDoneWithErrorMessage();
                    return;
                } else if (backupJsonFiles.size() > 1) {
                    Toast.makeText(getContext(), "Found multiple JSON files in unzipped backup, only one is expected. Abort import.", Toast.LENGTH_LONG).show();
                    showDoneWithErrorMessage();
                    return;
                }

                NoteBackupModel noteBackupModel;
                try {
                    noteBackupModel = new GsonBuilder().create().fromJson(new FileReader(backupJsonFiles.get(0)), NoteBackupModel.class);
                } catch (FileNotFoundException e) {
                    Log.e("import", "Error turning JSON file to Java object", e);
                    Toast.makeText(getContext(), "Could not open file stream to backup JSON file: " + e.getMessage() + ". Abort import.", Toast.LENGTH_LONG).show();
                    showDoneWithErrorMessage();
                    return;
                }

                if (!shouldAppend) {
                    database.removeAllNotes(externalFilesDir);
                    database.removeAllCategories();
                }

                // TODO version check of the backup

                if (shouldImportPhotos) {
                    Arrays.stream(backupExtractDir.listFiles()).filter(f -> f.getName().toLowerCase().endsWith(".jpg"))
                            .forEach(photoFile -> {
                                boolean movedSuccessfully = photoFile.renameTo(new File(externalFilesDir, photoFile.getName()));
                                if (!movedSuccessfully) {
                                    Toast.makeText(getContext(), "Could not move file " + photoFile.getName() + " to the app folder.", Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    noteBackupModel.notes.forEach(note -> note.photosFileNames = new ArrayList<>());
                }

                // Old ID to new ID
                HashMap<Long, Long> categoryIdMap = new HashMap<>();
                if (shouldImportCategories) {
                    for (int i = 0; i < noteBackupModel.categories.size(); i++) {
                        CategoryModel category = noteBackupModel.categories.get(i);
                        long newId = database.addCategory(category.color, category.name, i);
                        categoryIdMap.put(category.id, newId);
                    }
                } else {
                    // TODO Take first category from DB as default category and use it below.
                }

                if (shouldImportNotes) {
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

                // TODO import settings

                noteIconProvider.updateIcons();
                map.reloadAllNotes();
                markerFragment.reloadCategories();
                // TODO NoteList not updated, all icons are empty.

                hideAllBottomControls();
                view.findViewById(R.id.backup_import_done_layout).setVisibility(View.VISIBLE);
                view.findViewById(R.id.backup_import_done_label).setVisibility(View.VISIBLE);
            });
        });

        view.findViewById(R.id.backup_import_close_button).setOnClickListener(v -> dismiss());

        return view;
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
