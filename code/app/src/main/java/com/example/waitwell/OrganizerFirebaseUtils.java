package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Small Organizer-only helper that wraps a bit of Firestore document reading.
 * The idea is to mirror the logic we use in the Organizer UI, but keep it
 * Android-free so it can be tested cleanly with Mockito and plain JUnit.
 */
public class OrganizerFirebaseUtils {

    /**
     * Reads the event title from a Firestore document, falling back to a friendly
     * "Untitled Event" label when the title is missing or blank.
     *
     * @param doc Firestore document for an event (mocked in tests)
     * @return a non-empty title string
     */
    public static String getTitleOrFallback(DocumentSnapshot doc) {
        String title = doc.getString("title");
        if (title == null || title.trim().isEmpty()) {
            return "Untitled Event";
        }
        return title;
    }

    /**
     * Reads the event status from Firestore and falls back to "open" when it is
     * missing or empty. This keeps the rest of the Organizer code from having
     * to handle null checks every time.
     *
     * @param doc Firestore document for an event (mocked in tests)
     * @return a non-null status string
     */
    public static String getStatusOrDefault(DocumentSnapshot doc) {
        String status = doc.getString("status");
        if (status == null || status.trim().isEmpty()) {
            return "open";
        }
        return status;
    }
}

