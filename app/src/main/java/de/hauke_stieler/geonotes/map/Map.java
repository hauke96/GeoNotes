package de.hauke_stieler.geonotes.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
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

import java.io.File;
import java.util.List;

import de.hauke_stieler.geonotes.Database.Database;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class Map {
    private final Context context;
    private MapView map;
    private IMapController mapController;
    private MarkerWindow markerInfoWindow;
    private Marker.OnMarkerClickListener markerClickListener;

    private Marker markerToMove;

    private MyLocationNewOverlay locationOverlay;
    private GpsMyLocationProvider gpsLocationProvider;
    private CompassOverlay compassOverlay;
    private ScaleBarOverlay scaleBarOverlay;

    private PowerManager.WakeLock wakeLock;
    private Drawable normalIcon;
    private Drawable selectedIcon;

    private Database database;

    private boolean snapNoteToGps;

    public Map(Context context,
               MapView map,
               PowerManager.WakeLock wakeLock,
               Database database,
               Drawable locationIcon,
               Drawable arrowIcon,
               Drawable normalIcon,
               Drawable selectedIcon) {
        this.context = context;
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

        createOverlays(context, map, (BitmapDrawable) locationIcon, (BitmapDrawable) arrowIcon);
        createMarkerWindow(map);

        this.database = database;
        for (Note n : this.database.getAllNotes()) {
            Marker marker = createMarker("" + n.id, n.description, new GeoPoint(n.lat, n.lon), markerClickListener);
        }
    }

    private void createOverlays(Context context, MapView map, BitmapDrawable locationIcon, BitmapDrawable arrowIcon) {
        // Add location icon
        gpsLocationProvider = new GpsMyLocationProvider(context);
        locationOverlay = new MyLocationNewOverlay(gpsLocationProvider, map);
        locationOverlay.enableMyLocation();
        locationOverlay.setDirectionArrow(locationIcon.getBitmap(), arrowIcon.getBitmap());
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
                        database.updateLocation(Long.parseLong(id), p);
                    }

                    markerToMove = null;
                } else {
                    // No marker to move here -> deselect or create marker
                    // (selecting marker on the map is handles via the separate markerClickListener)
                    if (markerInfoWindow.getSelectedMarker() != null) {
                        // Deselect selected marker:
                        setNormalIcon(markerInfoWindow.getSelectedMarker());
                        markerInfoWindow.close();
                    } else {
                        // No marker currently selected -> create new marker at this location
                        initAndSelectMarker(p);
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
                // We always have an ID and can therefore delete the note
                database.removeNote(Long.parseLong(marker.getId()));
                database.removePhotos(Long.parseLong(marker.getId()), context.getExternalFilesDir("GeoNotes"));
                map.getOverlays().remove(marker);
            }

            @Override
            public void onSave(Marker marker) {
                // We always have an ID and can therefore update the note
                database.updateDescription(Long.parseLong(marker.getId()), marker.getSnippet());
                setNormalIcon(marker);
            }

            @Override
            public void onMove(Marker marker) {
                markerToMove = marker;
                // The new position is determined and stored in the click handler of the map
            }
        });
    }

    /**
     * Creates a new note in the database, creates a corresponding marker (s. createMarker()) and also selects this new marker.
     */
    private void initAndSelectMarker(GeoPoint location) {
        long id = database.addNote("", location.getLatitude(), location.getLongitude());

        if (snapNoteToGps) {
            location = snapToGpsLocation(location);
        }

        Marker newMarker = createMarker("" + id, "", location, markerClickListener);
        selectMarker(newMarker);
    }

    /**
     * Tries to snap the location to the last known GPS of the distance on the screen is below 50dp.
     *
     * @return If distance <50dp then GPS location is returned, if not, the input is returned.
     */
    private GeoPoint snapToGpsLocation(GeoPoint location) {
        GeoPoint gpsLocation = new GeoPoint(gpsLocationProvider.getLastKnownLocation());

        Point markerLocationOnScreen = map.getProjection().toPixels(location, null);
        Point gpsLocationOnScreen = map.getProjection().toPixels(gpsLocation, null);

        int diffY = gpsLocationOnScreen.y - markerLocationOnScreen.y;
        int diffX = gpsLocationOnScreen.x - markerLocationOnScreen.x;
        double distanceOnScreen = Math.sqrt(diffY * diffY + diffX * diffX);

        if (distanceOnScreen < 50) {
            location = gpsLocation;
        }

        return location;
    }

    private void selectMarker(Marker marker) {
        // Reset icon of previous selection
        Marker selectedMarker = markerInfoWindow.getSelectedMarker();
        if (selectedMarker != null) {
            // This icon will not be the selected marker after "showInfoWindow", therefore we set the normal icon here.
            setNormalIcon(selectedMarker);
        }

        setSelectedIcon(marker);
        marker.showInfoWindow();
        markerInfoWindow.focusEditField();

        addImagesToMarkerWindow();
    }

    /**
     * Loads images of current marker (which contains the note-ID) from database and show them.
     */
    public void addImagesToMarkerWindow() {
        markerInfoWindow.resetImageList();
        Marker marker = markerInfoWindow.getSelectedMarker();

        // It could happen that the user rotates the device (e.g. while taking a photo) and this
        // causes the whole activity to be reset. Therefore we might not have a marker here.
        if (marker == null) {
            return;
        }

        List<String> photoFileNames = database.getPhotos(marker.getId());
        for (String photoFileName : photoFileNames) {
            File storageDir = context.getExternalFilesDir("GeoNotes");
            File image = new File(storageDir, photoFileName);
            markerInfoWindow.addPhoto(image);
        }
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

    /**
     * Just creates a new marker and adds it to the map overlay. No database operations or selection is performed.
     */
    private Marker createMarker(String id, String description, GeoPoint p, Marker.OnMarkerClickListener markerClickListener) {
        Marker marker = new Marker(map);
        marker.setId(id);
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

    public void addRequestPhotoHandler(MarkerWindow.RequestPhotoEventHandler requestPhotoEventHandler) {
        this.markerInfoWindow.addRequestPhotoHandler(requestPhotoEventHandler);
    }

    public void setSnapNoteToGps(boolean snapNoteToGps) {
        this.snapNoteToGps = snapNoteToGps;
    }
}
