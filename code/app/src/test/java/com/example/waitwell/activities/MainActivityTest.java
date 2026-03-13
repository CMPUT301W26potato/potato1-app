package com.example.waitwell.activities;

import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        // Happy path: we should get back exactly what Firestore stored.
        assertEquals("Campus Concert", title);
    }

    @Test
    public void testGetEventTitle_NullTitle_UsesFallback() {
        when(doc.getString("title")).thenReturn(null);

        String title = MainActivity.getEventTitle(doc);

        // When there is no title we fall back to a friendly default.
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
    public void testGetPriceText_WithPrice_ShowsFormattedDollars() {
        when(doc.getDouble("price")).thenReturn(15.5);

        String priceText = MainActivity.getPriceText(doc);

        // Non-null price should be formatted as dollars with two decimals.
        assertEquals("$15.50", priceText);
    }

    @Test
    public void testGetPriceText_NullPrice_ShowsFree() {
        when(doc.getDouble("price")).thenReturn(null);

        String priceText = MainActivity.getPriceText(doc);

        // Null price is treated as a free event in the UI.
        assertEquals("Free", priceText);
    }

    @Test
    public void testIsOpen_StatusOpen_ReturnsTrue() {
        when(doc.getString("status")).thenReturn("open");

        assertTrue(MainActivity.isOpen(doc));
    }

    @Test
    public void testIsOpen_StatusClosedOrNull_ReturnsFalse() {
        when(doc.getString("status")).thenReturn("closed");
        assertFalse(MainActivity.isOpen(doc));

        when(doc.getString("status")).thenReturn(null);
        assertFalse(MainActivity.isOpen(doc));
    }
}

