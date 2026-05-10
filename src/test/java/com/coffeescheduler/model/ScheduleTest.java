package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 5); // a Monday
    private static final Clinician ADAMS = clinician("Dr. Adams");
    private static final Clinician BAKER = clinician("Dr. Baker");

    @Test
    void newScheduleHasNoOnClinicians() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));

        assertTrue(s.onClinicians(1).isEmpty());
    }

    @Test
    void settingStateOnAddsClinicianToOnSet() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));

        s.setState(ADAMS, 1, WeekState.ON);

        assertEquals(Set.of(ADAMS), s.onClinicians(1));
    }

    @Test
    void stateOfReturnsSetState() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));

        s.setState(ADAMS, 1, WeekState.ON);
        s.setState(BAKER, 2, WeekState.UNAVAILABLE);

        assertEquals(WeekState.ON, s.stateOf(ADAMS, 1));
        assertEquals(WeekState.UNAVAILABLE, s.stateOf(BAKER, 2));
    }

    @Test
    void stateOfIsNullWhenUnset() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));

        assertEquals(null, s.stateOf(ADAMS, 1));
    }

    @Test
    void setStateToNullClearsState() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.setState(ADAMS, 1, WeekState.ON);

        s.setState(ADAMS, 1, null);

        assertEquals(null, s.stateOf(ADAMS, 1));
        assertTrue(s.onClinicians(1).isEmpty());
    }

    @Test
    void markersDefaultToEmpty() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertTrue(s.markersOf(ADAMS, 1).isEmpty());
    }

    @Test
    void setMarkerAddsToMarkerSet() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        s.setMarker(ADAMS, 1, WeekMarker.PREFER_ON);

        assertEquals(Set.of(WeekMarker.PREFER_ON), s.markersOf(ADAMS, 1));
    }

    @Test
    void swapAssignmentExchangesStatesBetweenTwoClinicians() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));
        s.setState(ADAMS, 1, WeekState.ON);
        s.setState(BAKER, 1, WeekState.UNAVAILABLE);

        s.swapAssignment(1, ADAMS, BAKER);

        assertEquals(WeekState.UNAVAILABLE, s.stateOf(ADAMS, 1));
        assertEquals(WeekState.ON, s.stateOf(BAKER, 1));
    }

    @Test
    void swapAssignmentHandlesUnsetCells() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));
        s.setState(ADAMS, 1, WeekState.ON);

        s.swapAssignment(1, ADAMS, BAKER);

        assertEquals(null, s.stateOf(ADAMS, 1));
        assertEquals(WeekState.ON, s.stateOf(BAKER, 1));
    }

    @Test
    void blocksForEmptyScheduleIsEmpty() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertTrue(s.blocksFor(ADAMS).isEmpty());
    }

    @Test
    void blocksForSingleOnWeekIsOneBlockOfLengthOne() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.setState(ADAMS, 5, WeekState.ON);

        assertEquals(List.of(new Block(5, 1)), s.blocksFor(ADAMS));
    }

    @Test
    void blocksForConsecutiveOnWeeksIsOneBlock() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.setState(ADAMS, 5, WeekState.ON);
        s.setState(ADAMS, 6, WeekState.ON);
        s.setState(ADAMS, 7, WeekState.ON);

        assertEquals(List.of(new Block(5, 3)), s.blocksFor(ADAMS));
    }

    @Test
    void blocksForNonConsecutiveOnWeeksAreSeparateBlocks() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.setState(ADAMS, 5, WeekState.ON);
        s.setState(ADAMS, 6, WeekState.ON);
        s.setState(ADAMS, 10, WeekState.ON);
        s.setState(ADAMS, 11, WeekState.ON);

        assertEquals(List.of(new Block(5, 2), new Block(10, 2)), s.blocksFor(ADAMS));
    }

    @Test
    void unavailableWeekSplitsBlocks() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.setState(ADAMS, 5, WeekState.ON);
        s.setState(ADAMS, 6, WeekState.UNAVAILABLE);
        s.setState(ADAMS, 7, WeekState.ON);

        assertEquals(List.of(new Block(5, 1), new Block(7, 1)), s.blocksFor(ADAMS));
    }

    @Test
    void blocksForOnlyConsidersTheRequestedClinician() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));
        s.setState(ADAMS, 5, WeekState.ON);
        s.setState(BAKER, 5, WeekState.ON);
        s.setState(BAKER, 6, WeekState.ON);

        assertEquals(List.of(new Block(5, 1)), s.blocksFor(ADAMS));
        assertEquals(List.of(new Block(5, 2)), s.blocksFor(BAKER));
    }

    @Test
    void addClinicianAppendsToRoster() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        s.addClinician(BAKER);

        assertEquals(List.of(ADAMS, BAKER), s.roster());
    }

    @Test
    void addClinicianRejectsDuplicateName() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        Clinician adamsAgain = clinician("Dr. Adams");

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> s.addClinician(adamsAgain));
    }

    @Test
    void removeClinicianRemovesFromRoster() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));

        s.removeClinician(ADAMS);

        assertEquals(List.of(BAKER), s.roster());
    }

    @Test
    void removeClinicianClearsStatesAndMarkers() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));
        s.setState(ADAMS, 1, WeekState.ON);
        s.setMarker(ADAMS, 1, WeekMarker.PREFER_ON);
        s.setState(BAKER, 1, WeekState.ON);

        s.removeClinician(ADAMS);

        assertEquals(Set.of(BAKER), s.onClinicians(1));
    }

    @Test
    void removeClinicianRejectsUnknown() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> s.removeClinician(BAKER));
    }

    @Test
    void pinAndUnpinCells() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        assertFalse(s.isPinned(ADAMS, 3));

        s.pin(ADAMS, 3);
        assertTrue(s.isPinned(ADAMS, 3));

        s.unpin(ADAMS, 3);
        assertFalse(s.isPinned(ADAMS, 3));
    }

    @Test
    void clearAllPinsRemovesAllPins() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));
        s.pin(ADAMS, 1);
        s.pin(BAKER, 5);

        s.clearAllPins();

        assertFalse(s.isPinned(ADAMS, 1));
        assertFalse(s.isPinned(BAKER, 5));
    }

    @Test
    void toggleMarkers() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        assertFalse(s.hasMarker(ADAMS, 3, WeekMarker.PREFER_ON));

        s.setMarker(ADAMS, 3, WeekMarker.PREFER_ON);
        assertTrue(s.hasMarker(ADAMS, 3, WeekMarker.PREFER_ON));

        s.removeMarker(ADAMS, 3, WeekMarker.PREFER_ON);
        assertFalse(s.hasMarker(ADAMS, 3, WeekMarker.PREFER_ON));
        assertTrue(s.markersOf(ADAMS, 3).isEmpty());
    }

    @Test
    void removeClinicianClearsPins() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS, BAKER));
        s.pin(ADAMS, 1);
        s.pin(BAKER, 5);

        s.removeClinician(ADAMS);

        assertTrue(s.isPinned(BAKER, 5));
    }

    @Test
    void setLengthWeeksExtends() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS));
        s.setState(ADAMS, 5, WeekState.ON);

        s.setLengthWeeks(20);

        assertEquals(20, s.lengthWeeks());
        assertEquals(WeekState.ON, s.stateOf(ADAMS, 5));
    }

    @Test
    void setLengthWeeksTrimsBeyondNewLength() {
        Schedule s = new Schedule(START, 20, List.of(ADAMS));
        s.setState(ADAMS, 5, WeekState.ON);
        s.setState(ADAMS, 15, WeekState.ON);
        s.pin(ADAMS, 15);
        s.setMarker(ADAMS, 15, WeekMarker.PREFER_ON);

        s.setLengthWeeks(10);

        assertEquals(10, s.lengthWeeks());
        assertEquals(WeekState.ON, s.stateOf(ADAMS, 5));
        assertEquals(null, s.stateOf(ADAMS, 15));
        assertFalse(s.isPinned(ADAMS, 15));
        assertFalse(s.hasMarker(ADAMS, 15, WeekMarker.PREFER_ON));
    }

    @Test
    void setDefaultDemandUpdates() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        WeeklyDemand newDemand = new WeeklyDemand(3, 4, 6);

        s.setDefaultDemand(newDemand);

        assertEquals(newDemand, s.defaultDemand());
    }

    @Test
    void setRestWeeksUpdates() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        s.setRestWeeks(4);

        assertEquals(4, s.restWeeks());
    }

    @Test
    void setStartMondayUpdates() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        LocalDate newStart = LocalDate.of(2026, 3, 2);

        s.setStartMonday(newStart);

        assertEquals(newStart, s.startMonday());
    }

    @Test
    void replaceClinicianMigratesData() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.setState(ADAMS, 1, WeekState.ON);
        s.pin(ADAMS, 1);
        s.setMarker(ADAMS, 2, WeekMarker.PREFER_ON);

        Clinician updated = new Clinician("Dr. Adams",
                new ContractedWeeks(10, 12), 5, 1, new BlockLengthRange(3, 4));
        s.replaceClinician(ADAMS, updated);

        assertEquals(updated, s.roster().get(0));
        assertEquals(WeekState.ON, s.stateOf(updated, 1));
        assertTrue(s.isPinned(updated, 1));
        assertTrue(s.hasMarker(updated, 2, WeekMarker.PREFER_ON));
        assertEquals(null, s.stateOf(ADAMS, 1));
    }

    @Test
    void replaceClinicianPreservesRosterOrder() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        Clinician updated = new Clinician("Dr. Adams Jr.",
                new ContractedWeeks(20, 24), 6, 2, new BlockLengthRange(4, 5));
        s.replaceClinician(ADAMS, updated);

        assertEquals(updated, s.roster().get(0));
        assertEquals(BAKER, s.roster().get(1));
    }

    private static Clinician clinician(String name) {
        return new Clinician(
                name,
                new ContractedWeeks(20, 24),
                6, 2,
                new BlockLengthRange(4, 5));
    }

    @Test
    void defaultScheduleBlockSizesMatchesLength() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS));
        assertEquals(List.of(10), s.scheduleBlockSizes());
    }

    @Test
    void scheduleBlockOfReturnsCorrectBlock() {
        Schedule s = new Schedule(START, 8, List.of(ADAMS));
        s.setScheduleBlockSizes(List.of(2, 3, 3));
        assertEquals(1, s.scheduleBlockOf(1));
        assertEquals(1, s.scheduleBlockOf(2));
        assertEquals(2, s.scheduleBlockOf(3));
        assertEquals(2, s.scheduleBlockOf(5));
        assertEquals(3, s.scheduleBlockOf(6));
        assertEquals(3, s.scheduleBlockOf(8));
    }

    @Test
    void isScheduleBlockStartReturnsTrueForBoundaryWeeks() {
        Schedule s = new Schedule(START, 9, List.of(ADAMS));
        s.setScheduleBlockSizes(List.of(3, 3, 3));
        assertTrue(s.isScheduleBlockStart(1));
        assertTrue(s.isScheduleBlockStart(4));
        assertTrue(s.isScheduleBlockStart(7));
        assertFalse(s.isScheduleBlockStart(2));
        assertFalse(s.isScheduleBlockStart(3));
        assertFalse(s.isScheduleBlockStart(5));
    }

    @Test
    void isScheduleBlockStartReturnsFalseForSingleBlock() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS));
        assertFalse(s.isScheduleBlockStart(1));
        assertFalse(s.isScheduleBlockStart(5));
    }

    @Test
    void scheduleBlockSizesRoundTripThroughJson() {
        Schedule s = new Schedule(START, 8, List.of(ADAMS));
        s.setScheduleBlockSizes(List.of(2, 3, 3));
        String json = com.coffeescheduler.io.ScheduleJson.toJson(s);
        Schedule loaded = com.coffeescheduler.io.ScheduleJson.fromJson(json);
        assertEquals(List.of(2, 3, 3), loaded.scheduleBlockSizes());
    }

    // --- Exclusion group management ---

    @Test
    void newScheduleHasNoExclusionGroups() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertTrue(s.exclusionGroups().isEmpty());
    }

    @Test
    void addExclusionGroupAppendsToList() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        ExclusionGroup group = new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker"));

        s.addExclusionGroup(group);

        assertEquals(List.of(group), s.exclusionGroups());
    }

    @Test
    void addExclusionGroupRejectsDuplicateName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker")));

        assertThrows(IllegalArgumentException.class,
                () -> s.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void removeExclusionGroupByName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker")));

        s.removeExclusionGroup("Group 1");

        assertTrue(s.exclusionGroups().isEmpty());
    }

    @Test
    void removeExclusionGroupRejectsUnknownName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertThrows(IllegalArgumentException.class,
                () -> s.removeExclusionGroup("nonexistent"));
    }

    @Test
    void removeClinicianAutoRemovesFromExclusionGroup() {
        Clinician charlie = clinician("Dr. Charlie");
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER, charlie));
        s.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker", "Dr. Charlie")));

        s.removeClinician(charlie);

        assertEquals(1, s.exclusionGroups().size());
        assertEquals(Set.of("Dr. Adams", "Dr. Baker"), s.exclusionGroups().get(0).members());
    }

    @Test
    void removeClinicianDeletesGroupWhenBelowTwoMembers() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker")));

        s.removeClinician(BAKER);

        assertTrue(s.exclusionGroups().isEmpty());
    }

    @Test
    void exclusionGroupsListIsUnmodifiable() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertThrows(UnsupportedOperationException.class,
                () -> s.exclusionGroups().add(new ExclusionGroup("X", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void replaceExclusionGroupByName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        Clinician charlie = clinician("Dr. Charlie");
        s.addClinician(charlie);
        s.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker")));

        ExclusionGroup updated = new ExclusionGroup("Group 1 renamed", Set.of("Dr. Adams", "Dr. Charlie"));
        s.replaceExclusionGroup("Group 1", updated);

        assertEquals(1, s.exclusionGroups().size());
        assertEquals("Group 1 renamed", s.exclusionGroups().get(0).name());
        assertEquals(Set.of("Dr. Adams", "Dr. Charlie"), s.exclusionGroups().get(0).members());
    }

    @Test
    void replaceExclusionGroupRejectsUnknownName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertThrows(IllegalArgumentException.class,
                () -> s.replaceExclusionGroup("nonexistent",
                        new ExclusionGroup("X", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void replaceExclusionGroupRejectsDuplicateNewName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        Clinician charlie = clinician("Dr. Charlie");
        s.addClinician(charlie);
        s.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker")));
        s.addExclusionGroup(new ExclusionGroup("Group 2", Set.of("Dr. Adams", "Dr. Charlie")));

        assertThrows(IllegalArgumentException.class,
                () -> s.replaceExclusionGroup("Group 1",
                        new ExclusionGroup("Group 2", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    // --- Inclusion group management ---

    @Test
    void newScheduleHasNoInclusionGroups() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertTrue(s.inclusionGroups().isEmpty());
    }

    @Test
    void addInclusionGroupAppendsToList() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        InclusionGroup group = new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker"));

        s.addInclusionGroup(group);

        assertEquals(List.of(group), s.inclusionGroups());
    }

    @Test
    void addInclusionGroupRejectsDuplicateName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker")));

        assertThrows(IllegalArgumentException.class,
                () -> s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void addInclusionGroupRejectsNameTakenByExclusionGroup() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addExclusionGroup(new ExclusionGroup("Shared Name", Set.of("Dr. Adams", "Dr. Baker")));

        assertThrows(IllegalArgumentException.class,
                () -> s.addInclusionGroup(new InclusionGroup("Shared Name", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void addExclusionGroupRejectsNameTakenByInclusionGroup() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addInclusionGroup(new InclusionGroup("Shared Name", Set.of("Dr. Adams", "Dr. Baker")));

        assertThrows(IllegalArgumentException.class,
                () -> s.addExclusionGroup(new ExclusionGroup("Shared Name", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void removeInclusionGroupByName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker")));

        s.removeInclusionGroup("Coverage");

        assertTrue(s.inclusionGroups().isEmpty());
    }

    @Test
    void removeInclusionGroupRejectsUnknownName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertThrows(IllegalArgumentException.class,
                () -> s.removeInclusionGroup("nonexistent"));
    }

    @Test
    void removeClinicianAutoRemovesFromInclusionGroup() {
        Clinician charlie = clinician("Dr. Charlie");
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER, charlie));
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker", "Dr. Charlie")));

        s.removeClinician(charlie);

        assertEquals(1, s.inclusionGroups().size());
        assertEquals(Set.of("Dr. Adams", "Dr. Baker"), s.inclusionGroups().get(0).members());
    }

    @Test
    void removeClinicianDeletesInclusionGroupWhenBelowTwoMembers() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker")));

        s.removeClinician(BAKER);

        assertTrue(s.inclusionGroups().isEmpty());
    }

    @Test
    void inclusionGroupsListIsUnmodifiable() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertThrows(UnsupportedOperationException.class,
                () -> s.inclusionGroups().add(new InclusionGroup("X", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void replaceInclusionGroupByName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        Clinician charlie = clinician("Dr. Charlie");
        s.addClinician(charlie);
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker")));

        InclusionGroup updated = new InclusionGroup("Coverage renamed", Set.of("Dr. Adams", "Dr. Charlie"));
        s.replaceInclusionGroup("Coverage", updated);

        assertEquals(1, s.inclusionGroups().size());
        assertEquals("Coverage renamed", s.inclusionGroups().get(0).name());
        assertEquals(Set.of("Dr. Adams", "Dr. Charlie"), s.inclusionGroups().get(0).members());
    }

    @Test
    void replaceInclusionGroupRejectsUnknownName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));

        assertThrows(IllegalArgumentException.class,
                () -> s.replaceInclusionGroup("nonexistent",
                        new InclusionGroup("X", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void replaceInclusionGroupRejectsDuplicateNewName() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        Clinician charlie = clinician("Dr. Charlie");
        s.addClinician(charlie);
        s.addInclusionGroup(new InclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker")));
        s.addInclusionGroup(new InclusionGroup("Group 2", Set.of("Dr. Adams", "Dr. Charlie")));

        assertThrows(IllegalArgumentException.class,
                () -> s.replaceInclusionGroup("Group 1",
                        new InclusionGroup("Group 2", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    @Test
    void replaceInclusionGroupRejectsNameTakenByExclusionGroup() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        Clinician charlie = clinician("Dr. Charlie");
        s.addClinician(charlie);
        s.addInclusionGroup(new InclusionGroup("Inc Group", Set.of("Dr. Adams", "Dr. Baker")));
        s.addExclusionGroup(new ExclusionGroup("Exc Group", Set.of("Dr. Adams", "Dr. Charlie")));

        assertThrows(IllegalArgumentException.class,
                () -> s.replaceInclusionGroup("Inc Group",
                        new InclusionGroup("Exc Group", Set.of("Dr. Adams", "Dr. Baker"))));
    }

    // --- Demand overrides ---

    @Test
    void newScheduleHasNoDemandOverrides() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertTrue(s.demandOverrides().isEmpty());
    }

    @Test
    void demandForReturnsDefaultWhenNoOverride() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertEquals(s.defaultDemand(), s.demandFor(1));
        assertEquals(s.defaultDemand(), s.demandFor(52));
    }

    @Test
    void addDemandOverrideAndResolve() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        WeeklyDemand holiday = new WeeklyDemand(1, 2, 3);
        s.addDemandOverride(new DemandOverride(50, 52, holiday));

        assertEquals(holiday, s.demandFor(50));
        assertEquals(holiday, s.demandFor(51));
        assertEquals(holiday, s.demandFor(52));
        assertEquals(s.defaultDemand(), s.demandFor(49));
    }

    @Test
    void demandOverridesReturnsSortedList() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        WeeklyDemand d1 = new WeeklyDemand(1, 2, 3);
        WeeklyDemand d2 = new WeeklyDemand(0, 1, 2);
        s.addDemandOverride(new DemandOverride(30, 35, d1));
        s.addDemandOverride(new DemandOverride(10, 15, d2));

        List<DemandOverride> overrides = s.demandOverrides();
        assertEquals(2, overrides.size());
        assertEquals(10, overrides.get(0).startWeek());
        assertEquals(30, overrides.get(1).startWeek());
    }

    @Test
    void demandOverridesListIsUnmodifiable() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertThrows(UnsupportedOperationException.class,
                () -> s.demandOverrides().add(new DemandOverride(1, 5, new WeeklyDemand(1, 2, 3))));
    }

    @Test
    void addDemandOverrideRejectsOverlap() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));

        // Overlaps at week 15
        assertThrows(IllegalArgumentException.class,
                () -> s.addDemandOverride(new DemandOverride(15, 20, new WeeklyDemand(0, 1, 2))));
        // Contained within
        assertThrows(IllegalArgumentException.class,
                () -> s.addDemandOverride(new DemandOverride(12, 13, new WeeklyDemand(0, 1, 2))));
        // Containing
        assertThrows(IllegalArgumentException.class,
                () -> s.addDemandOverride(new DemandOverride(8, 18, new WeeklyDemand(0, 1, 2))));
    }

    @Test
    void addDemandOverrideAllowsAdjacentSpans() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));
        s.addDemandOverride(new DemandOverride(16, 20, new WeeklyDemand(0, 1, 2)));

        assertEquals(2, s.demandOverrides().size());
    }

    @Test
    void addDemandOverrideRejectsBeyondScheduleLength() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertThrows(IllegalArgumentException.class,
                () -> s.addDemandOverride(new DemandOverride(50, 55, new WeeklyDemand(1, 2, 3))));
    }

    @Test
    void removeDemandOverrideByStartWeek() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));

        s.removeDemandOverride(10);

        assertTrue(s.demandOverrides().isEmpty());
        assertEquals(s.defaultDemand(), s.demandFor(10));
    }

    @Test
    void removeDemandOverrideRejectsUnknownStartWeek() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertThrows(IllegalArgumentException.class,
                () -> s.removeDemandOverride(10));
    }

    @Test
    void replaceDemandOverride() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));

        WeeklyDemand newDemand = new WeeklyDemand(0, 1, 2);
        s.replaceDemandOverride(10, new DemandOverride(12, 18, newDemand));

        assertEquals(1, s.demandOverrides().size());
        assertEquals(12, s.demandOverrides().get(0).startWeek());
        assertEquals(18, s.demandOverrides().get(0).endWeek());
        assertEquals(newDemand, s.demandFor(12));
        assertEquals(s.defaultDemand(), s.demandFor(10));
    }

    @Test
    void replaceDemandOverrideRejectsOverlapWithOther() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));
        s.addDemandOverride(new DemandOverride(20, 25, new WeeklyDemand(0, 1, 2)));

        // Replace first to overlap with second
        assertThrows(IllegalArgumentException.class,
                () -> s.replaceDemandOverride(10,
                        new DemandOverride(14, 22, new WeeklyDemand(1, 2, 3))));
    }

    @Test
    void replaceDemandOverrideRejectsUnknownStartWeek() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));

        assertThrows(IllegalArgumentException.class,
                () -> s.replaceDemandOverride(10,
                        new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3))));
    }

    @Test
    void setLengthWeeksTrimsPartialOverride() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(45, 52, new WeeklyDemand(1, 2, 3)));

        s.setLengthWeeks(48);

        assertEquals(1, s.demandOverrides().size());
        assertEquals(45, s.demandOverrides().get(0).startWeek());
        assertEquals(48, s.demandOverrides().get(0).endWeek());
    }

    @Test
    void setLengthWeeksDeletesFullyOutOfBoundsOverride() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(45, 52, new WeeklyDemand(1, 2, 3)));

        s.setLengthWeeks(40);

        assertTrue(s.demandOverrides().isEmpty());
    }

    @Test
    void setLengthWeeksLeavesInBoundsOverridesUntouched() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));

        s.setLengthWeeks(40);

        assertEquals(1, s.demandOverrides().size());
        assertEquals(10, s.demandOverrides().get(0).startWeek());
        assertEquals(15, s.demandOverrides().get(0).endWeek());
    }

    @Test
    void setLengthWeeksGrowDoesNotAffectOverrides() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(50, 52, new WeeklyDemand(1, 2, 3)));

        s.setLengthWeeks(60);

        assertEquals(1, s.demandOverrides().size());
        assertEquals(52, s.demandOverrides().get(0).endWeek());
    }
}
