package com.coffeescheduler.io;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
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

    private static Clinician clinician(String name) {
        return new Clinician(
                name,
                new ContractedWeeks(20, 24),
                6, 2,
                new BlockLengthRange(4, 5));
    }
}
