package de.hauke_stieler.geonotes.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.PowerManager;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.gestures.MoveGestureDetector;
import org.maplibre.android.gestures.RotateGestureDetector;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.LocationComponentOptions;
import org.maplibre.android.location.engine.LocationEngineRequest;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapLibreMapOptions;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.plugins.annotation.Symbol;
import org.maplibre.android.plugins.annotation.SymbolManager;
import org.maplibre.android.plugins.annotation.SymbolOptions;
import org.maplibre.android.plugins.scalebar.ScaleBarOptions;
import org.maplibre.android.plugins.scalebar.ScaleBarPlugin;
import org.maplibre.android.utils.BitmapUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;

public class Map {

    public interface TouchDownListener {
        void onTouchedDown();
    }

    public interface NoteMovedListener {
        void onNoteMoveStarted(Long categoryId, boolean isPhotoNote);

        void onNoteMoveEnded(Long noteId, Double longitude, Double latitude);
    }

    private final Context context;
    private final PowerManager.WakeLock wakeLock;
    private final Database database;
    private final SharedPreferences preferences;
    private final NoteIconProvider noteIconProvider;
    private SymbolManager symbolManager;

    private MapLibreMap mlMap;
    private boolean mapFullyInitialized = false; // True when all listeners, preferences, notes, etc. are loaded and registered.

    private final SymbolFragment symbolFragment;

    private final int snapToGpsPixelTolerance = 50;

    // Variables used during moving a symbol. Do not use when no symbol is currently in move mode (aka when markerToMove==null)
    private Symbol symbolToMove;
    private PointF dragStartMarkerPosition;

    private TouchDownListener touchDownListener;
    private NoteMovedListener noteMovedCallback;

