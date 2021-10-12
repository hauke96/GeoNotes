package de.hauke_stieler.geonotes.map;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.apache.commons.text.StringEscapeUtils;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.Date;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class MarkerFragment extends Fragment {
    private static String LOGTAG = MarkerFragment.class.getName();

    public interface MarkerFragmentEventHandler {
        void onDelete(Marker marker);

        void onSave(Marker marker);

        void onMove(Marker marker);

        void onTextChanged();
    }

    public interface RequestPhotoEventHandler {
        void onRequestPhoto(Long noteId);
    }

    private MarkerFragmentEventHandler markerEventHandler;
    private RequestPhotoEventHandler requestPhotoHandler;
    private Marker selectedMarker;

    private final Database database;

    public MarkerFragment(Database database) {
        super(R.layout.marker_fragment);

        this.database = database;
    }

    public void addEventHandler(MarkerFragmentEventHandler markerEventHandler) {
        this.markerEventHandler = markerEventHandler;
    }

    void addRequestPhotoHandler(RequestPhotoEventHandler handler) {
        requestPhotoHandler = handler;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        EditText descriptionView = view.findViewById(R.id.bubble_description);
        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (selectedMarker != null) {
                    selectedMarker.setSnippet(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }

    public void selectMarker(Marker marker, boolean transferEditTextContent) {
        selectedMarker = marker;
        Note note = database.getNote(marker.getId());
        View mView = this.getView();

        // Title
        TextView titleView = mView.findViewById(R.id.bubble_title);
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
            Log.e(LOGTAG, String.format("Could not create creation date label for note %d with date string '%s'", note.getId(), note.getCreationDateTimeString()));
        }

        // Description / Snippet
        String description = "";

        // Use already typed text
        if (transferEditTextContent) {
            description = ((EditText) mView.findViewById(R.id.bubble_description)).getText().toString();
        } else { // Use text from marker
            description = marker.getSnippet();
            if (description == null) {
                description = "";
            }
        }

        // Escape as HTML to make sure line breaks are handled correctly everywhere
        description = StringEscapeUtils.escapeHtml4(description).replace("\n", "<br>");

        Spanned snippetHtml = Html.fromHtml(description);
        EditText descriptionView = mView.findViewById(R.id.bubble_description);
        if (descriptionView != null) {
            descriptionView.setText(snippetHtml);
        }

        Button deleteButton = mView.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            markerEventHandler.onDelete(marker);
            reset();
        });

        Button saveButton = mView.findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            markerEventHandler.onSave(marker);
            reset();
        });

        Button moveButton = mView.findViewById(R.id.move_button);
        moveButton.setOnClickListener(v -> {
            markerEventHandler.onMove(marker);
            reset();
        });

        ImageButton cameraButton = mView.findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(v -> {
            requestPhotoHandler.onRequestPhoto(Long.parseLong(marker.getId()));
        });
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    public void resetImageList() {
        // When the user rotates the device, this may be called before creating the UI.
        if (getView() == null) {
            return;
        }

        LinearLayout photoLayout = getView().findViewById(R.id.note_image_pane);
        photoLayout.removeAllViews();
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

    public void reset() {
        // First reset the marker, so that change events fired by input fields do not have any effect
        selectedMarker = null;

        ((EditText) getView().findViewById(R.id.bubble_description)).setText("");

        LinearLayout photoLayout = getView().findViewById(R.id.note_image_pane);
        photoLayout.removeAllViews();
    }
}
