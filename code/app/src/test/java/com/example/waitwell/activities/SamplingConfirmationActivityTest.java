package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 and intent-extra assertions.
 *
 * I used Gemini to understand how to verify navigation payload keys for
 * confirmation screens before writing Espresso intent verifications.
 *
 * Sites I looked at:
 *
 * ActivityScenario docs:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for {@link SamplingConfirmationActivity} intent extra keys.
 * Covers constant presence checks.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see SamplingConfirmationActivity
 */
public class SamplingConfirmationActivityTest {
    @Before public void setUp() throws Exception {}
    @After public void tearDown() throws Exception {}

    /**
     * Checks sampled count key exists for intent payload.
     * This is a sanity check.
     *
     * @author Karina Zhang
     */
    @Test
    public void testExtraSampledCount_isDefined() {
        assertNotNull(SamplingConfirmationActivity.EXTRA_SAMPLED_COUNT);
        assertFalse(SamplingConfirmationActivity.EXTRA_SAMPLED_COUNT.isEmpty());
    }

    /**
     * Checks event id key exists for intent payload.
     * This is a sanity check.
     *
     * @author Karina Zhang
     */
    @Test
    public void testExtraEventId_isDefined() {
        assertNotNull(SamplingConfirmationActivity.EXTRA_EVENT_ID);
        assertFalse(SamplingConfirmationActivity.EXTRA_EVENT_ID.isEmpty());
    }
}
