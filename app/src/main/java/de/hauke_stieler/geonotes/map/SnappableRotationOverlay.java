package de.hauke_stieler.geonotes.map;

import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureDetector;

/**
 * An overlay to rotate the map. The rotation however is not directly visible but after a certain
 * amount of rotation the map starts to rotate as well. This prevent unwanted rotation while zooming.
 */
public class SnappableRotationOverlay extends Overlay implements
        RotationGestureDetector.RotationListener, IOrientationProvider {

    /**
     * A listener reacting to rotation events of this overlay.
     */
    public interface RotationActionListener {
        /**
         * Gets called whenever a rotation action ends.
         * <p>
         * Usually this happens when a user rotates the map with two fingers and then lifts one up.
         * The rotation action then ends and this event is called.
         *
         * @param angle The current angle/orientation/rotation in degree of the map.
         */
        void onRotationEnd(float angle);
    }

    private final RotationGestureDetector mRotationDetector;
    private final MapView map;
    private IOrientationConsumer orientationConsumer;
    private RotationActionListener rotationActionListener;

    private final long rotationUpdateDelay = 25L; // in ms
    private long lastMapRotation = 0L;
    private float currentAngle = 0f;
    private float currentMotionAngle = 0f; // Angle rotated since start of interaction/motion

    /**
     * Amount of rotation before map actually rotates. Before this amount of rotation the map is
     * "locked" and therefore doesn't rotate. This prevent unwanted rotation while zooming.
     */
    private final float rotationLockMinAngle = 15;
    /**
     * True when the user started rotating and hasn't yet reached the "rotationLockMinAngle".
     * <p>
     * False when the user rotated at least "rotationLockMinAngle", the rotation-lock has now been
     * released/deactivated.
     */
    private boolean rotationLocked = true;

    public SnappableRotationOverlay(MapView mapView) {
        super();
        map = mapView;
        mRotationDetector = new RotationGestureDetector(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
        if (event.getPointerCount() < 2) {
            rotationLocked = true;
            currentMotionAngle = 0f;
        }
        if (event.getPointerCount() == 2 && rotationActionListener != null) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_CANCEL ||
                    action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_POINTER_UP ||
                    // action == MotionEvent.ACTION_POINTER_1_UP || <-- Same as ACTION_POINTER_UP duh!
                    action == MotionEvent.ACTION_POINTER_2_UP ||
                    action == MotionEvent.ACTION_POINTER_3_UP) {
                rotationActionListener.onRotationEnd(map.getMapOrientation());
            }
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
        currentMotionAngle += deltaAngle;

        boolean rotationPassedLockMinValue = Math.abs(currentMotionAngle) > rotationLockMinAngle;
        if (rotationLocked && rotationPassedLockMinValue) {
            rotationLocked = false;
        }

        long now = System.currentTimeMillis();
        boolean passedMapRotationDelay = now - rotationUpdateDelay > this.lastMapRotation;
        if (passedMapRotationDelay && !rotationLocked) {
            this.lastMapRotation = now;
            map.setMapOrientation(map.getMapOrientation() + currentMotionAngle);
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

    /**
     * If the control will be enabled: The rotation will be set to "angle".
     * <p>
     * If the control will be disabled: The rotation will be reset.
     */
    public void setEnabledAndRotation(boolean enabled, float angle) {
        if (!enabled) {
            resetRotation();
            rotationActionListener.onRotationEnd(map.getMapOrientation());
        } else {
            setRotation(angle);
        }

        setEnabled(enabled);
    }

    public void resetRotation() {
        setRotation(0);
    }

    private void setRotation(float newAngle) {
        currentAngle = newAngle;
        rotationLocked = true;
        currentMotionAngle = 0;

        map.setMapOrientation(currentAngle);
        if (orientationConsumer != null) {
            orientationConsumer.onOrientationChanged(currentAngle, this);
        }
    }

    public void setRotationActionListener(RotationActionListener rotationActionListener) {
        this.rotationActionListener = rotationActionListener;
    }
}
