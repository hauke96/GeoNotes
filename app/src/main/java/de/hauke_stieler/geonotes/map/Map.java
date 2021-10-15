package de.hauke_stieler.geonotes.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import androidx.core.content.res.ResourcesCompat;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;

public class Map {
    private final Context context;
    private final PowerManager.WakeLock wakeLock;
    private final Database database;
    private SharedPreferences preferences;

    private final MapView map;
    private final IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private GpsMyLocationProvider gpsLocationProvider;

    private MarkerFragment markerFragment;
    private Marker.OnMarkerClickListener markerClickListener;

    private final Drawable normalIcon;
    private final Drawable normalWithPhotoIcon;
    private final Drawable selectedIcon;
    private final Drawable selectedWithPhotoIcon;

    private boolean snapNoteToGps;

    // Variables used during moving a marker. Do not use when no marker is currently in move mode (aka when markerToMove==null)
    private Marker markerToMove;
    private Point dragStartMarkerPosition;

    private SnappableRotationOverlay rotationGestureOverlay;
    private ClickableMapCompass compassOverlay;

    public Map(Context context,
               MapView map,
               Database database,
               SharedPreferences preferences) {
        this.context = context;
        this.map = map;
        this.database = database;
        this.preferences = preferences;

        markerFragment = Injector.get(MarkerFragment.class);
        addMarkerFragmentEventHandler(markerFragment);

        // Keep device on
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "geonotes:wakelock");
        wakeLock.acquire();

        Drawable locationIcon = ResourcesCompat.getDrawable(context.getResources(), R.mipmap.ic_location, null);
        Drawable arrowIcon = ResourcesCompat.getDrawable(context.getResources(), R.mipmap.ic_arrow, null);
        normalIcon = ResourcesCompat.getDrawable(context.getResources(), R.mipmap.ic_note, null);
        normalWithPhotoIcon = ResourcesCompat.getDrawable(context.getResources(), R.mipmap.ic_note_photo, null);
        selectedIcon = ResourcesCompat.getDrawable(context.getResources(), R.mipmap.ic_note_selected, null);
        selectedWithPhotoIcon = ResourcesCompat.getDrawable(context.getResources(), R.mipmap.ic_note_photo_selected, null);

