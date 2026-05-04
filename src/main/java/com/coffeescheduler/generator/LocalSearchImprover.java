package com.coffeescheduler.generator;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;

import java.util.List;

public class LocalSearchImprover {

    private static final int MAX_PASSES = 50;

    private final ScheduleScorer scorer = new ScheduleScorer();

    public ScheduleScorer.ScoreResult improve(Schedule schedule) {
        ScheduleScorer.ScoreResult best = scorer.score(schedule);
        List<Clinician> roster = schedule.roster();

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            boolean improved = false;

            for (int w = 1; w <= schedule.lengthWeeks(); w++) {
                for (int i = 0; i < roster.size(); i++) {
                    for (int j = i + 1; j < roster.size(); j++) {
                        Clinician a = roster.get(i);
                        Clinician b = roster.get(j);
                        if (!isSwappable(schedule, a, b, w)) continue;

                        swap(schedule, a, b, w);
                        ScheduleScorer.ScoreResult after = scorer.score(schedule);

                        if (after.totalScore() > best.totalScore()) {
                            best = after;
                            improved = true;
                        } else {
                            swap(schedule, a, b, w);
                        }
                    }
                }
            }

            if (!improved) break;
        }

        return best;
    }

    private boolean isSwappable(Schedule schedule, Clinician a, Clinician b, int week) {
        WeekState sa = schedule.stateOf(a, week);
        WeekState sb = schedule.stateOf(b, week);
        if (sa == WeekState.UNAVAILABLE || sb == WeekState.UNAVAILABLE) return false;
        if (schedule.isPinned(a, week) || schedule.isPinned(b, week)) return false;
        boolean aOn = sa == WeekState.ON;
        boolean bOn = sb == WeekState.ON;
        if (aOn == bOn) return false;

        Clinician onClinician = aOn ? a : b;
        Clinician offClinician = aOn ? b : a;
        return !wouldBreakBlock(schedule, onClinician, week)
                && !wouldCreateShortBlock(schedule, offClinician, week);
    }

    private boolean wouldBreakBlock(Schedule schedule, Clinician c, int week) {
        int min = schedule.minBlockLength();
        int left = 0;
        for (int w = week - 1; w >= 1 && schedule.stateOf(c, w) == WeekState.ON; w--) left++;
        int right = 0;
        for (int w = week + 1; w <= schedule.lengthWeeks() && schedule.stateOf(c, w) == WeekState.ON; w++) right++;
        return (left > 0 && left < min) || (right > 0 && right < min);
    }

    private boolean wouldCreateShortBlock(Schedule schedule, Clinician c, int week) {
        int left = 0;
        for (int w = week - 1; w >= 1 && schedule.stateOf(c, w) == WeekState.ON; w--) left++;
        int right = 0;
        for (int w = week + 1; w <= schedule.lengthWeeks() && schedule.stateOf(c, w) == WeekState.ON; w++) right++;
        return left + 1 + right < schedule.minBlockLength();
    }

    private void swap(Schedule schedule, Clinician a, Clinician b, int week) {
        WeekState sa = schedule.stateOf(a, week);
        WeekState sb = schedule.stateOf(b, week);
        schedule.setState(a, week, sb);
        schedule.setState(b, week, sa);
    }
}
