package com.example.waitwell;

/**
 * Canonical {@code waitlist_entries.status} values stored in Firestore.
 * Keep in sync with {@code strings.xml} {@code firestore_waitlist_status_*} entries.
 */
public final class WaitlistFirestoreStatus {

    private WaitlistFirestoreStatus() {
    }

    public static final String WAITING = "waiting";
    public static final String PENDING = "pending";
    public static final String SELECTED = "selected";
    public static final String CONFIRMED = "confirmed";
    public static final String CANCELLED = "cancelled";
    public static final String REJECTED = "rejected";
}
