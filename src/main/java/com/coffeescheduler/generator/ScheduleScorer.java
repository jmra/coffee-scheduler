package com.coffeescheduler.generator;

import com.coffeescheduler.model.Block;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.RuleViolation;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScheduleScorer {

    public record ScoreResult(List<RuleViolation> violations, int softScore) {
        public int totalScore() {
            return -violations.size() * 10_000 + softScore;
        }
    }

    public ScoreResult score(Schedule schedule) {
        List<RuleViolation> violations = new ArrayList<>();
        int soft = 0;

        for (int w = 1; w <= schedule.lengthWeeks(); w++) {
            int onCount = schedule.onClinicians(w).size();
            if (onCount < schedule.defaultDemand().min()) {
                violations.add(new RuleViolation(
                        "Week " + w + ": only " + onCount + " on, min is " + schedule.defaultDemand().min(),
                        null, w));
            }
            if (onCount > schedule.defaultDemand().max()) {
                violations.add(new RuleViolation(
                        "Week " + w + ": " + onCount + " on exceeds max " + schedule.defaultDemand().max(),
                        null, w));
            }
            soft -= Math.abs(onCount - schedule.defaultDemand().ideal());
        }

        for (Clinician c : schedule.roster()) {
            soft += scoreClinicianSoft(c, schedule, violations);
        }

        return new ScoreResult(List.copyOf(violations), soft);
    }

    private int scoreClinicianSoft(Clinician c, Schedule schedule, List<RuleViolation> violations) {
        int soft = 0;
        int onCount = 0;
        for (int w = 1; w <= schedule.lengthWeeks(); w++) {
            if (schedule.stateOf(c, w) == WeekState.ON) {
                onCount++;
                Set<WeekMarker> markers = schedule.markersOf(c, w);
                if (markers.contains(WeekMarker.PREFER_ON)) soft++;
                if (markers.contains(WeekMarker.PREFER_OFF)) soft--;
            }
        }

        if (onCount < c.contractedWeeks().min()) {
            violations.add(new RuleViolation(
                    c.name() + ": only " + onCount + "/" + c.contractedWeeks().min() + " contracted weeks",
                    c, null));
        }
        if (onCount > c.contractedWeeks().max()) {
            violations.add(new RuleViolation(
                    c.name() + ": " + onCount + " weeks exceeds contracted max " + c.contractedWeeks().max(),
                    c, null));
        }

        List<Block> blocks = schedule.blocksFor(c);
        int blocksAtMax = 0;
        for (Block b : blocks) {
            if (b.length() < schedule.minBlockLength()) {
                violations.add(new RuleViolation(
                        c.name() + ": block at week " + b.startWeek() + " has length " + b.length()
                                + ", min is " + schedule.minBlockLength(),
                        c, b.startWeek()));
            }
            if (b.length() > c.maxBlockLength()) {
                violations.add(new RuleViolation(
                        c.name() + ": block at week " + b.startWeek() + " has length " + b.length()
                                + ", max is " + c.maxBlockLength(),
                        c, b.startWeek()));
            }
            if (b.length() == c.maxBlockLength()) {
                blocksAtMax++;
            }
            if (b.length() >= c.preferredBlockLength().min()
                    && b.length() <= c.preferredBlockLength().max()) {
                soft++;
            }
        }
        if (blocksAtMax > c.maxBlocksAtMaxLength()) {
            violations.add(new RuleViolation(
                    c.name() + ": " + blocksAtMax + " blocks at max length " + c.maxBlockLength()
                            + ", limit is " + c.maxBlocksAtMaxLength(),
                    c, null));
        }

        for (int i = 1; i < blocks.size(); i++) {
            int prevEnd = blocks.get(i - 1).startWeek() + blocks.get(i - 1).length() - 1;
            int gap = blocks.get(i).startWeek() - prevEnd - 1;
            if (gap < schedule.restWeeks()) {
                violations.add(new RuleViolation(
                        c.name() + ": rest gap " + gap + " between weeks " + prevEnd
                                + " and " + blocks.get(i).startWeek() + ", min is " + schedule.restWeeks(),
                        c, blocks.get(i).startWeek()));
            }
        }

        return soft;
    }
}
