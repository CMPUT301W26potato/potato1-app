package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OrganizerFirebaseUtils} helper reads from Firestore snapshots.
 * Covers normal values plus null/blank fallback behavior.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see OrganizerFirebaseUtils
 */
public class OrganizerFirebaseUtilsTest {

    private DocumentSnapshot doc;

    @Before
    public void setUp() {
        doc = mock(DocumentSnapshot.class);
    }

    @After
    public void tearDown() {
        doc = null;
    }

    @Test
    public void testGetTitleOrFallback_happyPath_usesTitleFromDocument() {
        when(doc.getString("title")).thenReturn("Organizer Party");
        String title = OrganizerFirebaseUtils.getTitleOrFallback(doc);
        assertEquals("Organizer Party", title);
    }

    @Test
    public void testGetTitleOrFallback_nullOrEmpty_usesFallback() {
        when(doc.getString("title")).thenReturn(null);
        assertEquals("Untitled Event", OrganizerFirebaseUtils.getTitleOrFallback(doc));
        when(doc.getString("title")).thenReturn("   ");
        assertEquals("Untitled Event", OrganizerFirebaseUtils.getTitleOrFallback(doc));
    }

    @Test
    public void testGetStatusOrDefault_happyPath_usesStoredStatus() {
        when(doc.getString("status")).thenReturn("closed");
        String status = OrganizerFirebaseUtils.getStatusOrDefault(doc);
        assertEquals("closed", status);
    }

    @Test
    public void testGetStatusOrDefault_nullOrEmpty_defaultsToOpen() {
        when(doc.getString("status")).thenReturn(null);
        assertEquals("open", OrganizerFirebaseUtils.getStatusOrDefault(doc));
        when(doc.getString("status")).thenReturn("   ");
        assertEquals("open", OrganizerFirebaseUtils.getStatusOrDefault(doc));
    }
}

