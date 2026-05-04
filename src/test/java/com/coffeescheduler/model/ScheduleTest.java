package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static Clinician clinician(String name) {
        return new Clinician(
                name,
                new ContractedWeeks(20, 24),
                6, 2,
                new BlockLengthRange(4, 5));
    }
}
