package com.coffeescheduler.generator;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;
import com.coffeescheduler.model.WeeklyDemand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwoPhaseGeneratorTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 5);

    @Test
    void producesValidSchedule() {
        Clinician a = clinician("Dr. A", 4, 5);
        Clinician b = clinician("Dr. B", 4, 5);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(1, 1, 1), 2);

        GeneratorResult result = new TwoPhaseGenerator().generate(s);

        assertTrue(result.violations().isEmpty(), "violations: " + result.violations());
        int onA = countOn(s, a);
        int onB = countOn(s, b);
        assertTrue(onA >= 4 && onA <= 5, "Dr. A on count: " + onA);
        assertTrue(onB >= 4 && onB <= 5, "Dr. B on count: " + onB);
    }

    @Test
    void phase2DoesNotWorsenPhase1() {
        Clinician a = clinician("Dr. A", 10, 20);
        Clinician b = clinician("Dr. B", 10, 20);
        Schedule s1 = new Schedule(START, 52, List.of(a, b), new WeeklyDemand(1, 2, 2), 2);
        Schedule s2 = new Schedule(START, 52, List.of(a, b), new WeeklyDemand(1, 2, 2), 2);

        new ConstructiveGenerator().generate(s1);
        ScheduleScorer.ScoreResult phase1Score = new ScheduleScorer().score(s1);

        new TwoPhaseGenerator().generate(s2);
        ScheduleScorer.ScoreResult phase2Score = new ScheduleScorer().score(s2);

        assertTrue(phase2Score.totalScore() >= phase1Score.totalScore(),
                "phase2 (" + phase2Score.totalScore() + ") should not be worse than phase1 ("
                        + phase1Score.totalScore() + ")");
    }

    @Test
    void deterministicOutput() {
        Clinician a = clinician("Dr. A", 10, 20);
        Clinician b = clinician("Dr. B", 10, 20);
        Schedule s1 = new Schedule(START, 52, List.of(a, b), new WeeklyDemand(1, 2, 2), 2);
        Schedule s2 = new Schedule(START, 52, List.of(a, b), new WeeklyDemand(1, 2, 2), 2);

        new TwoPhaseGenerator().generate(s1);
        new TwoPhaseGenerator().generate(s2);

        for (int w = 1; w <= 52; w++) {
            for (Clinician c : List.of(a, b)) {
                assertEquals(s1.stateOf(c, w), s2.stateOf(c, w),
                        "non-deterministic at " + c.name() + " week " + w);
            }
        }
    }

    @Test
    void respectsPinnedCells() {
        Clinician a = clinician("Dr. A", 2, 6);
        Clinician b = clinician("Dr. B", 2, 6);
        Schedule s = new Schedule(START, 12, List.of(a, b), new WeeklyDemand(1, 1, 1), 2);
        s.setState(b, 1, WeekState.ON);
        s.pin(b, 1);

        new TwoPhaseGenerator().generate(s);

        assertEquals(WeekState.ON, s.stateOf(b, 1), "pinned cell should be preserved");
    }

    @Test
    void multipleCliniciansMeetDemand() {
        Clinician a = clinician("Dr. A", 10, 20);
        Clinician b = clinician("Dr. B", 10, 20);
        Clinician c = clinician("Dr. C", 10, 20);
        Schedule s = new Schedule(START, 20, List.of(a, b, c), new WeeklyDemand(2, 2, 3), 2);

        GeneratorResult result = new TwoPhaseGenerator().generate(s);

        for (int w = 1; w <= 20; w++) {
            int onCount = s.onClinicians(w).size();
            assertTrue(onCount >= 2 || !result.violations().isEmpty(),
                    "week " + w + " has " + onCount + " on, min is 2");
        }
    }

    private static int countOn(Schedule s, Clinician c) {
        int count = 0;
        for (int w = 1; w <= s.lengthWeeks(); w++) {
            if (s.stateOf(c, w) == WeekState.ON) count++;
        }
        return count;
    }

    private static Clinician clinician(String name, int contractMin, int contractMax) {
        return new Clinician(name, new ContractedWeeks(contractMin, contractMax),
                6, 2, new BlockLengthRange(4, 5));
    }
}
