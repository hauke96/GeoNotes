package de.hauke_stieler.geonotes.export;

import java.util.List;
import java.util.Locale;

import de.hauke_stieler.geonotes.notes.Note;

public class GeoJson {
    private static final String GEOJSON_HEAD = "{\"type\": \"FeatureCollection\",\"features\": [";
    private static final String GEOJSON_FEATURE = "{\"type\": \"Feature\", \"properties\": { \"geonotes:id\": \"%d\", \"geonotes:note\": \"%s\" }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %f, %f ] } }";
    private static final String GEOJSON_FOOT = "] }";

    public static String toGeoJson(List<Note> notes) {
        StringBuilder result = new StringBuilder();
        result.append(GEOJSON_HEAD);

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            result.append(String.format(Locale.US, GEOJSON_FEATURE, note.id, note.description, note.lon, note.lat));

            boolean isLastElement = i == notes.size() - 1;
            if (!isLastElement) {
                result.append(", ");
            }
        }

        result.append(GEOJSON_FOOT);
        return result.toString();
    }
}
