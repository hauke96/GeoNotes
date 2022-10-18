package de.hauke_stieler.geonotes.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class BitmapRenderer {
    public static Drawable renderToBitmap(Context context, Drawable... drawables) {
        LayerDrawable layerDrawable = new LayerDrawable(drawables);
        Bitmap bitmap = Bitmap.createBitmap(layerDrawable.getIntrinsicWidth(), layerDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        layerDrawable.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
