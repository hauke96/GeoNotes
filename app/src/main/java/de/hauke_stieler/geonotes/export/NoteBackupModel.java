package de.hauke_stieler.geonotes.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.hauke_stieler.geonotes.BuildConfig;
import de.hauke_stieler.geonotes.notes.Note;

public class NoteBackupModel {
    int geonotesVersion = BuildConfig.VERSION_CODE;
    List<CategoryModel> categories;
    List<NoteModel> notes;
    HashMap<String, Object> preferences;

    public NoteBackupModel(List<Note> notes, Map<Long, List<String>> noteToPhotos, HashMap<String, Object> preferencesMap) {
        this.categories = notes.stream()
                .map(Note::getCategory)
                .distinct()
                .map(c -> new CategoryModel(c.getId(), c.getName(), c.getColorString()))
                .collect(Collectors.toList());
        this.preferences = preferencesMap;

        this.notes = notes.stream()
                .map(note -> {
                    List<String> photoFileNames = noteToPhotos.get(note.getId());

                    if (photoFileNames == null) {
                        photoFileNames = new ArrayList<>();
                    }

                    return new NoteModel(
                            note.getId(),
                            note.getDescription(),
                            note.getCreationDateTimeString(),
                            note.getLon(),
                            note.getLat(),
                            note.getCategory().getId(),
                            photoFileNames);
                })
                .collect(Collectors.toList());
    }
}

class NoteModel {
    long id;
    String description;
    String createdAt;
    double lon;
    double lat;
    long categoryId;
    List<String> photosFileNames;

    NoteModel(long id, String description, String createdAt, double lon, double lat, long categoryId, List<String> photoFileNames) {
        this.id = id;
        this.description = description;
        this.createdAt = createdAt;
        this.lon = lon;
        this.lat = lat;
        this.categoryId = categoryId;
        this.photosFileNames = photoFileNames;
    }
}