package com.example.waitwell;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Small JUnit test suite for {@link OrganizerPosterUtils} that focuses on
 * the photo/banner selection rules for Organizer events.
 */
public class OrganizerPosterUtilsTest {

    @Test
    public void testResolvePosterUrl_PrefersNewUriWhenPresent() {
        String result = OrganizerPosterUtils.resolvePosterUrl(
                "content://images/new-banner",
                "https://old.example.com/banner.png"
        );

        //  path: when the user just picked a new image we always use that URI.
        assertEquals("content://images/new-banner", result);
    }

    @Test
    public void testResolvePosterUrl_KeepsExistingWhenNoNewUri() {
        String result = OrganizerPosterUtils.resolvePosterUrl(
                null,
                "https://old.example.com/banner.png"
        );

        // Editing an event without changing the banner should keep the existing URL.
        assertEquals("https://old.example.com/banner.png", result);
    }

    @Test
    public void testResolvePosterUrl_ReturnsNullWhenNeitherProvided() {
        String result = OrganizerPosterUtils.resolvePosterUrl(
                null,
                null
        );

        // New event with no banner chosen at all.
        assertNull(result);
    }
}

