package com.example.waitwell.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * CancelledEntrantsActivity.java
 * Shows the organizer a list of entrants who cancelled or were cancelled (US 02.06.02).
 * Gets the event_id from the intent, queries waitlist_entries where status = "cancelled",
 * then loads each user's name and email from the users collection.
 * Javadoc written with help from Claude (claude.ai)
 */
public class CancelledEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "CancelledEntrants";
    private LinearLayout entrantListContainer;
    private TextView txtCount;
    private TextView txtLoading;
    private TextView txtEmpty;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelled_entrants);

        entrantListContainer = findViewById(R.id.entrantListContainer);
        txtCount = findViewById(R.id.txtCount);
        txtLoading = findViewById(R.id.txtLoading);
        txtEmpty = findViewById(R.id.txtEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCancelledEntrants();
    }

    /**
     * Queries Firestore for all waitlist entries with status "cancelled" for this event.
     * Shows a loading indicator while the query runs.
     */
    private void loadCancelledEntrants() {
        txtLoading.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);
        entrantListContainer.removeAllViews();

        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, "cancelled")
                .addOnSuccessListener(this::onEntriesLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load cancelled entrants", e);
                    txtLoading.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show();
                });
    }
    /**
     * Called when the waitlist_entries query finishes.
     * For each entry, looks up the user doc to get their name and email,
     * then adds a row with a red cancelled badge to the list.
     * @param snapshot the query results from Firestore
     */
    private void onEntriesLoaded(QuerySnapshot snapshot) {
        txtLoading.setVisibility(View.GONE);

        if (snapshot.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtCount.setText(getString(R.string.cancelled_count, 0));
            return;
        }

        int count = snapshot.size();
        txtCount.setText(getString(R.string.cancelled_count, count));

        FirebaseFirestore db = FirebaseHelper.getInstance().getDb();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
            String userId = entryDoc.getString("userId");
            if (userId == null) continue;

            db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        String name = userDoc.getString("name");
                        String email = userDoc.getString("email");
                        if (name == null) name = getString(R.string.unknown_user);
                        if (email == null) email = "";

                        View row = inflater.inflate(R.layout.item_entrant_row, entrantListContainer, false);
                        ((TextView) row.findViewById(R.id.txtEntrantName)).setText(name);
                        ((TextView) row.findViewById(R.id.txtEntrantEmail)).setText(email);

                        TextView statusBadge = row.findViewById(R.id.txtEntrantStatus);
                        statusBadge.setText(R.string.status_cancelled);
                        statusBadge.setTextColor(getColor(R.color.status_closed_text));
                        statusBadge.setBackgroundResource(R.drawable.bg_status_closed);

                        entrantListContainer.addView(row);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to load user: " + userId, e));
        }
    }
}