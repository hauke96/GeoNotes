package de.hauke_stieler.geonotes.map;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class GeoNotesMarker extends Marker {
    private long categoryId;

    public GeoNotesMarker(MapView mapView, String id, String description, GeoPoint position, long categoryId) {
        super(mapView);
        setId(id);
        setSnippet(description);
        setPosition(position);
        this.categoryId = categoryId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }
}
