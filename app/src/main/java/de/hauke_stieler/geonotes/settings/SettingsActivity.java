package de.hauke_stieler.geonotes.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import de.hauke_stieler.geonotes.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        load();
    }

    private void load() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean prefZoomButtons = preferences.getBoolean(getString(R.string.pref_zoom_buttons), true);
        ((Switch) findViewById(R.id.settings_zoom_switch)).setChecked(prefZoomButtons);

        float prefMapScaling = preferences.getFloat(getString(R.string.pref_map_scaling), 1.0f);
        ((EditText) findViewById(R.id.settings_scale_input)).setText("" + prefMapScaling);
    }

    private void save() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        boolean checked = ((Switch) findViewById(R.id.settings_zoom_switch)).isChecked();
        editor.putBoolean(getString(R.string.pref_zoom_buttons), checked);
        String s = ((EditText) findViewById(R.id.settings_scale_input)).getText().toString();
        editor.putFloat(getString(R.string.pref_map_scaling), Float.parseFloat(s));
        editor.commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        save();
        finish();
        return true;
    }
}