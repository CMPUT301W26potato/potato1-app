package com.example.waitwell.activities;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;

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
    public void testIsOpen_StatusOpen_ReturnsTrue() {
        // Arrange: document with status exactly "open".
        when(doc.getString("status")).thenReturn("open");

        // Act: ask the helper whether this event is open.
        boolean result = MainActivity.isOpen(doc);

        // Assert: with status "open", we expect true.
        assertTrue(result);
    }

    @Test
    public void testIsOpen_StatusClosed_ReturnsFalse() {
        // Arrange: document whose status is "closed" instead.
        when(doc.getString("status")).thenReturn("closed");

        // Act: call isOpen on this closed event.
        boolean result = MainActivity.isOpen(doc);

        // Assert: since status is not "open", it should be reported as not open.
        assertFalse(result);
    }

    @Test
    public void testIsOpen_StatusNull_ReturnsFalse() {
        // Arrange: document with no status field at all (null).
        when(doc.getString("status")).thenReturn(null);

        // Act: run the helper with this incomplete document.
        boolean result = MainActivity.isOpen(doc);

        // Assert: null status should safely be treated as "not open".
        assertFalse(result);
    }
}

