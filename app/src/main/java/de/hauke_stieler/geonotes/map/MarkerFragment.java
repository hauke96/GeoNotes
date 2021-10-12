package de.hauke_stieler.geonotes.map;

import androidx.fragment.app.Fragment;

import org.osmdroid.views.overlay.Marker;

import de.hauke_stieler.geonotes.R;

public class MarkerFragment extends Fragment {

    public interface MarkerFragmentEventHandler {
        void onDelete(Marker marker);

        void onSave(Marker marker);

        void onMove(Marker marker);

        void onTextChanged();

        void onRequestPhoto(Long noteId);
    }

    private MarkerFragmentEventHandler markerEventHandler;

    public MarkerFragment() {
        super(R.layout.marker_window);
    }

    public void addEventHandler(MarkerFragmentEventHandler markerEventHandler) {
        this.markerEventHandler = markerEventHandler;
    }
}
