package de.hauke_stieler.geonotes.map;

import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.gestures.RotationGestureDetector;

public class SnappableRotationOverlay extends Overlay implements
        RotationGestureDetector.RotationListener {

    private final RotationGestureDetector mRotationDetector;
    private final MapView mMapView;

    private final long deltaTime = 25L;
    private long timeLastSet = 0L;
    private float currentAngle = 0f;

    private final float minSnapAngle = 15; // Amount of rotation before map actually rotates
    private float currentSnapAngle = 0f; // Angle rotated since start of interaction
    private boolean rotationSnapped = false; // True when the user first reached the minSnapAngle

    public SnappableRotationOverlay(MapView mapView) {
        super();
        mMapView = mapView;
        mRotationDetector = new RotationGestureDetector(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
        if (event.getPointerCount() < 2) {
            rotationSnapped = false;
            currentSnapAngle = 0f;
        }
        mRotationDetector.onTouch(event);
        return super.onTouchEvent(event, mapView);
    }

    @Override
    public void onRotate(float deltaAngle) {
        currentAngle += deltaAngle;
        currentSnapAngle += deltaAngle;

        if (!rotationSnapped && Math.abs(currentSnapAngle) > minSnapAngle) {
            rotationSnapped = true;
        }

        if (System.currentTimeMillis() - deltaTime > timeLastSet && rotationSnapped) {
            timeLastSet = System.currentTimeMillis();
            mMapView.setMapOrientation(mMapView.getMapOrientation() + currentAngle);
        }
    }
}
