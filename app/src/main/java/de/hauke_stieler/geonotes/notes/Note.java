package de.hauke_stieler.geonotes.notes;

public class Note {
    private final long id;
    private final String description;
    private final double lat;
    private final double lon;

    public Note(long id, String description, double lat, double lon) {
        this.id = id;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
