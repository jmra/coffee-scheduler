package com.coffeescheduler.generator;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.DemandOverride;
import com.coffeescheduler.model.ExclusionGroup;
import com.coffeescheduler.model.InclusionGroup;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;
import com.coffeescheduler.model.WeeklyDemand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleScorerTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 5);
    private final ScheduleScorer scorer = new ScheduleScorer();

    @Test
    void cleanScheduleHasNoViolations() {
        Clinician a = clinician("Dr. A", 2, 4);
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().isEmpty(), "violations: " + result.violations());
    }

    @Test
    void detectsContractedMinViolation() {
        Clinician a = clinician("Dr. A", 5, 10);
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 0, 1), 2);
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("contracted weeks")));
    }

    @Test
    void detectsContractedMaxViolation() {
        Clinician a = clinician("Dr. A", 1, 2);
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        for (int w = 1; w <= 5; w++) s.setState(a, w, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("exceeds contracted max")));
    }

    @Test
    void detectsMinBlockLengthViolation() {
        Clinician a = clinician("Dr. A", 1, 4);
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setState(a, 1, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("min is 2")));
    }

    @Test
    void detectsMaxBlockLengthViolation() {
        Clinician a = new Clinician("Dr. A", new ContractedWeeks(1, 10), 3, 5, new BlockLengthRange(2, 3));
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        for (int w = 1; w <= 5; w++) s.setState(a, w, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("max is 3")));
    }

    @Test
    void detectsRestViolation() {
        Clinician a = clinician("Dr. A", 4, 10);
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);
        s.setState(a, 4, WeekState.ON);
        s.setState(a, 5, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("rest gap")));
    }

    @Test
    void detectsDemandMinViolation() {
        Clinician a = clinician("Dr. A", 0, 10);
        Schedule s = new Schedule(START, 5, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("only 0 on, min is 1")));
    }

    @Test
    void preferOnBonusIncreasesSoftScore() {
        Clinician a = clinician("Dr. A", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);

        int baseline = scorer.score(s).softScore();

        s.setMarker(a, 1, WeekMarker.PREFER_ON);
        int withMarker = scorer.score(s).softScore();

        assertTrue(withMarker > baseline);
    }

    @Test
    void preferredBlockLengthBonusIncreasesSoftScore() {
        Clinician a = new Clinician("Dr. A", new ContractedWeeks(2, 10), 6, 5, new BlockLengthRange(2, 3));
        Schedule s1 = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s1.setState(a, 1, WeekState.ON);
        s1.setState(a, 2, WeekState.ON);
        int scoreInRange = scorer.score(s1).softScore();

        Clinician b = new Clinician("Dr. A", new ContractedWeeks(5, 10), 6, 5, new BlockLengthRange(4, 5));
        Schedule s2 = new Schedule(START, 10, List.of(b), new WeeklyDemand(0, 1, 1), 2);
        s2.setState(b, 1, WeekState.ON);
        s2.setState(b, 2, WeekState.ON);
        int scoreOutOfRange = scorer.score(s2).softScore();

        assertTrue(scoreInRange > scoreOutOfRange);
    }

    @Test
    void totalScoreWeightsViolationsHeavily() {
        Clinician a = clinician("Dr. A", 10, 10);
        Schedule s = new Schedule(START, 5, List.of(a), new WeeklyDemand(0, 0, 5), 2);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.totalScore() < 0, "violations should make total score negative");
    }

    @Test
    void detectsExclusionGroupViolation() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 2, 2), 2);
        s.addExclusionGroup(new ExclusionGroup("No overlap", Set.of("Dr. A", "Dr. B")));
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);
        s.setState(b, 1, WeekState.ON);
        s.setState(b, 2, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("No overlap") && v.week() != null));
    }

    @Test
    void noExclusionViolationWhenMembersOnDifferentWeeks() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 1, 2), 2);
        s.addExclusionGroup(new ExclusionGroup("No overlap", Set.of("Dr. A", "Dr. B")));
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);
        s.setState(b, 5, WeekState.ON);
        s.setState(b, 6, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().noneMatch(v ->
                v.message().contains("No overlap")));
    }

    @Test
    void exclusionViolationIsOnePerGroupPerWeek() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Clinician c = clinician("Dr. C", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b, c), new WeeklyDemand(0, 3, 3), 2);
        s.addExclusionGroup(new ExclusionGroup("Triple", Set.of("Dr. A", "Dr. B", "Dr. C")));
        // All three on in weeks 1 and 2
        for (Clinician cl : List.of(a, b, c)) {
            s.setState(cl, 1, WeekState.ON);
            s.setState(cl, 2, WeekState.ON);
        }

        ScheduleScorer.ScoreResult result = scorer.score(s);

        long exclusionViolations = result.violations().stream()
                .filter(v -> v.message().contains("Triple"))
                .count();
        assertEquals(2, exclusionViolations, "one violation per week for weeks 1 and 2");
    }

    @Test
    void exclusionViolationHasNullClinician() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 2, 2), 2);
        s.addExclusionGroup(new ExclusionGroup("Group X", Set.of("Dr. A", "Dr. B")));
        s.setState(a, 3, WeekState.ON);
        s.setState(b, 3, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("Group X") && v.clinician() == null && v.week() == 3));
    }

    @Test
    void detectsInclusionGroupViolation() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 1, 2), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));
        // Week 1: neither is ON → violation
        s.setState(a, 3, WeekState.ON);
        s.setState(a, 4, WeekState.ON);
        s.setState(b, 7, WeekState.ON);
        s.setState(b, 8, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("Coverage") && v.week() != null));
    }

    @Test
    void noInclusionViolationWhenMemberIsOn() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 4, List.of(a, b), new WeeklyDemand(0, 1, 2), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));
        // Every week has at least one member ON
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);
        s.setState(b, 3, WeekState.ON);
        s.setState(b, 4, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().noneMatch(v ->
                v.message().contains("Coverage")));
    }

    @Test
    void inclusionViolationIsOnePerGroupPerWeek() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 1, 2), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));
        // Neither member ON for any week

        ScheduleScorer.ScoreResult result = scorer.score(s);

        long inclusionViolations = result.violations().stream()
                .filter(v -> v.message().contains("Coverage"))
                .count();
        assertEquals(10, inclusionViolations, "one violation per week for all 10 weeks");
    }

    @Test
    void inclusionViolationHasNullClinician() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 1, 2), 2);
        s.addInclusionGroup(new InclusionGroup("Group Y", Set.of("Dr. A", "Dr. B")));

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("Group Y") && v.clinician() == null && v.week() == 1));
    }

    @Test
    void scorerUsesOverriddenDemandForViolations() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        // Default demand min=0, max=2. Override weeks 1-2 to min=2.
        Schedule s = new Schedule(START, 4, List.of(a, b), new WeeklyDemand(0, 1, 2), 2);
        s.addDemandOverride(new DemandOverride(1, 2, new WeeklyDemand(2, 2, 2)));
        // Only A on for weeks 1-2 → violates overridden min=2
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("min") && v.week() == 1));
        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("min") && v.week() == 2));
    }

    @Test
    void scorerUsesDefaultDemandForNonOverriddenWeeks() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        // Default demand max=1. Override weeks 1-2 to max=2.
        Schedule s = new Schedule(START, 4, List.of(a, b), new WeeklyDemand(0, 1, 1), 2);
        s.addDemandOverride(new DemandOverride(1, 2, new WeeklyDemand(0, 2, 2)));
        // Both on for weeks 1-2 (ok, override max=2) and week 3 (violates default max=1)
        s.setState(a, 1, WeekState.ON);
        s.setState(b, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);
        s.setState(b, 2, WeekState.ON);
        s.setState(a, 3, WeekState.ON);
        s.setState(b, 3, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        // No max violation for weeks 1-2
        assertTrue(result.violations().stream().noneMatch(v ->
                v.message().contains("max") && (v.week() == 1 || v.week() == 2)));
        // Max violation for week 3
        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("max") && v.week() == 3));
    }

    @Test
    void scorerSoftScoreUsesOverriddenIdeal() {
        Clinician a = clinician("Dr. A", 2, 10);
        // Default ideal=1, override weeks 1-2 to ideal=0
        Schedule s = new Schedule(START, 4, List.of(a), new WeeklyDemand(0, 1, 2), 2);
        s.addDemandOverride(new DemandOverride(1, 2, new WeeklyDemand(0, 0, 2)));
        // A on for weeks 1-2: each deviates from overridden ideal=0 by 1
        // Weeks 3-4: no one on, deviates from default ideal=1 by 1
        s.setState(a, 1, WeekState.ON);
        s.setState(a, 2, WeekState.ON);

        ScheduleScorer.ScoreResult withOverride = scorer.score(s);

        // Compare: same schedule without override, ideal=1 everywhere
        Schedule s2 = new Schedule(START, 4, List.of(a), new WeeklyDemand(0, 1, 2), 2);
        s2.setState(a, 1, WeekState.ON);
        s2.setState(a, 2, WeekState.ON);

        ScheduleScorer.ScoreResult withoutOverride = scorer.score(s2);

        // With override: weeks 1-2 deviate by 1 from ideal=0, weeks 3-4 deviate by 1 from ideal=1 → total -4
        // Without override: weeks 1-2 deviate by 0 from ideal=1, weeks 3-4 deviate by 1 from ideal=1 → total -2
        assertTrue(withOverride.softScore() < withoutOverride.softScore(),
                "override should penalize weeks 1-2 for deviating from overridden ideal=0");
    }

    // --- Block-alignment tests ---

    @Test
    void penalizesMisalignedBlockStart() {
        Clinician a = clinician("Dr. A", 2, 10);
        Schedule s = new Schedule(START, 9, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setScheduleBlockSizes(List.of(3, 3, 3));
        // Block starting at week 2 — not a boundary (boundaries are 1, 4, 7)
        s.setState(a, 2, WeekState.ON);
        s.setState(a, 3, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        assertTrue(result.softScore() < 0, "misaligned start should produce negative soft score");
    }

    @Test
    void noPenaltyForAlignedBlockStart() {
        Clinician a = clinician("Dr. A", 2, 10);
        Schedule s = new Schedule(START, 9, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setScheduleBlockSizes(List.of(3, 3, 3));
        // Block starting at week 4 — a boundary
        s.setState(a, 4, WeekState.ON);
        s.setState(a, 5, WeekState.ON);

        ScheduleScorer.ScoreResult aligned = scorer.score(s);

        // Compare with misaligned
        Schedule s2 = new Schedule(START, 9, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s2.setScheduleBlockSizes(List.of(3, 3, 3));
        s2.setState(a, 5, WeekState.ON);
        s2.setState(a, 6, WeekState.ON);

        ScheduleScorer.ScoreResult misaligned = scorer.score(s2);

        assertTrue(aligned.softScore() > misaligned.softScore(),
                "aligned start should score higher than misaligned");
    }

    @Test
    void noPenaltyWhenSingleScheduleBlock() {
        Clinician a = clinician("Dr. A", 2, 10);
        Schedule s = new Schedule(START, 9, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        // Default: single schedule block = no alignment penalty
        s.setState(a, 2, WeekState.ON);
        s.setState(a, 3, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        // No alignment penalty (only demand deviation remains)
        // The soft score should not include -20 for misalignment
        Schedule s2 = new Schedule(START, 9, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s2.setScheduleBlockSizes(List.of(3, 3, 3));
        s2.setState(a, 2, WeekState.ON);
        s2.setState(a, 3, WeekState.ON);

        ScheduleScorer.ScoreResult withBlocks = scorer.score(s2);

        assertTrue(result.softScore() > withBlocks.softScore(),
                "single schedule block should not get alignment penalty but multi-block should");
    }

    @Test
    void misalignedBlockStartPenaltyIsHeavy() {
        Clinician a = clinician("Dr. A", 2, 10);
        Schedule s = new Schedule(START, 9, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setScheduleBlockSizes(List.of(3, 3, 3));
        s.setState(a, 2, WeekState.ON);
        s.setState(a, 3, WeekState.ON);

        ScheduleScorer.ScoreResult result = scorer.score(s);

        // Penalty should be at least -20 (the agreed weight)
        // Other soft factors (demand deviation) contribute small amounts
        assertTrue(result.softScore() <= -20,
                "misaligned penalty should be at least -20, got " + result.softScore());
    }

    private static Clinician clinician(String name, int contractMin, int contractMax) {
        return new Clinician(name, new ContractedWeeks(contractMin, contractMax),
                6, 2, new BlockLengthRange(4, 5));
    }
}
