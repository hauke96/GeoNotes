package de.hauke_stieler.geonotes.note_list;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;

public class NoteListActivity extends AppCompatActivity {
    public static final String EXTRA_CLICKED_NOTE = "clicked_note";

    private SharedPreferences preferences;
    private Database database;
    private NoteIconProvider noteIconProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        Toolbar toolbar = findViewById(R.id.toolbar_note_list);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        preferences = Injector.get(SharedPreferences.class);
        database = Injector.get(Database.class);
        noteIconProvider = Injector.get(NoteIconProvider.class);

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

        NoteListAdapter adapter = new NoteListAdapter(
                this,
                noteIconProvider,
                notes,
                notesWithPhoto,
                id -> {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_note_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_btn_delete_all:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Really delete all notes? This is not reversible!");
                builder.setPositiveButton(R.string.dialog_yes, (dialog, id) -> {
                    database.removeAllNotes();
                    load();
                });
                builder.setNegativeButton(R.string.dialog_no, (dialog, id) -> {
                });
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}