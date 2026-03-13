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
        assertNotNull(fragment);
    }
}

