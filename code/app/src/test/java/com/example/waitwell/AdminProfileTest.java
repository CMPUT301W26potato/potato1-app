package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for admin permissions related to Profiles.
 *
 * Verifies that:
 * - Admins can view and delete any profile
 * - Non-admins cannot perform these actions
 * - Null inputs are handled correctly
 */
public class AdminProfileTest {
    /**
     * Tests that an admin can view all profiles.
     */
    @Test
    public void adminCanViewAllProfiles() {
        assertTrue(ProfileUtils.canViewProfiles("admin"));
    }

    /**
     * Tests that an admin can delete any profile.
     */
    @Test
    public void adminCanDeleteAnyProfile() {
        assertTrue(ProfileUtils.canDeleteProfile("admin", "user123"));
    }

    /**
     * Tests that non-admin users cannot view profiles.
     */
    @Test
    public void nonAdminCannotViewProfiles() {
        assertFalse(ProfileUtils.canViewProfiles("organizer"));
        assertFalse(ProfileUtils.canViewProfiles("entrant"));
    }

    /**
     * Tests that non-admin users cannot delete profiles.
     */
    @Test
    public void nonAdminCannotDeleteProfiles() {
        assertFalse(ProfileUtils.canDeleteProfile("organizer", "user123"));
        assertFalse(ProfileUtils.canDeleteProfile("entrant", "user123"));
    }

    /**
     * Tests handling of null inputs.
     */
    @Test
    public void nullInputsCannotViewOrDeleteProfiles() {
        assertFalse(ProfileUtils.canViewProfiles(null));
        assertFalse(ProfileUtils.canDeleteProfile(null, "user123"));
        assertFalse(ProfileUtils.canDeleteProfile("admin", null));
    }
}