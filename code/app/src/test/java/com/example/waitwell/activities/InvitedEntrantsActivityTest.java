package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, using JUnit 4 setup and assertions.
 *
 * I used Gemini to understand a clean way to validate intent-extra keys
 * for activity routing in unit tests before full UI coverage.
 *
 * Sites I looked at:
 *
 * ActivityScenario docs:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for {@link InvitedEntrantsActivity} intent constant behavior.
 * Uses JVM-safe assertions only.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see InvitedEntrantsActivity
 */
public class InvitedEntrantsActivityTest {
    @Before public void setUp() throws Exception {}
    @After public void tearDown() throws Exception {}

    /**
     * Checks that EXTRA_EVENT_ID exists and is non-empty.
     * This is a boundary sanity check.
     *
     * @author Karina Zhang
     */
    @Test
    public void testExtraEventId_isNotBlank() {
        assertNotNull(InvitedEntrantsActivity.EXTRA_EVENT_ID);
        assertFalse(InvitedEntrantsActivity.EXTRA_EVENT_ID.trim().isEmpty());
    }
}
