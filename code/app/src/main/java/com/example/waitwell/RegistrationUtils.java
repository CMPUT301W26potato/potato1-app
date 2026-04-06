package com.example.waitwell;

public class RegistrationUtils {

    /**
     * Converts raw Firestore status to display status text.
     *
     * @param firestoreStatus the status from Firestore ("selected", "confirmed", "rejected", etc.)
     * @param isCompleted     whether the event is completed
     * @return Display string for badge
     * @author Sarang Kim
     */
    public static String getDisplayStatus(String firestoreStatus, boolean isCompleted) {
        if (isCompleted) {
            return "Completed";
        }
        if ("selected".equalsIgnoreCase(firestoreStatus) || "confirmed".equalsIgnoreCase(firestoreStatus)) {
            return "Selected";
        } else if ("rejected".equalsIgnoreCase(firestoreStatus)) {
            return "Not Selected";
        } else {
            return "Unknown";
        }
    }
}