package de.hauke_stieler.geonotes.export;

public class CategoryModel {
    final long id;
    final String name;
    final String color;

    public CategoryModel(long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }
}
