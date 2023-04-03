package de.hauke_stieler.geonotes.categories;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hauke_stieler.geonotes.R;

public class CategoryListAdapter extends BaseAdapter {

    private final List<Category> categories;
    private final List<Category> removedCategories;
    private final LayoutInflater inflater;
    private final Context context;

    public CategoryListAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.categories = categories;
        this.removedCategories = new ArrayList<>();

        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public List<Category> getAllItems() {
        return new ArrayList<>(categories);
    }

    public List<Category> getAllRemovedItems() {
        return new ArrayList<>(removedCategories);
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

        EditText editField = view.findViewById(R.id.category_list_row_input);
        editField.setText(category.getName());
        editField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                category.setName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton deleteButton = view.findViewById(R.id.category_list_row_delete_button);
        deleteButton.setOnClickListener(v -> {
            removedCategories.add(categories.get(index));
            categories.remove(index);
            notifyDataSetChanged();
        });

        ImageButton upButton = view.findViewById(R.id.category_list_row_up_button);
        upButton.setOnClickListener(v -> {
            int newIndex = index - 1;
            if (newIndex == -1) {
                newIndex = 0;
            }

            categories.get(index).setSortKey(newIndex);
            categories.get(newIndex).setSortKey(index);

            Collections.swap(categories, index, newIndex);
            notifyDataSetChanged();
        });

        View colorIcon = view.findViewById(R.id.category_list_spinner_layout);
        colorIcon.setOnClickListener(v -> Toast.makeText(this.context, R.string.category_list_color_notice, Toast.LENGTH_SHORT).show());

        return view;
    }
}
