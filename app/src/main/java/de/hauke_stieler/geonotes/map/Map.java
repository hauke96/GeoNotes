package de.hauke_stieler.geonotes.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.notes.NoteStore;

public class Map {
    private MapView map;
    private IMapController mapController;
    private MarkerWindow markerInfoWindow;
    private Marker.OnMarkerClickListener markerClickListener;

    private Marker markerToMove;

    private MyLocationNewOverlay locationOverlay;
    private CompassOverlay compassOverlay;
    private ScaleBarOverlay scaleBarOverlay;

    private PowerManager.WakeLock wakeLock;
    private Drawable normalIcon;
    private Drawable selectedIcon;

    private NoteStore noteStore;

    public Map(Context context, MapView map, PowerManager.WakeLock wakeLock, Drawable locationIcon, Drawable normalIcon, Drawable selectedIcon) {
        this.wakeLock = wakeLock;
        this.normalIcon = normalIcon;
        this.selectedIcon = selectedIcon;
        this.map = map;

        Configuration.getInstance().setUserAgentValue(context.getPackageName());

        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);

        // Initial location and zoom
        mapController = map.getController();
        mapController.setZoom(17.0);
        GeoPoint startPoint = new GeoPoint(53.563, 9.9866);
        mapController.setCenter(startPoint);

        createOverlays(context, map, (BitmapDrawable) locationIcon);
        createMarkerWindow(map);

