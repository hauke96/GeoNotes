package de.hauke_stieler.geonotes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ComponentActivity;

import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.Map;

import de.hauke_stieler.geonotes.database.Database;
import de.hauke_stieler.geonotes.export.Exporter;

import static android.content.Context.MODE_PRIVATE;

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
    private static Activity activity;

    static {
        classBuilders.put(Database.class, () -> buildDatabase());
        classBuilders.put(Exporter.class, () -> buildExporter());
        classBuilders.put(SharedPreferences.class, () -> buildSharedPreferences());
        classBuilders.put(de.hauke_stieler.geonotes.map.Map.class, () -> buildMap());
    }

    public static void registerActivity(Activity newActivity) {
        activity = newActivity;
        context = activity.getApplicationContext();

        // Example: The user rotates the device -> MainActivity will be recreated -> Dependencies may also need to be recreated (e.g. the map).
        classes = new HashMap<>();
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

    private static SharedPreferences buildSharedPreferences() {
        return context.getSharedPreferences(context.getString(R.string.pref_file), MODE_PRIVATE);
    }

    private static de.hauke_stieler.geonotes.map.Map buildMap() {
        MapView mapView = activity.findViewById(R.id.map);
        return new de.hauke_stieler.geonotes.map.Map(context, mapView, get(Database.class));
    }
}
