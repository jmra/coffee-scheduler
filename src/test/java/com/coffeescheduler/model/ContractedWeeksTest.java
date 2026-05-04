package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractedWeeksTest {

    @Test
    void holdsTheGivenMinAndMax() {
        ContractedWeeks cw = new ContractedWeeks(20, 24);
        assertEquals(20, cw.min());
        assertEquals(24, cw.max());
    }

    @Test
    void rejectsNegativeMin() {
        assertThrows(IllegalArgumentException.class, () -> new ContractedWeeks(-1, 5));
    }

    @Test
    void rejectsMaxLessThanMin() {
        assertThrows(IllegalArgumentException.class, () -> new ContractedWeeks(10, 9));
    }
}
