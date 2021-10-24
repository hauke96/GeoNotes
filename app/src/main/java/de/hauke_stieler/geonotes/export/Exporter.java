package de.hauke_stieler.geonotes.export;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;

public class Exporter {
    private static String LOGTAG = Exporter.class.getName();

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

        openShareIntent(geoJson, fileExtension, mimeType);
    }

    public void shareAsGpx() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<Note> notes = database.getAllNotes();

        try {
            GPX.Builder builder = GPX.builder();

            for (Note note : notes) {
                WayPoint waypoint = WayPoint.builder()
                        .lat(note.getLat())
                        .lon(note.getLon())
                        .name(note.getId() + "")
                        .desc(note.getDescription())
                        .build();
                builder.addWayPoint(waypoint);
            }

            GPX.write(builder.build(), outputStream);
        } catch (Exception e) {
            Log.e(LOGTAG, "GPX creation failed: " + e.toString());
        }

        String fileExtension = ".gpx";
        String mimeType = "application/gpx+xml";

        openShareIntent(new String(outputStream.toByteArray()), fileExtension, mimeType);
    }

    private void openShareIntent(String data, String fileExtension, String mimeType) {
        try {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

            File storageDir = context.getExternalFilesDir("GeoNotes");
            File exportFile = new File(storageDir, "geonotes-export_" + timeStamp + fileExtension);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(exportFile));
            outputStreamWriter.write(data);
            outputStreamWriter.close();

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileHelper.getFileUri(context, exportFile));
            sendIntent.setType(mimeType);

            Intent shareIntent = Intent.createChooser(sendIntent, null);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // needed because we're outside of an activity
            context.startActivity(shareIntent);
        } catch (Exception e) {
            Log.e(LOGTAG, "File write failed: " + e.toString());
        }
    }
}
