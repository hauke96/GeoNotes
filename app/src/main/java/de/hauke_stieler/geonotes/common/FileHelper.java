package de.hauke_stieler.geonotes.common;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

public class FileHelper {
    public static Uri getFileUri(Context context, File lastPhotoFile) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider",
                lastPhotoFile);
    }
}
