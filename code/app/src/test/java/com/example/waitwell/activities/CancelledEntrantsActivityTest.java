package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with small JUnit 4 sanity checks.
 *
 * I used Gemini to understand the baseline activity-test pattern where we
 * start with intent-key/class checks before adding full UI instrumentation.
 *
 * Sites I looked at:
 *
 * ActivityScenario docs:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for {@link CancelledEntrantsActivity} with unit-test-safe checks.
 * Focuses on public constants and class availability.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see CancelledEntrantsActivity
 */
public class CancelledEntrantsActivityTest {
    @Before public void setUp() throws Exception {}
    @After public void tearDown() throws Exception {}

    /**
     * Checks that event id extra key is defined and not empty.
     * This is a boundary sanity check.
     *
     * @author Karina Zhang
     */
    @Test
    public void testExtraEventId_isNotEmpty() {
        assertNotNull(CancelledEntrantsActivity.EXTRA_EVENT_ID);
        assertFalse(CancelledEntrantsActivity.EXTRA_EVENT_ID.isEmpty());
    }
}
