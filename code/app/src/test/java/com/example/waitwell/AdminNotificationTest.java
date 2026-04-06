package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for admin permissions related to Notifications.
 *
 * Verifies that:
 * - Admins can view all notifications
 * - Non-admins cannot view notifications
 * - Null roles are handled safely
 */
public class AdminNotificationTest {

    /**
     * Tests that an admin can view all notifications.
     */
    @Test
    public void adminCanViewAllNotifications() {
        assertTrue(NotificationUtils.canViewAllNotifications("admin"));
    }

    /**
     * Tests that non-admin users cannot view notifications.
     */
    @Test
    public void nonAdminCannotViewNotifications() {
        assertFalse(NotificationUtils.canViewAllNotifications("organizer"));
        assertFalse(NotificationUtils.canViewAllNotifications("entrant"));
    }

    /**
     * Tests that null roles are handled correctly.
     */
    @Test
    public void nullRoleCannotViewNotifications() {
        assertFalse(NotificationUtils.canViewAllNotifications(null));
    }
}