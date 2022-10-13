package de.hauke_stieler.geonotes.notes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;

import java.util.HashMap;
import java.util.List;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.database.Database;

public class NoteIconProvider {

    private final Context context;

    private final java.util.Map<Long, Drawable> categoryToNormalIcon;
    private final java.util.Map<Long, Drawable> categoryToCameraIcon;
    private final java.util.Map<Long, Drawable> categoryToNormalIconSelected;
    private final java.util.Map<Long, Drawable> categoryToCameraIconSelected;

    public NoteIconProvider(Context context, Database database) {
        this.context = context;

        categoryToNormalIcon = new HashMap<>();
        categoryToCameraIcon = new HashMap<>();
        categoryToNormalIconSelected = new HashMap<>();
        categoryToCameraIconSelected = new HashMap<>();

        List<Category> allCategories = database.getAllCategories();
        for (int i = 0; i < allCategories.size(); i++) {
            Category category = allCategories.get(i);

            Drawable exclamationMarkIcon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_exclamation_mark, null);
            Drawable cameraForegroundIcon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_camera, null);

            // We render the drawables to a single bitmap because OsmDroid (or Android?) has problem
            // with these LayerDrawables. Parts of these layered drawables just disappear after some
            // time o.O This does not happen to a single pre-rendered bitmap.

            // Normal icons
            Drawable backgroundOuterNormalIcon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_background, null);

            Drawable backgroundInnerIcon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_background_inner, null);
            backgroundInnerIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(category.getColor(), BlendModeCompat.SRC_IN));

            Drawable noteIcon = renderToBitmap(backgroundOuterNormalIcon, backgroundInnerIcon, exclamationMarkIcon);
            Drawable noteWithCameraIcon = renderToBitmap(backgroundOuterNormalIcon, backgroundInnerIcon, cameraForegroundIcon);

            categoryToNormalIcon.put(category.getId(), noteIcon);
            categoryToCameraIcon.put(category.getId(), noteWithCameraIcon);

            // Selection icons
            Drawable backgroundOuterSelectedIcon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_background, null);
            backgroundOuterSelectedIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(0xFF000000, BlendModeCompat.SRC_IN));

            Drawable backgroundInnerSmallIcon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_background_inner_small, null);
            backgroundInnerSmallIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(category.getColor(), BlendModeCompat.SRC_IN));

            Drawable noteSelectedIcon = renderToBitmap(backgroundOuterSelectedIcon, backgroundInnerSmallIcon, exclamationMarkIcon);
            Drawable noteWithCameraSelectedIcon = renderToBitmap(backgroundOuterSelectedIcon, backgroundInnerSmallIcon, cameraForegroundIcon);

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

    private Drawable renderToBitmap(Drawable... drawables) {
        LayerDrawable layerDrawable = new LayerDrawable(drawables);
        Bitmap bitmap = Bitmap.createBitmap(layerDrawable.getIntrinsicWidth(), layerDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        layerDrawable.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
