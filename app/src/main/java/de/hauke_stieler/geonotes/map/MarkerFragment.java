package de.hauke_stieler.geonotes.map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.util.Date;
import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.common.FileHelper;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.photo.ThumbnailUtil;

public class MarkerFragment extends Fragment {
    private static final String LOGTAG = MarkerFragment.class.getName();

    public interface MarkerFragmentEventHandler {
        void onDelete(GeoNotesMarker marker);

        void onSave(GeoNotesMarker marker);

        void onMove(GeoNotesMarker marker);

        void onCategoryChanged(GeoNotesMarker marker);
    }

    public interface RequestPhotoEventHandler {
        void onRequestPhoto(Long noteId);
    }

    /**
     * The state of the fragment describes the actions that are possible.
     * <p>
     * In NEW-state the user should not be able to add photos because no note exists yet.
     * <p>
     * During DRAGGING no actions are possible because the user is busy dragging the map, therefore
     * a notice-panel is shown.
     * <p>
     * When EDITING a note, the normal buttons and input field are visible.
     */
    public enum State {
        DRAGGING,
        NEW,
        EDITING
    }

    private MarkerFragmentEventHandler markerEventHandler;
    private RequestPhotoEventHandler requestPhotoHandler;
    private GeoNotesMarker selectedMarker;
    private State state;
    private Spinner categorySpinner;
    private CategorySpinnerAdapter categorySpinnerAdapter;

    private Database database;
    private SharedPreferences preferences;

    public MarkerFragment() {
        super(R.layout.marker_fragment);
    }

    public void addEventHandler(MarkerFragmentEventHandler markerEventHandler) {
        this.markerEventHandler = markerEventHandler;
    }

    void addRequestPhotoHandler(RequestPhotoEventHandler handler) {
        requestPhotoHandler = handler;
    }

