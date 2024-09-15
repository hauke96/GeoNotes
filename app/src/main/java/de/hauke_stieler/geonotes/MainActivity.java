package de.hauke_stieler.geonotes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import de.hauke_stieler.geonotes.categories.CategoryConfigurationActivity;
import de.hauke_stieler.geonotes.common.ExifHelper;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.databinding.ActivityMainBinding;
import de.hauke_stieler.geonotes.export.Exporter;
import de.hauke_stieler.geonotes.map.GeoNotesMarker;
import de.hauke_stieler.geonotes.map.Map;
import de.hauke_stieler.geonotes.map.MarkerFragment;
import de.hauke_stieler.geonotes.note_list.NoteListActivity;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;
import de.hauke_stieler.geonotes.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    static final String BUNDLE_KEY_CAMERA_IS_OPEN = "CAMERA_IS_OPEN";
    static final String BUNDLE_KEY_SELECTED_NOTE_ID = "SELECTED_NOTE_ID";

    static final int REQUEST_CATEGORIES_REQUEST_CODE = 5;
    static final int REQUEST_NOTE_LIST_REQUEST_CODE = 4;
    static final int REQUEST_PERMISSIONS_REQUEST_CODE = 3;
    static final int REQUEST_CAMERA_PERMISSIONS_REQUEST_CODE = 2;

    private Map map;
    private SharedPreferences preferences;
    private Database database;
    private Exporter exporter;
    private Toolbar toolbar;
    private NoteIconProvider noteIconProvider;
    private ActivityMainBinding viewBinding;
    private LifecycleCameraController cameraController;
    private Bundle savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.registerActivity(this);

        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        database = Injector.get(Database.class);
        preferences = Injector.get(SharedPreferences.class);
        exporter = Injector.get(Exporter.class);
        noteIconProvider = Injector.get(NoteIconProvider.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set HTML text of copyright label
        ((TextView) findViewById(R.id.copyright)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.copyright)).setText(Html.fromHtml(getString(R.string.osm_contribution)));

        String storagePermission = "";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storagePermission = Manifest.permission.MANAGE_EXTERNAL_STORAGE;
        }

        requestPermissionsIfNecessary(new String[]{
                storagePermission,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
        });

        addBackListener();

        createMarkerFragment();
        createMap();

        this.savedInstanceState = savedInstanceState;
    }

    private void createMarkerFragment() {
        MarkerFragment markerFragment = (MarkerFragment) getSupportFragmentManager().findFragmentById(R.id.map_marker_fragment);
        if (markerFragment == null) {
            markerFragment = new MarkerFragment();

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.map_marker_fragment, markerFragment, null)
                    .commit();
        }

        markerFragment.setOnCreatedHandler(() -> {
            if (savedInstanceState != null) {
                long selectedNoteId = savedInstanceState.getLong(BUNDLE_KEY_SELECTED_NOTE_ID, -1);
                if (selectedNoteId != -1) {
                    map.selectNote(selectedNoteId);
                }

                if (savedInstanceState.getBoolean(BUNDLE_KEY_CAMERA_IS_OPEN, false)) {
                    GeoNotesMarker marker = map.getSelectedMarker();
                    startCamera(Long.parseLong(marker.getId()), marker.getPosition().getLongitude(), marker.getPosition().getLatitude());
                }
            }
        });

        Injector.put(markerFragment);
    }

    private void createMap() {
        map = Injector.get(Map.class);

        addMapListener();
    }

    void loadPreferences() {
        boolean showZoomButtons = preferences.getBoolean(getString(R.string.pref_zoom_buttons), true);
        map.setZoomButtonVisibility(showZoomButtons);

        float mapScale = preferences.getFloat(getString(R.string.pref_map_scaling), 1.0f);
        map.setMapScaleFactor(mapScale);

        boolean snapNoteToGps = preferences.getBoolean(getString(R.string.pref_snap_note_gps), false);
        map.setSnapNoteToGps(snapNoteToGps);

        boolean enableRotatingMap = preferences.getBoolean(getString(R.string.pref_enable_rotating_map), false);
        float mapRotation = preferences.getFloat(getString(R.string.pref_map_rotation), 0f);
        map.updateMapRotation(enableRotatingMap, mapRotation);

        float lat = preferences.getFloat(getString(R.string.pref_last_location_lat), 0f);
        float lon = preferences.getFloat(getString(R.string.pref_last_location_lon), 0f);
        float zoom = preferences.getFloat(getString(R.string.pref_last_location_zoom), 2);

        map.setLocation(lat, lon, zoom);
    }

    private void showExportPopupMenu() {
        PopupMenu exportPopupMenu = new PopupMenu(this, findViewById(R.id.toolbar_btn_export));

        exportPopupMenu.getMenu().add(0, 0, 0, "GeoJson");
        exportPopupMenu.getMenu().add(0, 1, 1, "GPX");

        exportPopupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 0:
                    exporter.shareAsGeoJson();
                    break;
                case 1:
                    exporter.shareAsGpx();
                    break;
            }
            return true;
        });
        exportPopupMenu.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_btn_gps_follow:
                boolean followingLocationEnabled = !map.isFollowLocationEnabled();
                this.map.setLocationFollowMode(followingLocationEnabled);

                if (followingLocationEnabled) {
                    item.setIcon(R.drawable.ic_my_location);
                } else {
                    item.setIcon(R.drawable.ic_location_searching);
                }
                return true;
            case R.id.toolbar_btn_export:
                showExportPopupMenu();
                return true;
            case R.id.toolbar_btn_import:
                // TODO Import
                Toast.makeText(getApplicationContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.toolbar_btn_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.toolbar_btn_categories:
                startActivityForResult(new Intent(this, CategoryConfigurationActivity.class), REQUEST_CATEGORIES_REQUEST_CODE);
                return true;
            case R.id.toolbar_btn_note_list:
                startActivityForResult(new Intent(this, NoteListActivity.class), REQUEST_NOTE_LIST_REQUEST_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(BUNDLE_KEY_CAMERA_IS_OPEN, findViewById(R.id.camera_layout).getVisibility() == View.VISIBLE);

        GeoNotesMarker marker = map.getSelectedMarker();
        if (marker != null) {
            outState.putLong(BUNDLE_KEY_SELECTED_NOTE_ID, Long.parseLong(marker.getId()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPreferences();
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

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (!hasPermission(permission)) { // Permission is not granted
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;

            switch (permission) {
                case Manifest.permission.ACCESS_FINE_LOCATION:
                    if (!granted) {
                        toolbar.getMenu().findItem(R.id.toolbar_btn_gps_follow).setVisible(false);
                    }
            }
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
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
        Map.TouchDownListener touchDownCallback = () -> {
            ActionMenuItemView menuItem = findViewById(R.id.toolbar_btn_gps_follow);
            if (menuItem != null) {
                menuItem.setIcon(getResources().getDrawable(R.drawable.ic_location_searching));
            }
        };

        Map.NoteMovedListener noteMovedCallback = (noteId, longitude, latitude) -> {
            File externalFilesDir = getExternalFilesDir(FileHelper.GEONOTES_EXTERNAL_DIR_NAME);
            database.getPhotos(noteId).forEach(photo -> {
                File photoFile = new File(externalFilesDir, photo);
                addPositionToImageExifData(photoFile, longitude, latitude);
            });
        };

        map.addMapListener(delayedMapListener, touchDownCallback, noteMovedCallback);
        map.addRequestPhotoHandler(this::startCamera);
    }

    private void animateFocusRing(float x, float y) {
        ImageView focusView = findViewById(R.id.camera_preview_focus_view);

        // Move the focus ring so that its center is at the tap location (x, y)
        float width = focusView.getWidth();
        float height = focusView.getHeight();
        focusView.setX(x - width / 2);
        focusView.setY(y - height / 2);

        // Show focus ring
        focusView.setVisibility(View.VISIBLE);
        focusView.setAlpha(0.75F);

        // Animate the focus ring to disappear
        focusView.animate()
                .setStartDelay(200)
                .setDuration(600)
                .alpha(0F)
                .withEndAction(() -> focusView.setVisibility(View.INVISIBLE))
                .start();
    }

    private void addBackListener() {
        // Back-button of the phone
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeCamera();
                // TODO What to do when back-button pressed but camera not on? Nothing?
            }
        });

        // Back-button of the photo preview
        findViewById(R.id.image_capture_back).setOnClickListener(v -> {
            closeCamera();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Maybe some or all notes got deleted via the note list -> reload map
        if (requestCode == REQUEST_NOTE_LIST_REQUEST_CODE) {
            map.reloadAllNotes();
        }

        // If Intent was successful
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_NOTE_LIST_REQUEST_CODE:
                    long selectedNoteId = data.getLongExtra(NoteListActivity.EXTRA_CLICKED_NOTE, -1L);
                    if (selectedNoteId != -1) {
                        // Note selected in the note list -> also select on the map
                        map.selectNote(selectedNoteId);
                    }
                    break;
                case REQUEST_CATEGORIES_REQUEST_CODE:
                    noteIconProvider.updateIcons();
                    break;
            }
        }
    }

    private void startCamera(Long noteId, Double longitude, Double latitude) {
        String[] permissions = new String[1];
        permissions[0] = Manifest.permission.CAMERA;

        boolean hasCameraPermissions = hasPermission(Manifest.permission.CAMERA);
        boolean hasStoragePermissions = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            hasStoragePermissions = hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            String[] oldPermissions = permissions;
            permissions = new String[oldPermissions.length + 1];
            permissions[permissions.length - 1] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }

        if (!hasCameraPermissions || !hasStoragePermissions) {
            // We don't have camera and/or storage permissions -> ask for them
            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUEST_CAMERA_PERMISSIONS_REQUEST_CODE);
            return;
        }

        findViewById(R.id.toolbar).setVisibility(View.INVISIBLE);
        findViewById(R.id.main_layout).setVisibility(View.INVISIBLE);
        findViewById(R.id.map_marker_fragment).setVisibility(View.INVISIBLE);

        findViewById(R.id.camera_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.image_capture_button).setOnClickListener(view -> {
            disableCameraButtons();
            takePhoto(noteId, longitude, latitude);
        });

        cameraController = new LifecycleCameraController(getBaseContext());

        try {
            cameraController.bindToLifecycle(this);
            cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
            viewBinding.cameraPreview.setController(cameraController);
        } catch (Exception e) {
            Log.e("startCamera", "Error while unbinding and binding camera lifecycle: ", e);
            throw new RuntimeException(e);
        }

        AtomicBoolean wasPinching = new AtomicBoolean(false);

        findViewById(R.id.camera_preview).setOnTouchListener((v, event) -> {
            Log.i("cam", "startCamera: "+event.getPointerCount() + " - " + MotionEvent.actionToString(event.getAction()));
            boolean touchHandled = false;

            boolean actionDown = event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN;
            boolean actionUp = event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP;

            if (event.getPointerCount() > 1 && actionDown) {
                wasPinching.set(true);
            }
            if (event.getPointerCount() == 1 && actionUp) {
                if (!wasPinching.get()) {
                    animateFocusRing(event.getX(), event.getY());
                    v.performClick();
                    touchHandled = true;
                }

                wasPinching.set(false);
            }
            return touchHandled;
        });
    }

    private void closeCamera() {
        findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.map_marker_fragment).setVisibility(View.VISIBLE);

        findViewById(R.id.camera_layout).setVisibility(View.INVISIBLE);

        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (Exception e) {
            Log.e("closeCamera", "Error while unbinding camera lifecycle: ", e);
            throw new RuntimeException(e);
        }
    }

    private void takePhoto(Long noteId, Double longitude, Double latitude) {
        // TODO Is lastPhotoFile needed, because we get its Uri in the callback below.
        File photoFile = createImageFile();

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build();

        cameraController.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.i("capture", "Saved photo to " + outputFileResults.getSavedUri());

                        addPositionToImageExifData(photoFile, longitude, latitude);

                        addPhotoToDatabase(noteId, photoFile);
                        map.addImagesToMarkerFragment();

                        enableCameraButtons();

                        boolean keepCameraOpen = preferences.getBoolean(getApplicationContext().getString(R.string.pref_keep_camera_open), false);
                        if (!keepCameraOpen) {
                            closeCamera();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("capture", "Error: ", exception);
                        Toast.makeText(getBaseContext(), "Error taking picture: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        enableCameraButtons();
                        closeCamera();
                    }
                }
        );
    }

    private void enableCameraButtons() {
        findViewById(R.id.image_capture_button).setEnabled(true);
        findViewById(R.id.image_capture_button).setAlpha(1f);
        findViewById(R.id.image_capture_back).setEnabled(true);
        findViewById(R.id.image_capture_back).setAlpha(1f);
    }

    private void disableCameraButtons() {
        findViewById(R.id.image_capture_button).setEnabled(false);
        findViewById(R.id.image_capture_button).setAlpha(0.35f);
        findViewById(R.id.image_capture_back).setEnabled(false);
        findViewById(R.id.image_capture_back).setAlpha(0.35f);
    }

    private void addPositionToImageExifData(File photoFile, Double longitude, Double latitude) {
        Log.i("addExifData", "Add location to EXIF data of file " + photoFile.getAbsolutePath());
        try {
            ExifInterface exif = new ExifInterface(getContentResolver().openFileDescriptor(Uri.fromFile(photoFile), "rw").getFileDescriptor());
            ExifHelper.fillExifAttributesWithGps(exif, longitude, latitude);
            exif.saveAttributes();
        } catch (Exception e) {
            Log.e("addExifData", "Error getting/setting/saving EXIF data from freshly taken photo file " + photoFile.getAbsolutePath(), e);
            throw new RuntimeException(e);
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
            Toast.makeText(getApplicationContext(), R.string.note_list_create_thumbnail_failed, Toast.LENGTH_SHORT);
        }
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