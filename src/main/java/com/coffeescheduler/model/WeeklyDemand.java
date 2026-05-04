package com.coffeescheduler.model;

public record WeeklyDemand(int min, int ideal, int max) {
    public WeeklyDemand {
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0, was " + min);
        }
        if (ideal < min) {
            throw new IllegalArgumentException("ideal (" + ideal + ") must be >= min (" + min + ")");
        }
        if (max < ideal) {
            throw new IllegalArgumentException("max (" + max + ") must be >= ideal (" + ideal + ")");
        }
    }
}
