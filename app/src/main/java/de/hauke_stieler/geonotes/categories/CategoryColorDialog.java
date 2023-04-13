package de.hauke_stieler.geonotes.categories;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.fragment.app.DialogFragment;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.common.BitmapRenderer;

public class CategoryColorDialog extends DialogFragment {

    public interface CategoryColorChangedListener {
        void onColorChanged(int color);
    }

    private final CategoryColorChangedListener categoryColorChangedListener;
    private final Category category;

    private SeekBar colorSliderHue;
    private SeekBar colorSliderSaturation;
    private SeekBar colorSliderValue;
    private View iconPreview;

    public CategoryColorDialog(CategoryColorChangedListener categoryColorChangedListener, Category category) {
        this.categoryColorChangedListener = categoryColorChangedListener;
        this.category = category;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.category_color_dialog, container);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        iconPreview = view.findViewById(R.id.category_color_dialog_icon_preview);

        initSliderHue(view);
        initSliderSaturation(view);
        initSliderValue(view);

        resetSlider();

        setSliderHueGradient();
        updateSliderSaturationGradient();
        updateSliderValueGradient();

        view.findViewById(R.id.category_color_dialog_btn_reset).setOnClickListener(v -> onResetClicked());
        view.findViewById(R.id.category_color_dialog_btn_ok).setOnClickListener(v -> onOkClicked());

        return view;
    }

    private void initSliderHue(View view) {
        colorSliderHue = view.findViewById(R.id.category_color_dialog_color_slider_hue);
        colorSliderHue.setThumb(ResourcesCompat.getDrawable(view.getResources(), R.drawable.ic_vertical_line, null));
        colorSliderHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSliderSaturationGradient();
                updateSliderValueGradient();
                // TODO Update preview drawable
                updateIconPreview();
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
    }

    private void initSliderSaturation(View view) {
        colorSliderSaturation = view.findViewById(R.id.category_color_dialog_color_slider_sat);
        colorSliderSaturation.setThumb(ResourcesCompat.getDrawable(view.getResources(), R.drawable.ic_vertical_line, null));
        colorSliderSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Update preview drawable
                updateIconPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void initSliderValue(View view) {
        colorSliderValue = view.findViewById(R.id.category_color_dialog_color_slider_val);
        colorSliderValue.setThumb(ResourcesCompat.getDrawable(view.getResources(), R.drawable.ic_vertical_line, null));
        colorSliderValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Update preview drawable
                updateIconPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setSliderHueGradient() {
        GradientDrawable rainbowDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        hsvToColor(0, 1, 1),
                        hsvToColor(60, 1, 1),
                        hsvToColor(120, 1, 1),
                        hsvToColor(180, 1, 1),
                        hsvToColor(240, 1, 1),
                        hsvToColor(300, 1, 1),
                        hsvToColor(360, 1, 1)
                });
        rainbowDrawable.setSize(-1, 2);
        colorSliderHue.setProgressDrawable(rainbowDrawable);
    }

    private void updateSliderSaturationGradient() {
        updateSimpleGradient(colorSliderSaturation,
                hsvToColor(colorSliderHue.getProgress(), 0, colorSliderValue.getProgress()),
                hsvToColor(colorSliderHue.getProgress(), 1, colorSliderValue.getProgress())
        );
    }

    private void updateSliderValueGradient() {
        updateSimpleGradient(colorSliderValue,
                hsvToColor(colorSliderHue.getProgress(), colorSliderSaturation.getProgress(), 0),
                hsvToColor(colorSliderHue.getProgress(), colorSliderSaturation.getProgress(), 1)
        );
    }

    private void updateSimpleGradient(SeekBar seekBar, int colorA, int colorB) {
        int[] colors = {
                colorA,
                colorB
        };
        Drawable progressDrawable = seekBar.getProgressDrawable();

        if (progressDrawable instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) progressDrawable;
            gradientDrawable.setColors(colors);
        } else {
            GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
            gradientDrawable.setSize(-1, 2);
            seekBar.setProgressDrawable(gradientDrawable);
        }
    }

    private void updateIconPreview() {
        // TODO extract this rendering into separate class and reuse it for the real notes.
        Drawable exclamationMarkIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_note_exclamation_mark, null);
        Drawable backgroundOuterNormalIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_note_background, null);

        Drawable backgroundInnerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_note_background_inner, null);
        backgroundInnerIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(getCurrentColor(), BlendModeCompat.SRC_IN));

        Drawable noteIcon = BitmapRenderer.renderToBitmap(getContext(), backgroundOuterNormalIcon, backgroundInnerIcon, exclamationMarkIcon);

        iconPreview.setBackground(noteIcon);
    }

    private void onOkClicked() {
        fireChangeEvent();
        dismiss();
    }

    private void onResetClicked() {
        resetSlider();
        fireChangeEvent();
    }

    private void fireChangeEvent() {
        categoryColorChangedListener.onColorChanged(getCurrentColor());
    }

    private int getCurrentColor() {
        return hsvToColor(colorSliderHue.getProgress(), colorSliderSaturation.getProgress() / 100.0f, colorSliderValue.getProgress() / 100.0f);
    }

    private void resetSlider() {
        float[] hsv = colorToHsv(category.getColor());
        colorSliderHue.setProgress((int) hsv[0]);
        colorSliderSaturation.setProgress((int) (hsv[1] * 100));
        colorSliderValue.setProgress((int) (hsv[2] * 100));
    }

    @NonNull
    private float[] colorToHsv(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }

    private int hsvToColor(float hue, float saturation, float value) {
        float[] hsv = new float[3];
        hsv[0] = hue;
        hsv[1] = saturation;
        hsv[2] = value;
        return Color.HSVToColor(hsv);
    }
}
