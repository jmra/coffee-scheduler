package com.coffeescheduler.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class WeekHeader {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);

    private WeekHeader() {}

    public static String format(int weekIndex, LocalDate startMonday) {
        LocalDate weekDate = startMonday.plusWeeks(weekIndex - 1);
        return "W" + weekIndex + " — " + DATE.format(weekDate);
    }
}
