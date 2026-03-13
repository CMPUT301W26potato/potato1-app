package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mockito-based tests for {@link OrganizerFirebaseUtils}.
 * I used ChatGPT and the article
 * https://www.bacancytechnology.com/blog/unit-testing-using-mockito-in-android
 * as references for how to structure these Mockito unit tests.
 * The actual assertions and field choices are specific to our Organizer module.
 *  * These follow the same general style as the lab 6 example:
 * I set up a shared object in @Before, exercise a method, and then
 * assert on the result using the JUnit assert helpers.
 */
public class OrganizerFirebaseUtilsTest {

    private DocumentSnapshot doc;

    @Before
    public void setUp() {
        // instead we use Mockito to fake a DocumentSnapshot.
        doc = mock(DocumentSnapshot.class);
    }

    @Test
    public void testGetTitleOrFallback_HappyPath_UsesTitleFromDocument() {
        // The "database" has a normal title
        when(doc.getString("title")).thenReturn("Organizer Party");

        // Call the helper under test
        String title = OrganizerFirebaseUtils.getTitleOrFallback(doc);

        // The exact title from the doc is returned
        assertEquals("Organizer Party", title);
    }

    @Test
    public void testGetTitleOrFallback_NullOrEmpty_UsesFallback() {
        // First scenario: completely missing title should fall back.
        when(doc.getString("title")).thenReturn(null);
        assertEquals("Untitled Event", OrganizerFirebaseUtils.getTitleOrFallback(doc));

        // Second scenario: title is only whitespace and still falls back.
        when(doc.getString("title")).thenReturn("   ");
        assertEquals("Untitled Event", OrganizerFirebaseUtils.getTitleOrFallback(doc));
    }

    @Test
    public void testGetStatusOrDefault_HappyPath_UsesStoredStatus() {
        // Stored as some nonempty value
        when(doc.getString("status")).thenReturn("closed");

        String status = OrganizerFirebaseUtils.getStatusOrDefault(doc);

        assertEquals("closed", status);
    }

    @Test
    public void testGetStatusOrDefault_NullOrEmpty_DefaultsToOpen() {
        // Null status from Firestore should default to "open".
        when(doc.getString("status")).thenReturn(null);
        assertEquals("open", OrganizerFirebaseUtils.getStatusOrDefault(doc));

        // Empty / whitespace status from Firestore
        when(doc.getString("status")).thenReturn("   ");
        assertEquals("open", OrganizerFirebaseUtils.getStatusOrDefault(doc));
    }
}

