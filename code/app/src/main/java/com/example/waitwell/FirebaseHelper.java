package com.example.waitwell;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FirebaseHelper.java
 * Singleton helper class that handles all Firestore reads and writes.
 * Collections used: "events" and "waitlist_entries".
 * Document ID format for waitlist_entries is userId_eventId.
 * Javadoc written with help from Claude (claude.ai)
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

    /**
     * Direct access to Firestore.
     */
    public FirebaseFirestore getDb() {
        return db;
    }

    //Event Queries

    /**
     * Get all events, newest first.
     */
    public Task<QuerySnapshot> getAllEvents() {
        return db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Get a single event by its Firestore document ID.
     */
    public Task<DocumentSnapshot> getEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .get();
    }

    //Waitlist Actions

    /**
     * Join a waitlist - Firestore transaction (US 01.01.01).
     * <p>
     * This is a  so two things happen atomically:
     * 1. The user's ID is added to the event's waitlistEntrantIds array
     * 2. A WaitlistEntry document is created for tracking
     *  Troubleshoot with the help from Claude (claude.ai)
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
            if (listener != null)
                listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
        }).addOnFailureListener(e -> {
            if (listener != null)
                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
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
            if (listener != null)
                listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
        }).addOnFailureListener(e -> {
            if (listener != null)
                listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
        });
    }

    /**
     * Get all waitlist entries belonging to a specific user.
     */
    public Task<QuerySnapshot> getUserWaitlistEntries(String userId) {
        return db.collection("waitlist_entries")
                .whereEqualTo("userId", userId)
                .get();
    }

    /**
     * Gets all waitlist entries for a given event filtered by status.
     * Used by US 02.06.01 (view invited) and US 02.06.02 (view cancelled).
     *
     * @param eventId Firestore document ID of the event
     * @param status  the status to filter by e.g. "selected", "cancelled"
     * @return query task with matching waitlist_entries documents
     *
     */
    public Task<QuerySnapshot> getEntriesByEventAndStatus(String eventId, String status) {
        return db.collection("waitlist_entries")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", status)
                .get();
    }

    /**
     * Runs the lottery for an event (US 02.05.02).
     * Grabs all waiting entries, randomly picks sampleSize of them using
     * Lottery.sample(), then batch updates their status to "selected".
     * Used a single loop to build both lists to avoid double-adding userIds.
     * Referenced Firestore batch writes from:
     * https://stackoverflow.com/questions/53335104/firestore-batch-update-in-android
    /**
     * Create a notification for a user.
     *
     * @param userId The device ID of the recipient
     * @param eventId The event ID this notification is about
     * @param eventName The event title for display
     * @param message The notification message
     * @param type The type of notification (CHOSEN, NOT_CHOSEN, etc.)
     * @param listener Callback when complete
     */
    public void createNotification(String userId, String eventId, String eventName,
                                   String message, String type, OnCompleteListener<Void> listener) {
        Notification notification = new Notification(userId, eventId, eventName, message, type);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    if (listener != null) listener.onComplete(Tasks.forResult(null));
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onComplete(Tasks.forException(e));
                });
    }

    /**
     * Create multiple notifications in a batch.
     * More efficient for creating many notifications at once.
     *
     * @param notifications List of notifications to create
     * @param listener Callback when complete
     */
    public void createNotificationsBatch(List<Notification> notifications, OnCompleteListener<Void> listener) {
        if (notifications.isEmpty()) {
            if (listener != null) listener.onComplete(Tasks.forResult(null));
            return;
        }

        WriteBatch batch = db.batch();
        for (Notification notification : notifications) {
            DocumentReference docRef = db.collection("notifications").document();
            batch.set(docRef, notification);
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    if (listener != null) listener.onComplete(Tasks.forResult(null));
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onComplete(Tasks.forException(e));
                });
    }

    /**
     * Get all notifications for a specific user.
     *
     * @param userId The device ID to get notifications for
     * @return Task containing the query results
     */
    public Task<QuerySnapshot> getUserNotifications(String userId) {
        return db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("responded", false)  // Only show unresponded notifications
                .get();
    }

    /**
     * Mark a notification as responded.
     *
     * @param notificationId The notification document ID
     * @param listener Callback when complete
     */
    public void markNotificationResponded(String notificationId, OnCompleteListener<Void> listener) {
        db.collection("notifications")
                .document(notificationId)
                .update("responded", true)
                .addOnSuccessListener(v -> {
                    if (listener != null) listener.onComplete(Tasks.forResult(null));
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onComplete(Tasks.forException(e));
                });
    }

    /** (Rehaan's Addition)
     * US 02.05.02: Executes lottery sampling for an event.
     * Fetches all waiting entries for the event, randomly selects
     *  them using the Lottery engine, and batch-updates
     * their status to selected in Firestore.
     *
     * @param eventId    Firestore document ID of the event
     * @param sampleSize how many entrants to select
     * @param listener   called when done, check task.isSuccessful()
     */
    public void executeLotterySampling(String eventId, int sampleSize, OnCompleteListener<Void> listener) {
        // First, get the event details for the notification
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    String eventName = eventDoc.getString("title");
                    if (eventName == null) eventName = "Event";

                    final String finalEventName = eventName;

                    // Now get all waiting entries
                    db.collection("waitlist_entries")
                            .whereEqualTo("eventId", eventId)
                            .whereEqualTo("status", "waiting")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                List<String> waitingIds = new ArrayList<>();
                                java.util.Map<String, DocumentReference> userIdToRef = new java.util.HashMap<>();

                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    String userId = doc.getString("userId");
                                    if (userId != null) {
                                        waitingIds.add(userId);
                                        userIdToRef.put(userId, doc.getReference());
                                    }
                                }

                                if (waitingIds.isEmpty()) {
                                    if (listener != null) listener.onComplete(Tasks.forResult(null));
                                    return;
                                }

                                // Run lottery
                                List<String> selectedIds = Lottery.sample(waitingIds, sampleSize);
                                Set<String> selectedSet = new HashSet<>(selectedIds);

                                if (selectedIds.isEmpty()) {
                                    if (listener != null) listener.onComplete(Tasks.forResult(null));
                                    return;
                                }

                                // Create batch for updating statuses
                                WriteBatch batch = db.batch();
                                for (String userId : selectedIds) {
                                    DocumentReference ref = userIdToRef.get(userId);
                                    if (ref != null) {
                                        batch.update(ref, "status", "selected");
                                    }
                                }

                                // Create notifications for selected users
                                List<Notification> notifications = new ArrayList<>();
                                for (String userId : selectedIds) {
                                    String message = "Congratulations! You have been selected for " + finalEventName + ". Please accept or decline the invitation.";
                                    Notification notification = new Notification(userId, eventId, finalEventName, message, "CHOSEN");
                                    notifications.add(notification);
                                }

                                // Optionally create NOT_CHOSEN notifications for non-selected users
                                // Uncomment if you want to notify non-selected users
                                /*
                                for (String userId : waitingIds) {
                                    if (!selectedSet.contains(userId)) {
                                        String message = "Unfortunately, you were not selected for " + finalEventName + " in this round. You can choose to re-enter the lottery pool for future draws.";
                                        Notification notification = new Notification(userId, eventId, finalEventName, message, "NOT_CHOSEN");
                                        notifications.add(notification);
                                    }
                                }
                                */

                                // Commit the status updates first
                                batch.commit()
                                        .addOnSuccessListener(v -> {
                                            // Then create notifications
                                            createNotificationsBatch(notifications, task -> {
                                                if (listener != null) listener.onComplete(Tasks.forResult(null));
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            if (listener != null) listener.onComplete(Tasks.forException(e));
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onComplete(Tasks.forException(e));
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onComplete(Tasks.forException(e));
                });
    }

    public Task<Void> deleteUser(String userId) {
        WriteBatch batch = db.batch();

        // delete waitlist entries for this user
        db.collection("waitlist_entries")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    // delete the user document
                    batch.delete(db.collection("users").document(userId));
                    batch.commit();
                });

        return Tasks.forResult(null);
    }

    /**
     * Draws one replacement applicant from the waiting list (US 02.05.03).
     * Called when a previously selected entrant cancels or rejects.
     * Just reuses executeLotterySampling with sampleSize=1.
     *
     * @param eventId  Firestore document ID of the event
     * @param listener called when done, check task.isSuccessful()
     */

    public void drawReplacementApplicant(String eventId, OnCompleteListener<Void> listener) {
        executeLotterySampling(eventId, 1, listener);
    }
    public Task<List<DocumentSnapshot>> getUserRegistrations(String userId) {
        return db.collection("registrations")
                .whereEqualTo("userId", userId)
                .get()
                .continueWith(task -> task.getResult().getDocuments());
    }

}