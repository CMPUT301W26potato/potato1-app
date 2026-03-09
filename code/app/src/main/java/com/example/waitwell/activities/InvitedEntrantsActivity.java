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
 * (Rehaan's addition)
 * Displays the list of entrants who have been invited (chosen) for a specific event.
 * US 02.06.01: As an organizer I want to view a list of all chosen entrants who are invited to apply.
 * Expects an intent extra event_id containing the Firestore document ID of the event.
 * Queries the waitlist_entries collection for entries matching the event with status selected.
 * For each entry, fetches the user's name and email from the users collection.
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
            Toast.makeText(this, "No event specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadInvitedEntrants();
    }


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
                    Toast.makeText(this, "Could not load entrants", Toast.LENGTH_SHORT).show();
                });
    }


    private void onEntriesLoaded(QuerySnapshot snapshot) {
        txtLoading.setVisibility(View.GONE);

        if (snapshot.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtCount.setText("0 invited");
            return;
        }

        int count = snapshot.size();
        txtCount.setText(count + " invited");

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