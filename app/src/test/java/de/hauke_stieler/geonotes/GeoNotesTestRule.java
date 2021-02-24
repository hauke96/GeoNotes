package de.hauke_stieler.geonotes;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.Exporter;
import de.hauke_stieler.geonotes.map.Map;

public class GeoNotesTestRule extends Injector implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        classes = new HashMap<>();

        add(Database.class);
        add(Exporter.class);
        add(SharedPreferences.class);
        add(Map.class);

        return base;
    }

    private static void add(Class<?> clazz) {
        Object mock = Mockito.mock(clazz);
        classes.put(clazz, mock);
    }
}
