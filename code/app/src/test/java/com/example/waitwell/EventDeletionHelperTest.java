package com.example.waitwell;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 setup and cleanup.
 *
 * I used Gemini to understand how to test delete-flow boundary behavior through
 * callback assertions without hitting a real Firestore backend.
 *
 * Sites I looked at:
 *
 * Firestore local emulator:
 * https://firebase.google.com/docs/emulator-suite/connect_firestore
 *
 * Mockito docs:
 * https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
 */

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for {@link EventDeletionHelper} callback behavior in boundary inputs.
 * Focuses on inputs that do not require live Firestore setup.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see EventDeletionHelper
 */
public class EventDeletionHelperTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks that empty event id immediately reports false to callback.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testDeleteEvent_emptyEventId_callsListenerFalse() {
        final boolean[] result = new boolean[] { true };
        EventDeletionHelper.deleteEvent("", success -> result[0] = success);
        assertFalse(result[0]);
    }
}
