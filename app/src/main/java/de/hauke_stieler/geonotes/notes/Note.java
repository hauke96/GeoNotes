package de.hauke_stieler.geonotes.notes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.hauke_stieler.geonotes.categories.Category;

public class Note {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault());

    private final long id;
    private final String description;
    private final double lat;
    private final double lon;
    private final String creationDateTime;
    private final Category category;

    public Note(long id, String description, double lat, double lon, String creationDateTime, Category category) {
        this.id = id;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
        this.creationDateTime = creationDateTime;
        this.category = category;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getCreationDateTimeString() {
        return creationDateTime;
    }

    public Calendar getCreationDateTime() throws ParseException {
        long time = SIMPLE_DATE_FORMAT.parse(creationDateTime).getTime();

        GregorianCalendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTimeInMillis(time);

        return calendar;
    }

    public static String getDateTimeString(Calendar now) {
        return SIMPLE_DATE_FORMAT.format(now.getTime());
    }

    public Category getCategory() {
        return category;
    }
}
