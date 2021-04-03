package de.hauke_stieler.geonotes.note_list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.notes.Note;

public class NoteListAdapter extends BaseAdapter {
    private final List<Note> data;
    private final LayoutInflater inflater;

    public NoteListAdapter(Context context, List<Note> data) {
        this.data = data;

        this.inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Note getItem(int index) {
        return data.get(index);
    }

    @Override
    public long getItemId(int index) {
        return data.get(index).getId();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.note_list_row, null);
        }

        TextView text = view.findViewById(R.id.note_list_row_text_view);
        text.setText(getItem(index).getDescription());

        return view;
    }
}
