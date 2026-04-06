package com.example.waitwell;

/**
 * Small helper that picks which poster URL should be saved for an organizer event.
 * This keeps upload/update poster decision logic simple and testable.
 *
 * Addresses: US 02.04.01 - Organizer: Upload Event Poster, US 02.04.02 - Organizer: Update Event Poster
 *
 * @author Karina Zhang
 * @version 1.0
 */
public class OrganizerPosterUtils {

    /**
     * Decides which poster URL should be stored for an event.
     *
     * @param newPosterUriString string form of a newly picked image URI, or null if user didn't pick
     * @param existingPosterUrl  previously stored poster URL when editing, or null for new events
     * @return the URL we should save to Firestore, or null if there is none
     * @author Karina Zhang
     */
    public static String resolvePosterUrl(String newPosterUriString, String existingPosterUrl) {
        if (newPosterUriString != null) {
            // For new selections we always prefer the fresh URI string.
            return newPosterUriString;
        }
        // If we are editing and there was already a poster, keep it.
        return existingPosterUrl;
    }
}

