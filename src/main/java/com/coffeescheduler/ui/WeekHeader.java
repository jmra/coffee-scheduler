package com.coffeescheduler.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public final class WeekHeader {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(UIConstants.DATE_PATTERN_WEEK_HEADER, Locale.ENGLISH);

    private WeekHeader() {}

    public static String format(int weekIndex, LocalDate startMonday, int totalWeeks) {
        return format(weekIndex, startMonday, totalWeeks, 0);
    }

    public static String format(int weekIndex, LocalDate startMonday, int totalWeeks, int scheduleBlock) {
        String prefix = scheduleBlock > 0 ? "B" + scheduleBlock + " " : "";
        String weekNum = "W" + weekIndex + pad(weekIndex, totalWeeks);
        LocalDate weekDate = startMonday.plusWeeks(weekIndex - 1);
        return prefix + weekNum + " — " + DATE.format(weekDate);
    }

    private static String pad(int index, int total) {
        int indexLen = digitCount(index);
        int totalLen = digitCount(total);
        int spacerLen = totalLen - indexLen;
        if (spacerLen <= 0) return "";
        char[] arr = new char[spacerLen];
        Arrays.fill(arr, ' ');
        return new String(arr);
    }

    private static int digitCount(int n) {
        if (n <= 0) return 1;
        int count = 0;
        while (n > 0) { n /= 10; count++; }
        return count;
    }
}
