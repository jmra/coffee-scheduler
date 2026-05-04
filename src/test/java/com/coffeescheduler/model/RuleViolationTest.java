package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleViolationTest {

    private static final Clinician ADAMS = new Clinician(
            "Dr. Adams",
            new ContractedWeeks(20, 24),
            6, 2,
            new BlockLengthRange(4, 5));

    @Test
    void holdsTheGivenFields() {
        RuleViolation v = new RuleViolation("Dr. Adams: max block exceeded", ADAMS, 12);

        assertEquals("Dr. Adams: max block exceeded", v.message());
        assertEquals(ADAMS, v.clinician());
        assertEquals(12, v.week());
    }

    @Test
    void allowsNullClinicianForWeekOnlyViolations() {
        RuleViolation v = new RuleViolation("Week 7 understaffed", null, 7);

        assertNull(v.clinician());
        assertEquals(7, v.week());
    }

    @Test
    void allowsNullWeekForClinicianOnlyViolations() {
        RuleViolation v = new RuleViolation("Dr. Adams: under contracted weeks", ADAMS, null);

        assertEquals(ADAMS, v.clinician());
        assertNull(v.week());
    }

    @Test
    void rejectsBlankMessage() {
        assertThrows(IllegalArgumentException.class,
                () -> new RuleViolation("   ", ADAMS, 1));
    }
}
