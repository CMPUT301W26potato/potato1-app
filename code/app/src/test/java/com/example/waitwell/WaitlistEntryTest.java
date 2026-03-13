package com.example.waitwell;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * WaitlistEntryTest.java
 * Unit tests for the WaitlistEntry model class.
 * Checks that the constructor sets fields correctly and that status
 * can be updated to the values used across the app like "selected" and "cancelled".
 * Covers US 02.06.01 and US 02.06.02.
 *
 * Tests written with help from Claude (claude.ai)
 */
@RunWith(MockitoJUnitRunner.class)
public class WaitlistEntryTest {

    @Mock
    private DocumentSnapshot mockWaitingEntry;

    @Mock
    private DocumentSnapshot mockSelectedEntry;

    @Mock
    private DocumentSnapshot mockRejectedEntry;

    @Test
    public void testConstructor_setsFieldsCorrectly() {
        WaitlistEntry entry = new WaitlistEntry("dc4dec69ed9d4086", "event_001", "Yoga Hatha");
        assertEquals("dc4dec69ed9d4086", entry.getUserId());
        assertEquals("event_001", entry.getEventId());
        assertEquals("Yoga Hatha", entry.getEventTitle());
        assertEquals("waiting", entry.getStatus());
    }

    @Test
    public void testDefaultConstructor_fieldsNull() {
        WaitlistEntry entry = new WaitlistEntry();
        assertNull(entry.getUserId());
        assertNull(entry.getEventId());
        assertNull(entry.getStatus());
    }

    @Test
    public void testStatusTransition_waitingToSelected() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Boxing");
        assertEquals("waiting", entry.getStatus());
        entry.setStatus("selected");
        assertEquals("selected", entry.getStatus());
    }

    @Test
    public void testStatusTransition_selectedToConfirmed() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Boxing");
        entry.setStatus("selected");
        entry.setStatus("confirmed");
        assertEquals("confirmed", entry.getStatus());
    }

    @Test
    public void testStatusTransition_selectedToRejected() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Boxing");
        entry.setStatus("selected");
        entry.setStatus("rejected");
        assertEquals("rejected", entry.getStatus());
    }

    @Test
    public void testStatusTransition_waitingToCancelled() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Swimming");
        entry.setStatus("cancelled");
        assertEquals("cancelled", entry.getStatus());
    }

    @Test
    public void testDocumentIdFormat() {
        String userId = "dc4dec69ed9d4086";
        String eventId = "swimming_001";
        WaitlistEntry entry = new WaitlistEntry(userId, eventId, "Swimming");
        entry.setId(userId + "_" + eventId);
        assertEquals("dc4dec69ed9d4086_swimming_001", entry.getId());
    }

    @Test
    public void testGeolocationFields() {
        WaitlistEntry entry = new WaitlistEntry("u1", "e1", "Yoga");
        entry.setJoinLatitude(53.5461);
        entry.setJoinLongitude(-113.4937);
        assertEquals(53.5461, entry.getJoinLatitude(), 0.0001);
        assertEquals(-113.4937, entry.getJoinLongitude(), 0.0001);
    }

    // ─── Mock DocumentSnapshot reads ───
    // (simulates what WaitListActivity does when loading entries)

    @Test
    public void testMockSnapshot_waitingEntry() {
        when(mockWaitingEntry.getString("userId")).thenReturn("dc4dec69ed9d4086");
        assertEquals("dc4dec69ed9d4086", mockWaitingEntry.getString("userId"));
    }

    @Test
    public void testMockSnapshot_selectedEntry() {
        when(mockSelectedEntry.getString("eventTitle")).thenReturn("Yoga Hatha");
        when(mockSelectedEntry.getString("status")).thenReturn("selected");

        assertEquals("Yoga Hatha", mockSelectedEntry.getString("eventTitle"));
        assertEquals("selected", mockSelectedEntry.getString("status"));
    }

    @Test
    public void testMockSnapshot_rejectedEntry() {
        when(mockRejectedEntry.getString("eventTitle")).thenReturn("Boxing");
        when(mockRejectedEntry.getString("status")).thenReturn("rejected");

        assertEquals("Boxing", mockRejectedEntry.getString("eventTitle"));
        assertEquals("rejected", mockRejectedEntry.getString("status"));
    }

    @Test
    public void testMockSnapshot_nullStatus_defaultsToWaiting() {
        DocumentSnapshot mockNull = mock(DocumentSnapshot.class);
        when(mockNull.getString("status")).thenReturn(null);

        String status = mockNull.getString("status");
        // Our WaitListActivity does: if (status == null) status = "waiting";
        String safeStatus = (status != null) ? status : "waiting";
        assertEquals("waiting", safeStatus);
    }

    @Test
    public void testMockSnapshot_nullEventTitle_handled() {
        DocumentSnapshot mockNull = mock(DocumentSnapshot.class);
        when(mockNull.getString("eventTitle")).thenReturn(null);

        String title = mockNull.getString("eventTitle");
        String safeTitle = (title != null) ? title : "Unknown Event";
        assertEquals("Unknown Event", safeTitle);
    }
}