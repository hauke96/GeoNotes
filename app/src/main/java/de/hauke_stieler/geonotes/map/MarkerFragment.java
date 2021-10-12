package de.hauke_stieler.geonotes.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
