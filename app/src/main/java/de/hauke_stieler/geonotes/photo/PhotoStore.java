package de.hauke_stieler.geonotes.photo;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PhotoStore {
    private static final String PHOTOS_TABLE_NAME = "photos";
    private static final String PHOTOS_COL_ID = "id";
    private static final String PHOTOS_COL_NOTE_ID = "note";
    private static final String PHOTOS_COL_PATH = "path";

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s(%s INTEGER PRIMARY KEY, %s INTEGER NOT NULL, %s VARCHAR NOT NULL);",
                PHOTOS_TABLE_NAME,
                PHOTOS_COL_ID,
                PHOTOS_COL_NOTE_ID,
                PHOTOS_COL_PATH));
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("NoteStore", String.format("onUpgrade: from version %d to version %d", oldVersion, newVersion));
    }
}
