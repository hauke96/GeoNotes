package de.hauke_stieler.geonotes.map;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;

/**
 * A normal compass rose which can be clicked to reset the rotation overlays rotation (and therefore
 * to reset the maps rotation).
 */
public class ClickableMapCompass extends CompassOverlay {
    private final SnappableRotationOverlay orientationProvider;

    public ClickableMapCompass(Context context, SnappableRotationOverlay orientationProvider, MapView mapView) {
        super(context, orientationProvider, mapView);
        this.orientationProvider = orientationProvider;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
        Point pixelPosition = mapView.getProjection().rotateAndScalePoint((int) e.getX(), (int) e.getY(), null);
        return hitTest(pixelPosition.x, pixelPosition.y);
    }

    /**
     * Checks in a hacky way whether the given pixel position is roughly within the compass rose.
     */
    private boolean hitTest(float x, float y) {
        float xStart = mCompassFrameCenterX - mCompassFrameBitmap.getWidth() / 2f;
        float xEnd = mCompassFrameCenterX + mCompassFrameBitmap.getWidth() / 2f;
        float yStart = mCompassFrameCenterY - mCompassFrameBitmap.getHeight() / 2f;
        float yEnd = mCompassFrameCenterY + mCompassFrameBitmap.getHeight() / 2f;

        if (x >= xStart && x <= xEnd && y >= yStart && y <= yEnd) {
            orientationProvider.resetRotation();
            return true; // prevent further propagation of event to other event listeners
        }
        return false;
    }
}
