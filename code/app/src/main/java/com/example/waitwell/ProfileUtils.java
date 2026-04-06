package com.example.waitwell;

public class ProfileUtils {

    /**
     * Determines if a user can delete or view a profile
     *
     * @param role the role of the current user
     * @param userId the profile being deleted
     */
    public static boolean canDeleteProfile(String role, String userId) {
        if (role == null || userId == null) return false;

        return role.equalsIgnoreCase("admin");
    }
    public static boolean canViewProfiles(String role) {
        return role != null && role.equalsIgnoreCase("admin");
    }
}