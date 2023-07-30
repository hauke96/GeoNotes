package de.hauke_stieler.geonotes.export;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.List;

import de.hauke_stieler.geonotes.notes.Note;
import me.himanshusoni.gpxparser.GPXWriter;
import me.himanshusoni.gpxparser.modal.GPX;
import me.himanshusoni.gpxparser.modal.Waypoint;

public class Gpx {
    private static final String LOGTAG = Gpx.class.getName();

    static String toGpx(List<Note> notes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GPX gpx = new GPX();

        try {
            for (Note note : notes) {
                Waypoint waypoint = new Waypoint(note.getLat(), note.getLon());
                waypoint.setName(note.getId() + "");
                waypoint.setTime(note.getCreationDateTime().getTime());
                waypoint.setDescription(note.getDescription());
                waypoint.setType(note.getCategory().getId() + ";" + note.getCategory().getColorString() + ";" + note.getCategory().getName());
                gpx.addWaypoint(waypoint);
            }

            // TODO Use normal GPXWriter from library, when indentation is supported
            GPXWriter writer = new CustomGpxWriter();
            writer.writeGPX(gpx, outputStream);
        } catch (Exception e) {
            Log.e(LOGTAG, "GPX creation failed: " + e.toString());
            return "";
        }

        return new String(outputStream.toByteArray());
    }
}
