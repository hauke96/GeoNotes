package de.hauke_stieler.geonotes.note_list;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import de.hauke_stieler.geonotes.BuildConfig;
import de.hauke_stieler.geonotes.R;

public class NoteListActivity extends AppCompatActivity {
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        preferences = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE);

//        load();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}