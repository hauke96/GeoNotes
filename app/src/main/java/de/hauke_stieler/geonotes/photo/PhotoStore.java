package de.hauke_stieler.geonotes.photo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoStore {
    private static final String PHOTOS_TABLE_NAME = "photos";
    private static final String PHOTOS_COL_ID = "id";
    private static final String PHOTOS_COL_NOTE_ID = "note";
    private static final String PHOTOS_COL_FILE_NAME = "path";

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s(%s INTEGER PRIMARY KEY, %s INTEGER NOT NULL, %s VARCHAR NOT NULL);",
                PHOTOS_TABLE_NAME,
                PHOTOS_COL_ID,
                PHOTOS_COL_NOTE_ID,
                PHOTOS_COL_FILE_NAME));
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            onCreate(db);
        }
        Log.i("PhotoStore", String.format("onUpgrade: from version %d to version %d", oldVersion, newVersion));
    }

    public void addPhoto(SQLiteDatabase db, Long noteId, File photoFile) {
        ContentValues values = new ContentValues();
        values.put(PHOTOS_COL_NOTE_ID, noteId);
        values.put(PHOTOS_COL_FILE_NAME, photoFile.getName());

        db.insert(PHOTOS_TABLE_NAME, null, values);
    }

    public List<String> getPhotos(SQLiteDatabase db, String noteId) {
        Cursor cursor = db.query(PHOTOS_TABLE_NAME, new String[]{PHOTOS_COL_NOTE_ID, PHOTOS_COL_FILE_NAME}, PHOTOS_COL_NOTE_ID + "=?", new String[]{noteId}, null, null, null);

        List<String> photos = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                photos.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }

        return photos;
    }

    public void removePhotos(SQLiteDatabase db, long noteId) {
        db.delete(PHOTOS_TABLE_NAME, PHOTOS_COL_NOTE_ID + " = ?", new String[]{"" + noteId});
    }
}
