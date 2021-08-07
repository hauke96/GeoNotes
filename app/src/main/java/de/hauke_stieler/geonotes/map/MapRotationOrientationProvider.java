package de.hauke_stieler.geonotes.map;

import android.annotation.SuppressLint;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;

public class MapRotationOrientationProvider implements IOrientationProvider {

    private final MapView map;
    private IOrientationConsumer orientationConsumer;

    public MapRotationOrientationProvider(MapView map) {
        this.map = map;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean startOrientationProvider(IOrientationConsumer orientationConsumer) {
        this.orientationConsumer = orientationConsumer;
        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                orientationConsumer.onOrientationChanged(map.getMapOrientation(), this);
            }

            return false;
        });

        return true;
    }

    @Override
    public void stopOrientationProvider() {
        orientationConsumer = null;
    }

    @Override
    public float getLastKnownOrientation() {
        return map.getMapOrientation();
    }

    @Override
    public void destroy() {
        stopOrientationProvider();
    }
}