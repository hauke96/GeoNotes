package de.hauke_stieler.geonotes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.BarringInfo;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;

import de.hauke_stieler.geonotes.Database.Database;
import de.hauke_stieler.geonotes.map.Map;
import de.hauke_stieler.geonotes.map.MarkerWindow;
import de.hauke_stieler.geonotes.map.TouchDownListener;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;
import de.hauke_stieler.geonotes.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private Map map;
    private SharedPreferences preferences;
    private Database database;

    // These fields exist to remember the photo data when the photo Intent is started. This is
    // because the Intent doesn't return anything and works asynchronously. In the result handler
    // only "null" is passed but we want to store the photo for the note, that's why we store the
    // data in these fields here. Ugly and horrorfying but that's how it works in the Android
    // world ...
    private static File lastPhotoFile;
    private static Long lastPhotoNoteId;

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

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        });

        database = new Database(context);

        createMap(context);

        preferences = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> preferenceChanged(sharedPreferences, key));

        loadPreferences();
    }

    private void createMap(Context context) {
        // Keep device on
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "geonotes:wakelock");
        wakeLock.acquire();

        Drawable locationIcon = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_location, null);
        Drawable arrowIcon = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_arrow, null);
        Drawable selectedIcon = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_note_selected, null);
        Drawable normalIcon = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_note, null);

        MapView mapView = findViewById(R.id.map);
        map = new Map(context, mapView, wakeLock, database, locationIcon, arrowIcon, normalIcon, selectedIcon);

        addMapListener();
        addCameraListener();
    }

    private void loadPreferences() {
        for (String key : preferences.getAll().keySet()) {
            preferenceChanged(preferences, key);
        }

        float lat = preferences.getFloat(getString(R.string.pref_last_location_lat), 0f);
        float lon = preferences.getFloat(getString(R.string.pref_last_location_lon), 0f);
        float zoom = preferences.getFloat(getString(R.string.pref_last_location_zoom), 2);

        map.setLocation(lat, lon, zoom);
    }

    private void preferenceChanged(SharedPreferences pref, String key) {
        if (getString(R.string.pref_zoom_buttons).equals(key)) {
            boolean showZoomButtons = pref.getBoolean(key, true);
            map.setZoomButtonVisibility(showZoomButtons);
        } else if (getString(R.string.pref_map_scaling).equals(key)) {
            float mapScale = pref.getFloat(key, 1.0f);
            map.setMapScaleFactor(mapScale);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_btn_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.toolbar_btn_gps_follow:
                boolean followingLocationEnabled = !map.isFollowLocationEnabled();
                this.map.setLocationFollowMode(followingLocationEnabled);

                if (followingLocationEnabled) {
                    item.setIcon(R.drawable.ic_my_location);
                } else {
                    item.setIcon(R.drawable.ic_location_searching);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        map.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        requestPermissionsIfNecessary(permissions);
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

    private void addMapListener() {
        DelayedMapListener delayedMapListener = new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                storeLocation();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                storeLocation();
                return true;
            }
        }, 500);

        @SuppressLint("RestrictedApi")
        TouchDownListener touchDownListener = () -> {
            ActionMenuItemView menuItem = (ActionMenuItemView) findViewById(R.id.toolbar_btn_gps_follow);
            if (menuItem != null) {
                menuItem.setIcon(getResources().getDrawable(R.drawable.ic_location_searching));
            }
        };

        map.addMapListener(delayedMapListener, touchDownListener);
    }

    /**
     * Adds a listener for the camera button. The camera action can only be performed from within an activity.
     */
    private void addCameraListener() {
        // TODO check whether camera is available at all: hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        MarkerWindow.RequestPhotoEventHandler requestPhotoEventHandler = (Long noteId) -> {
            // TODO check if app has permissions
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                // Create the File where the photo should go
                lastPhotoFile = createImageFile();
                lastPhotoNoteId = noteId;

                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        lastPhotoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                Log.e("TakingPhoto", "Opening camera to take photo failed", e);
                return;
            }
        };

        map.addRequestPhotoHandler(requestPhotoEventHandler);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If photo-Intent was successful
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            addPhotoToDatabase(lastPhotoNoteId, lastPhotoFile);
            addPhotoToGallery(lastPhotoFile);
            map.addImagesToMarkerWindow();
        }
    }

    /**
     * Creates an empty file in the Environment.DIRECTORY_PICTURES directory.
     */
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "geonotes_" + timeStamp;

        File storageDir = getExternalFilesDir("GeoNotes");
        File image = new File(storageDir, imageFileName + ".jpg");

        return image;
    }

    private void addPhotoToDatabase(Long noteId, File photoFile) {
        database.addPhoto(noteId, photoFile);

        int sizeInPixel = getResources().getDimensionPixelSize(R.dimen.ImageButton);

        try {
            ThumbnailUtil.writeThumbnail(sizeInPixel, photoFile);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Creating thumbnail failed", Toast.LENGTH_SHORT);
        }
    }

    private void addPhotoToGallery(File photoFile) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, photoFile.getName());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, photoFile.lastModified());
        values.put(MediaStore.Images.Media.DATA, photoFile.toString());

        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    /**
     * Stores the current map location and zoom in the shared preferences.
     */
    private void storeLocation() {
        IGeoPoint location = map.getLocation();
        float zoom = map.getZoom();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(getString(R.string.pref_last_location_lat), (float) location.getLatitude());
        editor.putFloat(getString(R.string.pref_last_location_lon), (float) location.getLongitude());
        editor.putFloat(getString(R.string.pref_last_location_zoom), zoom);
        editor.commit();
    }
}