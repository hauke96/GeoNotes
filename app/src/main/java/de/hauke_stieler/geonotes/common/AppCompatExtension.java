package de.hauke_stieler.geonotes.common;

import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

import androidx.core.view.ViewCompat;

import de.hauke_stieler.geonotes.R;

public class AppCompatExtension {
    /**
     * Initializes window inset listener for Android 15+. In case the app is running in <15 nothing
     * happens.
     * <p>
     * The inset listeners add padding to the toolbar and root view so that the edge-to-edge
     * enforcement is circumvented and the look-and-feel is the same on Android 15+ as on older
     * Android versions. This is wanted because otherwise the toolbar would be hidden by the
     * system status bar and the system navigation bar overlaps the input field and control buttons.
     *
     * @param rootView    The overall view of the activity.
     * @param toolbarView The view of the toolbar.
     */
    public static void setupWindowInsetListener(View rootView, View toolbarView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
            // Add padding above the toolbar, since edge-to-edge enforcement would otherwise
            // break the color scheme (i.e. the system status bar would be white and unreadable).
            ViewCompat.setOnApplyWindowInsetsListener(toolbarView, (view, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars());
                view.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });

            // Sinde Android 15 (API 36 or so), it's necessary for applications to set
            // android:windowSoftInputMode="adjustResize" in the manifest and to react for window
            // inset changes manually. This means, the padding of the view needs to be adjusted
            // manually when e.g. the IME (soft keyboard) pops up.
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
                androidx.core.graphics.Insets mergedInsets = insets.getInsets(WindowInsets.Type.navigationBars() | WindowInsets.Type.ime());
                view.setPadding(mergedInsets.left, mergedInsets.top, mergedInsets.right, mergedInsets.bottom);
                return insets;
            });
        }
    }
}
