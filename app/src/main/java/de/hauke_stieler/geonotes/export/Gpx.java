package de.hauke_stieler.geonotes.export;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hauke_stieler.geonotes.notes.Note;

public class Gpx {
    private static final String LOGTAG = Gpx.class.getName();
    private static final Locale exportLocale = Locale.US;

    static String toGpx(List<Note> notes) {
        StringBuilder output = new StringBuilder();
        output.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        output.append("<gpx version=\"1.1\">\n");

        try {
            for (Note note : notes) {
                output.append(String.format(exportLocale, "  <wpt lat=\"%f\" lon=\"%f\">\n", note.getLat(), note.getLon()));
                output.append(String.format("    <time>%s</time>\n", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", exportLocale).format(note.getCreationDateTime().getTime())));
                output.append(String.format(exportLocale, "    <name>%d</name>\n", note.getId()));
                output.append(String.format("    <desc>%s</desc>\n", note.getDescription()));
                output.append(String.format(exportLocale, "    <type>%d;%s;%s</type>\n", note.getCategory().getId(), note.getCategory().getColorString(), note.getCategory().getName()));
                output.append("  </wpt>\n");
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "GPX creation failed: ", e);
            return "";
        }

        output.append("</gpx>\n");

        return output.toString();
    }
}
