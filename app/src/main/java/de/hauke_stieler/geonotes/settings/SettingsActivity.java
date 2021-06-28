package de.hauke_stieler.geonotes.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.system.OsConstants;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import de.hauke_stieler.geonotes.BuildConfig;
import de.hauke_stieler.geonotes.R;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        preferences = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE);

        load();

        TextView versionLabel = (TextView) findViewById(R.id.settings_version_label);
        versionLabel.setText("GeoNotes version: " + BuildConfig.VERSION_NAME);

        Button feedbackButton = (Button) findViewById(R.id.settings_feedback_button);
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
                Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void load() {
        boolean prefZoomButtons = preferences.getBoolean(getString(R.string.pref_zoom_buttons), true);
        ((Switch) findViewById(R.id.settings_zoom_switch)).setChecked(prefZoomButtons);

        float prefMapScaling = preferences.getFloat(getString(R.string.pref_map_scaling), 1.0f);
        ((EditText) findViewById(R.id.settings_scale_input)).setText("" + prefMapScaling);

        boolean prefSnapNoteGps = preferences.getBoolean(getString(R.string.pref_snap_note_gps), false);
        ((Switch) findViewById(R.id.settings_snap_note_gps)).setChecked(prefSnapNoteGps);
    }

    private void save() {
        SharedPreferences.Editor editor = preferences.edit();

        boolean zoomSwitchChecked = ((Switch) findViewById(R.id.settings_zoom_switch)).isChecked();
        editor.putBoolean(getString(R.string.pref_zoom_buttons), zoomSwitchChecked);

        String mapScaleString = ((EditText) findViewById(R.id.settings_scale_input)).getText().toString();
        float mapScale = 1.0f;
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

        editor.commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        save();
        finish();
        return true;
    }
}