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
        // Just check that the fragment can be new'ed up on the JVM.
        assertNotNull(fragment);
    }

    @Test
    public void testParseDate_ValidDateString_ReturnsDate() throws Exception {
        // I used the official Android Studio testing docs and Gemini
        // (`https://developer.android.com/studio/test/test-in-android-studio`)
        // to learn this reflection-based approach so we can exercise private
        // helpers without changing the fragment's public API.
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
        // Same reflection pattern as above, based on guidance from the
        // Android Studio testing docs and Gemini walkthroughs.
        Method parseDate = OrganizerCreateEventFragment.class
                .getDeclaredMethod("parseDate", String.class);
        parseDate.setAccessible(true);

        // These cases cover bad or empty input strings so the validator
        // can handle them as "no date selected" instead of crashing.
        Object resultEmpty = parseDate.invoke(fragment, "");
        Object resultGarbage = parseDate.invoke(fragment, "not-a-date");

        assertNull(resultEmpty);
        assertNull(resultGarbage);
    }

    /**
     * Reflection based test that {@code saveEventToFirestore(...)} still
     * exists on the fragment with the same basic signature. This helps catch
     * accidental renames without forcing that method to become public.
     */
    @Test
    public void testSaveEventToFirestore_MethodExists() throws Exception {
        // Reflection pattern inspired by the Android Studio "Test in Android
        // Studio" guide and Gemini, just to assert the method signature exists.
        // Added missing boolean isPrivate parameter
        Method m = OrganizerCreateEventFragment.class.getDeclaredMethod(
                "saveEventToFirestore",
                String.class,      // organizerId
                String.class,      // title
                String.class,      // description
                String.class,      // location
                boolean.class,     // geolocationRequired
                boolean.class,     // isPrivate (was missing!)
                Date.class,        // registrationOpen
                Date.class,        // registrationClose
                Date.class,        // eventDateTime
                Integer.class,     // waitlistLimit
                double.class,      // price
                String.class       // posterUrl (nullable)
        );
        // If we get here the method is present with the expected shape.
        assertNotNull(m);
    }

    /**
     * Similar reflection check for {@code updateEventInFirestore(...)} which
     * is used when organizers edit an existing event instead of creating one.
     */
    @Test
    public void testUpdateEventInFirestore_MethodExists() throws Exception {
        // Same reflection technique as above, again based on the official
        // testing docs and Gemini suggestions for JVM-only sanity checks.
        // Added missing boolean isPrivate parameter
        Method m = OrganizerCreateEventFragment.class.getDeclaredMethod(
                "updateEventInFirestore",
                String.class,      // eventId
                String.class,      // organizerId
                String.class,      // title
                String.class,      // description
                String.class,      // location
                boolean.class,     // geolocationRequired
                boolean.class,     // isPrivate (was missing!)
                Date.class,        // registrationOpen
                Date.class,        // registrationClose
                Date.class,        // eventDateTime
                Integer.class,     // waitlistLimit
                double.class,      // price
                String.class       // posterUrl (nullable)
        );
        assertNotNull(m);
    }
}