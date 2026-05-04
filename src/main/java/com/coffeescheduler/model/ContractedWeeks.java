package com.coffeescheduler.model;

public record ContractedWeeks(int min, int max) {
    public ContractedWeeks {
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0, was " + min);
        }
        if (max < min) {
            throw new IllegalArgumentException("max (" + max + ") must be >= min (" + min + ")");
        }
    }
}
