package com.coffeescheduler.model;

public record DemandOverride(int startWeek, int endWeek, WeeklyDemand demand) {
    public DemandOverride {
        if (startWeek < 1) {
            throw new IllegalArgumentException("startWeek must be >= 1, was " + startWeek);
        }
        if (endWeek < startWeek) {
            throw new IllegalArgumentException("endWeek (" + endWeek + ") must be >= startWeek (" + startWeek + ")");
        }
        if (demand == null) {
            throw new IllegalArgumentException("demand must not be null");
        }
    }
}