    @Override
    public void onResume() {
        super.onResume();

        loadCategories();
        categorySpinnerAdapter.notifyDataSetChanged();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.database = Injector.get(Database.class);
        this.preferences = Injector.get(SharedPreferences.class);

        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Resize window when the keyboard popups up
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        EditText descriptionView = view.findViewById(R.id.note_description);

        // Show/hide keyboard on edit field focus
        descriptionView.setOnFocusChangeListener((v, hasFocus) -> {
            InputMethodManager inputMethodManager = (InputMethodManager) descriptionView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (hasFocus) {
                inputMethodManager.showSoftInput(descriptionView, 0);
            } else {
                inputMethodManager.hideSoftInputFromWindow(descriptionView.getWindowToken(), 0);
            }
        });

        // React to changed text and update the content of the marker
        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (selectedMarker != null) {
                    selectedMarker.setSnippet(s.toString());
                    markerEventHandler.onSave(selectedMarker);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        categorySpinnerAdapter = new CategorySpinnerAdapter(getContext(), R.layout.category_spinner_item);
        long lastUsedCategoryId = preferences.getLong(getString(R.string.pref_last_category_id), 1);

        loadCategories();

        categorySpinner = view.findViewById(R.id.category_spinner);
        categorySpinner.setAdapter(categorySpinnerAdapter);
        selectCategory(lastUsedCategoryId);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Category selectedCategory = categorySpinnerAdapter.getItem(position);
                selectedMarker.setCategoryId(selectedCategory.getId());
                if (markerEventHandler != null) {
                    markerEventHandler.onCategoryChanged(selectedMarker);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return view;
    }

    private void loadCategories() {
        categorySpinnerAdapter.setCategories(database.getAllCategories());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        state = State.NEW;
        updatePanelVisibility();
    }

    public void selectMarker(GeoNotesMarker marker, boolean transferEditTextContent) {
        selectedMarker = marker;
        state = State.EDITING;

        Note note = database.getNote(marker.getId());
        View view = getView();

        // Title
        TextView titleView = view.findViewById(R.id.bubble_title);
        String title = marker.getTitle();
        if (title != null && titleView != null) {
            titleView.setText(title);
        }

        // Creation date
        try {
            TextView creationDateLabel = view.findViewById(R.id.creation_date_label);
            if (creationDateLabel != null) {
                Date time = note.getCreationDateTime().getTime();
                String creationDateString = DateFormat.getDateFormat(getView().getContext()).format(time) + " " + DateFormat.getTimeFormat(getView().getContext()).format(time);
                creationDateLabel.setText(creationDateString);
            }
        } catch (Exception e) {
            Log.e(LOGTAG, String.format("Could not create creation date label for note %d with date string '%s'", note.getId(), note.getCreationDateTimeString()));
        }

        // Description / Snippet
        EditText descriptionView = view.findViewById(R.id.note_description);
        if (descriptionView != null) {
            String description = "";

            // Use already typed text
            if (transferEditTextContent) {
                description = descriptionView.getText().toString();
            } else { // Use text from marker
                description = marker.getSnippet();
                if (description == null) {
                    description = "";
                }
            }

            // Escape as HTML to make sure line breaks are handled correctly everywhere
            description = StringEscapeUtils.escapeHtml4(description).replace("\n", "<br>");

            Spanned snippetHtml = Html.fromHtml(description);
            descriptionView.setText(snippetHtml);
            descriptionView.requestFocus();
        }

        // Button
        updatePanelVisibility();

        Button deleteButton = view.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            reset();
            markerEventHandler.onDelete(marker);
        });

        Button saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            reset();
            markerEventHandler.onSave(marker);
        });

        Button moveButton = view.findViewById(R.id.move_button);
        moveButton.setOnClickListener(v -> {
            state = State.DRAGGING;
            updatePanelVisibility();
            markerEventHandler.onMove(marker);
        });

        ImageButton cameraButton = view.findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(v -> {
            markerEventHandler.onSave(marker);
            requestPhotoHandler.onRequestPhoto(Long.parseLong(marker.getId()));
        });

        selectCategory(marker.getCategoryId());
    }

    public GeoNotesMarker getSelectedMarker() {
        return selectedMarker;
    }

    public void resetImageList() {
        // When the user rotates the device, this may be called before creating the UI.
        if (getView() == null) {
            return;
        }

        LinearLayout photoLayout = getView().findViewById(R.id.note_image_panel);
        photoLayout.removeAllViews();
    }

    public void addPhoto(File photo) {
        Context context = getView().getContext();

        int sizeInPixel = context.getResources().getDimensionPixelSize(R.dimen.ImageButton);
        int marginInPixel = context.getResources().getDimensionPixelSize(R.dimen.ImageButtonMargin);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(sizeInPixel, sizeInPixel);
        layoutParams.leftMargin = marginInPixel;

        ImageButton imageButton = new ImageButton(context);
        imageButton.setLayoutParams(layoutParams);
        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(FileHelper.getFileUri(context, photo), "image/jpg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        });

        // Get thumbnail that can be shown on image button
        imageButton.setImageBitmap(ThumbnailUtil.loadThumbnail(photo));

        LinearLayout photoLayout = getView().findViewById(R.id.note_image_panel);
        photoLayout.addView(imageButton);

        Space space = new Space(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(marginInPixel, ViewGroup.LayoutParams.WRAP_CONTENT));
        photoLayout.addView(space);
    }

    public void reset() {
        if (getView() == null) {
            return;
        }

        // First reset the marker, so that change events fired by input fields do not have any effect
        selectedMarker = null;

        ((EditText) getView().findViewById(R.id.note_description)).setText("");

        LinearLayout photoLayout = getView().findViewById(R.id.note_image_panel);
        photoLayout.removeAllViews();

        state = State.NEW;
        updatePanelVisibility();
    }

    private void updatePanelVisibility() {
        // Default state: Note panel with buttons visible
        getView().findViewById(R.id.layout_drag_notice).setVisibility(View.GONE);
        getView().findViewById(R.id.layout_note).setVisibility(View.VISIBLE);

        getView().findViewById(R.id.button_panel).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.new_note_notice).setVisibility(View.GONE);

        switch (state) {
            case DRAGGING:
                getView().findViewById(R.id.layout_drag_notice).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.layout_note).setVisibility(View.GONE);
                break;
            case NEW:
                getView().findViewById(R.id.button_panel).setVisibility(View.GONE);
                getView().findViewById(R.id.new_note_notice).setVisibility(View.VISIBLE);
                break;
            case EDITING:
                // This is the above configured default case, nothing to do here.
                break;
        }
    }

    private void selectCategory(long categoryId) {
        List<Category> allCategories = database.getAllCategories();
        for (int i = 0; i < allCategories.size(); i++) {
            if (allCategories.get(i).getId() == categoryId) {
                categorySpinner.setSelection(i);
                return;
            }
        }
    }
}
