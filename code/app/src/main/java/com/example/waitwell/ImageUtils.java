package com.example.waitwell;

public class ImageUtils {

    /**
     * Determines if a user can delete or view an image
     */
    public static boolean canDeleteImage(String role, String imageUrl) {
        if (role == null || imageUrl == null) return false;

        return role.equalsIgnoreCase("admin");
    }
    public static boolean canViewImages(String role) {
        return role != null && role.equalsIgnoreCase("admin");
    }
}