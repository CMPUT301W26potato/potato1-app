package com.example.waitwell;

public class NotificationUtils {

    /**
     * Determines if a user can view all notifications
     */
    public static boolean canViewAllNotifications(String role) {
        if (role == null) return false;

        return role.equalsIgnoreCase("admin");
    }
}