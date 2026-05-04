package com.coffeescheduler.model;

public record Clinician(
        String name,
        ContractedWeeks contractedWeeks,
        int maxBlockLength,
        int maxBlocksAtMaxLength,
        BlockLengthRange preferredBlockLength
) {
    public Clinician {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-blank");
        }
        if (contractedWeeks == null) {
            throw new IllegalArgumentException("contractedWeeks must not be null");
        }
        if (preferredBlockLength == null) {
            throw new IllegalArgumentException("preferredBlockLength must not be null");
        }
        if (maxBlockLength < 2) {
            throw new IllegalArgumentException("maxBlockLength must be >= 2 (global block minimum), was " + maxBlockLength);
        }
        if (maxBlocksAtMaxLength < 0) {
            throw new IllegalArgumentException("maxBlocksAtMaxLength must be >= 0, was " + maxBlocksAtMaxLength);
        }
    }
}
