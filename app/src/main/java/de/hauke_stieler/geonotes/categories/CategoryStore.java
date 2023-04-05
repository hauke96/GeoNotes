package de.hauke_stieler.geonotes.categories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.notes.Note;

public class CategoryStore {
    private static final String CATEGORIES_TABLE_NAME = "categories";
    private static final String CATEGORIES_COL_ID = "id";
    private static final String CATEGORIES_COL_COLOR = "color";
    private static final String CATEGORIES_COL_NAME = "name";
    private static final String CATEGORIES_COL_SORT_KEY = "sort_key";

    public void onCreate(SQLiteDatabase db, final Context context) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s(%s INTEGER PRIMARY KEY, %s VARCHAR NOT NULL, %s VARCHAR NOT NULL, %s INTEGER);",
                CATEGORIES_TABLE_NAME,
                CATEGORIES_COL_ID,
                CATEGORIES_COL_COLOR,
                CATEGORIES_COL_NAME,
                CATEGORIES_COL_SORT_KEY));
        addInitialCategories(db, context);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, final Context context) {
        if (oldVersion < 6) {
            onCreate(db, context);
        }
        if (oldVersion < 7) {
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT 0", CATEGORIES_TABLE_NAME, CATEGORIES_COL_SORT_KEY));
        }

        Log.i("CategoryStore", String.format("onUpgrade: from version %d to version %d", oldVersion, newVersion));
    }

    private void addInitialCategories(SQLiteDatabase db, final Context context) {
        addCategory(db, "#f44336", context.getString(R.string.initial_color_red), 0);
        addCategory(db, "#e91e63", context.getString(R.string.initial_color_pink), 1);
        addCategory(db, "#9c27b0", context.getString(R.string.initial_color_purple), 2);
        addCategory(db, "#3f51b5", context.getString(R.string.initial_color_blue), 3);
        addCategory(db, "#03a9f4", context.getString(R.string.initial_color_light_blue), 4);
        addCategory(db, "#009688", context.getString(R.string.initial_color_teal), 5);
        addCategory(db, "#4caf50", context.getString(R.string.initial_color_green), 6);
        addCategory(db, "#fdd835", context.getString(R.string.initial_color_yellow), 7);
        addCategory(db, "#ff9800", context.getString(R.string.initial_color_orange), 8);
        addCategory(db, "#795548", context.getString(R.string.initial_color_brown), 9);
        addCategory(db, "#9e9e9e", context.getString(R.string.initial_color_grey), 10);
    }

    public long addCategory(SQLiteDatabase db, String color, String name, long sortKey) {
        ContentValues values = new ContentValues();
        values.put(CATEGORIES_COL_COLOR, color);
        values.put(CATEGORIES_COL_NAME, name);
        values.put(CATEGORIES_COL_SORT_KEY, sortKey);

        return db.insert(CATEGORIES_TABLE_NAME, null, values);
    }

    public Category getCategory(SQLiteDatabase db, String id) {
        Cursor cursor = db.query(CATEGORIES_TABLE_NAME, new String[]{CATEGORIES_COL_ID, CATEGORIES_COL_COLOR, CATEGORIES_COL_NAME, CATEGORIES_COL_SORT_KEY}, CATEGORIES_COL_ID + "=?", new String[]{id}, null, null, null);
        cursor.moveToFirst();
        return getCategoryFromCursor(cursor);
    }

    public List<Category> getAllCategories(SQLiteDatabase db) {
        Cursor cursor = db.query(CATEGORIES_TABLE_NAME, new String[]{CATEGORIES_COL_ID, CATEGORIES_COL_COLOR, CATEGORIES_COL_NAME, CATEGORIES_COL_SORT_KEY}, null, null, null, null, null);

        List<Category> categories = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                categories.add(getCategoryFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        Collections.sort(categories, (c1, c2) -> (int) (c1.getSortKey() - c2.getSortKey()));
        return categories;
    }

    private Category getCategoryFromCursor(Cursor cursor) {
        return new Category(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3));
    }

    public void update(SQLiteDatabase db, long id, String newName, String newColor, long sortKey) {
        ContentValues values = new ContentValues();
        values.put(CATEGORIES_COL_ID, id);
        values.put(CATEGORIES_COL_NAME, newName);
        values.put(CATEGORIES_COL_COLOR, newColor);
        values.put(CATEGORIES_COL_SORT_KEY, sortKey);

        db.update(CATEGORIES_TABLE_NAME, values, CATEGORIES_COL_ID + " = ?", new String[]{"" + id});
    }

    public void removeCategory(SQLiteDatabase db, long id) {
        db.delete(CATEGORIES_TABLE_NAME, CATEGORIES_COL_ID + " = ?", new String[]{"" + id});
    }
}
