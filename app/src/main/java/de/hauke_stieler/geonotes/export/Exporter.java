package de.hauke_stieler.geonotes.export;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class Exporter {
    private static final String LOGTAG = Exporter.class.getName();

    private final Database database;
    private final Context context;

    public Exporter(Database database, Context context) {
        this.database = database;
        this.context = context;
    }

    public void shareAsGeoJson() {
        String geoJson = GeoJson.toGeoJson(database.getAllNotes());
        String fileExtension = ".geojson";
        String mimeType = "application/geo+json";

        openShareIntent(geoJson.getBytes(), "geojson-export", fileExtension, mimeType);
    }

    public void shareAsGpx() {
        List<Note> notes = database.getAllNotes();
        String gpxString = Gpx.toGpx(notes);

        if ("".equals(gpxString)) {
            Toast.makeText(context, R.string.gpx_export_failed, Toast.LENGTH_SHORT).show();
        }

        String fileExtension = ".gpx";
        String mimeType = "application/gpx+xml";

        openShareIntent(gpxString.getBytes(), "gpx-export", fileExtension, mimeType);
    }

    public void shareAsBackup(SharedPreferences preferences) throws IOException {

        File externalFilesDir = context.getExternalFilesDir(FileHelper.GEONOTES_EXTERNAL_DIR_NAME);

        // Collect all data
        HashMap<String, Object> preferencesMap = new HashMap<>();

        String key = context.getString(R.string.pref_zoom_buttons);
        boolean prefZoomButtons = preferences.getBoolean(key, true);
        preferencesMap.put(key, prefZoomButtons);

        key = context.getString(R.string.pref_map_scaling);
        float prefMapScaling = preferences.getFloat(key, 1.0f);
        preferencesMap.put(key, prefMapScaling);

        key = context.getString(R.string.pref_snap_note_gps);
        boolean prefSnapNoteGps = preferences.getBoolean(key, false);
        preferencesMap.put(key, prefSnapNoteGps);

        key = context.getString(R.string.pref_enable_rotating_map);
        boolean prefEnableRotatingMap = preferences.getBoolean(key, false);
        preferencesMap.put(key, prefEnableRotatingMap);

        key = context.getString(R.string.pref_tap_duration);
        boolean prefLongTap = preferences.getBoolean(key, false);
        preferencesMap.put(key, prefLongTap);

        key = context.getString(R.string.pref_keep_camera_open);
        boolean prefKeepCameraOpen = preferences.getBoolean(key, false);
        preferencesMap.put(key, prefKeepCameraOpen);

        List<Note> allNotes = database.getAllNotes();
        Map<Long, List<String>> noteToPhotosMap = database.getAllPhotosMap();
        List<File> photoFiles = new ArrayList<>();
        noteToPhotosMap.values()
                .stream()
                .flatMap(List::stream)
                .forEach(filename -> {
                    photoFiles.add(new File(externalFilesDir, filename));

                    File thumbnailFile = ThumbnailUtil.getThumbnailFile(new File(filename));
                    photoFiles.add(new File(externalFilesDir, thumbnailFile.getName()));
                });

        // Create JSON file for the notes backup
        File notesBackupFile = getFile("notes-backup", ".json");
        String notesBackupJson = new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(new NoteBackupModel(allNotes, noteToPhotosMap, preferencesMap));
        Log.i("export", "Backup JSON:\n" + notesBackupJson);
        FileOutputStream notesBackupOutput = new FileOutputStream(notesBackupFile);
        notesBackupOutput.write(notesBackupJson.getBytes());
        notesBackupOutput.close();

        // Create ZIP file
        ArrayList<File> allFiles = new ArrayList<>();
        allFiles.add(notesBackupFile);
        allFiles.addAll(photoFiles);

        File backupFile = getFile("backup", ".zip");
        Zip.zip(allFiles, backupFile);

        // Share ZIP file
        String mimeType = "application/zip";
        openShareIntentForFile(backupFile, mimeType);
    }

    private void openShareIntent(byte[] data, String fileSuffix, String fileExtension, String mimeType) {
        File exportFile = getFile(fileExtension, fileSuffix);

        try {
            DataOutputStream output = new DataOutputStream(new FileOutputStream(exportFile));
            output.write(data);
            output.close();
        } catch (Exception e) {
            Log.e(LOGTAG, "Writing data to stream failed", e);
        }

        openShareIntentForFile(exportFile, mimeType);
    }

    private void openShareIntentForFile(File exportFile, String mimeType) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, FileHelper.getFileUri(context, exportFile));
        sendIntent.setType(mimeType);

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // needed because we're outside of an activity
        context.startActivity(shareIntent);
    }

    private @NonNull File getFile(String suffix, String fileExtension) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File storageDir = context.getExternalFilesDir("GeoNotes");
        return new File(storageDir, "geonotes-" + suffix + "_" + timeStamp + fileExtension);
    }
}
