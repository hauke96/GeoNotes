package de.hauke_stieler.geonotes.categories;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import de.hauke_stieler.geonotes.R;

public class CategoryColorDialog extends DialogFragment {
    public interface CategoryColorChangedListener {
        void onColorChanged(int color);
    }

    private final CategoryColorChangedListener categoryColorChangedListener;
    private final Category category;

    private SeekBar colorSlider;
    private int currentColor;

    public CategoryColorDialog(CategoryColorChangedListener categoryColorChangedListener, Category category) {
        this.categoryColorChangedListener = categoryColorChangedListener;
        this.category = category;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.category_color_dialog, container);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        GradientDrawable rainbowDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        hueToColor(0),
                        hueToColor(60),
                        hueToColor(120),
                        hueToColor(180),
                        hueToColor(240),
                        hueToColor(300),
                        hueToColor(360)});
        rainbowDrawable.setSize(-1, 2);

        colorSlider = view.findViewById(R.id.category_color_dialog_color_slider);
        resetSlider();
        colorSlider.setProgressDrawable(rainbowDrawable);
        colorSlider.setThumb(ResourcesCompat.getDrawable(view.getResources(), R.drawable.ic_vertical_line, null));
        colorSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentColor = hueToColor(progress);
                // TODO Update preview drawable
//                if (innerLayout.getBackground() instanceof GradientDrawable) {
//                    GradientDrawable background = (GradientDrawable) innerLayout.getBackground();
//                    background.mutate();
//                    background.setColor(currentColor);
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        view.findViewById(R.id.category_color_dialog_btn_reset).setOnClickListener(v -> onResetClicked());
        view.findViewById(R.id.category_color_dialog_btn_ok).setOnClickListener(v -> onOkClicked());

        return view;
    }

    private void onOkClicked() {
        fireChangeEvent();
        dismiss();
    }

    private void fireChangeEvent() {
        categoryColorChangedListener.onColorChanged(currentColor);
    }

    private void onResetClicked() {
        resetSlider();
        fireChangeEvent();
    }

    private void resetSlider() {
        float[] colorToHSV = new float[3];
        Color.colorToHSV(category.getColor(), colorToHSV);
        colorSlider.setProgress((int) colorToHSV[0]);
    }

    private int hueToColor(int progress) {
        float[] hsv = new float[3];
        hsv[0] = progress;
        hsv[1] = 0.8f;
        hsv[2] = 0.95f;
        int color = Color.HSVToColor(hsv);
        return color;
    }
}
