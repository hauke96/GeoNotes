package de.hauke_stieler.geonotes.common;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;

import de.hauke_stieler.geonotes.R;

public class NoteIconRenderer {
    @NonNull
    public static Drawable render(Context context, int color, int noteForegroundDrawable, boolean selected) {
        Drawable backgroundDrawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_background, null);
        Drawable foregroundDrawable = ResourcesCompat.getDrawable(context.getResources(), noteForegroundDrawable, null);
        Drawable backgroundInnerDrawable;

        if (selected) {
            backgroundDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(0xFF000000, BlendModeCompat.SRC_IN));
            backgroundInnerDrawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_background_inner_small, null);
        } else {
            backgroundDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(0xFFFFFFFF, BlendModeCompat.SRC_IN));
            backgroundInnerDrawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_note_background_inner, null);
        }

        backgroundInnerDrawable.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN));

        Drawable noteIcon = BitmapRenderer.renderToBitmap(context, backgroundDrawable, backgroundInnerDrawable, foregroundDrawable);
        return noteIcon;
    }
}
