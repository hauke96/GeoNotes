package de.hauke_stieler.geonotes.categories;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import de.hauke_stieler.geonotes.R;

public class CategoryListAdapter extends BaseAdapter {

    private final List<Category> categories;
    private final LayoutInflater inflater;

    public CategoryListAdapter(Context context, List<Category> categories) {
        this.categories = categories;

        this.inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Category getItem(int index) {
        return categories.get(index);
    }

    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.category_list_row, null);

        Category category = getItem(index);

        View innerLayout = view.findViewById(R.id.category_list_row_spinner_circle);
        innerLayout.setBackgroundResource(category.getDrawableId());
        if (innerLayout.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) innerLayout.getBackground()).setColor(category.getColor());
        }

        View label = view.findViewById(R.id.category_list_row_input);
        ((TextView) label).setText(category.getName());

        return view;
    }
}
