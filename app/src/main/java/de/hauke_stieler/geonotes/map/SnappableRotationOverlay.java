package de.hauke_stieler.geonotes.map;

import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureDetector;

public class SnappableRotationOverlay extends Overlay implements
        RotationGestureDetector.RotationListener, IOrientationProvider {

    private final RotationGestureDetector mRotationDetector;
    private final MapView map;
    private IOrientationConsumer orientationConsumer;

    private final long deltaTime = 25L;
    private long timeLastSet = 0L;
    private float currentAngle = 0f;

    private final float minSnapAngle = 15; // Amount of rotation before map actually rotates
    private float currentSnapAngle = 0f; // Angle rotated since start of interaction
    private boolean rotationSnapped = false; // True when the user first reached the minSnapAngle

    public SnappableRotationOverlay(MapView mapView) {
        super();
        map = mapView;
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
        if (!isEnabled() || orientationConsumer == null) {
            return;
        }

        currentAngle += deltaAngle;
        currentSnapAngle += deltaAngle;

        if (!rotationSnapped && Math.abs(currentSnapAngle) > minSnapAngle) {
            rotationSnapped = true;
        }

        if (System.currentTimeMillis() - deltaTime > timeLastSet && rotationSnapped) {
            timeLastSet = System.currentTimeMillis();
            map.setMapOrientation(map.getMapOrientation() + currentAngle);
            orientationConsumer.onOrientationChanged(map.getMapOrientation(), this);
        }
    }

    @Override
    public boolean startOrientationProvider(IOrientationConsumer orientationConsumer) {
        this.orientationConsumer = orientationConsumer;
        orientationConsumer.onOrientationChanged(currentAngle, this);
        return true;
    }

    @Override
    public void stopOrientationProvider() {
        orientationConsumer = null;
    }

    @Override
    public float getLastKnownOrientation() {
        return currentAngle;
    }

    @Override
    public void destroy() {
        stopOrientationProvider();
    }

    public void resetRotation() {
        currentAngle = 0;
        rotationSnapped = false;
        currentSnapAngle = 0f;

        map.setMapOrientation(currentAngle);
        if (orientationConsumer != null) {
            orientationConsumer.onOrientationChanged(currentAngle, this);
        }
    }
}
