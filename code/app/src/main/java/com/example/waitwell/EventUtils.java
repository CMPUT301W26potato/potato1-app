package com.example.waitwell;

public class EventUtils {

    /**
     * Determines if a user can delete or view an event
     *
     * @param role the role of the current user
     * @return true if admin can delete
     */
    public static boolean canDeleteEvent(String role) {
        if (role == null) return false;

        return role.equalsIgnoreCase("admin");
    }
    public static boolean canViewEvents(String role) {
        return role != null && role.equalsIgnoreCase("admin");
    }
}