package de.hauke_stieler.geonotes.common;

import androidx.exifinterface.media.ExifInterface;

public class ExifHelper {

    public static void fillExifAttributesWithGps(ExifInterface exif, Double longitude, Double latitude) {
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, getGpsExifStringForOrdinate(latitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, getLatitudeRef(latitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, getGpsExifStringForOrdinate(longitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, getLongitudeRef(longitude));
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
}
