package de.hauke_stieler.geonotes;

import android.content.SharedPreferences;
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
import de.hauke_stieler.geonotes.map.Map;
import de.hauke_stieler.geonotes.notes.Note;

public class MainActivityTest extends GeoNotesTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule(MainActivity.class);

    private Database databaseMock;
    private Exporter exporterMock;
    private SharedPreferences sharedPreferencesMock;
    private Map mapMock;

    @Before
    public void setup() {
        databaseMock = get(Database.class);
        exporterMock = get(Exporter.class);
        sharedPreferencesMock = get(SharedPreferences.class);
        mapMock = get(Map.class);
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

    @Test
    public void testloadPreferences_setsLocation() {
        // Arrange
        Mockito.when(sharedPreferencesMock.getFloat("PREF_LAST_LOCATION_LAT", 0f)).thenReturn(1.23f);
        Mockito.when(sharedPreferencesMock.getFloat("PREF_LAST_LOCATION_LON", 0f)).thenReturn(4.56f);
        Mockito.when(sharedPreferencesMock.getFloat("PREF_LAST_LOCATION_ZOOM", 2)).thenReturn(7f);

        // Act
        rule.getScenario().onActivity(activity -> activity.loadPreferences());

        // Assert
        Mockito.verify(mapMock).setLocation(1.23f, 4.56f, 7f);
    }

    private MenuItem getMenuItem(int id) {
        MenuItem mock = Mockito.mock(MenuItem.class);
        Mockito.when(mock.getItemId()).thenReturn(id);
        return mock;
    }
}
