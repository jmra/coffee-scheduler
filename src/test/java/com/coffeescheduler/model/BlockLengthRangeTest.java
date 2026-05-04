package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockLengthRangeTest {

    @Test
    void holdsTheGivenMinAndMax() {
        BlockLengthRange range = new BlockLengthRange(4, 5);
        assertEquals(4, range.min());
        assertEquals(5, range.max());
    }

    @Test
    void rejectsMinLessThanTwo() {
        assertThrows(IllegalArgumentException.class, () -> new BlockLengthRange(1, 5));
    }

    @Test
    void rejectsMaxLessThanMin() {
        assertThrows(IllegalArgumentException.class, () -> new BlockLengthRange(5, 4));
    }
}
