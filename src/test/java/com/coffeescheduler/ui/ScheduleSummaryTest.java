package com.coffeescheduler.ui;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduleSummaryTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 5);

    @Test
    void emptyRosterShowsZeroes() {
        Schedule s = new Schedule(START, 52, List.of());

        assertEquals("52 weeks · 0 clinicians · 0/0 clinician-weeks scheduled",
                ScheduleSummary.format(s));
    }

    @Test
    void countsScheduledOnCellsAgainstSumOfContractedMins() {
        Clinician adams = clinician("Dr. Adams", 20, 24);
        Clinician baker = clinician("Dr. Baker", 18, 22);
        Schedule s = new Schedule(START, 52, List.of(adams, baker));
        s.setState(adams, 1, WeekState.ON);
        s.setState(adams, 2, WeekState.ON);
        s.setState(baker, 5, WeekState.ON);
        s.setState(baker, 6, WeekState.UNAVAILABLE); // does not count

        assertEquals("52 weeks · 2 clinicians · 3/38 clinician-weeks scheduled",
                ScheduleSummary.format(s));
    }

    @Test
    void singleClinicianUsesSingularForm() {
        Clinician adams = clinician("Dr. Adams", 20, 24);
        Schedule s = new Schedule(START, 52, List.of(adams));

        assertEquals("52 weeks · 1 clinician · 0/20 clinician-weeks scheduled",
                ScheduleSummary.format(s));
    }

    private static Clinician clinician(String name, int minWeeks, int maxWeeks) {
        return new Clinician(
                name,
                new ContractedWeeks(minWeeks, maxWeeks),
                6, 2,
                new BlockLengthRange(4, 5));
    }
}
