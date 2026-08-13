package de.hauke_stieler.geonotes.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import org.maplibre.android.plugins.annotation.Symbol;

import de.hauke_stieler.geonotes.notes.Note;

public class GeoNotesSymbol {
    public static final String DATA_KEY_NOTE_ID = "note-id";
    public static final String DATA_KEY_CATEGORY = "category";
    public static final String DATA_KEY_DESCRIPTION = "description";
    public static final String DATA_KEY_ICON_NAME_NORMAL = "icon-name-normal";
    public static final String DATA_KEY_ICON_NAME_NORMAL_SELECTED = "icon-name-normal-selected";
    public static final String DATA_KEY_ICON_NAME_CAMERA = "icon-name-camera";
    public static final String DATA_KEY_ICON_NAME_CAMERA_SELECTED = "icon-name-camera-selected";

    public static String getIconName(Note note, boolean hasPhoto, boolean isSelected) {
        long categoryId = note.getCategory().getId();
        return getIconName(hasPhoto, isSelected, categoryId);
    }

    public static String getIconName(Symbol symbol, boolean hasPhoto, boolean isSelected) {
        long categoryId = getCategoryId(symbol);
        return getIconName(hasPhoto, isSelected, categoryId);
    }

    @NonNull
    private static String getIconName(boolean hasPhoto, boolean isSelected, long categoryId) {
        String iconStyle;
        if (hasPhoto) {
            iconStyle = "camera";
        } else {
            iconStyle = "normal";
        }

        String iconName = "category-" + categoryId + "-" + iconStyle;

        if (isSelected) {
            iconName += "-selected";
        }

        return iconName;
    }

    public static JsonObject getData(Note note) {

        JsonObject data = new JsonObject();
        data.addProperty(GeoNotesSymbol.DATA_KEY_NOTE_ID, note.getId());
        data.addProperty(GeoNotesSymbol.DATA_KEY_DESCRIPTION, note.getDescription());
        data.addProperty(GeoNotesSymbol.DATA_KEY_CATEGORY, note.getCategory().getId());
        data.addProperty(GeoNotesSymbol.DATA_KEY_ICON_NAME_NORMAL, GeoNotesSymbol.getIconName(note, false, false));
        data.addProperty(GeoNotesSymbol.DATA_KEY_ICON_NAME_NORMAL_SELECTED, GeoNotesSymbol.getIconName(note, false, true));
        data.addProperty(GeoNotesSymbol.DATA_KEY_ICON_NAME_CAMERA, GeoNotesSymbol.getIconName(note, true, false));
        data.addProperty(GeoNotesSymbol.DATA_KEY_ICON_NAME_CAMERA_SELECTED, GeoNotesSymbol.getIconName(note, true, true));

        return data;
    }

    public static String getIconName(JsonObject data) {
        return data.get(DATA_KEY_ICON_NAME_NORMAL).getAsString();
    }

    public static Long getNoteId(Symbol symbol) {
        return symbol.getData().getAsJsonObject().get(DATA_KEY_NOTE_ID).getAsLong();
    }

    public static long getCategoryId(Symbol symbol) {
        return symbol.getData().getAsJsonObject().get(DATA_KEY_CATEGORY).getAsLong();
    }

    public static String getIconName(Symbol symbol) {
        return symbol.getData().getAsJsonObject().get(DATA_KEY_ICON_NAME_NORMAL).getAsString();
    }

    public static String getIconNameSelected(Symbol symbol) {
        return symbol.getData().getAsJsonObject().get(DATA_KEY_ICON_NAME_NORMAL_SELECTED).getAsString();
    }

    public static boolean hasSelectedStyle(Symbol symbol) {
        return symbol.getIconImage().endsWith("selected");
    }

    public static String getDescription(Symbol symbol) {
        return symbol.getData().getAsJsonObject().get(DATA_KEY_DESCRIPTION).getAsString();
    }

    public static void setDescription(Symbol symbol, String newDescription) {
        symbol.getData().getAsJsonObject().addProperty(DATA_KEY_DESCRIPTION, newDescription);
    }

    public static void setCategoryId(Symbol symbol, long newCategoryId) {
        symbol.getData().getAsJsonObject().addProperty(DATA_KEY_CATEGORY, newCategoryId);
    }
}
