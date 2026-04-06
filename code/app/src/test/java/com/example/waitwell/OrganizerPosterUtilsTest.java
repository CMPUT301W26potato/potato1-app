package com.example.waitwell;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link OrganizerPosterUtils} poster URL selection logic.
 * Covers happy path and null-input boundary cases.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see OrganizerPosterUtils
 */
public class OrganizerPosterUtilsTest {

    /**
     * Checks that a newly picked URI is preferred over an existing saved URL.
     * This is the happy path.
     *
     * @author Karina Zhang
     */
    @Test
    public void testResolvePosterUrl_PrefersNewUriWhenPresent() {
        String result = OrganizerPosterUtils.resolvePosterUrl(
                "content://images/new-banner",
                "https://old.example.com/banner.png"
        );

        assertEquals("content://images/new-banner", result);
    }

    /**
     * Checks that existing URL is kept when no new URI is provided.
     * This is an alternative flow.
     *
     * @author Karina Zhang
     */
    @Test
    public void testResolvePosterUrl_KeepsExistingWhenNoNewUri() {
        String result = OrganizerPosterUtils.resolvePosterUrl(
                null,
                "https://old.example.com/banner.png"
        );

        assertEquals("https://old.example.com/banner.png", result);
    }

    /**
     * Checks that null is returned when neither value is provided.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testResolvePosterUrl_ReturnsNullWhenNeitherProvided() {
        String result = OrganizerPosterUtils.resolvePosterUrl(
                null,
                null
        );

        assertNull(result);
    }
}

