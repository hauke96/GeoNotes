package de.hauke_stieler.geonotes.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ThumbnailUtil {
    public static void writeThumbnail(int sizeInPixel, File photoFile) throws IOException {
        // Get thumbnail that can be shown on image button
        Bitmap bmp = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bmp, sizeInPixel, sizeInPixel);

        // Get according file
        File thumbnailFile = getThumbnailFile(photoFile);

        // Write thumbnail
        FileOutputStream thumbStream = new FileOutputStream(thumbnailFile);
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, thumbStream);
        thumbStream.close();
    }

    public static Bitmap loadThumbnail(File photoFile) {
        String thumbnailPath = ThumbnailUtil.getThumbnailFile(photoFile).getAbsolutePath();
        return BitmapFactory.decodeFile(thumbnailPath);
    }

    public static void deleteThumbnail(File photoFile) {
        getThumbnailFile(photoFile).delete();
    }

    /**
     * Converts the file to a big photo into the file to a thumbnail. That file does not necessarily exist.
     */
    public static File getThumbnailFile(File photoFile) {
        String photoPathWithoutExtension = photoFile.getAbsolutePath().substring(0, photoFile.getAbsolutePath().length() - 4);
        return new File(photoPathWithoutExtension + "_thumb.jpg");
    }
}
