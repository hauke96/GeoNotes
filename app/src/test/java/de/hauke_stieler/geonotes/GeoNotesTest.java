package de.hauke_stieler.geonotes;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.Exporter;
import de.hauke_stieler.geonotes.map.Map;

@RunWith(AndroidJUnit4.class)
@Config(maxSdk = Build.VERSION_CODES.P, minSdk = Build.VERSION_CODES.P) // Value of Build.VERSION_CODES.P is 28
public abstract class GeoNotesTest extends Injector {

    @BeforeClass
    public static void init() {
        add(Database.class);
        add(Exporter.class);
        add(SharedPreferences.class);
        add(Map.class);
    }

    private static void add(Class<?> clazz) {
        classes.put(clazz, Mockito.mock(clazz));
    }
}
