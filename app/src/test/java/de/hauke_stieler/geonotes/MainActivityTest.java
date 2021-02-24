package de.hauke_stieler.geonotes;

import android.view.MenuItem;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.Exporter;
import de.hauke_stieler.geonotes.notes.Note;

public class MainActivityTest extends GeoNotesTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule(MainActivity.class);

    private Database databaseMock;
    private Exporter exporterMock;

    @Before
    public void setup() {
        databaseMock = get(Database.class);
        exporterMock = get(Exporter.class);
    }

    @Test
    public void testOptionItemSelected_Export_CallsExporter() {
        // Arrange
        List<Note> notes = new ArrayList<>();
        notes.add(new Note(1, "foo", 1.23f, 4.56f));
        notes.add(new Note(2, "bar", 2.34f, 5.67f));
        Mockito.when(databaseMock.getAllNotes()).thenReturn(notes);

        // Act
        rule.getScenario().onActivity(activity -> activity.onOptionsItemSelected(getMenuItem(R.id.toolbar_btn_export)));

        // Assert
        Mockito.verify(exporterMock).export();
    }

    private MenuItem getMenuItem(int id) {
        MenuItem mock = Mockito.mock(MenuItem.class);
        Mockito.when(mock.getItemId()).thenReturn(id);
        return mock;
    }
}
