package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClinicianTest {

    private static final ContractedWeeks CONTRACTED = new ContractedWeeks(20, 24);
    private static final BlockLengthRange PREFERRED = new BlockLengthRange(4, 5);

    @Test
    void holdsTheGivenConfig() {
        Clinician c = new Clinician("Dr. Adams", CONTRACTED, 6, 2, PREFERRED);

        assertEquals("Dr. Adams", c.name());
        assertEquals(CONTRACTED, c.contractedWeeks());
        assertEquals(6, c.maxBlockLength());
        assertEquals(2, c.maxBlocksAtMaxLength());
        assertEquals(PREFERRED, c.preferredBlockLength());
    }

    @Test
    void rejectsNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Clinician(null, CONTRACTED, 6, 2, PREFERRED));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Clinician("   ", CONTRACTED, 6, 2, PREFERRED));
    }

    @Test
    void rejectsNullContractedWeeks() {
        assertThrows(IllegalArgumentException.class,
                () -> new Clinician("Dr. Adams", null, 6, 2, PREFERRED));
    }

    @Test
    void rejectsNullPreferredBlockLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new Clinician("Dr. Adams", CONTRACTED, 6, 2, null));
    }

    @Test
    void rejectsMaxBlockLengthBelowTwo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Clinician("Dr. Adams", CONTRACTED, 1, 2, PREFERRED));
    }

    @Test
    void rejectsNegativeMaxBlocksAtMaxLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new Clinician("Dr. Adams", CONTRACTED, 6, -1, PREFERRED));
    }
}
