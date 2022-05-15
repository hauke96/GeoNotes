package de.hauke_stieler.geonotes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.util.List;

import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.categories.CategoryStore;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.notes.NoteStore;
import de.hauke_stieler.geonotes.photo.PhotoStore;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class Database extends SQLiteOpenHelper {
    private static final int DB_VERSION = 6;
    private static final String DB_NAME = "geonotes";

    private final NoteStore noteStore;
    private final PhotoStore photoStore;
    private final CategoryStore categoryStore;

    public Database(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

        categoryStore = new CategoryStore();
        noteStore = new NoteStore(categoryStore);
        photoStore = new PhotoStore();

        // This will call the onCreate and onUpgrade methods.
        getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        noteStore.onCreate(db);
        photoStore.onCreate(db);
        categoryStore.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Ordered by reference relation: Photo references notes, notes references categories.
        categoryStore.onUpgrade(db, oldVersion, newVersion);
        noteStore.onUpgrade(db, oldVersion, newVersion);
        photoStore.onUpgrade(db, oldVersion, newVersion);
    }

    public long addNote(String description, double lat, double lon, long categoryId) {
        return noteStore.addNote(getWritableDatabase(), description, lat, lon, categoryId);
    }

    public void updateDescription(long id, String newDescription) {
        noteStore.updateDescription(getWritableDatabase(), id, newDescription);
    }

    public void updateLocation(long id, GeoPoint location) {
        noteStore.updateLocation(getWritableDatabase(), id, location);
    }

    public void removeNote(long id) {
        noteStore.removeNote(getWritableDatabase(), id);
    }

    public void removeAllNotes() {
        noteStore.removeAllNotes(getWritableDatabase());
    }

    public List<Note> getAllNotes() {
        return noteStore.getAllNotes(getWritableDatabase());
    }

    public void addPhoto(Long noteId, File photoFile) {
        photoStore.addPhoto(getWritableDatabase(), noteId, photoFile);
    }

    public List<String> getPhotos(String noteId) {
        return photoStore.getPhotos(getReadableDatabase(), noteId);
    }

    public boolean hasPhotos(long noteId) {
        return hasPhotos("" + noteId);
    }

    public boolean hasPhotos(String noteId) {
        return getPhotos(noteId).size() > 0;
    }

    public void removePhotos(long noteId, File storageDir) {
        List<String> photos = getPhotos("" + noteId);

        photoStore.removePhotos(getWritableDatabase(), noteId);

        for (String photo : photos) {
            File photoFile = new File(storageDir, photo);
            File thumbnailFile = ThumbnailUtil.getThumbnailFile(photoFile);

            photoFile.delete();
            thumbnailFile.delete();
        }
    }

    public Note getNote(String noteId) {
        return noteStore.getNote(getReadableDatabase(), noteId);
    }


    public long addCategory(String color, String name) {
        return categoryStore.addCategory(getWritableDatabase(), color, name);
    }

    public Category getCategory(String id) {
        return categoryStore.getCategory(getReadableDatabase(), id);
    }

    public List<Category> getAllCategories() {
        return categoryStore.getAllCategories(getReadableDatabase());
    }
}
