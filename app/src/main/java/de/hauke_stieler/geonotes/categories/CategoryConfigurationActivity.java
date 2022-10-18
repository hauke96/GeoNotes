package de.hauke_stieler.geonotes.categories;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

import de.hauke_stieler.geonotes.Injector;
import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.database.Database;

public class CategoryConfigurationActivity extends AppCompatActivity {

    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        Toolbar toolbar = findViewById(R.id.category_list_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        database = Injector.get(Database.class);
        List<Category> categories = database.getAllCategories();

        CategoryListAdapter adapter = new CategoryListAdapter(this, categories);

        ListView listView = findViewById(R.id.category_list_view);
        listView.setAdapter(adapter);

        Button saveButton = findViewById(R.id.category_list_save);
        saveButton.setOnClickListener(v -> {
            for (Category category : adapter.getAllItems()) {
                database.updateCategory(category.getId(), category.getName(), category.getColorString());
            }
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}