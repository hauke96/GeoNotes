package de.hauke_stieler.geonotes.map;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.categories.Category;

public class CategorySpinnerAdapter extends BaseAdapter {
    private final LayoutInflater layoutInflater;
    private final ArrayList<Category> categories;
    private final int resource;

    public CategorySpinnerAdapter(@NonNull Context context, int resource) {
        this.layoutInflater = LayoutInflater.from(context);
        this.resource = resource;
        this.categories = new ArrayList<>();
    }

    public void add(Category color) {
        this.categories.add(color);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(resource, parent, false);
        }
        View innerLayout = view.findViewById(R.id.item_category_spinner_circle);
        ((GradientDrawable) innerLayout.getBackground()).setColor(getItem(position).getColor());
        return view;
    }

    @Override
    public int getCount() {
        return this.categories.size();
    }

    @Override
    public Category getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return this.getDropDownView(position, convertView, parent);
    }
}
