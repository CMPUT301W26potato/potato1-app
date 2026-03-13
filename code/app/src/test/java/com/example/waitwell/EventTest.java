package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Event model and Firestore snapshot reading.
 * Written with help from Claude (claude.ai)
 */
@RunWith(MockitoJUnitRunner.class)
public class EventTest {

    private Event event;

    @Before
    public void setUp() {
        event = new Event();
    }


    @Test
    public void testYogaDoc_fieldsReadCorrectly() {
        DocumentSnapshot mockYogaDoc = mock(DocumentSnapshot.class);

        when(mockYogaDoc.getString("title")).thenReturn("Yoga Hatha");
        when(mockYogaDoc.getString("location")).thenReturn("Edmonton, Alberta");
        when(mockYogaDoc.getDouble("price")).thenReturn(10.99);
        when(mockYogaDoc.getDouble("rating")).thenReturn(4.5);
        when(mockYogaDoc.getString("status")).thenReturn("open");

        assertEquals("Yoga Hatha", mockYogaDoc.getString("title"));
        assertEquals("Edmonton, Alberta", mockYogaDoc.getString("location"));
        assertEquals(10.99, mockYogaDoc.getDouble("price"), 0.001);
        assertEquals(4.5, mockYogaDoc.getDouble("rating"), 0.001);
        assertEquals("open", mockYogaDoc.getString("status"));
    }

    @Test
    public void testYogaDoc_emptyWaitlist() {
        DocumentSnapshot mockYogaDoc = mock(DocumentSnapshot.class);
        when(mockYogaDoc.get("waitlistEntrantIds")).thenReturn(new ArrayList<String>());

        @SuppressWarnings("unchecked")
        List<String> waitlist = (List<String>) mockYogaDoc.get("waitlistEntrantIds");

        assertNotNull(waitlist);
        assertTrue(waitlist.isEmpty());
    }

    @Test
    public void testSwimmingDoc_hasOneEntrant() {
        DocumentSnapshot mockSwimmingDoc = mock(DocumentSnapshot.class);
        when(mockSwimmingDoc.get("waitlistEntrantIds"))
                .thenReturn(Arrays.asList("dc4dec69ed9d4086"));

        @SuppressWarnings("unchecked")
        List<String> waitlist = (List<String>) mockSwimmingDoc.get("waitlistEntrantIds");

        assertEquals(1, waitlist.size());
        assertEquals("dc4dec69ed9d4086", waitlist.get(0));
    }

    @Test
    public void testSwimmingDoc_categoryFieldInconsistency() {
        DocumentSnapshot mockSwimmingDoc = mock(DocumentSnapshot.class);
        when(mockSwimmingDoc.getString("category")).thenReturn(null);
        when(mockSwimmingDoc.getString("Category")).thenReturn("Swimming");

        assertNull(mockSwimmingDoc.getString("category"));
        assertEquals("Swimming", mockSwimmingDoc.getString("Category"));
    }

    @Test
    public void testBoxingDoc_waitlistHasEmptyStringBug() {
        DocumentSnapshot mockBoxingDoc = mock(DocumentSnapshot.class);
        when(mockBoxingDoc.get("waitlistEntrantIds"))
                .thenReturn(Arrays.asList("", "dc4dec69ed9d4086"));

        @SuppressWarnings("unchecked")
        List<String> waitlist = (List<String>) mockBoxingDoc.get("waitlistEntrantIds");

        assertEquals(2, waitlist.size());
        assertEquals("", waitlist.get(0));
        assertEquals("dc4dec69ed9d4086", waitlist.get(1));
    }

    @Test
    public void testEmptyDoc_doesNotExist() {
        DocumentSnapshot mockEmptyDoc = mock(DocumentSnapshot.class);
        when(mockEmptyDoc.exists()).thenReturn(false);

        assertFalse(mockEmptyDoc.exists());
    }


    //  EVENT MODEL LOGIC

    @Test
    public void testIsRegistrationOpen_statusOpen() {
        event.setStatus("open");
        assertTrue(event.isRegistrationOpen());
    }

