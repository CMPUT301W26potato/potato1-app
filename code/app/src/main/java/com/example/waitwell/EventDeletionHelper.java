package com.example.waitwell;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper that deletes an event and all linked data (waitlist entries, notifications,
 * registrations, comments, and stored poster files). Used when organizer/admin removes an event.
 *
 * Addresses: US 02.06.03 - Organizer: View Enrolled Entrants
 *
 * @author Karina Zhang
 * @version 1.0
 */
public final class EventDeletionHelper {

    private static final int MAX_BATCH = 500;

    /**
     * Callback used after event delete finishes.
     *
     * Addresses: US 02.06.03 - Organizer: View Enrolled Entrants
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public interface OnDeleteCompleteListener {
        /**
         * Returns delete result to caller.
         *
         * @param success true if delete flow finished successfully
         * @author Karina Zhang
         */
        void onDeleteComplete(boolean success);
    }

    /**
     * Deletes event doc and related sub-data, then reports success/failure.
     *
     * @param eventId id of event to delete
     * @param listener callback for final result
     * @author Karina Zhang
     */
    public static void deleteEvent(String eventId, OnDeleteCompleteListener listener) {
        if (eventId == null || eventId.trim().isEmpty()) {
            listener.onDeleteComplete(false);
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                listener.onDeleteComplete(false);
                return;
            }
            DocumentSnapshot eventSnap = task.getResult();
            String imageUrl = eventSnap.exists() ? eventSnap.getString("imageUrl") : null;
            String posterUrl = eventSnap.exists() ? eventSnap.getString("posterUrl") : null;

            Task<Void> delWait =
                    deleteQueryDocuments(db.collection("waitlist_entries")
                            .whereEqualTo("eventId", eventId));
            Task<Void> delNotif =
                    deleteQueryDocuments(db.collection("notifications")
                            .whereEqualTo("eventId", eventId));
            Task<Void> delReg =
                    deleteQueryDocuments(db.collection("registrations")
                            .whereEqualTo("eventId", eventId));
            Task<Void> delComments =
                    deleteSubcollection(db.collection("events")
                            .document(eventId)
                            .collection("comments"));

            Tasks.whenAll(delWait, delNotif, delReg, delComments).addOnCompleteListener(allTask -> {
                if (!allTask.isSuccessful()) {
                    listener.onDeleteComplete(false);
                    return;
                }
                deleteStorageBestEffort(imageUrl, posterUrl).addOnCompleteListener(st ->
                        eventRef.delete().addOnCompleteListener(delEv ->
                                listener.onDeleteComplete(delEv.isSuccessful())));
            });
        });
    }

    /**
     * Deletes all documents returned by a query using batched deletes.
     *
     * @param query Firestore query to delete from
     * @return task that completes when all docs are removed
     * @author Karina Zhang
     */
    private static Task<Void> deleteQueryDocuments(Query query) {
        return query.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception ex = task.getException();
                return Tasks.forException(ex != null ? ex : new Exception("query"));
            }
            QuerySnapshot snap = task.getResult();
            if (snap == null) {
                return Tasks.forResult(null);
            }
            return deleteDocumentsInBatches(snap.getDocuments());
        });
    }

    /**
     * Deletes every document inside a subcollection.
     *
     * @param col target subcollection reference
     * @return task that completes when subcollection docs are deleted
     * @author Karina Zhang
     */
    private static Task<Void> deleteSubcollection(CollectionReference col) {
        return col.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception ex = task.getException();
                return Tasks.forException(ex != null ? ex : new Exception("subcollection"));
            }
            QuerySnapshot snap = task.getResult();
            if (snap == null) {
                return Tasks.forResult(null);
            }
            return deleteDocumentsInBatches(snap.getDocuments());
        });
    }

    /**
     * Deletes docs recursively in chunks so we do not exceed batch limits.
     *
     * @param docs docs to delete
     * @return task that completes when all docs are deleted
     * @author Karina Zhang
     */
    private static Task<Void> deleteDocumentsInBatches(List<DocumentSnapshot> docs) {
        if (docs == null || docs.isEmpty()) {
            return Tasks.forResult(null);
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        int n = Math.min(MAX_BATCH, docs.size());
        for (int i = 0; i < n; i++) {
            batch.delete(docs.get(i).getReference());
        }
        List<DocumentSnapshot> remaining = new ArrayList<>(docs.subList(n, docs.size()));
        return batch.commit().continueWithTask(t -> {
            if (!t.isSuccessful()) {
                Exception ex = t.getException();
                return Tasks.forException(ex != null ? ex : new Exception("batch"));
            }
            return deleteDocumentsInBatches(remaining);
        });
    }

    /**
     * Best-effort cleanup for storage objects linked by event image urls.
     *
     * @param imageUrl primary image url
     * @param posterUrl fallback/legacy poster url
     * @return task that completes after delete attempts finish
     * @author Karina Zhang
     */
    private static Task<Void> deleteStorageBestEffort(@Nullable String imageUrl, @Nullable String posterUrl) {
        List<Task<Void>> tasks = new ArrayList<>();
        tryAddStorageDelete(tasks, imageUrl);
        tryAddStorageDelete(tasks, posterUrl);
        if (tasks.isEmpty()) {
            return Tasks.forResult(null);
        }
        return Tasks.whenAllComplete(tasks).continueWith(finished -> null);
    }

    /**
     * Tries to add one storage delete task if URL is valid.
     *
     * @param tasks aggregate task list
     * @param url storage url candidate
     * @author Karina Zhang
     */
    private static void tryAddStorageDelete(List<Task<Void>> tasks, @Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            tasks.add(ref.delete());
        } catch (IllegalArgumentException ignored) {
        }
    }

    private EventDeletionHelper() {}
}
