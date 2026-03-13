package com.example.waitwell;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Rehaan;s addition
 * Unit tests for the WaitlistEntry model.
 * Verifies that status values used by InvitedEntrantsActivity are handled correctly.
 */
public class WaitlistEntryTest {

    @Test
    public void testDefaultStatusIsWaiting() {
        WaitlistEntry entry = new WaitlistEntry("user1", "event1", "Swim Lessons");
        // Fresh entries should start out in the "waiting" state.
        assertEquals("waiting", entry.getStatus());
    }

    @Test
    public void testSetStatusToSelected() {
        WaitlistEntry entry = new WaitlistEntry("user1", "event1", "Swim Lessons");
        entry.setStatus("selected");
        // After being picked, the status should change to "selected".
        assertEquals("selected", entry.getStatus());
    }

    @Test
    public void testSetStatusToCancelled() {
        WaitlistEntry entry = new WaitlistEntry("user1", "event1", "Swim Lessons");
        entry.setStatus("cancelled");
        // When a user cancels, we expect the "cancelled" status to stick.
        assertEquals("cancelled", entry.getStatus());
    }

    @Test
    public void testConstructorSetsFields() {
        WaitlistEntry entry = new WaitlistEntry("user123", "event456", "Dance Class");
        // This checks the main constructor wires all three core fields correctly.
        assertEquals("user123", entry.getUserId());
        assertEquals("event456", entry.getEventId());
        assertEquals("Dance Class", entry.getEventTitle());
    }

    @Test
    public void testEmptyConstructor() {
        WaitlistEntry entry = new WaitlistEntry();
        // The no-arg constructor is only for Firebase; all fields should start null.
        assertNull(entry.getUserId());
        assertNull(entry.getEventId());
        assertNull(entry.getStatus());
    }
}