package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemandOverrideTest {

    @Test
    void createsOverrideWithStartEndAndDemand() {
        WeeklyDemand demand = new WeeklyDemand(1, 2, 3);
        DemandOverride override = new DemandOverride(10, 15, demand);

        assertEquals(10, override.startWeek());
        assertEquals(15, override.endWeek());
        assertEquals(demand, override.demand());
    }

    @Test
    void singleWeekSpanIsValid() {
        DemandOverride override = new DemandOverride(5, 5, new WeeklyDemand(1, 2, 3));

        assertEquals(5, override.startWeek());
        assertEquals(5, override.endWeek());
    }

    @Test
    void rejectsStartWeekBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new DemandOverride(0, 5, new WeeklyDemand(1, 2, 3)));
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class,
                () -> new DemandOverride(10, 9, new WeeklyDemand(1, 2, 3)));
    }

    @Test
    void rejectsNullDemand() {
        assertThrows(IllegalArgumentException.class,
                () -> new DemandOverride(1, 5, null));
    }
}
