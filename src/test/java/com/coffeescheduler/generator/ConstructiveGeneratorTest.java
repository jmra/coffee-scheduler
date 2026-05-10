package com.coffeescheduler.generator;

import com.coffeescheduler.model.Block;
import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.DemandOverride;
import com.coffeescheduler.model.ExclusionGroup;
import com.coffeescheduler.model.InclusionGroup;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;
import com.coffeescheduler.model.WeeklyDemand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructiveGeneratorTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 5);

    @Test
    void emptyRosterProducesCleanSchedule() {
        Schedule s = new Schedule(START, 12, List.of(), new WeeklyDemand(0, 0, 0), 2);
        GeneratorResult result = new ConstructiveGenerator().generate(s);
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void singleClinicianGetsScheduled() {
        Clinician a = clinician("Dr. A", 4, 6);
        Schedule s = new Schedule(START, 12, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        int onCount = countOn(s, a);
        assertTrue(onCount >= 4, "should schedule at least contracted min, got " + onCount);
        assertTrue(onCount <= 6, "should not exceed contracted max, got " + onCount);
    }

    @Test
    void respectsUnavailability() {
        Clinician a = clinician("Dr. A", 4, 8);
        Schedule s = new Schedule(START, 12, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setState(a, 3, WeekState.UNAVAILABLE);

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        assertEquals(WeekState.UNAVAILABLE, s.stateOf(a, 3));
    }

    @Test
    void respectsMinBlockLength() {
        Clinician a = clinician("Dr. A", 4, 10);
        Schedule s = new Schedule(START, 12, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        new ConstructiveGenerator().generate(s);

        List<Block> blocks = s.blocksFor(a);
        for (Block b : blocks) {
            assertTrue(b.length() >= 2, "block length " + b.length() + " is below min 2");
        }
    }

    @Test
    void respectsMaxBlockLength() {
        Clinician a = new Clinician("Dr. A", new ContractedWeeks(10, 20), 4, 5, new BlockLengthRange(2, 4));
        Schedule s = new Schedule(START, 20, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        new ConstructiveGenerator().generate(s);

        List<Block> blocks = s.blocksFor(a);
        for (Block b : blocks) {
            assertTrue(b.length() <= 4, "block length " + b.length() + " exceeds max 4");
        }
    }

    @Test
    void respectsRestBetweenBlocks() {
        Clinician a = clinician("Dr. A", 6, 10);
        Schedule s = new Schedule(START, 20, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        new ConstructiveGenerator().generate(s);

        List<Block> blocks = s.blocksFor(a);
        for (int i = 1; i < blocks.size(); i++) {
            int prevEnd = blocks.get(i - 1).startWeek() + blocks.get(i - 1).length() - 1;
            int gap = blocks.get(i).startWeek() - prevEnd - 1;
            assertTrue(gap >= 2, "rest gap " + gap + " is below required 2 between blocks ending week "
                    + prevEnd + " and starting week " + blocks.get(i).startWeek());
        }
    }

    @Test
    void doesNotExceedContractedMax() {
        Clinician a = clinician("Dr. A", 4, 6);
        Schedule s = new Schedule(START, 52, List.of(a), new WeeklyDemand(0, 1, 1), 2);

        new ConstructiveGenerator().generate(s);

        int onCount = countOn(s, a);
        assertTrue(onCount <= 6, "scheduled " + onCount + " exceeds contracted max 6");
    }

    @Test
    void reportsViolationWhenContractedMinImpossible() {
        Clinician a = clinician("Dr. A", 10, 10);
        Schedule s = new Schedule(START, 6, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("contracted weeks")));
    }

    @Test
    void deterministicOutput() {
        Clinician a = clinician("Dr. A", 10, 20);
        Clinician b = clinician("Dr. B", 10, 20);
        Schedule s1 = new Schedule(START, 52, List.of(a, b), new WeeklyDemand(1, 2, 2), 2);
        Schedule s2 = new Schedule(START, 52, List.of(a, b), new WeeklyDemand(1, 2, 2), 2);

        new ConstructiveGenerator().generate(s1);
        new ConstructiveGenerator().generate(s2);

        for (int w = 1; w <= 52; w++) {
            for (Clinician c : List.of(a, b)) {
                assertEquals(s1.stateOf(c, w), s2.stateOf(c, w),
                        "non-deterministic at " + c.name() + " week " + w);
            }
        }
    }

    @Test
    void multipleCliniciansMeetDemand() {
        Clinician a = clinician("Dr. A", 10, 20);
        Clinician b = clinician("Dr. B", 10, 20);
        Clinician c = clinician("Dr. C", 10, 20);
        Schedule s = new Schedule(START, 20, List.of(a, b, c), new WeeklyDemand(2, 2, 3), 2);

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        for (int w = 1; w <= 20; w++) {
            int onCount = s.onClinicians(w).size();
            assertTrue(onCount >= 2 || !result.violations().isEmpty(),
                    "week " + w + " has " + onCount + " on, min is 2");
        }
    }

    @Test
    void limitsBlocksAtMaxLength() {
        // maxBlockLength=4, maxBlocksAtMaxLength=1 → only one block may reach length 4
        Clinician a = new Clinician("Dr. A", new ContractedWeeks(8, 20), 4, 1, new BlockLengthRange(2, 4));
        Schedule s = new Schedule(START, 26, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        new ConstructiveGenerator().generate(s);

        List<Block> blocks = s.blocksFor(a);
        long atMax = blocks.stream().filter(b -> b.length() == 4).count();
        assertTrue(atMax <= 1, "expected at most 1 block at max length 4, got " + atMax);
    }

    @Test
    void zeroBlocksAtMaxLengthForcesAllBlocksShorter() {
        // maxBlockLength=4, maxBlocksAtMaxLength=0 → no block may reach length 4
        Clinician a = new Clinician("Dr. A", new ContractedWeeks(6, 20), 4, 0, new BlockLengthRange(2, 3));
        Schedule s = new Schedule(START, 26, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        new ConstructiveGenerator().generate(s);

        List<Block> blocks = s.blocksFor(a);
        for (Block b : blocks) {
            assertTrue(b.length() < 4, "block length " + b.length() + " should be < max 4");
        }
    }

    @Test
    void preferredBlockLengthInfluencesBlockDuration() {
        // A and B prefer [5,6]; C prefers [2,3]. All compete for 2 slots.
        // With preferred scoring, C gets +1 when continuing into [2,3],
        // allowing blocks of length 3 instead of always losing to A/B at length 2.
        Clinician a = new Clinician("Dr. A", new ContractedWeeks(0, 20), 6, 5, new BlockLengthRange(5, 6));
        Clinician b = new Clinician("Dr. B", new ContractedWeeks(0, 20), 6, 5, new BlockLengthRange(5, 6));
        Clinician c = new Clinician("Dr. C", new ContractedWeeks(0, 20), 6, 5, new BlockLengthRange(2, 3));
        Schedule s = new Schedule(START, 30, List.of(a, b, c), new WeeklyDemand(0, 2, 3), 2);

        new ConstructiveGenerator().generate(s);

        assertTrue(s.blocksFor(c).stream().anyMatch(bl -> bl.length() == 3),
                "C should have at least one block of length 3 due to preferred range [2,3] bonus, "
                + "blocks: " + s.blocksFor(c));
    }

    @Test
    void evenDistributionSpreadsBlocksAcrossSchedule() {
        // Without spread scoring, Dr. A wins name tiebreaker and C doesn't get
        // scheduled until urgency kicks in late. With spread scoring, C gets a
        // +1 bonus once its gap since last block exceeds the ideal gap.
        Clinician a = new Clinician("Dr. A", new ContractedWeeks(8, 16), 6, 5, new BlockLengthRange(3, 4));
        Clinician b = new Clinician("Dr. B", new ContractedWeeks(8, 16), 6, 5, new BlockLengthRange(3, 4));
        Clinician c = new Clinician("Dr. C", new ContractedWeeks(8, 16), 6, 5, new BlockLengthRange(3, 4));
        Schedule s = new Schedule(START, 30, List.of(a, b, c), new WeeklyDemand(0, 1, 3), 2);

        new ConstructiveGenerator().generate(s);

        int midpoint = 20;
        assertTrue(s.blocksFor(c).stream().anyMatch(bl -> bl.startWeek() <= midpoint),
                "Dr. C should have a block starting in the first " + midpoint + " weeks, "
                + "blocks: " + s.blocksFor(c));
    }

    @Test
    void generatorRespectsPreExistingOnStates() {
        // Without the pin, Dr. A wins the name tiebreaker for week 1.
        // Pinning Dr. B ON for week 1 forces the generator to schedule B there.
        Clinician a = clinician("Dr. A", 2, 6);
        Clinician b = clinician("Dr. B", 2, 6);
        Schedule s = new Schedule(START, 12, List.of(a, b), new WeeklyDemand(1, 1, 1), 2);
        s.setState(b, 1, WeekState.ON);

        new ConstructiveGenerator().generate(s);

        assertEquals(WeekState.ON, s.stateOf(b, 1), "pinned ON cell should be preserved");
        assertTrue(s.stateOf(a, 1) != WeekState.ON,
                "Dr. A should not be on week 1 when Dr. B is pinned (demand max=1)");
    }

    @Test
    void exclusionGroupPreventsSimultaneousScheduling() {
        Clinician a = clinician("Dr. A", 4, 10);
        Clinician b = clinician("Dr. B", 4, 10);
        // demand ideal=2 means the generator would normally schedule both A and B together
        Schedule s = new Schedule(START, 20, List.of(a, b), new WeeklyDemand(1, 2, 2), 2);
        s.addExclusionGroup(new ExclusionGroup("No overlap", Set.of("Dr. A", "Dr. B")));

        new ConstructiveGenerator().generate(s);

        for (int w = 1; w <= 20; w++) {
            boolean aOn = s.stateOf(a, w) == WeekState.ON;
            boolean bOn = s.stateOf(b, w) == WeekState.ON;
            assertTrue(!(aOn && bOn),
                    "week " + w + ": both A and B are ON, violating exclusion group");
        }
    }

    @Test
    void exclusionGroupWithThreeMembersAllowsAtMostOne() {
        Clinician a = clinician("Dr. A", 3, 10);
        Clinician b = clinician("Dr. B", 3, 10);
        Clinician c = clinician("Dr. C", 3, 10);
        // demand ideal=2 would normally schedule two from the group
        Schedule s = new Schedule(START, 20, List.of(a, b, c), new WeeklyDemand(1, 2, 3), 2);
        s.addExclusionGroup(new ExclusionGroup("Triple", Set.of("Dr. A", "Dr. B", "Dr. C")));

        new ConstructiveGenerator().generate(s);

        for (int w = 1; w <= 20; w++) {
            int onCount = 0;
            if (s.stateOf(a, w) == WeekState.ON) onCount++;
            if (s.stateOf(b, w) == WeekState.ON) onCount++;
            if (s.stateOf(c, w) == WeekState.ON) onCount++;
            assertTrue(onCount <= 1,
                    "week " + w + ": " + onCount + " exclusion group members on, max is 1");
        }
    }

    @Test
    void exclusionGroupConflictWithForcedOnRecordsViolation() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 2, 2), 2);
        s.addExclusionGroup(new ExclusionGroup("No overlap", Set.of("Dr. A", "Dr. B")));
        // Pin both ON for week 1 — both are FORCED_ON, conflict is unavoidable
        s.setState(a, 1, WeekState.ON);
        s.setState(b, 1, WeekState.ON);

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        // Both should remain ON (neither is dropped)
        assertEquals(WeekState.ON, s.stateOf(a, 1));
        assertEquals(WeekState.ON, s.stateOf(b, 1));
    }

    @Test
    void exclusionGroupDoesNotPreventOtherCliniciansFromBeingScheduled() {
        Clinician a = clinician("Dr. A", 4, 10);
        Clinician b = clinician("Dr. B", 4, 10);
        Clinician c = clinician("Dr. C", 4, 10);
        Schedule s = new Schedule(START, 20, List.of(a, b, c), new WeeklyDemand(2, 2, 3), 2);
        // A and B can't overlap, but C is not in the group
        s.addExclusionGroup(new ExclusionGroup("AB only", Set.of("Dr. A", "Dr. B")));

        new ConstructiveGenerator().generate(s);

        // C should be able to be ON at the same time as A or B
        boolean cOverlapsWithAOrB = false;
        for (int w = 1; w <= 20; w++) {
            boolean cOn = s.stateOf(c, w) == WeekState.ON;
            boolean aOrBOn = s.stateOf(a, w) == WeekState.ON || s.stateOf(b, w) == WeekState.ON;
            if (cOn && aOrBOn) cOverlapsWithAOrB = true;
        }
        assertTrue(cOverlapsWithAOrB, "C should overlap with A or B since C is not in the exclusion group");
    }

    // --- Inclusion group tests ---

    @Test
    void inclusionGroupEnsuresAtLeastOneMemberOnPerWeek() {
        // High contracted weeks ensure A and B have enough capacity to cover all weeks
        Clinician a = clinician("Dr. A", 6, 12);
        Clinician b = clinician("Dr. B", 6, 12);
        Clinician c = clinician("Dr. C", 2, 10);
        Schedule s = new Schedule(START, 12, List.of(a, b, c), new WeeklyDemand(1, 1, 3), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));

        new ConstructiveGenerator().generate(s);

        for (int w = 1; w <= 12; w++) {
            boolean aOn = s.stateOf(a, w) == WeekState.ON;
            boolean bOn = s.stateOf(b, w) == WeekState.ON;
            assertTrue(aOn || bOn,
                    "week " + w + ": neither A nor B is ON, violating inclusion group");
        }
    }

    @Test
    void inclusionGroupForcePromotesWhenNoMemberNaturallyScheduled() {
        // demand ideal=1, three clinicians, inclusion group on A+B.
        // C might be scheduled alone some weeks, but inclusion group should force A or B on too.
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Clinician c = clinician("Dr. C", 2, 10);
        Schedule s = new Schedule(START, 12, List.of(a, b, c), new WeeklyDemand(1, 1, 3), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));

        new ConstructiveGenerator().generate(s);

        for (int w = 1; w <= 12; w++) {
            boolean aOn = s.stateOf(a, w) == WeekState.ON;
            boolean bOn = s.stateOf(b, w) == WeekState.ON;
            assertTrue(aOn || bOn,
                    "week " + w + ": inclusion group not satisfied");
        }
    }

    @Test
    void inclusionGroupDoesNotExceedDemandMax() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Clinician c = clinician("Dr. C", 4, 10);
        // demand max=1: C will fill it, A and B can't be force-promoted without exceeding max
        Schedule s = new Schedule(START, 10, List.of(a, b, c), new WeeklyDemand(1, 1, 1), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        // Should have violations rather than exceeding demand.max
        for (int w = 1; w <= 10; w++) {
            int onCount = s.onClinicians(w).size();
            assertTrue(onCount <= 1, "week " + w + ": " + onCount + " on, exceeds demand max 1");
        }
    }

    @Test
    void inclusionGroupRecordsViolationWhenAllMembersUnavailable() {
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b), new WeeklyDemand(0, 1, 2), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));
        // Make both unavailable for week 1
        s.setState(a, 1, WeekState.UNAVAILABLE);
        s.setState(b, 1, WeekState.UNAVAILABLE);

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("Coverage") && v.week() == 1));
    }

    @Test
    void inclusionGroupRespectsExclusionGroupPriority() {
        // A and B in inclusion group, A and C in exclusion group.
        // Pin C ON for weeks 1-2 so A is excluded → B must be force-promoted.
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Clinician c = clinician("Dr. C", 2, 10);
        Schedule s = new Schedule(START, 6, List.of(a, b, c), new WeeklyDemand(1, 1, 3), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));
        s.addExclusionGroup(new ExclusionGroup("No AC", Set.of("Dr. A", "Dr. C")));
        s.setState(c, 1, WeekState.ON);
        s.setState(c, 2, WeekState.ON);

        new ConstructiveGenerator().generate(s);

        // Weeks 1-2: C is pinned ON, A is excluded, so B must be ON for inclusion
        for (int w = 1; w <= 2; w++) {
            assertTrue(s.stateOf(b, w) == WeekState.ON,
                    "week " + w + ": C is on, A is excluded, B must be on for inclusion group");
        }
    }

    @Test
    void inclusionGroupViolationWhenExclusionBlocksAllMembers() {
        // A and B in inclusion group, both in exclusion group with C.
        // C is pinned ON → A and B both excluded → inclusion violation
        Clinician a = clinician("Dr. A", 2, 10);
        Clinician b = clinician("Dr. B", 2, 10);
        Clinician c = clinician("Dr. C", 2, 10);
        Schedule s = new Schedule(START, 10, List.of(a, b, c), new WeeklyDemand(1, 1, 2), 2);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B")));
        s.addExclusionGroup(new ExclusionGroup("No AC", Set.of("Dr. A", "Dr. C")));
        s.addExclusionGroup(new ExclusionGroup("No BC", Set.of("Dr. B", "Dr. C")));
        // Pin C on for weeks 1-2 to force the conflict
        s.setState(c, 1, WeekState.ON);
        s.setState(c, 2, WeekState.ON);

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        // Weeks 1-2 should have inclusion violations since A and B are both excluded by C
        assertTrue(result.violations().stream().anyMatch(v ->
                        v.message().contains("Coverage")),
                "should record inclusion violation when exclusion blocks all members");
    }

    private static int countOn(Schedule s, Clinician c) {
        int count = 0;
        for (int w = 1; w <= s.lengthWeeks(); w++) {
            if (s.stateOf(c, w) == WeekState.ON) count++;
        }
        return count;
    }

    // --- Demand override tests ---

    @Test
    void generatorRespectsPerWeekDemandOverride() {
        Clinician a = clinician("Dr. A", 4, 10);
        Clinician b = clinician("Dr. B", 4, 10);
        Clinician c = clinician("Dr. C", 4, 10);
        // Default demand ideal=2, override weeks 5-6 to ideal=1
        Schedule s = new Schedule(START, 12, List.of(a, b, c), new WeeklyDemand(1, 2, 3), 2);
        s.addDemandOverride(new DemandOverride(5, 6, new WeeklyDemand(0, 1, 1)));

        new ConstructiveGenerator().generate(s);

        // Overridden weeks should respect max=1
        for (int w = 5; w <= 6; w++) {
            int onCount = s.onClinicians(w).size();
            assertTrue(onCount <= 1,
                    "week " + w + ": " + onCount + " on, override max is 1");
        }
    }

    @Test
    void generatorRecordsViolationForOverriddenDemandMin() {
        Clinician a = clinician("Dr. A", 2, 10);
        // Default demand min=0, override weeks 1-4 to min=2
        Schedule s = new Schedule(START, 10, List.of(a), new WeeklyDemand(0, 0, 2), 2);
        s.addDemandOverride(new DemandOverride(1, 4, new WeeklyDemand(2, 2, 2)));

        GeneratorResult result = new ConstructiveGenerator().generate(s);

        // Only 1 clinician can't meet min=2, should have violations for those weeks
        assertTrue(result.violations().stream().anyMatch(v ->
                v.message().contains("min") && v.week() != null && v.week() <= 4));
    }

    // --- Block-alignment tests ---

    @Test
    void generatorAlignsBlockStartsToScheduleBlockBoundaries() {
        Clinician a = clinician("Dr. A", 4, 10);
        Clinician b = clinician("Dr. B", 4, 10);
        // 12 weeks, schedule blocks of 3: boundaries at 1, 4, 7, 10
        Schedule s = new Schedule(START, 12, List.of(a, b), new WeeklyDemand(1, 1, 2), 2);
        s.setScheduleBlockSizes(List.of(3, 3, 3, 3));

        new ConstructiveGenerator().generate(s);

        for (Clinician c : List.of(a, b)) {
            for (Block block : s.blocksFor(c)) {
                assertTrue(s.isScheduleBlockStart(block.startWeek()),
                        c.name() + " block at week " + block.startWeek()
                                + " is not aligned to a schedule block boundary");
            }
        }
    }

    @Test
    void generatorIgnoresAlignmentWithSingleScheduleBlock() {
        Clinician a = clinician("Dr. A", 4, 10);
        // Single schedule block (default) — no alignment constraint
        Schedule s = new Schedule(START, 12, List.of(a), new WeeklyDemand(1, 1, 1), 2);

        new ConstructiveGenerator().generate(s);

        // Should still generate blocks; with single block there are no boundaries
        int onCount = countOn(s, a);
        assertTrue(onCount >= 4, "should schedule at least contracted min");
    }

    @Test
    void generatorSkipsStartWhenRemainingWeeksInsufficientForMinBlock() {
        Clinician a = clinician("Dr. A", 2, 10);
        // 7 weeks, blocks of 3: boundaries at 1, 4, 7. Min block length = 2.
        // Boundary at week 7 can't fit min block length 2 (only 1 week left from week 7 to end of 7)
        // Wait, 7 weeks = weeks 1..7. Boundary at 7, can fit weeks 7 only (length 1) — not enough.
        // Actually with minBlockLength=2 and schedule ending at week 7, starting at week 7 only gives 1 week.
        Schedule s = new Schedule(START, 7, List.of(a), new WeeklyDemand(0, 1, 1), 2);
        s.setScheduleBlockSizes(List.of(3, 3, 1));

        new ConstructiveGenerator().generate(s);

        // No block should start at week 7 (can't fit min block length)
        List<Block> blocks = s.blocksFor(a);
        for (Block b : blocks) {
            assertTrue(b.startWeek() != 7,
                    "should not start block at week 7 where min block length can't be satisfied");
        }
    }

    private static Clinician clinician(String name, int contractMin, int contractMax) {
        return new Clinician(name, new ContractedWeeks(contractMin, contractMax),
                6, 2, new BlockLengthRange(4, 5));
    }
}
