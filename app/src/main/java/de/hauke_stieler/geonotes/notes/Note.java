package de.hauke_stieler.geonotes.notes;

public class Note {
    public final long id;
    public final String description;
    public final double lat;
    public final double lon;

    public Note(long id, String description, double lat, double lon) {
        this.id = id;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
    }
}