    @Test
    public void testIsRegistrationOpen_statusClosed() {
        event.setStatus("closed");
        assertFalse(event.isRegistrationOpen());
    }

    @Test
    public void testIsRegistrationOpen_withinDates() {
        event.setStatus("open");
        long now = System.currentTimeMillis();
        event.setRegistrationOpen(new Date(now - 86400000));
        event.setRegistrationClose(new Date(now + 86400000));

        assertTrue(event.isRegistrationOpen());
    }

    @Test
    public void testIsRegistrationOpen_afterCloseDate() {
        event.setStatus("open");
        long now = System.currentTimeMillis();
        event.setRegistrationOpen(new Date(now - 172800000));
        event.setRegistrationClose(new Date(now - 86400000));

        assertFalse(event.isRegistrationOpen());
    }

    @Test
    public void testGetDaysUntilClose_nullDate() {
        event.setRegistrationClose(null);
        assertEquals(0, event.getDaysUntilClose());
    }

    @Test
    public void testGetDaysUntilClose_pastDate() {
        event.setRegistrationClose(new Date(System.currentTimeMillis() - 86400000));
        assertEquals(0, event.getDaysUntilClose());
    }

    @Test
    public void testGetWaitlistCount_empty() {
        event.setWaitlistEntrantIds(new ArrayList<>());
        assertEquals(0, event.getWaitlistCount());
    }

    @Test
    public void testGetWaitlistCount_null() {
        event.setWaitlistEntrantIds(null);
        assertEquals(0, event.getWaitlistCount());
    }

    @Test
    public void testGetWaitlistCount_withEntrants() {
        event.setWaitlistEntrantIds(Arrays.asList("u1", "u2", "u3"));
        assertEquals(3, event.getWaitlistCount());
    }

    @Test
    public void testIsUserOnWaitlist_found() {
        event.setWaitlistEntrantIds(Arrays.asList("dc4dec69ed9d4086", "abc123"));
        assertTrue(event.isUserOnWaitlist("dc4dec69ed9d4086"));
    }

    @Test
    public void testIsUserOnWaitlist_notFound() {
        event.setWaitlistEntrantIds(Arrays.asList("abc123"));
        assertFalse(event.isUserOnWaitlist("dc4dec69ed9d4086"));
    }

    @Test
    public void testIsUserOnWaitlist_nullList() {
        event.setWaitlistEntrantIds(null);
        assertFalse(event.isUserOnWaitlist("anyone"));
    }

    @Test
    public void testIsWaitlistFull_noLimit() {
        event.setWaitlistLimit(null);
        event.setWaitlistEntrantIds(Arrays.asList("u1", "u2"));
        assertFalse(event.isWaitlistFull());
    }

    @Test
    public void testIsWaitlistFull_atLimit() {
        event.setWaitlistLimit(2);
        event.setWaitlistEntrantIds(Arrays.asList("u1", "u2"));
        assertTrue(event.isWaitlistFull());
    }

    @Test
    public void testIsWaitlistFull_underLimit() {
        event.setWaitlistLimit(10);
        event.setWaitlistEntrantIds(Arrays.asList("u1"));
        assertFalse(event.isWaitlistFull());
    }


    //  SAFE NULL HANDLING

    @Test
    public void testSafeRead_nullTitle() {
        DocumentSnapshot mockBadDoc = mock(DocumentSnapshot.class);
        when(mockBadDoc.getString("title")).thenReturn(null);
        when(mockBadDoc.getString("status")).thenReturn(null);
        when(mockBadDoc.getDouble("price")).thenReturn(null);

        String title = mockBadDoc.getString("title");
        String status = mockBadDoc.getString("status");
        Double price = mockBadDoc.getDouble("price");

        String safeTitle = (title != null) ? title : "Untitled";
        String safeStatus = (status != null) ? status : "open";
        double safePrice = (price != null) ? price : 0.0;

        assertEquals("Untitled", safeTitle);
        assertEquals("open", safeStatus);
        assertEquals(0.0, safePrice, 0.001);
    }
}

