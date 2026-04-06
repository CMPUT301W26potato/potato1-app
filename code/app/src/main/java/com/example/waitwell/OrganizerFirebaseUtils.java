package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Tiny organizer helper with Android-free Firestore document reads used by tests.
 * It keeps title/status fallback logic in one place for organizer event lists.
 *
 * Addresses: US 02.01.01 - Organizer: Create Public Event and Generate QR, US 02.01.02 - Organizer: Create Private Event (No QR), US 02.04.01 - Organizer: Upload Event Poster, US 02.04.02 - Organizer: Update Event Poster
 *
 * @author Karina Zhang
 * @version 1.0
 */
public class OrganizerFirebaseUtils {

    /**
     * Reads the event title from a Firestore document, falling back to a friendly
     * "Untitled Event" label when the title is missing or blank.
     *
     * @param doc Firestore document for an event (mocked in tests)
     * @return a non-empty title string
     * @author Karina Zhang
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
     * @author Karina Zhang
     */
    public static String getStatusOrDefault(DocumentSnapshot doc) {
        String status = doc.getString("status");
        if (status == null || status.trim().isEmpty()) {
            return "open";
        }
        return status;
    }
}

