package de.hauke_stieler.geonotes.export;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;

public class Exporter {
    private final Database database;
    private final Context context;

    public Exporter(Database database, Context context){
        this.database = database;
        this.context = context;
    }

    public void export() {
        String geoJson = GeoJson.toGeoJson(database.getAllNotes());

        try {
            File storageDir = context.getExternalFilesDir("GeoNotes");
            File exportFile = new File(storageDir, "geonotes-export.geojson");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(exportFile));
            outputStreamWriter.write(geoJson);
            outputStreamWriter.close();

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, FileHelper.getFileUri(context, exportFile));
            sendIntent.setType("application/json");

            Intent shareIntent = Intent.createChooser(sendIntent, null);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // needed because we're outside of an activity
            context.startActivity(shareIntent);
        } catch (IOException e) {
            Log.e("Export", "File write failed: " + e.toString());
        }
    }
}
