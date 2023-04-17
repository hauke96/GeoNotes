package de.hauke_stieler.geonotes.categories;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.database.Database;

public class CategoryConfigurationActivity extends AppCompatActivity {

    private Database database;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        Toolbar toolbar = findViewById(R.id.category_list_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        preferences = Injector.get(SharedPreferences.class);

        database = Injector.get(Database.class);
        List<Category> categories = database.getAllCategories();

        CategoryListAdapter adapter = new CategoryListAdapter(this, categories);

        RecyclerView listView = findViewById(R.id.category_list_view);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        listView.setLayoutManager(manager);
        listView.setHasFixedSize(true);
        listView.setAdapter(adapter);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        listView.addItemDecoration(divider);

        Button saveButton = findViewById(R.id.category_list_save);
        saveButton.setOnClickListener(v -> {
            saveAllCategories(adapter);
            setResult(Activity.RESULT_OK);
            finish();
        });

        Button newButton = findViewById(R.id.category_new_button);
        newButton.setOnClickListener(v -> adapter.addCategory("#505050"));
    }

    private void saveAllCategories(CategoryListAdapter adapter) {
        for (Category category : adapter.getAllRemovedItems()) {
            database.removeCategory(preferences, category.getId());
        }

        for (Category category : adapter.getAllItems()) {
            if (category.getId() == Category.UNKNOWN_ID) {
                database.addCategory(category.getColorString(), category.getName(), category.getSortKey());
            } else {
                database.updateCategory(category.getId(), category.getName(), category.getColorString(), category.getSortKey());
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}