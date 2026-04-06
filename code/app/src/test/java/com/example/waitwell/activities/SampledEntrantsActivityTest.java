package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 constant checks.
 *
 * I used Gemini to understand activity intent-key checks as a first testing layer
 * for routing-heavy screens.
 *
 * Sites I looked at:
 *
 * ActivityScenario docs:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for {@link SampledEntrantsActivity} routing constants.
 * Keeps checks JVM-safe without UI rendering.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see SampledEntrantsActivity
 */
public class SampledEntrantsActivityTest {
    @Before public void setUp() throws Exception {}
    @After public void tearDown() throws Exception {}

    /**
     * Checks that EXTRA_EVENT_ID key is available for navigation intents.
     * This is a sanity case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testExtraEventId_exists() {
        assertNotNull(SampledEntrantsActivity.EXTRA_EVENT_ID);
        assertFalse(SampledEntrantsActivity.EXTRA_EVENT_ID.isEmpty());
    }
}
