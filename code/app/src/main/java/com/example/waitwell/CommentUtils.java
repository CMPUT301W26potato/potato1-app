package com.example.waitwell;

import com.google.firebase.Timestamp;
public class CommentUtils {

    /**
     * Determines if a user can delete a comment
     *
     * @param role            the role of the current user (admin, organizer, entrant)
     * @param commentEventOwnerId the userId of the organizer who created the event the comment belongs to
     * @param currentUserId   the userId of the current user
     * @return true if the user can delete the comment
     */
    public static boolean canDelete(String role, String commentEventOwnerId, String currentUserId) {
        if (role == null || currentUserId == null) return false;

        // Admin can delete any comment
        if (role.equalsIgnoreCase("admin")) {
            return true;
        }

        // Organizer can delete ONLY comments under events they created
        if (role.equalsIgnoreCase("organizer")) {
            return commentEventOwnerId != null && commentEventOwnerId.equals(currentUserId);
        }

        // Entrants cannot delete comments
        return false;
    }

    public static boolean canEntrantComment(String role, Timestamp registrationClose) {
        if (role == null || !role.equalsIgnoreCase("entrant") || registrationClose == null) {
            return false;
        }
        return registrationClose.toDate().after(new java.util.Date());
    }
}