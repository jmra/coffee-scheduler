package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeeklyDemandTest {

    @Test
    void holdsTheGivenMinIdealMax() {
        WeeklyDemand demand = new WeeklyDemand(3, 4, 5);
        assertEquals(3, demand.min());
        assertEquals(4, demand.ideal());
        assertEquals(5, demand.max());
    }

    @Test
    void rejectsNegativeMin() {
        assertThrows(IllegalArgumentException.class, () -> new WeeklyDemand(-1, 4, 5));
    }

    @Test
    void rejectsIdealLessThanMin() {
        assertThrows(IllegalArgumentException.class, () -> new WeeklyDemand(3, 2, 5));
    }

    @Test
    void rejectsMaxLessThanIdeal() {
        assertThrows(IllegalArgumentException.class, () -> new WeeklyDemand(3, 4, 3));
    }
}
