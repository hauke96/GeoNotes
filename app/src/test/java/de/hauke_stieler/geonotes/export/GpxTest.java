package de.hauke_stieler.geonotes.export;

import org.junit.Test;

import java.util.ArrayList;

import de.hauke_stieler.geonotes.notes.Note;

import static org.junit.Assert.assertEquals;

public class GpxTest {
    @Test
    public void testGpxExport()  {
        // Arrange
        ArrayList<Note> notes = new ArrayList<>();
        notes.add(new Note(123, "foo bar", 1.23, 2.34,"2022-01-30 12:34:56"));

        // Act
        String gpxString = Gpx.toGpx(notes);

        // Assert
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><gpx version=\"1.1\"><wpt lat=\"1.23\" lon=\"2.34\"><time>2022-01-30T11:34:56Z</time><name>123</name><desc>foo bar</desc></wpt></gpx>", gpxString);
    }
}
