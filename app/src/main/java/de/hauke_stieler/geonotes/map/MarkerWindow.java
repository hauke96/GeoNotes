package de.hauke_stieler.geonotes.map;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import org.apache.commons.text.StringEscapeUtils;
import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.File;
import java.util.Date;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class MarkerWindow extends InfoWindow {
    private RequestPhotoEventHandler requestPhotoHandler;

    public interface MarkerEventHandler {
        void onDelete(Marker marker);

        void onSave(Marker marker);

        void onMove(Marker marker);
    }

    public interface RequestPhotoEventHandler {
        void onRequestPhoto(Long noteId);
    }

    private Database database;
    private MarkerEventHandler markerEventHandler;

    private Marker selectedMarker;

    /**
     * resource id value meaning "undefined resource id"
     */
    public static final int UNDEFINED_RES_ID = 0;

    static int mCreationDateLabelId = UNDEFINED_RES_ID,
            mTitleId = UNDEFINED_RES_ID,
            mDescriptionId = UNDEFINED_RES_ID,
            mDeleteButtonId = UNDEFINED_RES_ID,
            mSaveButtonId = UNDEFINED_RES_ID,
            mMoveButtonId = UNDEFINED_RES_ID,
            mCameraButtonId = UNDEFINED_RES_ID,
            mSubDescriptionId = UNDEFINED_RES_ID,
            mImageId = UNDEFINED_RES_ID;

    public MarkerWindow(int layoutResId, MapView mapView, Database database, MarkerEventHandler markerEventHandler) {
        super(layoutResId, mapView);

        this.database = database;
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
        mCreationDateLabelId = context.getResources().getIdentifier("id/creation_date_label", null, packageName);
        mTitleId = context.getResources().getIdentifier("id/bubble_title", null, packageName);
        mDescriptionId = context.getResources().getIdentifier("id/bubble_description", null, packageName);
        mDeleteButtonId = context.getResources().getIdentifier("id/delete_button", null, packageName);
        mSaveButtonId = context.getResources().getIdentifier("id/save_button", null, packageName);
        mMoveButtonId = context.getResources().getIdentifier("id/move_button", null, packageName);
        mCameraButtonId = context.getResources().getIdentifier("id/camera_button", null, packageName);
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
        Note note = database.getNote(marker.getId());

        // Title
        TextView titleView = mView.findViewById(mTitleId /*R.id.title*/);
        String title = marker.getTitle();
        if (title != null && titleView != null) {
            titleView.setText(title);
        }

        // Creation date
        try {
            TextView creationDateLabel = mView.findViewById(R.id.creation_date_label);
            if (creationDateLabel != null) {
                Date time = note.getCreationDateTime().getTime();
                String creationDateString = DateFormat.getDateFormat(getView().getContext()).format(time) + " " + DateFormat.getTimeFormat(getView().getContext()).format(time);
                creationDateLabel.setText(creationDateString);
            }
        } catch (Exception e) {
            Log.e("MarkerWindow", String.format("Could not create creation date label for note %d with date string '%s'", note.getId(), note.getCreationDateTimeString()));
        }

        // Description / Snippet
        String snippet = marker.getSnippet();
        if (snippet == null) {
            snippet = "";
        }
        snippet = StringEscapeUtils.escapeHtml4(snippet).replace("\n", "<br>");

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

        ImageButton cameraButton = mView.findViewById(mCameraButtonId /* R.id.save_button */);
        cameraButton.setOnClickListener(v -> {
            requestPhotoHandler.onRequestPhoto(Long.parseLong(marker.getId()));
        });
    }

    @Override
    public void onClose() {
        this.selectedMarker = null;

        resetImageList();
    }

    public void resetImageList() {
        // When the user rotates the device, this may be called before creating the UI.
        if (getView() == null) {
            return;
        }

        LinearLayout photoLayout = getView().findViewById(R.id.note_image_pane);
        photoLayout.removeAllViews();
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    public void focusEditField() {
        EditText descriptionView = mView.findViewById(mDescriptionId /*R.id.description*/);
        descriptionView.requestFocus();
    }

    public int getHeight() {
        return mView.getHeight();
    }

    public void addPhoto(File photo) {
        int sizeInPixel = getView().getContext().getResources().getDimensionPixelSize(R.dimen.ImageButton);
        int paddingInPixel = getView().getContext().getResources().getDimensionPixelSize(R.dimen.ImageButtonPadding);

        ImageButton imageButton = new ImageButton(getView().getContext());
        imageButton.setLayoutParams(new LinearLayout.LayoutParams(sizeInPixel, sizeInPixel));
        imageButton.setPadding(paddingInPixel, paddingInPixel, paddingInPixel, paddingInPixel);
        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(FileHelper.getFileUri(getView().getContext(), photo), "image/jpg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getView().getContext().startActivity(intent);
        });

        // Get thumbnail that can be shown on image button
        imageButton.setImageBitmap(ThumbnailUtil.loadThumbnail(photo));

        LinearLayout photoLayout = getView().findViewById(R.id.note_image_pane);
        photoLayout.addView(imageButton);

        Space space = new Space(getView().getContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(paddingInPixel, ViewGroup.LayoutParams.WRAP_CONTENT));
        photoLayout.addView(space);
    }

    void addRequestPhotoHandler(RequestPhotoEventHandler handler) {
        requestPhotoHandler = handler;
    }
}
