package com.coffeescheduler.generator;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ExclusionGroup;
import com.coffeescheduler.model.InclusionGroup;
import com.coffeescheduler.model.RuleViolation;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;
import com.coffeescheduler.model.WeeklyDemand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstructiveGenerator implements ScheduleGenerator {

    @Override

    public GeneratorResult generate(Schedule schedule) {
        List<RuleViolation> violations = new ArrayList<>();
        Map<Clinician, ClinicianTracker> trackers = new HashMap<>();
        for (Clinician c : schedule.roster()) {
            trackers.put(c, new ClinicianTracker(c));
        }

        for (int week = 1; week <= schedule.lengthWeeks(); week++) {
            stepWeek(week, schedule, trackers, violations);
        }

        for (Clinician c : schedule.roster()) {
            ClinicianTracker t = trackers.get(c);
            if (t.weeksScheduled < c.contractedWeeks().min()) {
                violations.add(new RuleViolation(
                        c.name() + ": only " + t.weeksScheduled + "/" + c.contractedWeeks().min() + " contracted weeks",
                        c, null));
            }
        }

        return new GeneratorResult(schedule, violations);
    }

    private void stepWeek(int week, Schedule schedule, Map<Clinician, ClinicianTracker> trackers,
                          List<RuleViolation> violations) {
        WeeklyDemand demand = schedule.demandFor(week);
        List<Clinician> forcedOn = new ArrayList<>();
        List<Clinician> forcedOff = new ArrayList<>();
        List<Clinician> optional = new ArrayList<>();

        for (Clinician c : schedule.roster()) {
            ClinicianTracker t = trackers.get(c);
            Category cat = categorize(c, t, week, schedule);
            switch (cat) {
                case FORCED_ON -> forcedOn.add(c);
                case FORCED_OFF -> forcedOff.add(c);
                case OPTIONAL -> optional.add(c);
            }
        }

        Set<String> excludedNames = excludedByForcedOn(forcedOn, schedule);

        if (forcedOn.size() > demand.max()) {
            violations.add(new RuleViolation(
                    "Week " + week + ": " + forcedOn.size() + " forced on exceeds max " + demand.max(),
                    null, week));
        }

        int wanted = Math.max(0, demand.ideal() - forcedOn.size());

        optional.sort(optionalComparator(schedule, trackers, week));

        int promoted = 0;
        for (Clinician c : optional) {
            if (promoted >= wanted) break;
            if (excludedNames.contains(c.name())) continue;
            forcedOn.add(c);
            addExcludedPeers(c, schedule, excludedNames);
            promoted++;
        }

        // Post-promotion: enforce inclusion groups.
        // Exclusion groups typically encode a harder real-world constraint (e.g., two
        // clinicians literally can't both be present), while inclusion groups express
        // something more like a coverage preference. When the two conflict, exclusion
        // wins and an inclusion violation is recorded rather than breaking exclusion.
        for (InclusionGroup group : schedule.inclusionGroups()) {
            boolean satisfied = false;
            for (Clinician c : forcedOn) {
                if (group.members().contains(c.name())) { satisfied = true; break; }
            }
            if (!satisfied) {
                if (forcedOn.size() >= demand.max()) {
                    violations.add(new RuleViolation(
                            "Inclusion group '" + group.name() + "' violated in week " + week
                                    + ": no members on (demand max reached)",
                            null, week));
                    continue;
                }
                Clinician best = null;
                for (Clinician c : optional) {
                    if (!group.members().contains(c.name())) continue;
                    if (excludedNames.contains(c.name())) continue;
                    if (forcedOn.contains(c)) continue;
                    best = c;
                    break;
                }
                if (best != null) {
                    forcedOn.add(best);
                    addExcludedPeers(best, schedule, excludedNames);
                } else {
                    violations.add(new RuleViolation(
                            "Inclusion group '" + group.name() + "' violated in week " + week
                                    + ": no eligible members available",
                            null, week));
                }
            }
        }

        if (forcedOn.size() < demand.min()) {
            violations.add(new RuleViolation(
                    "Week " + week + ": only " + forcedOn.size() + " on, min is " + demand.min(),
                    null, week));
        }

        for (Clinician c : forcedOn) {
            schedule.setState(c, week, WeekState.ON);
            ClinicianTracker t = trackers.get(c);
            t.weeksScheduled++;
            t.currentBlockLength++;
            t.restCountdown = 0;
            t.state = TrackerState.MID_BLOCK;
        }

        for (Clinician c : schedule.roster()) {
            if (!forcedOn.contains(c)) {
                ClinicianTracker t = trackers.get(c);
                if (t.state == TrackerState.MID_BLOCK) {
                    if (t.currentBlockLength >= schedule.minBlockLength()) {
                        t.endBlock(schedule, week);
                    }
                }
                if (t.state == TrackerState.RESTING) {
                    t.restCountdown--;
                    if (t.restCountdown <= 0) {
                        t.state = TrackerState.INACTIVE;
                    }
                }
            }
        }
    }

    private Category categorize(Clinician c, ClinicianTracker t, int week, Schedule schedule) {
        if (schedule.stateOf(c, week) == WeekState.UNAVAILABLE) {
            return Category.FORCED_OFF;
        }
        if (schedule.stateOf(c, week) == WeekState.ON) {
            return Category.FORCED_ON;
        }
        if (t.state == TrackerState.RESTING) {
            return Category.FORCED_OFF;
        }
        if (t.state == TrackerState.MID_BLOCK) {
            if (t.currentBlockLength < schedule.minBlockLength()) {
                return Category.FORCED_ON;
            }
            if (t.weeksScheduled >= c.contractedWeeks().max()) {
                return Category.FORCED_OFF;
            }
            int effectiveMax = t.blocksAtMaxLength >= c.maxBlocksAtMaxLength()
                    ? c.maxBlockLength() - 1 : c.maxBlockLength();
            if (t.currentBlockLength >= effectiveMax) {
                return Category.FORCED_OFF;
            }
            return Category.OPTIONAL;
        }
        // INACTIVE
        if (t.weeksScheduled >= c.contractedWeeks().max()) {
            return Category.FORCED_OFF;
        }
        int remainingCapacity = c.contractedWeeks().max() - t.weeksScheduled;
        if (remainingCapacity < schedule.minBlockLength()) {
            return Category.FORCED_OFF;
        }
        if (!canStartBlock(c, week, schedule)) {
            return Category.FORCED_OFF;
        }
        return Category.OPTIONAL;
    }

    private boolean canStartBlock(Clinician c, int week, Schedule schedule) {
        int lookahead = schedule.minBlockLength() - 1;
        for (int w = week; w <= Math.min(week + lookahead, schedule.lengthWeeks()); w++) {
            if (schedule.stateOf(c, w) == WeekState.UNAVAILABLE) {
                return false;
            }
        }
        return week + lookahead <= schedule.lengthWeeks();
    }

    private Comparator<Clinician> optionalComparator(Schedule schedule,
                                                      Map<Clinician, ClinicianTracker> trackers,
                                                      int week) {
        return Comparator
                .comparingInt((Clinician c) -> -urgency(c, trackers.get(c), schedule, week))
                .thenComparingInt(c -> -softScore(c, schedule, trackers.get(c), week))
                .thenComparing(Clinician::name);
    }

    private int urgency(Clinician c, ClinicianTracker t, Schedule schedule, int currentWeek) {
        int remaining = schedule.lengthWeeks() - currentWeek + 1;
        int feasible = estimateFeasibleWeeks(c, t, schedule, currentWeek, remaining);
        return Math.max(0, c.contractedWeeks().min() - t.weeksScheduled - feasible);
    }

    private int estimateFeasibleWeeks(Clinician c, ClinicianTracker t, Schedule schedule,
                                       int currentWeek, int remainingWeeks) {
        int available = 0;
        for (int w = currentWeek; w <= schedule.lengthWeeks(); w++) {
            if (schedule.stateOf(c, w) != WeekState.UNAVAILABLE) {
                available++;
            }
        }
        return Math.min(available, c.contractedWeeks().max() - t.weeksScheduled);
    }

    private int softScore(Clinician c, Schedule schedule, ClinicianTracker t, int week) {
        int score = 0;
        if (schedule.markersOf(c, week).contains(WeekMarker.PREFER_ON)) score++;
        if (schedule.markersOf(c, week).contains(WeekMarker.PREFER_OFF)) score--;
        int resultingLength = (t.state == TrackerState.MID_BLOCK) ? t.currentBlockLength + 1 : 1;
        if (resultingLength >= c.preferredBlockLength().min()
                && resultingLength <= c.preferredBlockLength().max()) {
            score++;
        }
        if (t.state == TrackerState.INACTIVE) {
            int gap = week - t.lastBlockEndWeek;
            double avgBlock = (c.preferredBlockLength().min() + c.preferredBlockLength().max()) / 2.0;
            double expectedBlocks = c.contractedWeeks().min() / avgBlock;
            double idealGap = (schedule.lengthWeeks() - c.contractedWeeks().min())
                    / Math.max(1.0, expectedBlocks + 1);
            if (gap >= idealGap) {
                score++;
            }
        }
        return score;
    }

    private Set<String> excludedByForcedOn(List<Clinician> forcedOn, Schedule schedule) {
        Set<String> excluded = new HashSet<>();
        for (Clinician c : forcedOn) {
            addExcludedPeers(c, schedule, excluded);
        }
        return excluded;
    }

    private void addExcludedPeers(Clinician c, Schedule schedule, Set<String> excluded) {
        for (ExclusionGroup group : schedule.exclusionGroups()) {
            if (group.members().contains(c.name())) {
                excluded.addAll(group.members());
            }
        }
    }

    private enum Category { FORCED_ON, FORCED_OFF, OPTIONAL }

    private enum TrackerState { INACTIVE, MID_BLOCK, RESTING }

    private static class ClinicianTracker {
        TrackerState state = TrackerState.INACTIVE;
        int weeksScheduled;
        int currentBlockLength;
        int restCountdown;
        int blocksAtMaxLength;
        int lastBlockEndWeek;
        private final Clinician clinician;

        ClinicianTracker(Clinician clinician) {
            this.clinician = clinician;
        }

        void endBlock(Schedule schedule, int week) {
            if (currentBlockLength >= clinician.maxBlockLength()) {
                blocksAtMaxLength++;
            }
            lastBlockEndWeek = week - 1;
            currentBlockLength = 0;
            state = TrackerState.RESTING;
            restCountdown = schedule.restWeeks();
        }
    }
}
