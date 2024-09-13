package de.hauke_stieler.geonotes.common;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;

public class FileHelper {
    public static final String GEONOTES_EXTERNAL_DIR_NAME = "GeoNotes";

    public static Uri getFileUri(Context context, File lastPhotoFile) {
        if (Build.VERSION.SDK_INT < 24) {
            return Uri.fromFile(lastPhotoFile);
        }

        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider",
                lastPhotoFile);
    }
}
