package com.example.waitwell.activities;

import org.junit.Before;
import org.junit.Test;

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
        // Super lightweight check that the Organizer home fragment
        // can be constructed without pulling in the Android runtime.
        assertNotNull(fragment);
    }
}

