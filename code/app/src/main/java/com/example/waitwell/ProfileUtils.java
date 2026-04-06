package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for building and extracting profile data from Firestore documents.
 * Provides helper methods to construct profile maps and safely read profile fields.
 * Also contains simple role-based permission checks.
 *
 * @author Sarang Kim and Grace Shin
 */
public class ProfileUtils {

    /**
     * Builds a profile map suitable for saving to Firestore.
     * Includes name, email, phone, and optionally profile image URL.
     *
     * @param name Name of the user
     * @param email Email of the user
     * @param phone Phone number of the user
     * @param profileImageUrl Optional URL of the user's profile image
     * @return A map representing the user's profile fields
     */
    public static Map<String, Object> buildProfileMap(String name, String email, String phone, String profileImageUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone);
        if (profileImageUrl != null) {
            map.put("profileImageUrl", profileImageUrl);
        }
        return map;
    }

    /**
     * Safely retrieves the "name" field from a Firestore document.
     *
     * @param doc Firestore document snapshot
     * @return The name value, or null if not present
     */
    public static String getNameFromDoc(DocumentSnapshot doc) {
        if (doc == null) return null;
        return doc.getString("name");
    }

    /**
     * Safely retrieves the "email" field from a Firestore document.
     *
     * @param doc Firestore document snapshot
     * @return The email value, or null if not present
     */
    public static String getEmailFromDoc(DocumentSnapshot doc) {
        if (doc == null) return null;
        return doc.getString("email");
    }

    /**
     * Safely retrieves the "phone" field from a Firestore document.
     *
     * @param doc Firestore document snapshot
     * @return The phone value, or null if not present
     */
    public static String getPhoneFromDoc(DocumentSnapshot doc) {
        if (doc == null) return null;
        return doc.getString("phone");
    }

    /**
     * Determines if the current user is allowed to delete a profile.
     * Only admins can delete profiles.
     *
     * @param role The role of the user (e.g., "admin")
     * @param userId The ID of the user being checked
     * @return True if the user can delete profiles, false otherwise
     */
    public static boolean canDeleteProfile(String role, String userId) {
        if (role == null || userId == null) return false;
        return role.equalsIgnoreCase("admin");
    }

    /**
     * Determines if the current user can view profiles.
     * Only admins have permission to view all profiles.
     *
     * @param role The role of the user (e.g., "admin")
     * @return True if the user can view profiles, false otherwise
     */
    public static boolean canViewProfiles(String role) {
        return role != null && role.equalsIgnoreCase("admin");
    }
}