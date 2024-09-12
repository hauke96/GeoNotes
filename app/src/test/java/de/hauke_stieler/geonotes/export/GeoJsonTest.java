package de.hauke_stieler.geonotes.export;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.categories.Category;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

public class GeoJsonTest {
    @Test
    public void testNoNotes() {
        // Arrange
        List<Note> notes = new ArrayList<>();

        String expectedResult = "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": []\n" +
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
        notes.add(new Note(1, "foo", 1.23f, 4.56f, "2021-03-01 12:34:56", new Category(1, "#ff00aa", "Bar", 1)));

        String expectedResult = "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": 1,\n" +
                "        \"description\": \"foo\",\n" +
                "        \"created_at\": \"2021-03-01 12:34:56\",\n" +
                "        \"category\": {\n" +
                "          \"id\": 1,\n" +
                "          \"name\": \"Bar\",\n" +
                "          \"color\": \"#ff00aa\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Point\",\n" +
                "        \"coordinates\": [\n" +
                "          4.559999942779541,\n" +
                "          1.2300000190734863\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Act
        String actualResult = GeoJson.toGeoJson(notes);

        // Assert
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testSingleNote_withLineBreak() {
        // Arrange
        List<Note> notes = new ArrayList<>();
        notes.add(new Note(1, "foo\nbar", 1.23f, 4.56f, "2021-03-01 12:34:56", new Category(1, "#ff00aa", "Bar", 1)));

        String expectedResult = "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": 1,\n" +
                "        \"description\": \"foo\\nbar\",\n" +
                "        \"created_at\": \"2021-03-01 12:34:56\",\n" +
                "        \"category\": {\n" +
                "          \"id\": 1,\n" +
                "          \"name\": \"Bar\",\n" +
                "          \"color\": \"#ff00aa\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Point\",\n" +
                "        \"coordinates\": [\n" +
                "          4.559999942779541,\n" +
                "          1.2300000190734863\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Act
        String actualResult = GeoJson.toGeoJson(notes);

        // Assert
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testSingleNote_withQuotes() {
        // Arrange
        List<Note> notes = new ArrayList<>();
        notes.add(new Note(1, "\"foo\"", 1.23f, 4.56f, "2021-03-01 12:34:56", new Category(1, "#ff00aa", "Bar", 1)));

        String expectedResult = "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": 1,\n" +
                "        \"description\": \"\\\"foo\\\"\",\n" +
                "        \"created_at\": \"2021-03-01 12:34:56\",\n" +
                "        \"category\": {\n" +
                "          \"id\": 1,\n" +
                "          \"name\": \"Bar\",\n" +
                "          \"color\": \"#ff00aa\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Point\",\n" +
                "        \"coordinates\": [\n" +
                "          4.559999942779541,\n" +
                "          1.2300000190734863\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
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
        notes.add(new Note(1, "foo", 1.23f, 4.56f, "2021-03-01 12:34:56", new Category(1, "#abc123", "One", 1)));
        notes.add(new Note(2, "\"bar\" with quotes", 2.34f, 5.67f, "2010-12-21 01:23:45", new Category(42, "#aabbcc", "Fourty-Two", 1)));

        String expectedResult = "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": 1,\n" +
                "        \"description\": \"foo\",\n" +
                "        \"created_at\": \"2021-03-01 12:34:56\",\n" +
                "        \"category\": {\n" +
                "          \"id\": 1,\n" +
                "          \"name\": \"One\",\n" +
                "          \"color\": \"#abc123\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Point\",\n" +
                "        \"coordinates\": [\n" +
                "          4.559999942779541,\n" +
                "          1.2300000190734863\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": 2,\n" +
                "        \"description\": \"\\\"bar\\\" with quotes\",\n" +
                "        \"created_at\": \"2010-12-21 01:23:45\",\n" +
                "        \"category\": {\n" +
                "          \"id\": 42,\n" +
                "          \"name\": \"Fourty-Two\",\n" +
                "          \"color\": \"#aabbcc\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Point\",\n" +
                "        \"coordinates\": [\n" +
                "          5.670000076293945,\n" +
                "          2.3399999141693115\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Act
        String actualResult = GeoJson.toGeoJson(notes);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
