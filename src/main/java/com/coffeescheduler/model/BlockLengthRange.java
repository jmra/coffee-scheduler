package com.coffeescheduler.model;

public record BlockLengthRange(int min, int max) {
    public BlockLengthRange {
        if (min < 2) {
            throw new IllegalArgumentException("min must be >= 2 (global block minimum), was " + min);
        }
        if (max < min) {
            throw new IllegalArgumentException("max (" + max + ") must be >= min (" + min + ")");
        }
    }
}
