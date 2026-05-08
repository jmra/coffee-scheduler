package com.coffeescheduler.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExclusionGroupTest {

    @Test
    void createsGroupWithNameAndMembers() {
        ExclusionGroup group = new ExclusionGroup("Cardiology", Set.of("Dr. A", "Dr. B"));

        assertEquals("Cardiology", group.name());
        assertEquals(Set.of("Dr. A", "Dr. B"), group.members());
    }

    @Test
    void rejectsNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExclusionGroup(null, Set.of("Dr. A", "Dr. B")));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExclusionGroup("  ", Set.of("Dr. A", "Dr. B")));
    }

    @Test
    void rejectsNullMembers() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExclusionGroup("Group", null));
    }

    @Test
    void rejectsFewerThanTwoMembers() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExclusionGroup("Group", Set.of("Dr. A")));
    }

    @Test
    void rejectsEmptyMembers() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExclusionGroup("Group", Set.of()));
    }

    @Test
    void membersAreDefensivelyCopied() {
        var members = new java.util.HashSet<>(Set.of("Dr. A", "Dr. B"));
        ExclusionGroup group = new ExclusionGroup("Group", members);
        members.add("Dr. C");

        assertEquals(2, group.members().size());
    }

    @Test
    void membersAreUnmodifiable() {
        ExclusionGroup group = new ExclusionGroup("Group", Set.of("Dr. A", "Dr. B"));

        assertThrows(UnsupportedOperationException.class,
                () -> group.members().add("Dr. C"));
    }

    @Test
    void threeOrMoreMembersAllowed() {
        ExclusionGroup group = new ExclusionGroup("Big group", Set.of("Dr. A", "Dr. B", "Dr. C"));

        assertEquals(3, group.members().size());
    }
}
