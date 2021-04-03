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
    public interface NoteListClickListener {
        public void onClick(long id);
    }

    private final List<Note> data;
    private final LayoutInflater inflater;
    private final NoteListClickListener clickListener;

    public NoteListAdapter(Context context, List<Note> data, NoteListClickListener clickListener) {
        this.data = data;
        this.clickListener = clickListener;

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

        Note item = getItem(index);

        TextView text = view.findViewById(R.id.note_list_row_text_view);
        text.setText(item.getDescription());
        text.setOnClickListener(v -> this.clickListener.onClick(item.getId()));

        return view;
    }
}
