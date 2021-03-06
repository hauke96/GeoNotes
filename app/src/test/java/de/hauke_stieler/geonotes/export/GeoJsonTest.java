package de.hauke_stieler.geonotes.export;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.notes.Note;

import static org.junit.Assert.assertEquals;

public class GeoJsonTest {
    @Test
    public void testNoNotes() {
        // Arrange
        List<Note> notes = new ArrayList<>();

        String expectedResult = "{\n" +
                "\t\"type\": \"FeatureCollection\",\n" +
                "\t\"features\": []\n" +
                "}";

        // Act
        String actualResult = GeoJson.toGeoJson(notes);

        // Assert
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testSingleNote() {
        // Arrange
        List<Note> notes = new ArrayList<>();
        notes.add(new Note(1, "foo", 1.23f, 4.56f, "2021-03-01 12:34:56"));

        String expectedResult = "{\n" +
                "\t\"type\": \"FeatureCollection\",\n" +
                "\t\"features\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"Feature\",\n" +
                "\t\t\t\"properties\": {\n" +
                "\t\t\t\t\"geonotes:id\": \"1\",\n" +
                "\t\t\t\t\"geonotes:note\": \"foo\",\n" +
                "\t\t\t\t\"geonotes:created_at\": \"2021-03-01 12:34:56\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"geometry\": {\n" +
                "\t\t\t\t\"type\": \"Point\",\n" +
                "\t\t\t\t\"coordinates\": [\n" +
                "\t\t\t\t\t4.560000,\n" +
                "\t\t\t\t\t1.230000\n" +
                "\t\t\t\t]\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        // Act
        String actualResult = GeoJson.toGeoJson(notes);

        // Assert
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testMultipleNotes() {
        // Arrange
        List<Note> notes = new ArrayList<>();
        notes.add(new Note(1, "foo", 1.23f, 4.56f, "2021-03-01 12:34:56"));
        notes.add(new Note(2, "\"bar\" with quotes", 2.34f, 5.67f, "2010-12-21 01:23:45"));

        String expectedResult = "{\n" +
                "\t\"type\": \"FeatureCollection\",\n" +
                "\t\"features\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"Feature\",\n" +
                "\t\t\t\"properties\": {\n" +
                "\t\t\t\t\"geonotes:id\": \"1\",\n" +
                "\t\t\t\t\"geonotes:note\": \"foo\",\n" +
                "\t\t\t\t\"geonotes:created_at\": \"2021-03-01 12:34:56\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"geometry\": {\n" +
                "\t\t\t\t\"type\": \"Point\",\n" +
                "\t\t\t\t\"coordinates\": [\n" +
                "\t\t\t\t\t4.560000,\n" +
                "\t\t\t\t\t1.230000\n" +
                "\t\t\t\t]\n" +
                "\t\t\t}\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"type\": \"Feature\",\n" +
                "\t\t\t\"properties\": {\n" +
                "\t\t\t\t\"geonotes:id\": \"2\",\n" +
                "\t\t\t\t\"geonotes:note\": \"'bar' with quotes\",\n" +
                "\t\t\t\t\"geonotes:created_at\": \"2010-12-21 01:23:45\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"geometry\": {\n" +
                "\t\t\t\t\"type\": \"Point\",\n" +
                "\t\t\t\t\"coordinates\": [\n" +
                "\t\t\t\t\t5.670000,\n" +
                "\t\t\t\t\t2.340000\n" +
                "\t\t\t\t]\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";

        // Act
        String actualResult = GeoJson.toGeoJson(notes);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
