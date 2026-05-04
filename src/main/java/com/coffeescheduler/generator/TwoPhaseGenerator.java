package com.coffeescheduler.generator;

import com.coffeescheduler.model.Schedule;

public class TwoPhaseGenerator implements ScheduleGenerator {

    private final ConstructiveGenerator phase1 = new ConstructiveGenerator();
    private final LocalSearchImprover phase2 = new LocalSearchImprover();

    @Override
    public GeneratorResult generate(Schedule schedule) {
        phase1.generate(schedule);
        ScheduleScorer.ScoreResult result = phase2.improve(schedule);
        return new GeneratorResult(schedule, result.violations());
    }
}
