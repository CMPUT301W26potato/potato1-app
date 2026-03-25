package com.example.waitwell.activities;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * JUnit + Mockito tests for {@link MainActivity} helper logic.
 * I followed patterns from ChatGPT and the tutorial at
 * https://www.bacancytechnology.com/blog/unit-testing-using-mockito-in-android
 * to set up the mocks and structure these tests, but the actual checks are
 * tailored to how MainActivity reads Firestore data in this project.
 */
public class MainActivityTest {

    private DocumentSnapshot doc;

    @Before
    public void setUp() {
        // Mockito gives us a fake DocumentSnapshot.
        doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
    }

    @Test
    public void testMainActivity_ClassIsLoadable() {
        // Small check that the main activity class exists on the JVM classpath.
        assertNotNull(MainActivity.class);
    }

    @Test
    public void testGetEventTitle_HappyPath_UsesTitleFromDoc() {
        when(doc.getString("title")).thenReturn("Campus Concert");

        String title = MainActivity.getEventTitle(doc);

        // path: we should get back exactly what Firestore stored.
        assertEquals("Campus Concert", title);
    }

    @Test
    public void testGetEventTitle_NullTitle_UsesFallback() {
        when(doc.getString("title")).thenReturn(null);

        String title = MainActivity.getEventTitle(doc);

        // When there is no title we fall back to a default.
        assertEquals("Untitled Event", title);
    }

    @Test
    public void testGetOrganizerName_HappyPath_UsesOrganizerFromDoc() {
        when(doc.getString("organizerName")).thenReturn("CS Student Union");

        String organizer = MainActivity.getOrganizerName(doc);

        // Organizer name should be passed straight through when present.
        assertEquals("CS Student Union", organizer);
    }

    @Test
    public void testGetOrganizerName_NullOrganizer_ReturnsEmptyString() {
        when(doc.getString("organizerName")).thenReturn(null);

        String organizer = MainActivity.getOrganizerName(doc);

        // Null organizer names are normalized to an empty string for the UI.
        assertEquals("", organizer);
    }

    @Test
    public void testIsOpen_DeadlineStillFuture_ReturnsTrue() {
        when(doc.getDate("eventDate")).thenReturn(null);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 2);
        when(doc.getDate("registrationClose")).thenReturn(c.getTime());

        assertTrue(MainActivity.isOpen(doc));
    }

    @Test
    public void testIsOpen_DeadlinePassedButEventDayNotPast_ReturnsFalse() {
        Calendar close = Calendar.getInstance();
        close.add(Calendar.DAY_OF_MONTH, -2);
        when(doc.getDate("registrationClose")).thenReturn(close.getTime());
        Calendar ev = Calendar.getInstance();
        ev.add(Calendar.DAY_OF_MONTH, 5);
        when(doc.getDate("eventDate")).thenReturn(ev.getTime());

        assertFalse(MainActivity.isOpen(doc));
    }

    @Test
    public void testIsOpen_EventDayPassed_ReturnsFalse() {
        Calendar ev = Calendar.getInstance();
        ev.add(Calendar.DAY_OF_MONTH, -3);
        when(doc.getDate("eventDate")).thenReturn(ev.getTime());
        when(doc.getDate("registrationClose")).thenReturn(ev.getTime());

        assertFalse(MainActivity.isOpen(doc));
    }

    @Test
    public void testIsOpen_NoCloseDate_ReturnsTrue() {
        when(doc.getDate("eventDate")).thenReturn(null);
        when(doc.getDate("registrationClose")).thenReturn(null);

        assertTrue(MainActivity.isOpen(doc));
    }

    @Test
    public void testIsOpen_DocMissing_ReturnsFalse() {
        when(doc.exists()).thenReturn(false);

        assertFalse(MainActivity.isOpen(doc));
    }
}
