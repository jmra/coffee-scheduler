package com.coffeescheduler.model;

public record RuleViolation(String message, Clinician clinician, Integer week) {
    public RuleViolation {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must be non-blank");
        }
    }
}
