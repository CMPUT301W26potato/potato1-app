package com.example.waitwell.activities;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;

/**
 * JVM tests for {@link OrganizerEventCreatedFragment}.
 */
public class OrganizerEventCreatedFragmentTest {

    @Test
    public void testOrganizerEventCreatedFragment_ClassIsLoadable() {
        // Another JVM-only check that the confirmation fragment type exists.
        assertNotNull(OrganizerEventCreatedFragment.class);
    }

    /**
     * Reflection check that {@code generateQrCode(String)} is present on the
     * confirmation fragment. This keeps the QR behaviour discoverable for
     * tests without exposing it as a public API.
     */
    @Test
    public void testGenerateQrCode_MethodExists() throws Exception {
        // Learned this reflection-based sanity check pattern from the
        // Android Studio "Test in Android Studio" docs and Gemini, which
        // showed how to keep tests JVM-only while still touching internals.
        Method m = OrganizerEventCreatedFragment.class
                .getDeclaredMethod("generateQrCode", String.class);
        assertNotNull(m);
    }
}

