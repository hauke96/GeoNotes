package de.hauke_stieler.geonotes;

import android.content.SharedPreferences;

import org.junit.jupiter.api.BeforeAll;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;

import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.Exporter;
import de.hauke_stieler.geonotes.map.Map;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;

public class GeoNotesTestRule extends Injector implements TestRule {

    static {
        classBuilders.put(Database.class, () -> add(Database.class));
        classBuilders.put(Exporter.class, () -> add(Exporter.class));
        classBuilders.put(SharedPreferences.class, () -> add(SharedPreferences.class));
        classBuilders.put(de.hauke_stieler.geonotes.map.Map.class, () -> add(Map.class));
        classBuilders.put(NoteIconProvider.class, () -> add(NoteIconProvider.class));
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return base;
    }

    private static <T> T add(Class<T> clazz) {
        T mock = Mockito.mock(clazz);
        classes.put(clazz, mock);
        return mock;
    }
}
