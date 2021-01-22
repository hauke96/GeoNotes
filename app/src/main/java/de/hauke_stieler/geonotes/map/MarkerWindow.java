package de.hauke_stieler.geonotes.map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class MarkerWindow extends InfoWindow {
    public interface MarkerEventHandler {
        void onDelete(Marker marker);

        void onSave(Marker marker);

        void onMove(Marker marker);
    }

    private MarkerEventHandler markerEventHandler;

    private Marker selectedMarker;

    /**
     * resource id value meaning "undefined resource id"
     */
    public static final int UNDEFINED_RES_ID = 0;

    static int mTitleId = UNDEFINED_RES_ID,
            mDescriptionId = UNDEFINED_RES_ID,
            mDeleteButtonId = UNDEFINED_RES_ID,
            mSaveButtonId = UNDEFINED_RES_ID,
            mMoveButtonId = UNDEFINED_RES_ID,
            mSubDescriptionId = UNDEFINED_RES_ID,
            mImageId = UNDEFINED_RES_ID;

    public MarkerWindow(int layoutResId, MapView mapView, MarkerEventHandler markerEventHandler) {
        super(layoutResId, mapView);

        this.markerEventHandler = markerEventHandler;

        if (mTitleId == UNDEFINED_RES_ID)
            setResIds(mapView.getContext());

        //default behavior: close it when clicking on the bubble:
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP)
                    close();
                return true;
            }
        });

        // Show/hide keyboard on edit field focus
        EditText descriptionView = mView.findViewById(mDescriptionId /*R.id.description*/);
        if (descriptionView != null) {
            descriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    InputMethodManager inputMethodManager = (InputMethodManager) mView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (hasFocus) {
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                    } else {
                        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    }
                }
            });

            descriptionView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    selectedMarker.setSnippet(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private static void setResIds(Context context) {
        String packageName = context.getPackageName(); //get application package name
        mTitleId = context.getResources().getIdentifier("id/bubble_title", null, packageName);
        mDescriptionId = context.getResources().getIdentifier("id/bubble_description", null, packageName);
        mDeleteButtonId = context.getResources().getIdentifier("id/delete_button", null, packageName);
        mSaveButtonId = context.getResources().getIdentifier("id/save_button", null, packageName);
        mMoveButtonId = context.getResources().getIdentifier("id/move_button", null, packageName);
        mSubDescriptionId = context.getResources().getIdentifier("id/bubble_subdescription", null, packageName);
        mImageId = context.getResources().getIdentifier("id/bubble_image", null, packageName);
        if (mTitleId == UNDEFINED_RES_ID || mDescriptionId == UNDEFINED_RES_ID
                || mSubDescriptionId == UNDEFINED_RES_ID || mImageId == UNDEFINED_RES_ID) {
            Log.e(IMapView.LOGTAG, "BasicInfoWindow: unable to get res ids in " + packageName);
        }
    }

    @Override
    public void onOpen(Object item) {
        if (mView == null) {
            Log.w(IMapView.LOGTAG, "Error trapped, BasicInfoWindow.open, mView is null!");
            return;
        }
        if (!(item instanceof Marker)) {
            Log.e(IMapView.LOGTAG, "Opened item is not a marker!");
            return;
        }

        Marker marker = (Marker) item;
        selectedMarker = marker;

        // Title
        TextView titleView = mView.findViewById(mTitleId /*R.id.title*/);
        String title = marker.getTitle();
        if (title != null && titleView != null) {
            titleView.setText(title);
        }

        // Description / Snippet
        String snippet = marker.getSnippet();
        if (snippet == null) {
            snippet = "";
        }
        Spanned snippetHtml = Html.fromHtml(snippet);
        EditText descriptionView = mView.findViewById(mDescriptionId /*R.id.description*/);
        if (descriptionView != null) {
            descriptionView.setText(snippetHtml);
        }

        Button deleteButton = mView.findViewById(mDeleteButtonId /* R.id.delete_button */);
        deleteButton.setOnClickListener(v -> {
            markerEventHandler.onDelete(marker);
            close();
        });

        Button saveButton = mView.findViewById(mSaveButtonId /* R.id.save_button */);
        saveButton.setOnClickListener(v -> {
            markerEventHandler.onSave(marker);
            close();
        });

        Button moveButton = mView.findViewById(mMoveButtonId /* R.id.save_button */);
        moveButton.setOnClickListener(v -> {
            markerEventHandler.onMove(marker);
            close();
        });
    }

    @Override
    public void onClose() {
        this.selectedMarker = null;
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    public void focusEditField() {
        EditText descriptionView = mView.findViewById(mDescriptionId /*R.id.description*/);
        descriptionView.requestFocus();
    }
}
