package com.example.waitwell.activities;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link OrganizerCreateEventFragment}.
 */
public class OrganizerCreateEventFragmentTest {

    private OrganizerCreateEventFragment fragment;

    @Before
    public void setUp() {
        fragment = new OrganizerCreateEventFragment();
    }

    @Test
    public void testFragment_CanBeConstructed() {
        assertNotNull(fragment);
    }

    @Test
    public void testParseDate_ValidDateString_ReturnsDate() throws Exception {
        // Use reflection so we do not need to change the fragment API
        Method parseDate = OrganizerCreateEventFragment.class
                .getDeclaredMethod("parseDate", String.class);
        parseDate.setAccessible(true);

        String input = "2024-03-01";
        Object result = parseDate.invoke(fragment, input);

        assertNotNull(result);
        Date parsed = (Date) result;

        // Super basic sanity check on the formatting
        String roundTrip = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed);
        assertEquals(input, roundTrip);
    }

    @Test
    public void testParseDate_InvalidDateString_ReturnsNull() throws Exception {
        Method parseDate = OrganizerCreateEventFragment.class
                .getDeclaredMethod("parseDate", String.class);
        parseDate.setAccessible(true);

        Object resultEmpty = parseDate.invoke(fragment, "");
        Object resultGarbage = parseDate.invoke(fragment, "not-a-date");

        assertNull(resultEmpty);
        assertNull(resultGarbage);
    }
}

