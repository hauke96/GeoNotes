package de.hauke_stieler.geonotes.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.categories.CategoryStore;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.notes.NoteStore;
import de.hauke_stieler.geonotes.photo.PhotoStore;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class Database extends SQLiteOpenHelper {
    private static final int DB_VERSION = 7;
    private static final String DB_NAME = "geonotes";

    private final NoteStore noteStore;
    private final PhotoStore photoStore;
    private final CategoryStore categoryStore;
    private final Context context;

    public Database(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

        categoryStore = new CategoryStore();
        noteStore = new NoteStore(categoryStore);
        photoStore = new PhotoStore();
        this.context = context;

        // This will call the onCreate and onUpgrade methods.
        getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        noteStore.onCreate(db);
        photoStore.onCreate(db);
        categoryStore.onCreate(db, context);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Ordered by reference relation: Photo references notes, notes references categories.
        categoryStore.onUpgrade(db, oldVersion, newVersion, context);
        noteStore.onUpgrade(db, oldVersion, newVersion);
        photoStore.onUpgrade(db, oldVersion, newVersion);
    }

    public long addNote(String description, double lat, double lon, long categoryId) {
        return noteStore.addNote(getWritableDatabase(), description, lat, lon, categoryId);
    }

    public long addNote(String description, double lat, double lon, long categoryId, String createdAt) {
        return noteStore.addNote(getWritableDatabase(), description, lat, lon, categoryId, createdAt);
    }

    public void updateNoteDescription(long noteId, String newDescription) {
        noteStore.updateDescription(getWritableDatabase(), noteId, newDescription);
    }

    public void updateNoteCategory(long noteId, long categoryId) {
        noteStore.updateCategory(getWritableDatabase(), noteId, categoryId);
    }

    public void updateNoteLocation(long noteId, double lat, double lng) {
        noteStore.updateLocation(getWritableDatabase(), noteId, lat, lng);
    }

    public void removeNote(long id) {
        noteStore.removeNote(getWritableDatabase(), id);
    }

    public void removeAllNotes(File storageDir, String textFilter, Long categoryIdFilter) {
        List<Note> notesToRemove = noteStore.getAllNotes(getReadableDatabase(), textFilter, categoryIdFilter);

        for (Note note : notesToRemove) {
            removePhotos(note.getId(), storageDir);
            noteStore.removeNote(getWritableDatabase(), note.getId());
        }
    }

    public void removeAllNotes(File storageDir) {
        List<Note> notesToRemove = noteStore.getAllNotes(getReadableDatabase());

        for (Note note : notesToRemove) {
            removePhotos(note.getId(), storageDir);
            noteStore.removeNote(getWritableDatabase(), note.getId());
        }
    }

    public List<Note> getAllNotes() {
        return noteStore.getAllNotes(getWritableDatabase());
    }

    public List<Note> getAllNotes(String textFilter, Long categoryIdFilter) {
        return noteStore.getAllNotes(getWritableDatabase(), textFilter, categoryIdFilter);
    }

    public void addPhoto(Long noteId, File photoFile) {
        photoStore.addPhoto(getWritableDatabase(), noteId, photoFile);
    }

    public List<String> getPhotos(Long noteId) {
        return photoStore.getPhotos(getReadableDatabase(), noteId);
    }

    public Map<Long, List<String>> getAllPhotosMap() {
        HashMap<Long, List<String>> result = new HashMap<>();
        getAllNotes().forEach(n -> {
            List<String> photos = getPhotos(n.getId());
            result.put(n.getId(), photos);
        });
        return result;
    }

    public boolean hasPhotos(long noteId) {
        return !getPhotos(noteId).isEmpty();
    }

    public void removePhotos(long noteId, File storageDir) {
        List<String> photos = getPhotos(noteId);

        photoStore.removePhotos(getWritableDatabase(), noteId);

        for (String photo : photos) {
            File photoFile = new File(storageDir, photo);
            boolean deleted = photoFile.delete();
            if (deleted) {
                ThumbnailUtil.deleteThumbnail(photoFile);
            } else {
                Log.e("database", "Could not delete photo file " + photoFile.getAbsolutePath());
            }
        }
    }

    public Note getNote(long noteId) {
        return noteStore.getNote(getReadableDatabase(), noteId);
    }

    public long addCategory(String color, String name, long sortKey) {
        return categoryStore.addCategory(getWritableDatabase(), color, name, sortKey);
    }

    public List<Category> getAllCategories() {
        List<Category> categories = categoryStore.getAllCategories(getReadableDatabase());
        List<Note> allNotes = getAllNotes();
        for (Note note : allNotes) {
            for (Category category : categories) {
                // These are actually different instanced of the category, so we can't reuse the
                // category instances from the notes.
                if (category.getId() == note.getCategory().getId()) {
                    category.setHasNotes(true);
                }
            }
        }
        return categories;
    }

    public void updateCategory(long id, String newName, String newColor, long sortKey) {
        categoryStore.update(getWritableDatabase(), id, newName, newColor, sortKey);
    }

    public void removeCategory(SharedPreferences preferences, long id) {
        categoryStore.removeCategory(getWritableDatabase(), id);

        String preferenceKey = this.context.getString(R.string.pref_last_category_id);
        if (preferences.contains(preferenceKey) && preferences.getLong(preferenceKey, -1) == id) {
            // The currently stored category has been removed from the database.
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(preferenceKey, getAllCategories().get(0).getId());
            editor.commit();
        }
    }

    public void removeAllCategories() {
        categoryStore.removeAllCategories(getWritableDatabase());
    }
}
