package com.coffeescheduler.model;

public record Block(int startWeek, int length) {
    public Block {
        if (startWeek < 1) {
            throw new IllegalArgumentException("startWeek must be >= 1, was " + startWeek);
        }
        if (length < 1) {
            throw new IllegalArgumentException("length must be >= 1, was " + length);
        }
    }
}
