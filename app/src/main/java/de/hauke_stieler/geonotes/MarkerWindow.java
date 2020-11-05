package de.hauke_stieler.geonotes;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class MarkerWindow extends InfoWindow {
    public interface MarkerEventHandler {
        void onDelete(Marker marker);
        void onSave(Marker marker);
    }

    private MarkerEventHandler markerEventHandler;

    /**
     * resource id value meaning "undefined resource id"
     */
    public static final int UNDEFINED_RES_ID = 0;

    static int mTitleId = UNDEFINED_RES_ID,
            mDescriptionId = UNDEFINED_RES_ID,
            mDeleteButtonId = UNDEFINED_RES_ID,
            mSaveButtonId = UNDEFINED_RES_ID,
            mSubDescriptionId = UNDEFINED_RES_ID,
            mImageId = UNDEFINED_RES_ID; //resource ids

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
    }

    private static void setResIds(Context context) {
        String packageName = context.getPackageName(); //get application package name
        mTitleId = context.getResources().getIdentifier("id/bubble_title", null, packageName);
        mDescriptionId = context.getResources().getIdentifier("id/bubble_description", null, packageName);
        mDeleteButtonId = context.getResources().getIdentifier("id/delete_button", null, packageName);
        mSaveButtonId = context.getResources().getIdentifier("id/save_button", null, packageName);
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

        // Title
        TextView titleView = ((TextView) mView.findViewById(mTitleId /*R.id.title*/));
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
        TextView descriptionView = (TextView) mView.findViewById(mDescriptionId /*R.id.description*/);
        if (descriptionView != null) {
            descriptionView.setText(snippetHtml);
        }

        Button deleteButton = (Button) mView.findViewById(mDeleteButtonId /* R.id.delete_button */);
        deleteButton.setOnClickListener(v -> {
            markerEventHandler.onDelete(marker);
            close();
        });

        Button saveButton = (Button) mView.findViewById(mSaveButtonId /* R.id.save_button */);
        saveButton.setOnClickListener(v -> {
            marker.setSnippet(descriptionView.getText().toString());
            markerEventHandler.onSave(marker);
            close();
        });
    }

    @Override
    public void onClose() {
        //by default, do nothing
    }
}
