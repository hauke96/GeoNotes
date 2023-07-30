package de.hauke_stieler.geonotes.notes;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.common.NoteIconRenderer;
import de.hauke_stieler.geonotes.database.Database;

public class NoteIconProvider {

    private java.util.Map<Long, Drawable> categoryToNormalIcon;
    private java.util.Map<Long, Drawable> categoryToCameraIcon;
    private java.util.Map<Long, Drawable> categoryToNormalIconSelected;
    private java.util.Map<Long, Drawable> categoryToCameraIconSelected;

    private final Context context;
    private final Database database;

    public NoteIconProvider(Context context, Database database) {
        this.context = context;
        this.database = database;
        updateIcons();
    }

    public void updateIcons() {
        categoryToNormalIcon = new HashMap<>();
        categoryToCameraIcon = new HashMap<>();
        categoryToNormalIconSelected = new HashMap<>();
        categoryToCameraIconSelected = new HashMap<>();

        List<Category> allCategories = this.database.getAllCategories();
        for (int i = 0; i < allCategories.size(); i++) {
            Category category = allCategories.get(i);

            // We render the drawables to a single bitmap because OsmDroid (or Android?) has problem
            // with these LayerDrawables. Parts of these layered drawables just disappear after some
            // time o.O This does not happen to a single pre-rendered bitmap.
            Drawable noteIcon = NoteIconRenderer.render(context, category.getColor(), R.drawable.ic_note_exclamation_mark, false);
            Drawable noteWithCameraIcon = NoteIconRenderer.render(context, category.getColor(), R.drawable.ic_note_camera, false);

            categoryToNormalIcon.put(category.getId(), noteIcon);
            categoryToCameraIcon.put(category.getId(), noteWithCameraIcon);

            // Selection icons
            Drawable noteSelectedIcon = NoteIconRenderer.render(context, category.getColor(), R.drawable.ic_note_exclamation_mark, true);
            Drawable noteWithCameraSelectedIcon = NoteIconRenderer.render(context, category.getColor(), R.drawable.ic_note_camera, true);

            categoryToNormalIconSelected.put(category.getId(), noteSelectedIcon);
            categoryToCameraIconSelected.put(category.getId(), noteWithCameraSelectedIcon);
        }
    }

    public Drawable getIcon(long categoryId, boolean isSelected, boolean isPhotoNote) {
        if (isSelected) {
            if (isPhotoNote) {
                return categoryToCameraIconSelected.get(categoryId);
            } else {
                return categoryToNormalIconSelected.get(categoryId);
            }
        } else {
            if (isPhotoNote) {
                return categoryToCameraIcon.get(categoryId);
            } else {
                return categoryToNormalIcon.get(categoryId);
            }
        }
    }
}
