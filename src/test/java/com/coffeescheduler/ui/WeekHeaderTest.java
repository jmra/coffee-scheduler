package com.coffeescheduler.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeekHeaderTest {

    @Test
    void weekOneShowsStartMonday() {
        String label = WeekHeader.format(1, LocalDate.of(2026, 1, 5), 1);

        assertEquals("W1 — Mon Jan 5", label);
    }

    @Test
    void laterWeeksAdvanceBySevenDaysEach() {
        LocalDate start = LocalDate.of(2026, 1, 5);

        assertEquals("W2 — Mon Jan 12", WeekHeader.format(2, start, 5));
        assertEquals("W5 — Mon Feb 2", WeekHeader.format(5, start, 5));
    }

    @Test
    void crossesYearBoundary() {
        LocalDate start = LocalDate.of(2026, 12, 28);

        assertEquals("W2 — Mon Jan 4", WeekHeader.format(2, start, 2));
    }

    @Test
    void addSpacesToSyncTextLength() {
        LocalDate start = LocalDate.of(2026, 1, 5);

        assertEquals("W2   — Mon Jan 12", WeekHeader.format(2, start, 100));
        assertEquals("W10  — Mon Mar 9", WeekHeader.format(10, start, 100));
        assertEquals("W100 — Mon Nov 29", WeekHeader.format(100, start, 100));
    }
}
