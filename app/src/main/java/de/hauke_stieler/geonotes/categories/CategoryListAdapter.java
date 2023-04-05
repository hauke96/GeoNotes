package de.hauke_stieler.geonotes.categories;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hauke_stieler.geonotes.R;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.CategoryViewHolder> {

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout outerColorChooserLayout;
        public RelativeLayout innerColorChooserLayout;
        public EditText nameInput;
        public ImageButton deleteButton;
        public ImageButton sortButton;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            // Enabling recycling makes handling text watcher and other handler much more difficult.
            setIsRecyclable(false);

            outerColorChooserLayout = itemView.findViewById(R.id.category_list_spinner_layout);
            innerColorChooserLayout = itemView.findViewById(R.id.category_list_row_spinner_circle);
            nameInput = itemView.findViewById(R.id.category_list_row_input);
            deleteButton = itemView.findViewById(R.id.category_list_row_delete_button);
            sortButton = itemView.findViewById(R.id.category_list_row_up_button);
        }
    }

    private final List<Category> categories;
    private final List<Category> removedCategories;
    private final Context context;

    public CategoryListAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.categories = categories;
        this.removedCategories = new ArrayList<>();
    }

    public List<Category> getAllItems() {
        return new ArrayList<>(categories);
    }

    public List<Category> getAllRemovedItems() {
        return new ArrayList<>(removedCategories);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_list_row, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder viewHolder, int index) {
        Category category = categories.get(index);

        View innerLayout = viewHolder.innerColorChooserLayout;
        innerLayout.setBackgroundResource(category.getDrawableId());
        if (innerLayout.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) innerLayout.getBackground()).setColor(category.getColor());
        }

        EditText editField = viewHolder.nameInput;
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

        ImageButton deleteButton = viewHolder.deleteButton;
        deleteButton.setOnClickListener(v -> {
            if (category.hasNotes()) {
                Toast.makeText(context, R.string.category_cannot_delete_with_notes, Toast.LENGTH_LONG).show();
            } else {
                removedCategories.add(category);
                categories.remove(category);
                notifyDataSetChanged();
            }
        });
        if (category.hasNotes()) {
            deleteButton.getDrawable().mutate().setColorFilter(context.getResources().getColor(R.color.grey), PorterDuff.Mode.SRC_IN);
        } else {
            deleteButton.getDrawable().mutate().setColorFilter(context.getResources().getColor(R.color.dark_grey), PorterDuff.Mode.SRC_IN);
        }

        ImageButton upButton = viewHolder.sortButton;
        upButton.setOnClickListener(v -> {
            int oldPosition = index;
            int newPosition = oldPosition - 1;
            if (newPosition == -1) {
                newPosition = 0;
            }

            Collections.swap(categories, oldPosition, newPosition);

            for (int i = 0; i < categories.size(); i++) {
                categories.get(i).setSortKey(i);
            }

            notifyDataSetChanged();
        });

        View colorIcon = viewHolder.outerColorChooserLayout;
        colorIcon.setOnClickListener(v -> Toast.makeText(this.context, R.string.category_list_color_notice, Toast.LENGTH_SHORT).show());
    }

    public void addCategory(String color) {
        Category newCategory = new Category(color, "", this.categories.size());
        this.categories.add(newCategory);
        notifyDataSetChanged();
    }
}
