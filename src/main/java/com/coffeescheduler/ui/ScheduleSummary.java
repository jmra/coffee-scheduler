package com.coffeescheduler.ui;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;

public final class ScheduleSummary {

    private ScheduleSummary() {}

    public static String format(Schedule schedule) {
        int clinicianCount = schedule.roster().size();
        int scheduledOn = countScheduledOn(schedule);
        int contractedMinSum = schedule.roster().stream()
                .mapToInt(c -> c.contractedWeeks().min())
                .sum();
        return schedule.lengthWeeks() + " weeks · "
                + clinicianCount + pluralize(" clinician", clinicianCount) + " · "
                + scheduledOn + "/" + contractedMinSum + " clinician-weeks scheduled";
    }

    private static int countScheduledOn(Schedule schedule) {
        int count = 0;
        for (Clinician c : schedule.roster()) {
            for (int week = 1; week <= schedule.lengthWeeks(); week++) {
                if (schedule.stateOf(c, week) == WeekState.ON) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String pluralize(String word, int count) {
        return count == 1 ? word : word + "s";
    }
}
