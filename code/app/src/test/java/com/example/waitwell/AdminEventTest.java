package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for admin permissions related to Events.
 *
 * Verifies that:
 * - Admins can view and delete events
 * - Non-admins cannot perform these actions
 * - Null roles are handled safely
 */
public class AdminEventTest {
    /**
     * Tests that an admin can view all events.
     */
    @Test
    public void adminCanViewAllEvents() {
        assertTrue(EventUtils.canViewEvents("admin"));
    }

    /**
     * Tests that an admin can delete any event.
     */
    @Test
    public void adminCanDeleteAnyEvent() {
        assertTrue(EventUtils.canDeleteEvent("admin"));
    }

    /**
     * Tests that non-admin users cannot view events.
     */
    @Test
    public void nonAdminCannotViewEvents() {
        assertFalse(EventUtils.canViewEvents("organizer"));
        assertFalse(EventUtils.canViewEvents("entrant"));
    }

    /**
     * Tests that non-admin users cannot delete events.
     */
    @Test
    public void nonAdminCannotDeleteEvents() {
        assertFalse(EventUtils.canDeleteEvent("organizer"));
        assertFalse(EventUtils.canDeleteEvent("entrant"));
    }

    /**
     * Tests that null roles are safely rejected.
     */
    @Test
    public void nullRoleCannotViewOrDeleteEvents() {
        assertFalse(EventUtils.canViewEvents(null));
        assertFalse(EventUtils.canDeleteEvent(null));
    }
}