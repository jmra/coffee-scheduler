package com.coffeescheduler.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public final class WeekHeader {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);

    private WeekHeader() {}

    public static String format(int weekIndex, LocalDate startMonday, int totalWeeks) {
        int indexLength = 0;
        int localIndex = weekIndex;
        while (localIndex > 0)
        {
            localIndex /= 10;
            indexLength++;
        }

        int totalLength = 0;
        int localTotal = totalWeeks;
        while (localTotal > 0)
        {
            localTotal /= 10;
            totalLength++;
        }

        String spacer = "";
        int spacerLength = totalLength - indexLength;
        if (spacerLength > 0)
        {
            char[] arr = new char[spacerLength];
            Arrays.fill(arr, ' ');
            spacer = new String(arr);
        }
        LocalDate weekDate = startMonday.plusWeeks(weekIndex - 1);
        return "W" + weekIndex + spacer + " — " + DATE.format(weekDate);
    }
}