    public Map(Context context,
               Database database,
               SharedPreferences preferences,
               NoteIconProvider noteIconProvider) {
        this.context = context;
        this.database = database;
        this.preferences = preferences;
        this.noteIconProvider = noteIconProvider;

        symbolFragment = Injector.get(SymbolFragment.class);
        addSymbolFragmentEventHandler(symbolFragment);

        // Keep device on
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, "geonotes:wakelock");
        wakeLock.acquire();
    }

    /**
     * Initializes the MapView by setting style and registering handlers.
     */
    public void initMapViewAnyRegisterHandlers(MapView mapView) {
        mapView.getMapAsync(mlMap -> {
            mlMap.setStyle("asset://osm-map-style.json", style -> {
                // Don't assign this earlier, because some other methods require a loaded style.
                this.mlMap = mlMap;

                this.symbolManager = new SymbolManager(mapView, mlMap, style);
                this.symbolManager.setIconAllowOverlap(true);

                mlMap.addOnMapLongClickListener(coordinate -> {
                    boolean useLongTap = this.preferences.getBoolean(this.context.getString(R.string.pref_tap_duration), false);
                    if (!useLongTap) {
                        return false;
                    }

                    boolean isInNoteMovingMode = symbolToMove != null;
                    if (isInNoteMovingMode) {
                        endNoteMovingMode();
                        return true;
                    }

                    boolean snapToGpsPosition = this.preferences.getBoolean(this.context.getString(R.string.pref_snap_note_gps), false);
                    Location lastKnownLocation = mlMap.getLocationComponent().getLastKnownLocation();
                    if (snapToGpsPosition && lastKnownLocation != null) {
                        LatLng lastKnownCoordinate = new LatLng(lastKnownLocation);
                        // Handle snapping manually here, since the LocationComponent has no tolerance option
                        coordinate = snapToGpsLocation(coordinate, lastKnownCoordinate);
                    }
                    createMarker(coordinate);
                    return true;
                });

                mlMap.addOnMapClickListener(coordinate -> {
                    boolean useNormalTap = !this.preferences.getBoolean(this.context.getString(R.string.pref_tap_duration), false);
                    if (!useNormalTap) {
                        return false;
                    }

                    boolean isInNoteMovingMode = symbolToMove != null;
                    if (isInNoteMovingMode) {
                        endNoteMovingMode();
                        return true;
                    }

                    boolean snapToGpsPosition = this.preferences.getBoolean(this.context.getString(R.string.pref_snap_note_gps), false);
                    Location lastKnownLocation = mlMap.getLocationComponent().getLastKnownLocation();
                    if (snapToGpsPosition && lastKnownLocation != null) {
                        LatLng lastKnownCoordinate = new LatLng(lastKnownLocation);
                        // Handle snapping manually here, since the LocationComponent has no tolerance option
                        coordinate = snapToGpsLocation(coordinate, lastKnownCoordinate);
                    }
                    createMarker(coordinate);
                    return true;
                });

                this.symbolManager.addClickListener(clickedSymbol -> {
                    selectMarker(clickedSymbol, false);
                    return true;
                });

                enableLocationsComponent();

                loadPreferences();

                reloadAllNotes();

                mapFullyInitialized = true;
            });

            mlMap.addOnRotateListener(new MapLibreMap.OnRotateListener() {
                @Override
                public void onRotateBegin(@NonNull RotateGestureDetector rotateGestureDetector) {
                }

                @Override
                public void onRotate(@NonNull RotateGestureDetector rotateGestureDetector) {
                    saveMapProperties(mlMap);
                }

                @Override
                public void onRotateEnd(@NonNull RotateGestureDetector rotateGestureDetector) {
                }
            });

            mlMap.addOnMoveListener(new MapLibreMap.OnMoveListener() {
                // We use the general move listener instead of a camera listener, since the camera
                // listener (as of writing) fires the event only if the camera actually moves. This
                // means that the initial event is fired _after_ the map has moves. Storing the
                // screen location of a note that should be moved at that moment in time yields an
                // offset to the original location of that note, since the map has already moved.
                // Therefore, we listen to the general move event, that is fired as soon as the map
                // moves and not just afterwards. Getting the screen location of a note then yields
                // the correct location without any offsets.
                @Override
                public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {
                    if (symbolToMove != null) {
                        // First movement of the map in the "move note" mode. Save the screen
                        // location of the nove to later determine the new location.
                        dragStartMarkerPosition = mlMap.getProjection().toScreenLocation(symbolToMove.getLatLng());
                    }
                }

                @Override
                public void onMove(@NonNull MoveGestureDetector moveGestureDetector) {
                    // Do not move the symbolToMove so that it stays where it was. It's then visible
                    // as a "ghost" so that one can see its old location.
                }

                @Override
                public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {
                    if (symbolToMove != null) {
                        // Visually re-add the previously removed icon.
                        symbolToMove.setIconOpacity(1f);
                        symbolToMove.setLatLng(mlMap.getProjection().fromScreenLocation(dragStartMarkerPosition));
                        symbolManager.update(symbolToMove);

                        // If the ID is set, the symbol exists in the DB, therefore we store that new location
                        Long id = GeoNotesSymbol.getNoteId(symbolToMove);
                        double latitude = symbolToMove.getLatLng().getLatitude();
                        double longitude = symbolToMove.getLatLng().getLongitude();
                        database.updateNoteLocation(id, latitude, longitude);

                        endNoteMovingMode();

                        if (noteMovedCallback != null) {
                            noteMovedCallback.onNoteMoveEnded(id, longitude, latitude);
                        }
                    }

                    // Resetting the map rotation with the compass-icon also triggers this event and we
                    // want to store the new rotation
                    saveMapProperties(mlMap);
                }
            });
            mlMap.addOnCameraMoveStartedListener(reason -> {
                boolean userMovedMap = reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE;
                if (userMovedMap) {
                    if (touchDownListener != null) {
                        touchDownListener.onTouchedDown();
                    }
                }
            });

            mlMap.getUiSettings().setDisableRotateWhenScaling(true);

            ScaleBarPlugin scaleBarPlugin = new ScaleBarPlugin(mapView, mlMap);
            ScaleBarOptions scaleBarOptions = new ScaleBarOptions(this.context)
                    .setTextSize(32f)
                    .setBarHeight(5f)
                    .setBorderWidth(2f)
                    .setTextBarMargin(15f)
                    .setShowTextBorder(true)
                    .setTextBorderWidth(8f);
            scaleBarPlugin.create(scaleBarOptions);
        });
    }

    private void endNoteMovingMode() {
        selectMarker(symbolToMove, false);
        dragStartMarkerPosition = null;
        symbolToMove = null;
    }

    public void loadPreferences() {
        boolean enableRotatingMap = preferences.getBoolean(context.getString(R.string.pref_enable_rotating_map), false);
        float mapRotation = preferences.getFloat(context.getString(R.string.pref_map_rotation), 0f);
        updateMapRotation(enableRotatingMap, mapRotation);

        float lat = preferences.getFloat(context.getString(R.string.pref_last_location_lat), 0f);
        float lon = preferences.getFloat(context.getString(R.string.pref_last_location_lon), 0f);
        float zoom = preferences.getFloat(context.getString(R.string.pref_last_location_zoom), 2);

        setLocation(lat, lon, zoom);
    }

    public void reloadAllNotes() {
        symbolFragment.saveAndReset();
        symbolManager.deleteAll();

        this.noteIconProvider
                .getIconNameToDrawableMap()
                .forEach((name, drawable) -> mlMap.getStyle().addImage(name, BitmapUtils.getBitmapFromDrawable(drawable)));

        List<Note> allNotes = this.database.getAllNotes();
        for (Note n : allNotes) {
            createMarker(n);
        }
    }

    /**
     * Activates the location component when the user gave the location permissions.
     */
    public void enableLocationsComponent() {
        if (mlMap != null && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationComponent locationComponent = mlMap.getLocationComponent();
            Style style = mlMap.getStyle();

            if (!locationComponent.isLocationComponentActivated() && style != null) {
                LocationComponentOptions locationComponentOptions = LocationComponentOptions.builder(context)
                        .pulseEnabled(true)
                        .backgroundTintColor(Color.parseColor("#ffffff"))
                        .foregroundTintColor(Color.parseColor("#66bb6a"))
                        .bearingTintColor(Color.parseColor("#66bb6a"))
                        .build();
                LocationEngineRequest locationEngineRequest = (new LocationEngineRequest.Builder(1000))
                        .setFastestInterval(1000)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .build();
                LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions.builder(context, style)
                        .locationComponentOptions(locationComponentOptions)
                        .locationEngineRequest(locationEngineRequest)
                        .build();

                locationComponent.activateLocationComponent(locationComponentActivationOptions);
                locationComponent.setCameraMode(CameraMode.NONE);
                locationComponent.setLocationComponentEnabled(true);
            }
        }
    }

    private void saveMapProperties(MapLibreMap mlMap) {
        if (mlMap == null || !mapFullyInitialized) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();

        CameraPosition mapPos = mlMap.getCameraPosition();
        LatLng mapTarget = mapPos.target;
        if (mapTarget == null) {
            return;
        }

        editor.putFloat(context.getString(R.string.pref_map_rotation), (float) mapPos.bearing);
        editor.putFloat(context.getString(R.string.pref_last_location_zoom), (float) mapPos.zoom);
        editor.putFloat(context.getString(R.string.pref_last_location_lat), (float) mapTarget.getLatitude());
        editor.putFloat(context.getString(R.string.pref_last_location_lon), (float) mapTarget.getLongitude());

        editor.commit();
    }

    public void updateMapRotation(boolean rotatingMapEnabled, float angle) {
        if (mlMap == null) {
            return;
        }

        CameraPosition oldCameraPosition = mlMap.getCameraPosition();
        CameraPosition newCameraPosition = new CameraPosition.Builder(oldCameraPosition)
                .bearing(angle)
                .build();
        mlMap.setCameraPosition(newCameraPosition);
        mlMap.getUiSettings().setRotateGesturesEnabled(rotatingMapEnabled);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void addMapListener(TouchDownListener touchDownListener, NoteMovedListener noteMovedCallback) {
        this.touchDownListener = touchDownListener;
        this.noteMovedCallback = noteMovedCallback;
    }

    private void addSymbolFragmentEventHandler(SymbolFragment fragment) {
        fragment.addEventHandler(new SymbolFragment.SymbolFragmentEventHandler() {
            @Override
            public void onDelete(Symbol symbol) {
                // We always have an ID and can therefore delete the note
                database.removeNote(GeoNotesSymbol.getNoteId(symbol));
                database.removePhotos(GeoNotesSymbol.getNoteId(symbol), context.getExternalFilesDir("GeoNotes"));
                symbolManager.delete(symbol);
            }

            @Override
            public void onSave(Symbol symbol) {
                // We always have an ID and can therefore update the note
                database.updateNoteDescription(GeoNotesSymbol.getNoteId(symbol), GeoNotesSymbol.getDescription(symbol));
            }

            @Override
            public void onClose(Symbol symbol) {
                deselectMarker(symbol);
            }

            @Override
            public void onMove(Symbol symbol) {
                symbolToMove = symbol;

                // Visually remove the icon from the map. This is because the icon will be shown on
                // a separate UI-element. This is used to show the note always in the center of the
                // screen while the user can move the map underneath. Other implementations (i.e.
                // moving the coordinate of the icon) are ugly and the icon would wiggle around
                // while moving the map.
                // Keep the symbol on the map as a "ghost" to see its previous location.
                symbol.setIconOpacity(0.333f);
                symbolManager.update(symbol);

                boolean hasPhotos = database.hasPhotos(GeoNotesSymbol.getNoteId(symbol));
                noteMovedCallback.onNoteMoveStarted(GeoNotesSymbol.getCategoryId(symbol), hasPhotos);
            }

            @Override
            public void onCategoryChanged(Symbol symbol) {
                database.updateNoteCategory(GeoNotesSymbol.getNoteId(symbol), GeoNotesSymbol.getCategoryId(symbol));

                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(context.getString(R.string.pref_last_category_id), GeoNotesSymbol.getCategoryId(symbol));
                editor.commit();

                // Update Icon with the new color
                setIcon(symbol, getSelectedSymbol() == symbol);
            }
        });
    }

    /**
     * Tries to snap the given location to gpsLocation if it's close by.
     *
     * @return When the gpsLocation is close by, gpsLocation is returned. Otherwise, location is returned.
     */
    private LatLng snapToGpsLocation(LatLng location, LatLng gpsLocation) {
        PointF markerLocationOnScreen = mlMap.getProjection().toScreenLocation(location);
        PointF gpsLocationOnScreen = mlMap.getProjection().toScreenLocation(gpsLocation);

        float diffY = gpsLocationOnScreen.y - markerLocationOnScreen.y;
        float diffX = gpsLocationOnScreen.x - markerLocationOnScreen.x;
        double distanceOnScreen = Math.sqrt(diffY * diffY + diffX * diffX);

        if (distanceOnScreen < snapToGpsPixelTolerance) {
            location = gpsLocation;
        }

        return location;
    }

    public void selectNote(long noteId) {
        for (int i = 0; i < this.symbolManager.getAnnotations().size(); i++) {
            Symbol symbol = this.symbolManager.getAnnotations().valueAt(i);
            if (GeoNotesSymbol.getNoteId(symbol) == noteId) {
                selectMarker(symbol, false);
            }
        }
    }

    /**
     * @param symbolToSelect          The symbol to select.
     * @param transferEditTextContent When set to true: If the user typed any text into the input
     *                                field without a selected note and *then* tapped on the map
     *                                to create or select one, this prior entered text schould be
     *                                used as the content of the note.
     *                                When set to false: The text of the tapped note will be read
     *                                and shown in the edit field.
     */
    private void selectMarker(Symbol symbolToSelect, boolean transferEditTextContent) {
        Symbol currentlySelectedSymbol = getSelectedSymbol();
        if (currentlySelectedSymbol != null) {
            setIcon(currentlySelectedSymbol, false);
        }

        setIcon(symbolToSelect, true);

        this.symbolFragment.selectSymbol(symbolToSelect, transferEditTextContent);
        zoomToSelectedMarker();

        addImagesToSymbolFragment();
    }

    private void deselectMarker(Symbol symbol) {
        if (symbol == null) {
            return;
        }

        // This icon will not be the selected symbol after "showInfoWindow", therefore we set the normal icon here.
        setIcon(symbol, false);
    }

    public Symbol getSelectedSymbol() {
        return symbolFragment.getSelectedSymbol();
    }

    /**
     * Loads images of current symbol (which contains the note-ID) from database and show them.
     */
    public List<String> addImagesToSymbolFragment() {
        symbolFragment.resetImageList();
        Symbol symbol = getSelectedSymbol();

        // It could happen that the user rotates the device (e.g. while taking a photo) and this
        // causes the whole activity to be reset. Therefore we might not have a symbol here.
        if (symbol == null) {
            return Collections.emptyList();
        }

        List<String> photoFileNames = database.getPhotos(GeoNotesSymbol.getNoteId(symbol));
        for (String photoFileName : photoFileNames) {
            File storageDir = context.getExternalFilesDir("GeoNotes");
            File image = new File(storageDir, photoFileName);
            symbolFragment.addPhoto(image);
        }

        setIcon(symbol, true);

        return photoFileNames;
    }

    private void setIcon(Symbol symbol, boolean isSelected) {
        boolean hasPhotos = database.hasPhotos(GeoNotesSymbol.getNoteId(symbol));
        symbol.setIconImage(GeoNotesSymbol.getIconName(symbol, hasPhotos, isSelected));
        this.symbolManager.update(symbol);
    }

    private void zoomToSelectedMarker() {
        // Before resuming (e.g. when switching back from the list of notes to the main activity),
        // the map doesn't zoom to markers. Therefore we here zoom to the currently selected symbol.
        Symbol selectedSymbol = getSelectedSymbol();
        if (selectedSymbol != null) {
            zoomToLocation(selectedSymbol.getLatLng());
        }
    }

    private void zoomToLocation(LatLng p) {
        if (mlMap == null) {
            return;
        }

        zoomToLocation(p, mlMap.getCameraPosition().zoom);
    }

    private void zoomToLocation(LatLng p, double zoom) {
        if (mlMap == null) {
            return;
        }

        CameraPosition oldCameraPosition = mlMap.getCameraPosition();
        CameraPosition newCameraPosition = new CameraPosition.Builder(oldCameraPosition)
                .zoom(zoom)
                .target(p)
                .build();
        mlMap.setCameraPosition(newCameraPosition);
    }

    private void createMarker(LatLng location) {
        // No marker to move here -> deselect or create marker
        // (selecting marker on the map is handles via the separate markerClickListener)
        if (symbolFragment.getSelectedSymbol() != null) {
            // Deselect selected marker:
            setIcon(symbolFragment.getSelectedSymbol(), false);
        }

        // Create new marker at this location and select it
        long categoryId = preferences.getLong(context.getString(R.string.pref_last_category_id), 1);

        long id = database.addNote("", location.getLatitude(), location.getLongitude(), categoryId);
        Note note = database.getNote(id);

        Symbol newSymbol = createMarker(note);
        selectMarker(newSymbol, true);
    }

    private Symbol createMarker(Note note) {
        boolean hasPhoto = database.hasPhotos(note.getId());

        JsonObject data = GeoNotesSymbol.getData(note);

        return this.symbolManager.create(
                new SymbolOptions()
                        .withLatLng(new LatLng(note.getLat(), note.getLon()))
                        .withIconImage(GeoNotesSymbol.getIconName(note, hasPhoto, false))
                        .withIconAnchor("bottom")
                        .withData(data)
        );
    }

    public void onResume() {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        zoomToSelectedMarker();
    }

    public void onDestroy() {
        symbolFragment.saveAndReset();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void setLocation(float lat, float lon, float zoom) {
        zoomToLocation(new LatLng(lat, lon), zoom);
    }

    /**
     * Turns the follow mode on or off. If it's turned on, the map will follow the current location.
     */
    public void setLocationFollowMode(boolean followingLocationEnabled) {
        if (mlMap == null) {
            return;
        }

        LocationComponent locationComponent = mlMap.getLocationComponent();
        if (followingLocationEnabled) {
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS_NORTH);
        } else {
            locationComponent.setCameraMode(CameraMode.NONE);
        }
    }

    public void addRequestPhotoHandler(SymbolFragment.RequestPhotoEventHandler requestPhotoEventHandler) {
        this.symbolFragment.addRequestPhotoHandler(requestPhotoEventHandler);
    }
}
