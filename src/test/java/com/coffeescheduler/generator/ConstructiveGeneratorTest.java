package com.coffeescheduler.generator;

import com.coffeescheduler.model.Block;
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
