package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 and small intent-key checks.
 *
 * I used Gemini to understand how to keep activity routing tests simple in unit scope
 * by validating extras/constants before adding instrumentation paths.
 *
 * Sites I looked at:
 *
 * ActivityScenario docs:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for {@link FinalEntrantsActivity} constants used for intent routing.
 * Keeps coverage in plain JVM tests without touching Android UI.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see FinalEntrantsActivity
 */
public class FinalEntrantsActivityTest {
    @Before public void setUp() throws Exception {}
    @After public void tearDown() throws Exception {}

    /**
     * Checks that the event id extra key is present.
     * This is a boundary sanity check.
     *
     * @author Karina Zhang
     */
    @Test
    public void testExtraEventId_isDefined() {
        assertNotNull(FinalEntrantsActivity.EXTRA_EVENT_ID);
        assertFalse(FinalEntrantsActivity.EXTRA_EVENT_ID.isEmpty());
    }
}
