package de.hauke_stieler.geonotes.map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import de.hauke_stieler.geonotes.R;

public class CategorySpinnerAdapter extends BaseAdapter {
    private final LayoutInflater layoutInflater;
    private final ArrayList<String> categories;
    private int resource;

    public CategorySpinnerAdapter(@NonNull Context context, int resource) {
        this.layoutInflater = LayoutInflater.from(context);
        this.resource = resource;
        this.categories = new ArrayList<>();
    }

    // TODO Change <String> to correct type ("Category" or something).
    public void add(String color) {
        this.categories.add(color);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(resource, parent, false);
        }
        View innerLayout = ((RelativeLayout) view).getChildAt(0);
        ((GradientDrawable) innerLayout.getBackground()).setColor(Color.parseColor(getItem(position)));
        return view;
    }

    @Override
    public int getCount() {
        return this.categories.size();
    }

    @Override
    public String getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO return actual ID when own class exists
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return this.getDropDownView(position, convertView, parent);
    }
}
