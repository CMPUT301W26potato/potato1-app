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
 * InvitedEntrantsActivity.java
 * Shows the organizer a list of entrants who were selected in the lottery (US 02.06.01).
 * Gets the event_id from the intent, queries waitlist_entries where status = "selected",
 * then loads each user's name and email from the users collection.
 * Javadoc written with help from Claude (claude.ai)
 */
public class InvitedEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "InvitedEntrants";
    private LinearLayout entrantListContainer;
    private TextView txtCount;
    private TextView txtLoading;
    private TextView txtEmpty;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invited_entrants);

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

        loadInvitedEntrants();
    }

    /**
     * Queries Firestore for all waitlist entries with status "selected" for this event.
     * Shows a loading indicator while the query runs.
     */

    private void loadInvitedEntrants() {
        txtLoading.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);
        entrantListContainer.removeAllViews();

        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, "selected")
                .addOnSuccessListener(this::onEntriesLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load invited entrants", e);
                    txtLoading.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Called when the waitlist_entries query finishes.
     * For each entry, looks up the user doc to get their name and email,
     * then adds a row to the list.
     * Learned about chaining Firestore reads from:
     * https://stackoverflow.com/questions/46589757/firestore-get-multiple-documents
     *
     * @param snapshot the query results from Firestore
     */
    private void onEntriesLoaded(QuerySnapshot snapshot) {
        txtLoading.setVisibility(View.GONE);

        if (snapshot.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtCount.setText(getString(R.string.invited_count, 0));
            return;
        }

        int count = snapshot.size();
        txtCount.setText(getString(R.string.invited_count, count));

        FirebaseFirestore db = FirebaseHelper.getInstance().getDb();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
            String userId = entryDoc.getString("userId");
            if (userId == null) continue;

            db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        String name = userDoc.getString("name");
                        String email = userDoc.getString("email");
                        if (name == null) name = "Unknown";
                        if (email == null) email = "";

                        View row = inflater.inflate(R.layout.item_entrant_row, entrantListContainer, false);
                        ((TextView) row.findViewById(R.id.txtEntrantName)).setText(name);
                        ((TextView) row.findViewById(R.id.txtEntrantEmail)).setText(email);
                        entrantListContainer.addView(row);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to load user: " + userId, e));
        }
    }
}