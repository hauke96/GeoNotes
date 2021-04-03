package de.hauke_stieler.geonotes.note_list;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;

public class NoteListActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        preferences = Injector.get(SharedPreferences.class);
        database = Injector.get(Database.class);

        load();
    }

    private void load() {
        List<Note> notes = database.getAllNotes();

        NoteListAdapter adapter = new NoteListAdapter(this, notes);

        ListView listView = (ListView) findViewById(R.id.note_list_view);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}