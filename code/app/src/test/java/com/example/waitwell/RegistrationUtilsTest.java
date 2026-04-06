package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link RegistrationUtils#getDisplayStatus(String, boolean)}.
 * Verifies correct display text for different registration statuses.
 * @author Sarang Kim
 */
public class RegistrationUtilsTest {

    /** Tests that "selected" and "confirmed" statuses display as "Selected". */
    @Test
    public void testSelectedStatus() {
        assertEquals("Selected", RegistrationUtils.getDisplayStatus("selected", false));
        assertEquals("Selected", RegistrationUtils.getDisplayStatus("confirmed", false));
    }

    /** Tests that "rejected" status displays as "Not Selected". */
    @Test
    public void testRejectedStatus() {
        assertEquals("Not Selected", RegistrationUtils.getDisplayStatus("rejected", false));
    }

    /** Tests that completed registrations display as "Completed" regardless of status. */
    @Test
    public void testCompletedStatus() {
        assertEquals("Completed", RegistrationUtils.getDisplayStatus("selected", true));
        assertEquals("Completed", RegistrationUtils.getDisplayStatus("rejected", true));
    }

    /** Tests that unknown or null statuses display as "Unknown". */
    @Test
    public void testUnknownStatus() {
        assertEquals("Unknown", RegistrationUtils.getDisplayStatus("pending", false));
        assertEquals("Unknown", RegistrationUtils.getDisplayStatus(null, false));
    }
}