        Configuration.getInstance().setUserAgentValue(context.getPackageName());

        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);

        // Initial location and zoom
        mapController = map.getController();
        mapController.setZoom(17.0);
        GeoPoint startPoint = new GeoPoint(53.563, 9.9866);
        mapController.setCenter(startPoint);

        createOverlays((BitmapDrawable) locationIcon, (BitmapDrawable) arrowIcon);

        for (Note n : this.database.getAllNotes()) {
            createMarker("" + n.getId(), n.getDescription(), new GeoPoint(n.getLat(), n.getLon()), markerClickListener);
        }
    }

    private void createOverlays(BitmapDrawable locationIcon, BitmapDrawable arrowIcon) {
        // Add location icon
        gpsLocationProvider = new GpsMyLocationProvider(context);
        locationOverlay = new MyLocationNewOverlay(gpsLocationProvider, map);
        locationOverlay.enableMyLocation();
        locationOverlay.setDirectionArrow(locationIcon.getBitmap(), arrowIcon.getBitmap());
        locationOverlay.setPersonHotspot(32, 32);
        map.getOverlays().add(this.locationOverlay);

        // Add rotation overlay
        rotationGestureOverlay = new SnappableRotationOverlay(map);
        rotationGestureOverlay.setRotationActionListener(this::saveMapRotationProperty);
        map.setMultiTouchControls(true);
        map.getOverlays().add(rotationGestureOverlay);

        // Add scale bar
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 20);
        map.getOverlays().add(scaleBarOverlay);

        // Add marker click listener. Will be called when the user clicks/taps on a marker.
        markerClickListener = (marker, mapView) -> {
            selectMarker(marker, false);
            return true;
        };

        // React to touches on the map
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // No marker to move here -> deselect or create marker
                // (selecting marker on the map is handles via the separate markerClickListener)
                if (markerFragment.getSelectedMarker() != null) {
                    // Deselect selected marker:
                    setNormalIcon(markerFragment.getSelectedMarker());
                }

                // Create new marker at this location and select it
                initAndSelectMarker(p);

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mapEventsReceiver));

        // Add compass after mapEventReceiver so that a click on the compass does not create a new note
        compassOverlay = new ClickableMapCompass(context, rotationGestureOverlay, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);
    }

    private void saveMapRotationProperty(float angle) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(context.getString(R.string.pref_map_rotation), angle);
        editor.commit();
    }

    public void updateMapRotation(boolean rotatingMapEnabled, float angle) {
        rotationGestureOverlay.setEnabledAndRotation(rotatingMapEnabled, angle);
        compassOverlay.setPointerMode(rotatingMapEnabled);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void addMapListener(MapListener listener, TouchDownListener touchDownListener) {
        map.addMapListener(listener);
        map.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDownListener.onTouchDown();

                    // Initialize movement of the marker: Store current screen-location to keep marker there
                    if (markerToMove != null) {
                        dragStartMarkerPosition = map.getProjection().toPixels(markerToMove.getPosition(), null);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    // When in drag-mode: Keep marker at original screen location by setting its position
                    if (markerToMove != null && dragStartMarkerPosition != null) {
                        markerToMove.setPosition((GeoPoint) map.getProjection().fromPixels(dragStartMarkerPosition.x, dragStartMarkerPosition.y));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (markerToMove != null) {
                        selectMarker(markerToMove, false);

                        // If the ID is set, the marker exists in the DB, therefore we store that new location
                        String id = markerToMove.getId();
                        if (id != null) {
                            database.updateLocation(Long.parseLong(id), markerToMove.getPosition());
                        }

                        dragStartMarkerPosition = null;
                        markerToMove = null;
                    }
                    break;
            }
            return false;
        });
    }

    private void addMarkerFragmentEventHandler(MarkerFragment fragment) {
        fragment.addEventHandler(new MarkerFragment.MarkerFragmentEventHandler() {
            @Override
            public void onDelete(Marker marker) {
                // We always have an ID and can therefore delete the note
                database.removeNote(Long.parseLong(marker.getId()));
                database.removePhotos(Long.parseLong(marker.getId()), context.getExternalFilesDir("GeoNotes"));
                map.getOverlays().remove(marker);
                redraw();
            }

            @Override
            public void onSave(Marker marker) {
                // We always have an ID and can therefore update the note
                database.updateDescription(Long.parseLong(marker.getId()), marker.getSnippet());
                setNormalIcon(marker);
                redraw();
            }

            @Override
            public void onMove(Marker marker) {
                markerToMove = marker;
                redraw();
            }
        });
    }

    // This forces a re-draw of the map. Otherwise changes will only be visible when moving the map after e.g. the selected marker changed.
    private void redraw() {
        map.postInvalidate();
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
        selectMarker(newMarker, true);
    }

    /**
     * Tries to snap the location to the last known GPS of the distance on the screen is below 50dp.
     * If no GPS location available or if the distance to the current GPS location is lower than 50dp, then the GPS location is returned, otherwise the input is returned.
     *
     * @return The new location, snapped if possible.
     */
    private GeoPoint snapToGpsLocation(GeoPoint location) {
        if (gpsLocationProvider.getLastKnownLocation() == null) {
            return location;
        }

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

    public void selectNote(long noteId) {
        String noteIdString = "" + noteId;
        for (Overlay marker : map.getOverlays()) {
            if (marker instanceof Marker && ((Marker) marker).getId().equals(noteIdString)) {
                this.selectMarker((Marker) marker, false);
            }
        }
    }

    /**
     * @param marker                  The marker to select.
     * @param transferEditTextContent When set to true: If the user typed any text into the input
     *                                field without a selected note and *then* tapped on the map
     *                                to create or select one, this prior entered text schould be
     *                                used as the content of the note.
     *                                When set to false: The text of the tapped note will be read
     *                                and shown in the edit field.
     */
    private void selectMarker(Marker marker, boolean transferEditTextContent) {
        // Deselect previously selected marker
        Marker selectedMarker = markerFragment.getSelectedMarker();
        if (selectedMarker != null) {
            // This icon will not be the selected marker after "showInfoWindow", therefore we set the normal icon here.
            setNormalIcon(selectedMarker);
            markerFragment.reset();
        }

        setSelectedIcon(marker);
        markerFragment.selectMarker(marker, transferEditTextContent);

        addImagesToMarkerWindow();
        redraw();
    }

    private Marker getSelectedMarker() {
        return markerFragment.getSelectedMarker();
    }

    /**
     * Loads images of current marker (which contains the note-ID) from database and show them.
     */
    public void addImagesToMarkerWindow() {
        markerFragment.resetImageList();
        Marker marker = markerFragment.getSelectedMarker();

        // It could happen that the user rotates the device (e.g. while taking a photo) and this
        // causes the whole activity to be reset. Therefore we might not have a marker here.
        if (marker == null) {
            return;
        }

        List<String> photoFileNames = database.getPhotos(marker.getId());
        for (String photoFileName : photoFileNames) {
            File storageDir = context.getExternalFilesDir("GeoNotes");
            File image = new File(storageDir, photoFileName);
            markerFragment.addPhoto(image);
        }

        setSelectedIcon(marker);
    }

    private void setSelectedIcon(Marker marker) {
        if (database.hasPhotos(marker.getId())) {
            marker.setIcon(selectedWithPhotoIcon);
        } else {
            marker.setIcon(selectedIcon);
        }
    }

    private void setNormalIcon(Marker marker) {
        if (database.hasPhotos(marker.getId())) {
            marker.setIcon(normalWithPhotoIcon);
        } else {
            marker.setIcon(normalIcon);
        }
    }

    public void setZoomButtonVisibility(boolean visible) {
        map.getZoomController().setVisibility(visible ? CustomZoomButtonsController.Visibility.ALWAYS : CustomZoomButtonsController.Visibility.NEVER);
    }

    public void setMapScaleFactor(float factor) {
        map.setTilesScaleFactor(factor);
    }

    private void zoomToLocation(IGeoPoint p, double zoom) {
        mapController.setCenter(new GeoPoint(p));
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
        marker.setOnMarkerClickListener(markerClickListener);

        setNormalIcon(marker);

        map.getOverlays().add(marker);

        return marker;
    }

    public void onResume() {
        map.onResume();
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        // Before resuming (e.g. when switching back from the list of notes to the main activity),
        // the map doesn't zoom to markers. Therefore we here zoom to the currently selected marker.
        Marker selectedMarker = getSelectedMarker();
        if (selectedMarker != null) {
            zoomToLocation(selectedMarker.getPosition(), map.getZoomLevelDouble());
        }
    }

    public void onPause() {
        map.onPause();
    }

    public void onDestroy() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public IGeoPoint getLocation() {
        return map.getMapCenter();
    }

    public void setLocation(float lat, float lon, float zoom) {
        zoomToLocation(new GeoPoint(lat, lon), zoom);
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

    public void addRequestPhotoHandler(MarkerFragment.RequestPhotoEventHandler requestPhotoEventHandler) {
        this.markerFragment.addRequestPhotoHandler(requestPhotoEventHandler);
    }

    public void setSnapNoteToGps(boolean snapNoteToGps) {
        this.snapNoteToGps = snapNoteToGps;
    }
}
