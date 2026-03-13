package com.example.waitwell.activities;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;

/**
 * JVM level sanity tests for {@link OrganizerEventDetailFragment}.
 */
public class OrganizerEventDetailFragmentTest {

    @Test
    public void testOrganizerEventDetailFragment_ClassIsLoadable() {
        // This is just a classpath sanity check so plain JUnit can see the fragment.
        assertNotNull(OrganizerEventDetailFragment.class);
    }

    /**
     * Reflection level test to ensure the {@code bindEvent(DocumentSnapshot)}
     * helper remains available for wiring Firestore documents into the UI.
     * The reflection pattern itself comes from the Android Studio testing docs
     * (`https://developer.android.com/studio/test/test-in-android-studio`)
     * and Gemini guidance for checking internals from plain JUnit.
     */
    @Test
    public void testBindEvent_MethodExists() throws Exception {
        Method m = OrganizerEventDetailFragment.class.getDeclaredMethod(
                "bindEvent",
                com.google.firebase.firestore.DocumentSnapshot.class
        );
        assertNotNull(m);
    }
}