        noteStore = new NoteStore(context);
        for (Note n : noteStore.getAllNotes()) {
            Marker marker = createMarker(n.description, new GeoPoint(n.lat, n.lon), markerClickListener);
            marker.setId("" + n.id);
        }
    }

    private void createOverlays(Context context, MapView map, BitmapDrawable locationIcon) {
        // Add location icon
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
        locationOverlay.enableMyLocation();
        locationOverlay.setPersonIcon(locationIcon.getBitmap());
        locationOverlay.setPersonHotspot(32, 32);
        map.getOverlays().add(this.locationOverlay);

        // Add compass
        compassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), map);
        compassOverlay.enableCompass();
        map.getOverlays().add(this.compassOverlay);

        // Add scale bar
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        scaleBarOverlay = new ScaleBarOverlay(map);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 20);
        map.getOverlays().add(this.scaleBarOverlay);

        // Add marker click listener. Will be called when the user clicks/taps on a marker.
        markerClickListener = (marker, mapView) -> {
            // When we are in the state of moving an existing marker, we do not want to interact with other markers -> simply return
            if (markerToMove != null) {
                return true;
            }


            // If a marker is currently selected -> deselect it
            if (markerInfoWindow.getSelectedMarker() != null) {
                setNormalIcon(markerInfoWindow.getSelectedMarker());
                // We don't need to deselect the marker or close the window as we will directly assign a new marker below
            }

            centerLocationWithOffset(marker.getPosition());
            selectMarker(marker);

            return true;
        };

        // React to touches on the map
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // When we have a marker to move, set its new position, store that and disable move-state
                if (markerToMove != null) {
                    markerToMove.setPosition(p);
                    selectMarker(markerToMove);

                    // If the ID is set, the marker exists in the DB, therefore we store that new location
                    String id = markerToMove.getId();
                    if (id != null) {
                        noteStore.updateLocation(Long.parseLong(id), p);
                    }

                    markerToMove = null;
                } else {
                    // Marker move state is not active -> normally select or create marker
                    if (markerInfoWindow.getSelectedMarker() != null) {
                        // Deselect selected marker:
                        setNormalIcon(markerInfoWindow.getSelectedMarker());
                        markerInfoWindow.close();
                    } else {
                        // No marker currently selected -> create new marker at this location
                        Marker marker = createMarker("", p, markerClickListener);
                        selectMarker(marker);
                    }
                }

                centerLocationWithOffset(p);

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mapEventsReceiver));
    }

    @SuppressLint("ClickableViewAccessibility")
    public void addMapListener(MapListener listener, TouchDownListener touchDownListener) {
        map.addMapListener(listener);
        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchDownListener.onTouchDown();
            }
            return false;
        });
    }

    private void createMarkerWindow(MapView map) {
        // General marker info window
        markerInfoWindow = new MarkerWindow(R.layout.maker_window, map, new MarkerWindow.MarkerEventHandler() {
            @Override
            public void onDelete(Marker marker) {
                // Task came from database and should therefore be removed.
                if (marker.getId() != null) {
                    noteStore.removeNote(Long.parseLong(marker.getId()));
                }
                map.getOverlays().remove(marker);
            }

            @Override
            public void onSave(Marker marker) {
                // Check whether marker already exists in the database (this is the case when the
                // marker has an ID attached) and update the DB entry. Otherwise, we'll create a new DB entry.
                if (marker.getId() != null) {
                    noteStore.updateDescription(Long.parseLong(marker.getId()), marker.getSnippet());
                } else {
                    long id = noteStore.addNote(marker.getSnippet(), marker.getPosition().getLatitude(), marker.getPosition().getLongitude());
                    marker.setId("" + id);
                }

                setNormalIcon(marker);
            }

            @Override
            public void onMove(Marker marker) {
                markerToMove = marker;
            }
        });
    }

    private void selectMarker(Marker marker) {
        Marker selectedMarker = markerInfoWindow.getSelectedMarker();
        if (selectedMarker != null) {
            // This icon will not be the selected marker after "showInfoWindow", therefore we set the normal icon here.
            setNormalIcon(selectedMarker);
        }

        setSelectedIcon(marker);
        marker.showInfoWindow();
        markerInfoWindow.focusEditField();
    }

    private void setSelectedIcon(Marker marker) {
        marker.setIcon(selectedIcon);
    }

    private void setNormalIcon(Marker marker) {
        marker.setIcon(normalIcon);
    }

    public void setZoomButtonVisibility(boolean visible) {
        map.getZoomController().setVisibility(visible ? CustomZoomButtonsController.Visibility.ALWAYS : CustomZoomButtonsController.Visibility.NEVER);
    }

    public void setMapScaleFactor(float factor) {
        map.setTilesScaleFactor(factor);
    }

    private void centerLocationWithOffset(GeoPoint p) {
        centerLocationWithOffset(p, map.getZoomLevelDouble());
    }

    private void centerLocationWithOffset(GeoPoint p, double zoom) {
        Point locationInPixels = new Point();
        map.getProjection().toPixels(p, locationInPixels);
        IGeoPoint newPoint = map.getProjection().fromPixels(locationInPixels.x, locationInPixels.y);

        mapController.setCenter(newPoint);
        mapController.setZoom(zoom);
    }

    private Marker createMarker(String description, GeoPoint p, Marker.OnMarkerClickListener markerClickListener) {
        Marker marker = new Marker(map);
        marker.setPosition(p);
        marker.setSnippet(description);
        marker.setInfoWindow(markerInfoWindow);
        marker.setOnMarkerClickListener(markerClickListener);

        setNormalIcon(marker);

        map.getOverlays().add(marker);

        return marker;
    }

    public void onResume() {
        map.onResume();
    }

    public void onPause() {
        map.onPause();
    }

    public void onDestroy() {
        wakeLock.release();
    }

    public void setLatitude(float lat) {
        double lon = map.getMapCenter().getLongitude();
        centerLocationWithOffset(new GeoPoint(lat, lon));
    }

    public void setLongitude(float lon) {
        double lat = map.getMapCenter().getLatitude();
        centerLocationWithOffset(new GeoPoint(lat, lon));
    }

    public IGeoPoint getLocation() {
        return map.getMapCenter();
    }

    public void setLocation(float lat, float lon, float zoom) {
        centerLocationWithOffset(new GeoPoint(lat, lon), zoom);
    }

    public float getZoom() {
        return (float) map.getZoomLevelDouble();
    }

    /**
     * Turns the follow mode on or off. If it's turned on, the map will follow the current location.
     */
    public void setLocationFollowMode(boolean followingLocationEnabled) {
        if (followingLocationEnabled) {
            this.locationOverlay.enableFollowLocation();
        } else {
            this.locationOverlay.disableFollowLocation();
        }
    }

    public boolean isFollowLocationEnabled() {
        return this.locationOverlay.isFollowLocationEnabled();
    }
}
