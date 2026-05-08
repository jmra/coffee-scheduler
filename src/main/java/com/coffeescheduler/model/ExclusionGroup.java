package com.coffeescheduler.model;

import java.util.Set;

public record ExclusionGroup(String name, Set<String> members) {
    public ExclusionGroup {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-blank");
        }
        if (members == null) {
            throw new IllegalArgumentException("members must not be null");
        }
        if (members.size() < 2) {
            throw new IllegalArgumentException("exclusion group must have at least 2 members, got " + members.size());
        }
        members = Set.copyOf(members);
    }
}
