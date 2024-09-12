package de.hauke_stieler.geonotes.export;

import org.junit.Test;

import java.util.ArrayList;

import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.categories.Category;

import static org.junit.Assert.assertEquals;

public class GpxTest {
    @Test
    public void testGpxExport()  {
        // Arrange
        ArrayList<Note> notes = new ArrayList<>();
        notes.add(new Note(123, "foo bar", 1.23, 2.34,"2022-01-30 12:34:56", new Category(11, "#abc123", "Foo", 1)));

        // Act
        String gpxString = Gpx.toGpx(notes);

        // Assert
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<gpx version=\"1.1\">\n" +
                "  <wpt lat=\"1.230000\" lon=\"2.340000\">\n" +
                "    <time>2022-01-30T12:34:56+01</time>\n" +
                "    <name>123</name>\n" +
                "    <desc>foo bar</desc>\n" +
                "    <type>11;#abc123;Foo</type>\n" +
                "  </wpt>\n" +
                "</gpx>\n", gpxString);
    }
}
