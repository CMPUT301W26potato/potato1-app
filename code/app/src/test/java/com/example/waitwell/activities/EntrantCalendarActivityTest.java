package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, while this file stays in plain JUnit 4.
 *
 * I used Gemini to understand testing a private helper through reflection
 * so calendar month-label logic can be checked without UI rendering.
 *
 * Sites I looked at:
 *
 * ActivityScenario docs:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */

import static org.junit.Assert.*;

import org.junit.*;

import java.lang.reflect.Method;

/**
 * Tests for {@link EntrantCalendarActivity} helper behavior that can run in plain unit tests.
 * Focuses on month-name generation without touching Android UI rendering.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see EntrantCalendarActivity
 */
public class EntrantCalendarActivityTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks that month picker labels include 12 non-null values.
     * This is the happy path for the month selector helper.
     *
     * @author Karina Zhang
     */
    @Test
    public void testBuildMonthNames_returnsTwelveLabels() throws Exception {
        Method m = EntrantCalendarActivity.class.getDeclaredMethod("buildMonthNames");
        m.setAccessible(true);
        String[] names = (String[]) m.invoke(null);
        assertNotNull(names);
        assertEquals(12, names.length);
        for (String name : names) {
            assertNotNull(name);
            assertFalse(name.trim().isEmpty());
        }
    }
}
