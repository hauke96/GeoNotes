package de.hauke_stieler.geonotes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;
    private CompassOverlay compassOverlay;
    private ScaleBarOverlay scaleBarOverlay;
    private PowerManager.WakeLock wakeLock;
    private NoteStore noteStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();

        noteStore = new NoteStore(context);

        // Keep device on
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "geonotes:wakelock");
        wakeLock.acquire();

        Configuration.getInstance().setUserAgentValue(context.getPackageName());

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        });

        // Initial location and zoom
        IMapController mapController = map.getController();
        mapController.setZoom(17.0);
        GeoPoint startPoint = new GeoPoint(53.563, 9.9866);
        mapController.setCenter(startPoint);

        // Add location icon
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
        locationOverlay.enableMyLocation();
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
        MarkerWindow markerInfoWindow = new MarkerWindow(R.layout.maker_window, map, new MarkerWindow.MarkerEventHandler() {
            @Override
            public void onDelete(Marker marker) {
                // TODO delete from store
                map.getOverlays().remove(marker);
            }

            @Override
            public void onSave(Marker marker) {
                // TODO null checks
                noteStore.updateDescription(Long.parseLong(marker.getId()), marker.getSnippet());
            }
        });

        // Add marker stuff
        Marker.OnMarkerClickListener markerClickListener = (marker1, mapView) -> {
            if (!marker1.isInfoWindowShown()) {
                marker1.showInfoWindow();
            } else {
                marker1.closeInfoWindow();
            }
            return true;
        };

        // React to touches on the map
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                long id = noteStore.addNote("", p.getLatitude(), p.getLongitude());

                Marker marker = createMarker(id, "", p, markerInfoWindow, markerClickListener);
                marker.showInfoWindow();

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mapEventsReceiver));

        for (Note n : noteStore.getAllNotes()) {
            createMarker(n.id, n.description, new GeoPoint(n.lat, n.lon), markerInfoWindow, markerClickListener);
        }
    }

    private Marker createMarker(long id, String description, GeoPoint p, MarkerWindow markerInfoWindow, Marker.OnMarkerClickListener markerClickListener) {
        Marker marker = new Marker(map);
        marker.setPosition(p);
        marker.setId("" + id);
        marker.setSnippet(description);
        marker.setInfoWindow(markerInfoWindow);
        marker.setOnMarkerClickListener(markerClickListener);

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