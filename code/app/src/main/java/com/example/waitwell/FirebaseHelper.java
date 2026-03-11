package com.example.waitwell;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Firestore helper.
 *
 * Firestore collections: "events" -> Event documents
 * "waitlist_entries" -> one doc per user+event combo, id = "userId_eventId"
 */
public class FirebaseHelper {
    private static FirebaseHelper instance;
    private final FirebaseFirestore db;

    private FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    /** Direct access to Firestore. */
    public FirebaseFirestore getDb() {
        return db;
    }

    //Event Queries
    /** Get all events, newest first. */
    public Task<QuerySnapshot> getAllEvents() {
        return db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /** Get a single event by its Firestore document ID. */
    public Task<DocumentSnapshot> getEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .get();
    }

    //Waitlist Actions
    /**
     * Join a waitlist - Firestore transaction (US 01.01.01).
     *
     * This is a  so two things happen atomically:
     *   1. The user's ID is added to the event's waitlistEntrantIds array
     *   2. A WaitlistEntry document is created for tracking
     */
    public void joinWaitlist(String userId, String eventId, String eventTitle, OnCompleteListener<Void> listener) {
        String entryId = userId + "_" + eventId;
        db.runTransaction(transaction -> {
            DocumentReference eventRef = db.collection("events").document(eventId);
            DocumentReference entryRef = db.collection("waitlist_entries").document(entryId);
            //Add userId to the event's array of waitlist entrant IDs
            transaction.update(eventRef,
                    "waitlistEntrantIds", FieldValue.arrayUnion(userId));

            //Create the waitlist entry document
            java.util.Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("userId", userId);
            entry.put("eventId", eventId);
            entry.put("eventTitle", eventTitle);
            entry.put("status", "waiting");
            entry.put("joinedAt", FieldValue.serverTimestamp());
            transaction.set(entryRef, entry);

            return null;
        }).addOnSuccessListener(result -> {
            if (listener != null) listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
        });
    }

    /**
     * Leave a waitlist (US 01.01.02).
     * Reverse of joinWaitlist: removes the user from the array and
     * deletes the entry document, both inside a transaction.
     */
    public void leaveWaitlist(String userId, String eventId,
                              OnCompleteListener<Void> listener) {

        String entryId = userId + "_" + eventId;

        db.runTransaction(transaction -> {
            DocumentReference eventRef = db.collection("events").document(eventId);
            DocumentReference entryRef = db.collection("waitlist_entries").document(entryId);

            transaction.update(eventRef,
                    "waitlistEntrantIds", FieldValue.arrayRemove(userId));
            transaction.delete(entryRef);

            return null;
        }).addOnSuccessListener(result -> {
            if (listener != null) listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
        });
    }

    /** Get all waitlist entries belonging to a specific user. */
    public Task<QuerySnapshot> getUserWaitlistEntries(String userId) {
        return db.collection("waitlist_entries")
                .whereEqualTo("userId", userId)
                .get();
    }
    /** US 02.06.01 (Rehaan) :  Get all waitlist entries for a given event filtered by status. */
    public Task<QuerySnapshot> getEntriesByEventAndStatus(String eventId, String status) {
        return db.collection("waitlist_entries")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", status)
                .get();
    }
}
