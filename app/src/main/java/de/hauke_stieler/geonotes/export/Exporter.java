package de.hauke_stieler.geonotes.export;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hauke_stieler.geonotes.MainActivity;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class Exporter {
    private static final String LOGTAG = Exporter.class.getName();

    public static final String INTENT_OUTPUT_FILE_URI = "de.hauke_stieler.geonotes.export";

    public static final String GEOJSON_MIME_TYPE = "application/geo+json";
    public static final String GPX_MIME_TYPE = "application/gpx+xml";
    public static final String BACKUP_MIME_TYPE = "application/zip";

    private final Database database;
    private final Context context;

    public Exporter(Database database, Context context) {
        this.database = database;
        this.context = context;
    }

    public void shareAsGeoJson(Uri targetFile) {
        String geoJson = GeoJson.toGeoJson(database.getAllNotes());

        openShareIntent(geoJson.getBytes(), getGeojsonFilename(), targetFile);
    }

    public void shareAsGpx(Uri targetFile) {
        List<Note> notes = database.getAllNotes();
        String gpxString = Gpx.toGpx(notes);

        if ("".equals(gpxString)) {
            Toast.makeText(context, R.string.gpx_export_failed, Toast.LENGTH_SHORT).show();
        }

        openShareIntent(gpxString.getBytes(), getGpxFilename(), targetFile);
    }

    public void shareAsBackup(Uri targetFile, SharedPreferences preferences) throws IOException {

        File externalFilesDir = context.getExternalFilesDir(FileHelper.GEONOTES_EXTERNAL_DIR_NAME);

        // Collect all data
        HashMap<String, Object> preferencesMap = new HashMap<>();

        String key = context.getString(R.string.pref_map_scaling);
        float prefMapScaling = preferences.getFloat(key, Float.NaN);
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
        File notesBackupFile = getFile(getFilename("notes-backup", ".json"));
        String notesBackupJson = new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(new NoteBackupModel(allNotes, noteToPhotosMap, preferencesMap));
        Log.i("export", "Backup JSON:\n" + notesBackupJson);
        FileOutputStream notesBackupOutput = new FileOutputStream(notesBackupFile);
        notesBackupOutput.write(notesBackupJson.getBytes());
        notesBackupOutput.close();

        // Add GeoJson export for convenience in case someone wants to visit/edit data in a GIS tool.
        String geoJsonString = GeoJson.toGeoJson(allNotes);
        File geoJsonExportFile = getFile(getFilename("geojson-export", ".geojson"));
        try {
            DataOutputStream output = new DataOutputStream(new FileOutputStream(geoJsonExportFile));
            output.write(geoJsonString.getBytes());
            output.close();
        } catch (Exception e) {
            Log.e(LOGTAG, "Writing data to stream failed", e);
            geoJsonExportFile = null;
        }

        // Create ZIP file
        ArrayList<File> allFiles = new ArrayList<>();
        allFiles.add(notesBackupFile);
        allFiles.addAll(photoFiles);

        if (geoJsonExportFile != null) {
            allFiles.add(geoJsonExportFile);
        }

        File backupFile = getFile(getBackupFilename());
        Zip.zip(allFiles, backupFile);

        copyFile(Uri.fromFile(backupFile), targetFile);
    }

    private void openShareIntent(byte[] data, String filename, Uri targetFile) {
        File exportFile = getFile(filename);

        copyFile(Uri.fromFile(exportFile), targetFile);
        try {
            DataOutputStream output = new DataOutputStream(new FileOutputStream(exportFile));
            output.write(data);
            output.close();

            copyFile(Uri.fromFile(exportFile), targetFile);
        } catch (Exception e) {
            Log.e(LOGTAG, "Writing data to stream failed", e);
        }
    }

    private @NonNull File getFile(String filename) {
        File storageDir = context.getExternalFilesDir("GeoNotes");
        return new File(storageDir, filename);
    }

    public static String getGeojsonFilename() {
        return getFilename("geojson-export", ".geojson");
    }

    public static String getGpxFilename() {
        return getFilename("gpx-export", ".gpx");
    }

    public static String getBackupFilename() {
        return getFilename("backup", ".zip");
    }

    @NonNull
    private static String getFilename(String suffix, String fileExtension) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        return "geonotes-" + suffix + "_" + timeStamp + fileExtension;
    }

    private void copyFile(Uri fromFile, Uri toFile) {
        try {
            File tempFile = new File(fromFile.getPath());
            FileInputStream fileInputStream = new FileInputStream(tempFile);
            byte[] bytes = new byte[(int) tempFile.length()];
            fileInputStream.read(bytes);

            OutputStream output = context.getContentResolver().openOutputStream(toFile);
            output.write(bytes);
            output.flush();
            output.close();
        } catch (Exception e) {
            Log.e(MainActivity.class.getName(), "saveFile: ", e);
        }
    }
}
