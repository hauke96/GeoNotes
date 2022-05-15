package de.hauke_stieler.geonotes.categories;

import android.graphics.Color;

public class Category {
    private final long id;
    private final String color;
    private final String name;

    public Category(long id, String color, String name) {
        this.id = id;
        this.color = color;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getColorString() {
        return color;
    }

    public int getColor() {
        return Color.parseColor(getColorString());
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id && color.equals(category.color) && name.equals(category.name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int) id;
        hash = 31 * hash + color.hashCode();
        hash = 31 * hash + name.hashCode();
        return hash;
    }
}