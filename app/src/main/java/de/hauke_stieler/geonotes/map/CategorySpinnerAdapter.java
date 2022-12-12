package de.hauke_stieler.geonotes.map;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public void add(Category category) {
        this.categories.add(category);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(resource, parent, false);
        }
        Category category = getItem(position);

        View innerLayout = view.findViewById(R.id.item_category_spinner_circle);
        innerLayout.setBackgroundResource(category.getDrawableId());
        if (innerLayout.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) innerLayout.getBackground()).setColor(category.getColor());
        }

        View label = view.findViewById(R.id.item_category_label);
        ((TextView) label).setText(category.getName());

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

    public List<Category> getAllCategories() {
        return new ArrayList<>(categories);
    }

    /**
     * Clears the current list of categories and adds the given ones.
     */
    public void setCategories(Collection<Category> newCategories) {
        categories.clear();
        categories.addAll(newCategories);
    }
}
