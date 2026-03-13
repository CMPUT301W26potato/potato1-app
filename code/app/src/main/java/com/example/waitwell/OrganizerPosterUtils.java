package com.example.waitwell;

/**
 * Small helper to centralize how we decide which poster URL to save
 * for an Organizer event. This is intentionally Android-free so we
 * can unit test the logic without touching real {@code Uri} objects.
 */
public class OrganizerPosterUtils {

    /**
     * Decides which poster URL should be stored for an event.
     *
     * @param newPosterUriString string form of a newly picked image URI, or null if user didn't pick
     * @param existingPosterUrl  previously stored poster URL when editing, or null for new events
     * @return the URL we should save to Firestore, or null if there is none
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

