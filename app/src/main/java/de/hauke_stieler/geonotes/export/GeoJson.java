package de.hauke_stieler.geonotes.export;

import java.util.List;
import java.util.Locale;

import de.hauke_stieler.geonotes.notes.Note;

public class GeoJson {
    private static final String GEOJSON_HEAD = "{\n" +
            "\t\"type\": \"FeatureCollection\",\n" +
            "\t\"features\": [";
    private static final String GEOJSON_FEATURE = "\n\t\t{\n" +
            "\t\t\t\"type\": \"Feature\",\n" +
            "\t\t\t\"properties\": {\n" +
            "\t\t\t\t\"geonotes:id\": \"%d\",\n" +
            "\t\t\t\t\"geonotes:note\": \"%s\",\n" +
            "\t\t\t\t\"geonotes:created_at\": \"%s\"\n" +
            "\t\t\t},\n" +
            "\t\t\t\"geometry\": {\n" +
            "\t\t\t\t\"type\": \"Point\",\n" +
            "\t\t\t\t\"coordinates\": [\n" +
            "\t\t\t\t\t%f,\n" +
            "\t\t\t\t\t%f\n" +
            "\t\t\t\t]\n" +
            "\t\t\t}\n" +
            "\t\t}";
    private static final String GEOJSON_FOOT = "]\n}";

    public static String toGeoJson(List<Note> notes) {
        StringBuilder result = new StringBuilder();
        result.append(GEOJSON_HEAD);

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            String featureString = String.format(Locale.US,
                    GEOJSON_FEATURE,
                    note.getId(),
                    note.getDescription().replace("\"", "'"),
                    note.getCreationDateTimeString(),
                    note.getLon(),
                    note.getLat());
            result.append(featureString);

            boolean isLastElement = i == notes.size() - 1;
            if (!isLastElement) {
                result.append(",");
            } else {
                result.append("\n\t");
            }
        }

        result.append(GEOJSON_FOOT);
        return result.toString();
    }
}
