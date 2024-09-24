package de.hauke_stieler.geonotes.common;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;

public class ExifHelper {

    private static @NonNull ExifInterface getExif(ContentResolver contentResolver, File photoFile) throws IOException {
        return new ExifInterface(contentResolver.openFileDescriptor(Uri.fromFile(photoFile), "rw").getFileDescriptor());
    }

    public static void fillExifAttributesWithGps(ContentResolver contentResolver, File image, Double longitude, Double latitude) throws IOException {
        ExifInterface exif = getExif(contentResolver, image);

        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, getGpsExifStringForOrdinate(latitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, getLatitudeRef(latitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, getGpsExifStringForOrdinate(longitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, getLongitudeRef(longitude));

        exif.saveAttributes();
    }

    private static String getLatitudeRef(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    private static String getLongitudeRef(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }

    private static String getGpsExifStringForOrdinate(double value) {
        value = Math.abs(value);
        int degree = (int) value;
        value *= 60;
        value -= (degree * 60.0d);
        int minute = (int) value;
        value *= 60;
        value -= (minute * 60.0d);
        int second = (int) (value * 1000.0d);

        StringBuilder sb = new StringBuilder(20);
        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000");
        return sb.toString();
    }

    @SuppressLint("RestrictedApi")
    public static void setRotationTag(ContentResolver contentResolver, File image, int targetRotation) throws IOException {
        ExifInterface exif = getExif(contentResolver, image);

        exif.setAttribute(ExifInterface.TAG_ORIENTATION, targetRotation + "");

        exif.saveAttributes();
    }

    public static int getRotationTag(ContentResolver contentResolver, File image) throws IOException {
        ExifInterface exif = getExif(contentResolver, image);
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
    }

    public static int getRotationDegree(ContentResolver contentResolver, File image) throws IOException {
        int exifOrientation = getRotationTag(contentResolver, image);

        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
}
