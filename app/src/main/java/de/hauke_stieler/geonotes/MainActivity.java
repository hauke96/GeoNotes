package de.hauke_stieler.geonotes;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.maplibre.android.MapLibre;
import org.maplibre.android.maps.MapLibreMapOptions;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.plugins.annotation.Symbol;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.hauke_stieler.geonotes.categories.CategoryConfigurationActivity;
import de.hauke_stieler.geonotes.common.AppCompatExtension;
import de.hauke_stieler.geonotes.common.ExifHelper;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.BackupImportDialog;
import de.hauke_stieler.geonotes.export.Exporter;
import de.hauke_stieler.geonotes.map.GeoNotesSymbol;
import de.hauke_stieler.geonotes.map.Map;
import de.hauke_stieler.geonotes.map.SymbolFragment;
import de.hauke_stieler.geonotes.note_list.NoteListActivity;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;
import de.hauke_stieler.geonotes.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity implements LocationListener {

    static final String BUNDLE_KEY_CAMERA_IS_OPEN = "CAMERA_IS_OPEN";
    static final String BUNDLE_KEY_SELECTED_NOTE_ID = "SELECTED_NOTE_ID";

    static final int REQUEST_CAMERA_PERMISSIONS_REQUEST_CODE = 2;
    static final int REQUEST_PERMISSIONS_REQUEST_CODE = 3;
    static final int REQUEST_NOTE_LIST_REQUEST_CODE = 4;
    static final int REQUEST_CATEGORIES_REQUEST_CODE = 5;
    static final int REQUEST_SETTINGS_REQUEST_CODE = 6;
    static final int REQUEST_EXPORT_GEOJSON_RESULT_CODE = 7;
    static final int REQUEST_EXPORT_GPX_RESULT_CODE = 8;
    static final int REQUEST_EXPORT_BACKUP_RESULT_CODE = 9;

    private Map map;
    private int mapViewId;
    private SharedPreferences preferences;
    private Database database;
    private Exporter exporter;
    private Toolbar toolbar;
    private NoteIconProvider noteIconProvider;
    private LifecycleCameraController cameraController;
    private Bundle savedInstanceState;
    private BroadcastReceiver gpsSwitchStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        Injector.registerActivity(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View rootView = inflater.inflate(R.layout.activity_main, null);
        setContentView(rootView);

        AppCompatExtension.setupWindowInsetListener(rootView, findViewById(R.id.toolbar));

        database = Injector.get(Database.class);
        preferences = Injector.get(SharedPreferences.class);
        exporter = Injector.get(Exporter.class);
        noteIconProvider = Injector.get(NoteIconProvider.class);

        mapViewId = View.generateViewId();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        createSymbolFragment();
        createMap();

        gpsSwitchStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
                    map.enableLocationsComponent();
                }
            }
        };
        registerReceiver(gpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        this.savedInstanceState = savedInstanceState;
    }

    private void createSymbolFragment() {
        SymbolFragment symbolFragment = (SymbolFragment) getSupportFragmentManager().findFragmentById(R.id.map_symbol_fragment);
        if (symbolFragment == null) {
            symbolFragment = new SymbolFragment();

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.map_symbol_fragment, symbolFragment, null)
                    .commit();
        }

        symbolFragment.setOnCreatedHandler(() -> {
            if (savedInstanceState != null) {
                long selectedNoteId = savedInstanceState.getLong(BUNDLE_KEY_SELECTED_NOTE_ID, -1);
                if (selectedNoteId != -1) {
                    map.selectNote(selectedNoteId);
                }

                if (savedInstanceState.getBoolean(BUNDLE_KEY_CAMERA_IS_OPEN, false)) {
                    Symbol symbol = map.getSelectedSymbol();
                    startCamera(GeoNotesSymbol.getNoteId(symbol), symbol.getLatLng().getLongitude(), symbol.getLatLng().getLatitude());
                }
            }
        });

        Injector.put(symbolFragment);
    }

    private void createMap() {
        map = Injector.get(Map.class);

        addMapListener();

        createMapView();
    }

    private void showExportPopupMenu() {
        PopupMenu exportPopupMenu = new PopupMenu(this, findViewById(R.id.toolbar_btn_export));

        exportPopupMenu.getMenu().add(0, 0, 0, "GeoJson");
        exportPopupMenu.getMenu().add(0, 1, 1, "GPX");
        exportPopupMenu.getMenu().add(0, 2, 2, "Backup (ZIP)");

        exportPopupMenu.setOnMenuItemClickListener(menuItem -> {
            int requestCode = -1;
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            switch (menuItem.getItemId()) {
                case 0:
                    intent.putExtra(Intent.EXTRA_TITLE, Exporter.getGeojsonFilename());
                    intent.setType(Exporter.GEOJSON_MIME_TYPE);
                    requestCode = REQUEST_EXPORT_GEOJSON_RESULT_CODE;
                    break;
                case 1:
                    intent.putExtra(Intent.EXTRA_TITLE, Exporter.getGpxFilename());
                    intent.setType(Exporter.GPX_MIME_TYPE);
                    requestCode = REQUEST_EXPORT_GPX_RESULT_CODE;
                    break;
                case 2:
                    intent.putExtra(Intent.EXTRA_TITLE, Exporter.getBackupFilename());
                    intent.setType(Exporter.BACKUP_MIME_TYPE);
                    requestCode = REQUEST_EXPORT_BACKUP_RESULT_CODE;
                    break;
            }

            startActivityForResult(intent, requestCode);

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
        int itemId = item.getItemId();
        if (itemId == R.id.toolbar_btn_gps_follow) {
            boolean newFollowingLocationState = !item.isChecked();

            this.map.setLocationFollowMode(newFollowingLocationState);

            if (newFollowingLocationState) {
                item.setChecked(true);
                item.setIcon(R.drawable.ic_my_location);
            } else {
                item.setChecked(false);
                item.setIcon(R.drawable.ic_location_searching);
            }
            return true;
        } else if (itemId == R.id.toolbar_btn_export) {
            showExportPopupMenu();
            return true;
        } else if (itemId == R.id.toolbar_btn_import) {
            new BackupImportDialog().show(getSupportFragmentManager(), BackupImportDialog.class.getName());
            return true;
        } else if (itemId == R.id.toolbar_btn_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS_REQUEST_CODE);
            return true;
        } else if (itemId == R.id.toolbar_btn_categories) {
            startActivityForResult(new Intent(this, CategoryConfigurationActivity.class), REQUEST_CATEGORIES_REQUEST_CODE);
            return true;
        } else if (itemId == R.id.toolbar_btn_note_list) {
            startActivityForResult(new Intent(this, NoteListActivity.class), REQUEST_NOTE_LIST_REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(BUNDLE_KEY_CAMERA_IS_OPEN, findViewById(R.id.camera_layout).getVisibility() == View.VISIBLE);

        Symbol symbol = map.getSelectedSymbol();
        if (symbol != null) {
            outState.putLong(BUNDLE_KEY_SELECTED_NOTE_ID, GeoNotesSymbol.getNoteId(symbol));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        registerReceiver(gpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    protected void onDestroy() {
        map.onDestroy();
        unregisterReceiver(gpsSwitchStateReceiver);
        super.onDestroy();
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                handleGrantedPermission(permission, true);
            } else { // Permission is not granted yet
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
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
            handleGrantedPermission(permission, granted);
        }
    }

    private void handleGrantedPermission(String permission, boolean granted) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (granted) {
                    if (map != null) { // The map might not be loaded yet
                        map.enableLocationsComponent();
                    }

                    // Manually register for location updates. Otherwise, the default location
                    // provider of MapLibre will not properly update the location but instead show
                    // either just the last location or a very coarse location. Without this, it
                    // might also happen, that the location "jumps" quite a large distance of
                    // multiple hundred meters.
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1, this);
                    }
                } else {
                    toolbar.getMenu().findItem(R.id.toolbar_btn_gps_follow).setVisible(false);
                }
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void createMapView() {
        float mapScale = preferences.getFloat(getString(R.string.pref_map_scaling), Float.NaN);

        MapLibreMapOptions options = MapLibreMapOptions.createFromAttributes(this);
        if (!Float.isNaN(mapScale)) {
            options.pixelRatio(mapScale);
        }

        RelativeLayout layout = findViewById(R.id.main_layout);

        MapView oldMapView = layout.findViewById(this.mapViewId);

        MapView newMapView = new MapView(this, options);
        newMapView.setId(this.mapViewId);
        newMapView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newMapView.onCreate(savedInstanceState);

        if (oldMapView != null) {
            layout.removeView(oldMapView);
        }
        layout.addView(newMapView, 0);

        this.map.initMapViewAnyRegisterHandlers(newMapView);
    }

    private void addMapListener() {
        Map.TouchDownListener touchDownCallback = () -> {
            MenuItem menuItem = toolbar.getMenu().findItem(R.id.toolbar_btn_gps_follow);
            if (menuItem != null) {
                menuItem.setChecked(false);
                menuItem.setIcon(R.drawable.ic_location_searching);
            }
        };

        Map.NoteMovedListener noteMovedCallback = new Map.NoteMovedListener() {
            @Override
            public void onNoteMoveStarted(Long categoryId, boolean isPhotoNote) {
                Drawable icon = noteIconProvider.getIcon(categoryId, true, isPhotoNote);
                ((ImageView) findViewById(R.id.map_icon_move_view)).setImageDrawable(icon);
            }

            @Override
            public void onNoteMoveEnded(Long noteId, Double longitude, Double latitude) {
                File externalFilesDir = getExternalFilesDir(FileHelper.GEONOTES_EXTERNAL_DIR_NAME);
                database.getPhotos(noteId).forEach(photo -> {
                    File photoFile = new File(externalFilesDir, photo);
                    addPositionToImageExifData(photoFile, longitude, latitude);
                });

                ImageView imageView = findViewById(R.id.map_icon_move_view);
                imageView.setImageDrawable(null);
            }
        };

        map.addMapListener(touchDownCallback, noteMovedCallback);
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
            Uri targetFile;
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
                    map.reloadAllNotes();
                    break;
                case REQUEST_SETTINGS_REQUEST_CODE:
                    createMapView();
                    map.loadPreferences();
                    break;
                case REQUEST_EXPORT_GEOJSON_RESULT_CODE:
                    targetFile = data.getData();
                    exporter.shareAsGeoJson(targetFile);
                    break;
                case REQUEST_EXPORT_GPX_RESULT_CODE:
                    targetFile = data.getData();
                    exporter.shareAsGpx(targetFile);
                    break;
                case REQUEST_EXPORT_BACKUP_RESULT_CODE:
                    targetFile = data.getData();
                    try {
                        exporter.shareAsBackup(targetFile, preferences);
                    } catch (IOException e) {
                        Log.e(MainActivity.class.getName(), "save backup: ", e);
                        throw new RuntimeException(e);
                    }
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
        findViewById(R.id.map_symbol_fragment).setVisibility(View.INVISIBLE);

        findViewById(R.id.camera_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.image_capture_button).setOnClickListener(view -> {
            disableCameraButtons();
            takePhoto(noteId, longitude, latitude);
        });

        int numerOfPhotos = database.getPhotos(noteId).size();
        ((TextView) findViewById(R.id.image_capture_image_count_label)).setText(numerOfPhotos + "");

        cameraController = new LifecycleCameraController(getBaseContext());

        try {
            cameraController.bindToLifecycle(this);
            cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
            PreviewView cameraPreview = findViewById(R.id.camera_preview);
            cameraPreview.setController(cameraController);
        } catch (Exception e) {
            Log.e("startCamera", "Error while unbinding and binding camera lifecycle: ", e);
            throw new RuntimeException(e);
        }

        AtomicBoolean wasPinching = new AtomicBoolean(false);

        findViewById(R.id.camera_preview).setOnTouchListener((v, event) -> {
            Log.i("cam", "startCamera: " + event.getPointerCount() + " - " + MotionEvent.actionToString(event.getAction()));

            boolean actionDown = event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN;
            boolean actionUp = event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP;

            if (event.getPointerCount() > 1 && actionDown) {
                wasPinching.set(true);
            }
            if (event.getPointerCount() == 1 && actionUp) {
                if (!wasPinching.get()) {
                    animateFocusRing(event.getX(), event.getY());
                    v.performClick();
                }

                wasPinching.set(false);
            }

            return false;
        });
    }

    private void closeCamera() {
        findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.map_symbol_fragment).setVisibility(View.VISIBLE);

        findViewById(R.id.camera_layout).setVisibility(View.INVISIBLE);

        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();

            // Re-select the note so that the input field is selected and the keyboard comes up.
            // Makes it easier to add text after taking pictures.
            if (map.getSelectedSymbol() != null) {
                map.selectNote(GeoNotesSymbol.getNoteId(map.getSelectedSymbol()));
            }
        } catch (Exception e) {
            Log.e("closeCamera", "Error while unbinding camera lifecycle: ", e);
            throw new RuntimeException(e);
        }
    }

    private void takePhoto(Long noteId, Double longitude, Double latitude) {
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
                        List<String> photosOfFragment = map.addImagesToSymbolFragment();

                        ((TextView) findViewById(R.id.image_capture_image_count_label)).setText(photosOfFragment.size() + "");

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
            ExifHelper.fillExifAttributesWithGps(getContentResolver(), photoFile, longitude, latitude);
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
            ThumbnailUtil.writeThumbnail(getContentResolver(), photoFile, sizeInPixel);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), R.string.note_list_create_thumbnail_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(this.getClass().getName(), "On location update: " + location);
    }

    // Empty handler needed for Android backward compatibility (at least for Android 10)
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    // Empty handler needed for Android backward compatibility (at least for Android 10)
    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    // Empty handler needed for Android backward compatibility (at least for Android 10)
    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }
}