package de.hauke_stieler.geonotes.note_list;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.fragment.app.DialogFragment;

import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.map.CategorySpinnerAdapter;

public class FilterDialog extends DialogFragment {
    public interface FilterChangedListener {
        void onFilterChanged(String filterText, Long categoryId);
    }

    public static final int NONE_CATEGORY_ITEM_INDEX = 0;

    private final Database database;
    private final FilterChangedListener filterChangedListener;

    private final String initialFilterText;
    private final Long initialFilterCategoryId;

    private Category selectedCategory;
    private Spinner categorySpinner;
    private CategorySpinnerAdapter categorySpinnerAdapter;

    public FilterDialog(FilterChangedListener filterChangedListener, String initialFilterText, Long initialFilterCategoryId) {
        this.filterChangedListener = filterChangedListener;
        this.initialFilterText = initialFilterText;
        this.initialFilterCategoryId = initialFilterCategoryId;
        database = Injector.get(Database.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.note_list_filter_dialog, container);

        EditText textInput = view.findViewById(R.id.note_list_filter_textview);
        textInput.setText(initialFilterText);
        textInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fireChangeEvent();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        categorySpinnerAdapter = new CategorySpinnerAdapter(getContext(), R.layout.item_category_spinner);
        categorySpinnerAdapter.add(new Category(Category.NONE_ID, "#ffffff", "(none)", R.drawable.shape_item_cetagory_spinner_none));
        List<Category> allCategories = database.getAllCategories();
        for (int i = 0; i < allCategories.size(); i++) {
            Category category = allCategories.get(i);
            categorySpinnerAdapter.add(category);
        }

        categorySpinner = view.findViewById(R.id.note_list_filter_category_spinner);
        categorySpinner.setAdapter(categorySpinnerAdapter);
        selectCategory(initialFilterCategoryId);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categorySpinnerAdapter.getItem(position);
                fireChangeEvent();
                Log.i(FilterDialog.class.getName(), "onItemSelected: " + position + ", " + selectedCategory.getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        view.findViewById(R.id.note_list_filter_btn_reset).setOnClickListener(v -> onResetClicked());
        view.findViewById(R.id.note_list_filter_btn_ok).setOnClickListener(v -> onOkClicked());
        getDialog().getWindow().setGravity(Gravity.RIGHT);

        return view;
    }

    private void onOkClicked() {
        dismiss();
    }

    private void fireChangeEvent() {
        EditText textInput = getView().findViewById(R.id.note_list_filter_textview);

        Long categoryId = null;
        if (selectedCategory != null && selectedCategory.getId() != -1) {
            categoryId = selectedCategory.getId();
        }

        filterChangedListener.onFilterChanged(textInput.getText().toString(), categoryId);
    }

    private void onResetClicked() {
        ((EditText) getView().findViewById(R.id.note_list_filter_textview)).setText(null);
        selectCategory(null);
        fireChangeEvent();
    }

    private void selectCategory(Long categoryId) {
        if (categoryId == null) {
            categorySpinner.setSelection(NONE_CATEGORY_ITEM_INDEX);
            return;
        }

        List<Category> allCategories = categorySpinnerAdapter.getAllCategories();
        for (int i = 0; i < allCategories.size(); i++) {
            if (allCategories.get(i).getId() == categoryId) {
                categorySpinner.setSelection(i);
                return;
            }
        }
    }
}
