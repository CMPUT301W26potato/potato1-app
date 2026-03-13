package com.example.waitwell.activities;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;

/**
 * JMV Level sanity tests for {@link OrganizerHomeFragment}.
 */
public class OrganizerHomeFragmentTest {

    private OrganizerHomeFragment fragment;

    @Before
    public void setUp() {
        fragment = new OrganizerHomeFragment();
    }

    @Test
    public void testFragment_CanBeConstructed() {
        // Super small check that the Organizer home fragment
        // can be constructed without pulling in the Android runtime.
        assertNotNull(fragment);
    }

    /**
     * Checks that the private
     * {@code onEventsLoaded(QuerySnapshot)} helper still exists. This is where
     * the "My Events" rows and status badges are rendered.
     * The reflection pattern here follows what I learned from the Android
     * Studio testing guide (`https://developer.android.com/studio/test/test-in-android-studio`)
     * and Gemini, so the test stays JVM-only but can still see internals.
     */
    @Test
    public void testOnEventsLoaded_MethodExists() throws Exception {
        Method m = OrganizerHomeFragment.class.getDeclaredMethod(
                "onEventsLoaded",
                com.google.firebase.firestore.QuerySnapshot.class
        );
        assertNotNull(m);
    }

    /**
     * Similar reflection check for {@code applyStatusBadge(TextView, String)},
     * which is responsible for converting status strings like "open" or
     * "closed" into the styled status pill used in the organizer list.
     * Same reflection style as above
     */
    @Test
    public void testApplyStatusBadge_MethodExists() throws Exception {
        Method m = OrganizerHomeFragment.class.getDeclaredMethod(
                "applyStatusBadge",
                android.widget.TextView.class,
                String.class
        );
        assertNotNull(m);
    }
}

