package com.coffeescheduler.io;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.DemandOverride;
import com.coffeescheduler.model.ExclusionGroup;
import com.coffeescheduler.model.WeeklyDemand;
import com.coffeescheduler.model.InclusionGroup;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleJsonTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 5);
    private static final Clinician ADAMS = clinician("Dr. Adams");
    private static final Clinician BAKER = clinician("Dr. Baker");

    @Test
    void roundTripsScheduleConfig() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER));

        String json = ScheduleJson.toJson(original);
        Schedule restored = ScheduleJson.fromJson(json);

        assertEquals(START, restored.startMonday());
        assertEquals(52, restored.lengthWeeks());
        assertEquals(List.of(ADAMS, BAKER), restored.roster());
    }

    @Test
    void roundTripsAssignments() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER));
        original.setState(ADAMS, 5, WeekState.ON);
        original.setState(ADAMS, 6, WeekState.ON);
        original.setState(BAKER, 10, WeekState.UNAVAILABLE);

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(WeekState.ON, restored.stateOf(ADAMS, 5));
        assertEquals(WeekState.ON, restored.stateOf(ADAMS, 6));
        assertEquals(WeekState.UNAVAILABLE, restored.stateOf(BAKER, 10));
        assertEquals(null, restored.stateOf(ADAMS, 1));
    }

    @Test
    void roundTripsMarkers() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER));
        original.setMarker(ADAMS, 5, WeekMarker.PREFER_ON);
        original.setMarker(BAKER, 10, WeekMarker.PREFER_OFF);

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(Set.of(WeekMarker.PREFER_ON), restored.markersOf(ADAMS, 5));
        assertEquals(Set.of(WeekMarker.PREFER_OFF), restored.markersOf(BAKER, 10));
        assertTrue(restored.markersOf(ADAMS, 1).isEmpty());
    }

    @Test
    void roundTripsPinnedCells() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER));
        original.setState(ADAMS, 5, WeekState.ON);
        original.pin(ADAMS, 5);
        original.setState(BAKER, 10, WeekState.ON);

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertTrue(restored.isPinned(ADAMS, 5));
        assertFalse(restored.isPinned(BAKER, 10));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleJson.fromJson("not json"));
    }

    @Test
    void rejectsAssignmentReferencingUnknownClinician() {
        String json = """
                {
                  "startMonday": "2026-01-05",
                  "lengthWeeks": 52,
                  "roster": [],
                  "assignments": [{"clinician": "Dr. Ghost", "week": 5, "state": "ON"}],
                  "markers": []
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> ScheduleJson.fromJson(json));
    }

    @Test
    void roundTripsExclusionGroups() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER));
        original.addExclusionGroup(new ExclusionGroup("Group 1", Set.of("Dr. Adams", "Dr. Baker")));

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(1, restored.exclusionGroups().size());
        assertEquals("Group 1", restored.exclusionGroups().get(0).name());
        assertEquals(Set.of("Dr. Adams", "Dr. Baker"), restored.exclusionGroups().get(0).members());
    }

    @Test
    void roundTripsMultipleExclusionGroups() {
        Clinician charlie = clinician("Dr. Charlie");
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER, charlie));
        original.addExclusionGroup(new ExclusionGroup("Group A", Set.of("Dr. Adams", "Dr. Baker")));
        original.addExclusionGroup(new ExclusionGroup("Group B", Set.of("Dr. Adams", "Dr. Charlie")));

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(2, restored.exclusionGroups().size());
    }

    @Test
    void loadsOldJsonWithoutRulesField() {
        String json = """
                {
                  "startMonday": "2026-01-05",
                  "lengthWeeks": 52,
                  "roster": [],
                  "assignments": [],
                  "markers": []
                }
                """;

        Schedule restored = ScheduleJson.fromJson(json);

        assertTrue(restored.exclusionGroups().isEmpty());
    }

    @Test
    void savedJsonContainsRulesWrapper() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addExclusionGroup(new ExclusionGroup("Test", Set.of("Dr. Adams", "Dr. Baker")));

        String json = ScheduleJson.toJson(s);

        assertTrue(json.contains("\"rules\""), "JSON should contain rules wrapper");
        assertTrue(json.contains("\"exclusionGroups\""), "JSON should contain exclusionGroups key");
    }

    @Test
    void roundTripsInclusionGroups() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER));
        original.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker")));

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(1, restored.inclusionGroups().size());
        assertEquals("Coverage", restored.inclusionGroups().get(0).name());
        assertEquals(Set.of("Dr. Adams", "Dr. Baker"), restored.inclusionGroups().get(0).members());
    }

    @Test
    void roundTripsMultipleInclusionGroups() {
        Clinician charlie = clinician("Dr. Charlie");
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER, charlie));
        original.addInclusionGroup(new InclusionGroup("Group A", Set.of("Dr. Adams", "Dr. Baker")));
        original.addInclusionGroup(new InclusionGroup("Group B", Set.of("Dr. Adams", "Dr. Charlie")));

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(2, restored.inclusionGroups().size());
    }

    @Test
    void loadsOldJsonWithoutInclusionGroupsField() {
        String json = """
                {
                  "startMonday": "2026-01-05",
                  "lengthWeeks": 52,
                  "roster": [],
                  "assignments": [],
                  "markers": [],
                  "rules": { "exclusionGroups": [] }
                }
                """;

        Schedule restored = ScheduleJson.fromJson(json);

        assertTrue(restored.inclusionGroups().isEmpty());
    }

    @Test
    void savedJsonContainsInclusionGroupsKey() {
        Schedule s = new Schedule(START, 10, List.of(ADAMS, BAKER));
        s.addInclusionGroup(new InclusionGroup("Coverage", Set.of("Dr. Adams", "Dr. Baker")));

        String json = ScheduleJson.toJson(s);

        assertTrue(json.contains("\"inclusionGroups\""), "JSON should contain inclusionGroups key");
    }

    @Test
    void roundTripsBothGroupTypes() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS, BAKER));
        original.addExclusionGroup(new ExclusionGroup("Exc", Set.of("Dr. Adams", "Dr. Baker")));
        original.addInclusionGroup(new InclusionGroup("Inc", Set.of("Dr. Adams", "Dr. Baker")));

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(1, restored.exclusionGroups().size());
        assertEquals(1, restored.inclusionGroups().size());
        assertEquals("Exc", restored.exclusionGroups().get(0).name());
        assertEquals("Inc", restored.inclusionGroups().get(0).name());
    }

    @Test
    void roundTripsDemandOverrides() {
        Schedule original = new Schedule(START, 52, List.of(ADAMS));
        original.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));
        original.addDemandOverride(new DemandOverride(50, 52, new WeeklyDemand(0, 1, 2)));

        Schedule restored = ScheduleJson.fromJson(ScheduleJson.toJson(original));

        assertEquals(2, restored.demandOverrides().size());
        assertEquals(10, restored.demandOverrides().get(0).startWeek());
        assertEquals(15, restored.demandOverrides().get(0).endWeek());
        assertEquals(new WeeklyDemand(1, 2, 3), restored.demandOverrides().get(0).demand());
        assertEquals(50, restored.demandOverrides().get(1).startWeek());
    }

    @Test
    void loadsOldJsonWithoutDemandOverrides() {
        String json = """
                {
                  "startMonday": "2026-01-05",
                  "lengthWeeks": 52,
                  "roster": [],
                  "assignments": [],
                  "markers": []
                }
                """;

        Schedule restored = ScheduleJson.fromJson(json);

        assertTrue(restored.demandOverrides().isEmpty());
    }

    @Test
    void savedJsonContainsDemandOverridesKey() {
        Schedule s = new Schedule(START, 52, List.of(ADAMS));
        s.addDemandOverride(new DemandOverride(10, 15, new WeeklyDemand(1, 2, 3)));

        String json = ScheduleJson.toJson(s);

        assertTrue(json.contains("\"demandOverrides\""), "JSON should contain demandOverrides key");
    }

    private static Clinician clinician(String name) {
        return new Clinician(
                name,
                new ContractedWeeks(20, 24),
                6, 2,
                new BlockLengthRange(4, 5));
    }
}
