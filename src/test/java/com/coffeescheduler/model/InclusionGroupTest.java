package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InclusionGroupTest {

    @Test
    void createsGroupWithNameAndMembers() {
        InclusionGroup group = new InclusionGroup("Coverage", Set.of("Dr. A", "Dr. B"));

        assertEquals("Coverage", group.name());
        assertEquals(Set.of("Dr. A", "Dr. B"), group.members());
    }

    @Test
    void rejectsNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> new InclusionGroup(null, Set.of("Dr. A", "Dr. B")));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new InclusionGroup("  ", Set.of("Dr. A", "Dr. B")));
    }

    @Test
    void rejectsNullMembers() {
        assertThrows(IllegalArgumentException.class,
                () -> new InclusionGroup("Group", null));
    }

    @Test
    void rejectsFewerThanTwoMembers() {
        assertThrows(IllegalArgumentException.class,
                () -> new InclusionGroup("Group", Set.of("Dr. A")));
    }

    @Test
    void rejectsEmptyMembers() {
        assertThrows(IllegalArgumentException.class,
                () -> new InclusionGroup("Group", Set.of()));
    }

    @Test
    void membersAreDefensivelyCopied() {
        var members = new java.util.HashSet<>(Set.of("Dr. A", "Dr. B"));
        InclusionGroup group = new InclusionGroup("Group", members);
        members.add("Dr. C");

        assertEquals(2, group.members().size());
    }

    @Test
    void membersAreUnmodifiable() {
        InclusionGroup group = new InclusionGroup("Group", Set.of("Dr. A", "Dr. B"));

        assertThrows(UnsupportedOperationException.class,
                () -> group.members().add("Dr. C"));
    }

    @Test
    void threeOrMoreMembersAllowed() {
        InclusionGroup group = new InclusionGroup("Big group", Set.of("Dr. A", "Dr. B", "Dr. C"));

        assertEquals(3, group.members().size());
    }
}
