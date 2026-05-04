package com.coffeescheduler.generator;

import com.coffeescheduler.model.RuleViolation;
import com.coffeescheduler.model.Schedule;

import java.util.List;

public record GeneratorResult(Schedule schedule, List<RuleViolation> violations) {
    public GeneratorResult {
        violations = List.copyOf(violations);
    }
}
