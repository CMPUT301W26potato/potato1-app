package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 lifecycle methods.
 *
 * I used Gemini to understand lightweight activity sanity tests that stay
 * JVM-safe before switching to full Espresso interaction tests.
 *
 * Sites I looked at:
 *
 * ActivityScenario docs:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for {@link ConfirmationActivity} using unit-test-safe checks.
 * Keeps coverage on class loading in JVM tests.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see ConfirmationActivity
 */
public class ConfirmationActivityTest {
    @Before public void setUp() throws Exception {}
    @After public void tearDown() throws Exception {}

    /**
     * Checks that class is available to unit tests.
     * This is a basic sanity case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testClassLoadable_notNull() {
        assertNotNull(ConfirmationActivity.class);
    }
}
