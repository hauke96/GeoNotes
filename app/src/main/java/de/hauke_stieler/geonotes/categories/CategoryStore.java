package de.hauke_stieler.geonotes.categories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.R;

public class CategoryStore {
    private static final String CATEGORIES_TABLE_NAME = "categories";
    private static final String CATEGORIES_COL_ID = "id";
    private static final String CATEGORIES_COL_COLOR = "color";
    private static final String CATEGORIES_COL_NAME = "name";

    public void onCreate(SQLiteDatabase db, final Context context) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s(%s INTEGER PRIMARY KEY, %s VARCHAR NOT NULL, %s VARCHAR NOT NULL);",
                CATEGORIES_TABLE_NAME,
                CATEGORIES_COL_ID,
                CATEGORIES_COL_COLOR,
                CATEGORIES_COL_NAME));
        addInitialCategories(db, context);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, final Context context) {
        if (oldVersion < 6) {
            onCreate(db, context);
        }

        Log.i("CategoryStore", String.format("onUpgrade: from version %d to version %d", oldVersion, newVersion));
    }

    private void addInitialCategories(SQLiteDatabase db, final Context context) {
        addCategory(db, "#f44336", context.getString(R.string.initial_color_red));
        addCategory(db, "#e91e63", context.getString(R.string.initial_color_pink));
        addCategory(db, "#9c27b0", context.getString(R.string.initial_color_purple));
        addCategory(db, "#3f51b5", context.getString(R.string.initial_color_blue));
        addCategory(db, "#03a9f4", context.getString(R.string.initial_color_light_blue));
        addCategory(db, "#009688", context.getString(R.string.initial_color_teal));
        addCategory(db, "#4caf50", context.getString(R.string.initial_color_green));
        addCategory(db, "#fdd835", context.getString(R.string.initial_color_yellow));
        addCategory(db, "#ff9800", context.getString(R.string.initial_color_orange));
        addCategory(db, "#795548", context.getString(R.string.initial_color_brown));
        addCategory(db, "#9e9e9e", context.getString(R.string.initial_color_grey));
    }

    public long addCategory(SQLiteDatabase db, String color, String name) {
        ContentValues values = new ContentValues();
        values.put(CATEGORIES_COL_COLOR, color);
        values.put(CATEGORIES_COL_NAME, name);

        return db.insert(CATEGORIES_TABLE_NAME, null, values);
    }

    public Category getCategory(SQLiteDatabase db, String id) {
        Cursor cursor = db.query(CATEGORIES_TABLE_NAME, new String[]{CATEGORIES_COL_ID, CATEGORIES_COL_COLOR, CATEGORIES_COL_NAME}, CATEGORIES_COL_ID + "=?", new String[]{id}, null, null, null);
        cursor.moveToFirst();
        return getCategoryFromCursor(cursor);
    }

    public List<Category> getAllCategories(SQLiteDatabase db) {
        Cursor cursor = db.query(CATEGORIES_TABLE_NAME, new String[]{CATEGORIES_COL_ID, CATEGORIES_COL_COLOR, CATEGORIES_COL_NAME}, null, null, null, null, null);

        List<Category> categories = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                categories.add(getCategoryFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        return categories;
    }

    private Category getCategoryFromCursor(Cursor cursor) {
        return new Category(cursor.getLong(0), cursor.getString(1), cursor.getString(2));
    }

    public void update(SQLiteDatabase db, long id, String newName, String newColor) {
        ContentValues values = new ContentValues();
        values.put(CATEGORIES_COL_ID, id);
        values.put(CATEGORIES_COL_NAME, newName);
        values.put(CATEGORIES_COL_COLOR, newColor);

        db.update(CATEGORIES_TABLE_NAME, values, CATEGORIES_COL_ID + " = ?", new String[]{"" + id});
    }
}
