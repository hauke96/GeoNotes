package de.hauke_stieler.geonotes.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Locale;

import de.hauke_stieler.geonotes.notes.Note;

public class GeoJson {
    public static String toGeoJson(List<Note> notes) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(new NoteExportModel(notes));
    }
}
