package de.hauke_stieler.geonotes.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.maplibre.android.offline.OfflineManager;

import de.hauke_stieler.geonotes.BuildConfig;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.common.AppCompatExtension;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(this);
        View rootView = inflater.inflate(R.layout.settings_activity, null);
        setContentView(rootView);

        AppCompatExtension.setupWindowInsetListener(rootView, findViewById(R.id.settings_toolbar));

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                save();
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        preferences = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE);

        load();

        TextView versionLabel = findViewById(R.id.settings_version_label);
        versionLabel.setText(getString(R.string.geonotes_version) + " " + BuildConfig.VERSION_NAME);

        Button clearCacheButton = findViewById(R.id.settings_clear_cache);
        clearCacheButton.setOnClickListener(v -> {
            findViewById(R.id.settings_clear_cache_loading_spinner).setVisibility(View.VISIBLE);
            clearCacheButton.setEnabled(false);

            new Thread(() -> {
                SettingsActivity context = this;
                OfflineManager.getInstance(context).clearAmbientCache(new OfflineManager.FileSourceCallback() {
                    @Override
                    public void onSuccess() {
                        findViewById(R.id.settings_clear_cache_loading_spinner).setVisibility(View.GONE);
                        clearCacheButton.setEnabled(true);

                        Toast.makeText(context, getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull String s) {
                        findViewById(R.id.settings_clear_cache_loading_spinner).setVisibility(View.GONE);
                        clearCacheButton.setEnabled(true);

                        Toast.makeText(context, R.string.cache_cleared_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        Button feedbackButton = findViewById(R.id.settings_feedback_button);
        feedbackButton.setOnClickListener(v -> {
            String mailDomain = getString(R.string.feedback_mail_domain);
            String mailLocalPart = getString(R.string.feedback_mail_local_part);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{mailLocalPart + "@" + mailDomain});
            i.putExtra(Intent.EXTRA_SUBJECT, "[GeoNotes][" + BuildConfig.VERSION_NAME + "] Feedback");
            int sdkInt = Build.VERSION.SDK_INT;
            i.putExtra(Intent.EXTRA_TEXT, "Manufacturer: " + Build.MANUFACTURER +
                    "\nModel: " + Build.MODEL +
                    "\nDevice: " + Build.DEVICE +
                    "\nSDK-Version: " + sdkInt +
                    "\nGeoNotes-Version: " + BuildConfig.VERSION_NAME +
                    "\n\nFeedback: ");
            try {
                startActivity(Intent.createChooser(i, "Choose E-Mail client"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, getString(R.string.no_mail_client_found), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void load() {
        float prefMapScaling = preferences.getFloat(getString(R.string.pref_map_scaling), Float.NaN);
        if (Float.isNaN(prefMapScaling)) {
            ((EditText) findViewById(R.id.settings_scale_input)).setText("");
        } else {
            ((EditText) findViewById(R.id.settings_scale_input)).setText("" + prefMapScaling);
        }

        boolean prefSnapNoteGps = preferences.getBoolean(getString(R.string.pref_snap_note_gps), false);
        ((Switch) findViewById(R.id.settings_snap_note_gps)).setChecked(prefSnapNoteGps);

        boolean prefEnableRotatingMap = preferences.getBoolean(getString(R.string.pref_enable_rotating_map), false);
        ((Switch) findViewById(R.id.settings_enable_rotating_map)).setChecked(prefEnableRotatingMap);

        boolean prefLongTap = preferences.getBoolean(getString(R.string.pref_tap_duration), false);
        ((Switch) findViewById(R.id.settings_tap_long)).setChecked(prefLongTap);

        boolean prefKeepCameraOpen = preferences.getBoolean(getString(R.string.pref_keep_camera_open), false);
        ((Switch) findViewById(R.id.settings_keep_camera_open)).setChecked(prefKeepCameraOpen);
    }

    private void save() {
        SharedPreferences.Editor editor = preferences.edit();

        String mapScaleString = ((EditText) findViewById(R.id.settings_scale_input)).getText().toString();
        float mapScale = Float.NaN;
        try {
            mapScale = Float.parseFloat(mapScaleString);
            if (mapScale < 0.1f) {
                mapScale = 0.1f;
            }
        } catch (NumberFormatException e) {
            // Nothing to do, just don't crash because of wrong input
        }
        editor.putFloat(getString(R.string.pref_map_scaling), mapScale);

        boolean gpsSnapSwitchChecked = ((Switch) findViewById(R.id.settings_snap_note_gps)).isChecked();
        editor.putBoolean(getString(R.string.pref_snap_note_gps), gpsSnapSwitchChecked);

        boolean enableRotatingMapChecked = ((Switch) findViewById(R.id.settings_enable_rotating_map)).isChecked();
        editor.putBoolean(getString(R.string.pref_enable_rotating_map), enableRotatingMapChecked);

        boolean useLongTap = ((Switch) findViewById(R.id.settings_tap_long)).isChecked();
        editor.putBoolean(getString(R.string.pref_tap_duration), useLongTap);

        boolean keepCameraOpen = ((Switch) findViewById(R.id.settings_keep_camera_open)).isChecked();
        editor.putBoolean(getString(R.string.pref_keep_camera_open), keepCameraOpen);

        editor.commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        save();
        setResult(Activity.RESULT_OK);
        finish();
        return true;
    }
}