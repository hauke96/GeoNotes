package de.hauke_stieler.geonotes.categories;

import android.graphics.Color;

import de.hauke_stieler.geonotes.R;

public class Category {
    public final static int NONE_ID = -1;    // ID of a dummy category
    public final static int UNKNOWN_ID = -2; // ID of a new unsaved category.

    private final long id;
    private final int drawableId;

    private String color;
    private String name;
    private long sortKey;
    private boolean hasNotes;

    public Category(String color, String name, long sortKey) {
        this.id = UNKNOWN_ID;
        this.color = color;
        this.name = name;
        this.sortKey = sortKey;
        this.drawableId = R.drawable.shape_item_cetagory_spinner;
    }

    public Category(long id, String color, String name, long sortKey) {
        this.id = id;
        this.color = color;
        this.name = name;
        this.sortKey = sortKey;
        this.drawableId = R.drawable.shape_item_cetagory_spinner;
    }

    public Category(long id, String color, String name, int drawableId, long sortKey) {
        this.id = id;
        this.color = color;
        this.name = name;
        this.sortKey = sortKey;
        this.drawableId = drawableId;
    }

    public long getId() {
        return id;
    }

    public String getColorString() {
        return color;
    }

    public void setColor(int newColor) {
        this.color = String.format("#%06X", (0xFFFFFF & newColor));
    }

    public int getColor() {
        return Color.parseColor(getColorString());
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public int getDrawableId() {
        return drawableId;
    }

    public long getSortKey() {
        return sortKey;
    }

    public void setSortKey(int newKey) {
        this.sortKey = newKey;
    }

    public boolean hasNotes() {
        return hasNotes;
    }

    public void setHasNotes(boolean hasNotes) {
        this.hasNotes = hasNotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int) id;
        return hash;
    }
}
