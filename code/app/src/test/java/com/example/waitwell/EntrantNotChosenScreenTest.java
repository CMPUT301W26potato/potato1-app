package com.example.waitwell;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 and clear arrange/act/assert flow.
 *
 * I used Gemini to understand mocking Firestore DocumentSnapshot values and
 * testing private helper methods through reflection for boundary checks.
 *
 * Sites I looked at:
 *
 * Mockito docs:
 * https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
 *
 * Firestore local emulator:
 * https://firebase.google.com/docs/emulator-suite/connect_firestore
 */

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.*;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Tests for helper logic inside {@link EntrantNotChosenScreen} using reflection.
 * Covers event-full detection and redraw failure detection behavior.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see EntrantNotChosenScreen
 */
public class EntrantNotChosenScreenTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks that event-full helper returns true when confirmed list reaches limit.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testIsEventFullFromSnapshot_limitReached_returnsTrue() throws Exception {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getLong("waitlistLimit")).thenReturn(2L);
        when(doc.get("AttendingEntrants")).thenReturn(Arrays.asList("u1", "u2"));

        Method m = EntrantNotChosenScreen.class.getDeclaredMethod("isEventFullFromSnapshot", DocumentSnapshot.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, doc);
        assertTrue(result);
    }

    /**
     * Checks that event-full helper returns false when no limit is configured.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testIsEventFullFromSnapshot_noLimit_returnsFalse() throws Exception {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getLong("waitlistLimit")).thenReturn(0L);

        Method m = EntrantNotChosenScreen.class.getDeclaredMethod("isEventFullFromSnapshot", DocumentSnapshot.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, doc);
        assertFalse(result);
    }
}
