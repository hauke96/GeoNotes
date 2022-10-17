package de.hauke_stieler.geonotes.note_list;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;

public class NoteListActivity extends AppCompatActivity implements FilterDialog.FilterChangedListener {
    public static final String EXTRA_CLICKED_NOTE = "clicked_note";

    private Database database;
    private NoteIconProvider noteIconProvider;

    private String filterText;
    private Long filterCategoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        Toolbar toolbar = findViewById(R.id.note_list_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

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

        notes = filterNotes(notes);
        notesWithPhoto = filterNotes(notesWithPhoto);

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

    private List<Note> filterNotes(List<Note> notes) {
        if (this.filterText != null && !this.filterText.trim().isEmpty()) {
            List<Note> newNotes = new ArrayList<>();
            for (Note n : notes) {
                if (n.getDescription() != null && n.getDescription().contains(this.filterText)) {
                    newNotes.add(n);
                }
            }
            notes = newNotes;
        }

        if (this.filterCategoryId != null && this.filterCategoryId != Category.NONE_ID) {
            List<Note> newNotes = new ArrayList<>();
            for (Note n : notes) {
                if (n.getCategory() != null && n.getCategory().getId() == this.filterCategoryId) {
                    newNotes.add(n);
                }
            }
            notes = newNotes;
        }

        return notes;
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
            case R.id.toolbar_btn_filter:
                new FilterDialog(this, filterText, filterCategoryId).show(getSupportFragmentManager(), FilterDialog.class.getName());
                return true;
            case R.id.toolbar_btn_delete_all:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.delete_all_notes);
                builder.setPositiveButton(R.string.dialog_yes, (dialog, id) -> {
                    database.removeAllNotes(getExternalFilesDir("GeoNotes"));
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

    @Override
    public void onFilterChanged(String filterText, Long categoryId) {
        this.filterText = filterText;
        this.filterCategoryId = categoryId;
        Log.i(NoteListActivity.class.getName(), "onSave: " + filterText + ", " + categoryId);
        this.load();
    }
}