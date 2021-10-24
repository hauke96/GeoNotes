package de.hauke_stieler.geonotes;

import android.content.SharedPreferences;
import android.os.Build;
import android.view.MenuItem;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.osmdroid.events.DelayedMapListener;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.Exporter;
import de.hauke_stieler.geonotes.map.Map;
import de.hauke_stieler.geonotes.map.MarkerWindow;
import de.hauke_stieler.geonotes.map.TouchDownListener;
import de.hauke_stieler.geonotes.notes.Note;

@RunWith(AndroidJUnit4.class)
@Config(maxSdk = Build.VERSION_CODES.P, minSdk = Build.VERSION_CODES.P) // Value of Build.VERSION_CODES.P is 28
public class MainActivityTest {

    public ActivityScenarioRule<MainActivity> activityRule;
    public GeoNotesTestRule testRule;

    @Rule
    public TestRule chain = RuleChain.outerRule(testRule = new GeoNotesTestRule()).around(activityRule = new ActivityScenarioRule(MainActivity.class));

    private Database databaseMock;
    private Exporter exporterMock;
    private SharedPreferences sharedPreferencesMock;
    private Map mapMock;

    @Before
    public void setup() {
        databaseMock = testRule.get(Database.class);
        exporterMock = testRule.get(Exporter.class);
        sharedPreferencesMock = testRule.get(SharedPreferences.class);
        mapMock = testRule.get(Map.class);
    }

    @Test
    public void testOptionItemSelected_export_callsExporter() {
        // Arrange
        List<Note> notes = new ArrayList<>();
        notes.add(new Note(1, "foo", 1.23f, 4.56f, "2021-03-01 12:34:56"));
        notes.add(new Note(2, "bar", 2.34f, 5.67f, "2021-03-02 11:11:11"));
        Mockito.when(databaseMock.getAllNotes()).thenReturn(notes);

        // Act
        activityRule.getScenario().onActivity(activity -> activity.onOptionsItemSelected(getMenuItem(R.id.toolbar_btn_export)));

        // Assert
        Mockito.verify(exporterMock).shareAsGeoJson();
    }

    @Test
    public void testloadPreferences_setsLocation() {
        // Arrange
        Mockito.when(sharedPreferencesMock.getFloat("PREF_LAST_LOCATION_LAT", 0f)).thenReturn(1.23f);
        Mockito.when(sharedPreferencesMock.getFloat("PREF_LAST_LOCATION_LON", 0f)).thenReturn(4.56f);
        Mockito.when(sharedPreferencesMock.getFloat("PREF_LAST_LOCATION_ZOOM", 2)).thenReturn(7f);

        // Act
        activityRule.getScenario().onActivity(activity -> activity.loadPreferences());

        // Assert
        Mockito.verify(mapMock).setLocation(1.23f, 4.56f, 7f);
    }

    @Test
    public void testloadPreferences_setsMapListener() {
        // Act & Assert
        Mockito.verify(mapMock).addMapListener(Mockito.any(DelayedMapListener.class), Mockito.any(TouchDownListener.class));
    }

    @Test
    public void testloadPreferences_setsPhotoListener() {
        // Act & Assert
        Mockito.verify(mapMock).addRequestPhotoHandler(Mockito.any(MarkerWindow.RequestPhotoEventHandler.class));
    }

    private MenuItem getMenuItem(int id) {
        MenuItem mock = Mockito.mock(MenuItem.class);
        Mockito.when(mock.getItemId()).thenReturn(id);
        return mock;
    }
}
