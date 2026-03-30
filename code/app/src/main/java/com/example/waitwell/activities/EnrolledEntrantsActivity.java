package com.example.waitwell.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * EnrolledEntrantsActivity.java
 * Shows the organizer a list of confirmed (enrolled) entrants for an event (US 02.06.04).
 * Gets event_id from the intent, queries waitlist_entries where status = "confirmed",
 * then loads each user's name and email from the users collection.
 * The organizer can cancel any entrant who did not show up by tapping Cancel.
 * Javadoc written with help from Claude (claude.ai)
 */
public class EnrolledEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "EnrolledEntrants";

    // Intent extra key — matches the pattern used in InvitedEntrantsActivity
    public static final String EXTRA_EVENT_ID = "event_id";

    private LinearLayout entrantListContainer;
    private TextView txtCount;
    private TextView txtLoading;
    private TextView txtEmpty;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrolled_entrants);

        entrantListContainer = findViewById(R.id.entrantListContainer);
        txtCount = findViewById(R.id.txtCount);
        txtLoading = findViewById(R.id.txtLoading);
        txtEmpty = findViewById(R.id.txtEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEnrolledEntrants();
    }

    /**
     * Queries Firestore for all waitlist entries with status "confirmed" for this event.
     * Shows a loading indicator while the query runs.
     */
    private void loadEnrolledEntrants() {
        txtLoading.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);
        entrantListContainer.removeAllViews();

        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, "confirmed")
                .addOnSuccessListener(this::onEntriesLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load enrolled entrants", e);
                    txtLoading.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Called when the waitlist_entries query finishes.
     * For each confirmed entry, fetches the user's name and email, then adds
     * a row with a Cancel button so the organizer can mark no-shows.
     *
     * @param snapshot query results from Firestore
     */
    private void onEntriesLoaded(QuerySnapshot snapshot) {
        txtLoading.setVisibility(View.GONE);

        if (snapshot.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtCount.setText(getString(R.string.enrolled_count, 0));
            return;
        }

        int count = snapshot.size();
        txtCount.setText(getString(R.string.enrolled_count, count));

        FirebaseFirestore db = FirebaseHelper.getInstance().getDb();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
            String userId = entryDoc.getString("userId");
            String entryDocId = entryDoc.getId();
            if (userId == null) continue;

            db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        String name = userDoc.getString("name");
                        String email = userDoc.getString("email");
                        if (name == null) name = getString(R.string.unknown_user);
                        if (email == null) email = "";

                        View row = inflater.inflate(R.layout.item_enrolled_entrant_row, entrantListContainer, false);
                        ((TextView) row.findViewById(R.id.txtEntrantName)).setText(name);
                        ((TextView) row.findViewById(R.id.txtEntrantEmail)).setText(email);

                        // store final copies for use inside the lambda
                        String finalName = name;
                        Button btnCancel = row.findViewById(R.id.btnCancelEntrant);
                        btnCancel.setOnClickListener(v ->
                                showCancelConfirmDialog(entryDocId, finalName, row));

                        entrantListContainer.addView(row);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to load user: " + userId, e));
        }
    }

    /**
     * Shows a confirmation dialog before cancelling an entrant (US 02.06.04).
     * Organizer confirms before the status write goes through.
     *
     * @param entryDocId Firestore document ID of the waitlist_entries doc
     * @param name       display name of the entrant shown in the dialog
     * @param row        the list row view to remove on success
     */
    private void showCancelConfirmDialog(String entryDocId, String name, View row) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.enrolled_cancel_dialog_title))
                .setMessage(getString(R.string.enrolled_cancel_dialog_message, name))
                .setPositiveButton(getString(R.string.enrolled_cancel_dialog_confirm), (dialog, which) ->
                        cancelEntrant(entryDocId, row))
                .setNegativeButton(getString(R.string.lottery_dialog_cancel), null)
                .show();
    }

    /**
     * Updates the waitlist_entries document status from "confirmed" to "cancelled" (US 02.06.04).
     * On success the row is removed from the list and the count is updated.
     *
     * @param entryDocId Firestore document ID of the waitlist_entries doc to update
     * @param row        the list row view to remove on success
     */
    private void cancelEntrant(String entryDocId, View row) {
        FirebaseHelper.getInstance().cancelEnrolledEntrant(entryDocId, task -> {
            if (task.isSuccessful()) {
                entrantListContainer.removeView(row);
                // update the count label to reflect the removal
                int remaining = entrantListContainer.getChildCount();
                txtCount.setText(getString(R.string.enrolled_count, remaining));
                if (remaining == 0) {
                    txtEmpty.setVisibility(View.VISIBLE);
                }
                Toast.makeText(this, R.string.enrolled_cancel_success, Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to cancel entrant " + entryDocId, task.getException());
                Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}