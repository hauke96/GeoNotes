package de.hauke_stieler.geonotes.note_list;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;

public class NoteListActivity extends AppCompatActivity {
    public static final String EXTRA_CLICKED_NOTE = "clicked_note";

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

        List<Note> notesWithPhoto = new ArrayList<>();
        for (Note note : notes) {
            if (database.hasPhotos(note.getId())) {
                notesWithPhoto.add(note);
            }
        }

        NoteListAdapter adapter = new NoteListAdapter(this, notes, notesWithPhoto, id -> {
            // Close this activity and send back clicked note id
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_CLICKED_NOTE, id);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        ListView listView = findViewById(R.id.note_list_view);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}