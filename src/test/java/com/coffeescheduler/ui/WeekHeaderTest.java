package com.coffeescheduler.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeekHeaderTest {

    @Test
    void weekOneShowsStartMonday() {
        String label = WeekHeader.format(1, LocalDate.of(2026, 1, 5));

        assertEquals("W1 — Mon Jan 5", label);
    }

    @Test
    void laterWeeksAdvanceBySevenDaysEach() {
        LocalDate start = LocalDate.of(2026, 1, 5);

        assertEquals("W2 — Mon Jan 12", WeekHeader.format(2, start));
        assertEquals("W5 — Mon Feb 2", WeekHeader.format(5, start));
    }

    @Test
    void crossesYearBoundary() {
        LocalDate start = LocalDate.of(2026, 12, 28);

        assertEquals("W2 — Mon Jan 4", WeekHeader.format(2, start));
    }
}
