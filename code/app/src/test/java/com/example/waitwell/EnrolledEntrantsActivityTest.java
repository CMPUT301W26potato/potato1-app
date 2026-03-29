package com.example.waitwell;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the enrolled entrants cancel no-show flow (US 02.06.04).
 * Uses WaitlistEntry model to verify status transitions that the activity triggers.
 * Tests written with help from Claude (claude.ai)
 */
public class EnrolledEntrantsActivityTest {

    @Test
    public void testConfirmedStatusBeforeCancel() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Yoga");
        entry.setStatus("confirmed");
        assertEquals("confirmed", entry.getStatus());
    }

    @Test
    public void testStatusTransition_confirmedToCancelled() {
        // simulates what cancelEntrant() does in Firestore
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Yoga");
        entry.setStatus("confirmed");
        entry.setStatus("cancelled");
        assertEquals("cancelled", entry.getStatus());
    }

    @Test
    public void testCancelDoesNotAffectOtherFields() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Yoga");
        entry.setStatus("confirmed");
        entry.setStatus("cancelled");
        // user/event fields must be untouched
        assertEquals("u1", entry.getUserId());
        assertEquals("e1", entry.getEventId());
        assertEquals("Yoga", entry.getEventTitle());
    }

    @Test
    public void testEntryDocId_notNullAfterSet() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Yoga");
        entry.setId("u1_e1");
        assertNotNull(entry.getId());
        assertEquals("u1_e1", entry.getId());
    }

    @Test
    public void testMultipleStatusTransitions() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Yoga");
        entry.setStatus("selected");
        entry.setStatus("confirmed");
        entry.setStatus("cancelled");
        assertEquals("cancelled", entry.getStatus());
    }


}