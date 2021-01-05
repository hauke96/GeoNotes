package de.hauke_stieler.geonotes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private MapView map = null;
    private IMapController mapController;
    private MarkerWindow markerInfoWindow;
    private Marker.OnMarkerClickListener markerClickListener;

    private MyLocationNewOverlay locationOverlay;
    private CompassOverlay compassOverlay;
    private ScaleBarOverlay scaleBarOverlay;

    private PowerManager.WakeLock wakeLock;

    private NoteStore noteStore;

    private Marker markerToMove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set HTML text of copyright label
        ((TextView) findViewById(R.id.copyright)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.copyright)).setText(Html.fromHtml("© <a href=\"https://openstreetmap.org/copyright\">OpenStreetMap</a> contributors"));

        final Context context = getApplicationContext();

        noteStore = new NoteStore(context);

        // Keep device on
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "geonotes:wakelock");
        wakeLock.acquire();

        createMap(context);

        for (Note n : noteStore.getAllNotes()) {
            Marker marker = createMarker(n.description, new GeoPoint(n.lat, n.lon), markerClickListener);
            marker.setId("" + n.id);
        }
    }

    /**
     * Creates the map, mapController, markerInfoWindow, markerClickListener and the overlays.
     */
    private void createMap(Context context) {
        Configuration.getInstance().setUserAgentValue(context.getPackageName());

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        });

        // Initial location and zoom
        mapController = map.getController();
        mapController.setZoom(17.0);
        GeoPoint startPoint = new GeoPoint(53.563, 9.9866);
        mapController.setCenter(startPoint);

        Drawable currentDraw = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_location, null);
        Bitmap currentIcon = null;
        if (currentDraw != null) {
            currentIcon = ((BitmapDrawable) currentDraw).getBitmap();
        }

        // Add location icon
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
        locationOverlay.enableMyLocation();
        locationOverlay.setPersonIcon(currentIcon);
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
        Drawable draw = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_note_selected, null);
        marker.setIcon(draw);
    }

    private void setNormalIcon(Marker marker) {
        Drawable draw = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_note, null);
        marker.setIcon(draw);
    }

    private void centerLocationWithOffset(GeoPoint p) {
        Point locationInPixels = new Point();
        map.getProjection().toPixels(p, locationInPixels);
        IGeoPoint newPoint = map.getProjection().fromPixels(locationInPixels.x, locationInPixels.y);

        mapController.animateTo(newPoint, map.getZoomLevelDouble(), (long) Configuration.getInstance().getAnimationSpeedShort() / 2);
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

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        wakeLock.release();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}