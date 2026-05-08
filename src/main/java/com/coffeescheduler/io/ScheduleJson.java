package com.coffeescheduler.io;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ExclusionGroup;
import com.coffeescheduler.model.InclusionGroup;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;
import com.coffeescheduler.model.WeeklyDemand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScheduleJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    private ScheduleJson() {}

    public static String toJson(Schedule schedule) {
        List<AssignmentEntry> assignments = new ArrayList<>();
        List<MarkerEntry> markers = new ArrayList<>();
        List<PinEntry> pins = new ArrayList<>();
        for (Clinician c : schedule.roster()) {
            for (int week = 1; week <= schedule.lengthWeeks(); week++) {
                WeekState state = schedule.stateOf(c, week);
                if (state != null) {
                    assignments.add(new AssignmentEntry(c.name(), week, state));
                }
                Set<WeekMarker> cellMarkers = schedule.markersOf(c, week);
                if (!cellMarkers.isEmpty()) {
                    markers.add(new MarkerEntry(c.name(), week, List.copyOf(cellMarkers)));
                }
                if (schedule.isPinned(c, week)) {
                    pins.add(new PinEntry(c.name(), week));
                }
            }
        }
        List<GroupEntry> exclusionGroupEntries = new ArrayList<>();
        for (ExclusionGroup g : schedule.exclusionGroups()) {
            exclusionGroupEntries.add(new GroupEntry(g.name(), List.copyOf(g.members())));
        }
        List<GroupEntry> inclusionGroupEntries = new ArrayList<>();
        for (InclusionGroup g : schedule.inclusionGroups()) {
            inclusionGroupEntries.add(new GroupEntry(g.name(), List.copyOf(g.members())));
        }
        RulesBlock rules = new RulesBlock(exclusionGroupEntries, inclusionGroupEntries);

        ScheduleDocument doc = new ScheduleDocument(
                schedule.startMonday(),
                schedule.lengthWeeks(),
                schedule.scheduleBlockSizes(),
                schedule.defaultDemand(),
                schedule.restWeeks(),
                schedule.roster(),
                assignments,
                markers,
                pins,
                rules);
        try {
            return MAPPER.writeValueAsString(doc);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize schedule", e);
        }
    }

    public static Schedule fromJson(String json) {
        try {
            ScheduleDocument doc = MAPPER.readValue(json, ScheduleDocument.class);
            WeeklyDemand demand = doc.defaultDemand() != null ? doc.defaultDemand() : new WeeklyDemand(2, 3, 5);
            int rest = doc.restWeeks() > 0 ? doc.restWeeks() : 2;
            Schedule schedule = new Schedule(doc.startMonday(), doc.lengthWeeks(), doc.roster(), demand, rest);
            if (doc.scheduleBlockSizes() != null && !doc.scheduleBlockSizes().isEmpty()) {
                schedule.setScheduleBlockSizes(doc.scheduleBlockSizes());
            }
            Map<String, Clinician> byName = new HashMap<>();
            for (Clinician c : doc.roster()) {
                byName.put(c.name(), c);
            }
            for (AssignmentEntry entry : doc.assignments()) {
                Clinician c = lookup(byName, entry.clinician());
                schedule.setState(c, entry.week(), entry.state());
            }
            for (MarkerEntry entry : doc.markers()) {
                Clinician c = lookup(byName, entry.clinician());
                for (WeekMarker marker : entry.markers()) {
                    schedule.setMarker(c, entry.week(), marker);
                }
            }
            if (doc.pins() != null) {
                for (PinEntry entry : doc.pins()) {
                    Clinician c = lookup(byName, entry.clinician());
                    schedule.pin(c, entry.week());
                }
            }
            if (doc.rules() != null) {
                if (doc.rules().exclusionGroups() != null) {
                    for (GroupEntry entry : doc.rules().exclusionGroups()) {
                        schedule.addExclusionGroup(
                                new ExclusionGroup(entry.name(), Set.copyOf(entry.members())));
                    }
                }
                if (doc.rules().inclusionGroups() != null) {
                    for (GroupEntry entry : doc.rules().inclusionGroups()) {
                        schedule.addInclusionGroup(
                                new InclusionGroup(entry.name(), Set.copyOf(entry.members())));
                    }
                }
            }
            return schedule;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse schedule JSON", e);
        }
    }

    private static Clinician lookup(Map<String, Clinician> byName, String name) {
        Clinician c = byName.get(name);
        if (c == null) {
            throw new IllegalArgumentException("Reference to unknown clinician: " + name);
        }
        return c;
    }

    private record ScheduleDocument(
            LocalDate startMonday,
            int lengthWeeks,
            List<Integer> scheduleBlockSizes,
            WeeklyDemand defaultDemand,
            int restWeeks,
            List<Clinician> roster,
            List<AssignmentEntry> assignments,
            List<MarkerEntry> markers,
            List<PinEntry> pins,
            RulesBlock rules) {}

    private record RulesBlock(List<GroupEntry> exclusionGroups, List<GroupEntry> inclusionGroups) {}

    private record GroupEntry(String name, List<String> members) {}

    private record AssignmentEntry(String clinician, int week, WeekState state) {}

    private record MarkerEntry(String clinician, int week, List<WeekMarker> markers) {}

    private record PinEntry(String clinician, int week) {}
}
