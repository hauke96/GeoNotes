package de.hauke_stieler.geonotes;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.Exporter;

interface ClassBuilder<T> {
    T build();
}

/**
 * This class acts as a service provider. It builds all the needed classes and enables us to test the activity code.
 * <p>
 * Using a framework would also be possible but it would be more difficult because Android-SDK classes heavily rely on the application context.
 */
public class Injector {
    protected static Map<Class, Object> classes = new HashMap<>();
    private static Map<Class, ClassBuilder> classBuilders = new HashMap<>();
    private static Context context;

    static {
        classBuilders.put(Database.class, () -> buildDatabase());
        classBuilders.put(Exporter.class, () -> buildExporter());
    }

    public static void registerContext(Context newContext) {
        context = newContext;
    }

    public static <T> T get(Class<T> clazz) {
        // Have we built the class already? Then return it
        if (classes.containsKey(clazz)) return (T) classes.get(clazz);

        // If we haven't built the class -> build it and add it to the map
        Object instance = classBuilders.get(clazz).build();
        classes.put(clazz, instance);

        return (T) instance;
    }

    private static Database buildDatabase() {
        return new Database(context);
    }

    private static Exporter buildExporter() {
        return new Exporter(get(Database.class), context);
    }
}
