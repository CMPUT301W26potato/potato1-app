package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a user's registration history for all events they've interacted with.
 * <p>
 * This activity fetches the waitlist entries for the current device from Firestore,
 * filters out entries that aren't relevant (keeps only "selected", "confirmed", or "rejected"),
 * and displays them in list. Each item shows the event title and a status badge.
 * Clicking on an entry takes the user to the event's detail page.
 * <p>
 * The activity also includes bottom navigation so users can quickly switch between Home,
 * Waitlist, and Notifications. There's also a back button for easy navigation.
 *
 * @author Sarang
 */

public class RegistrationHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private TextView tvEmpty;
    private String deviceId;

    /**
     * Sets up the UI, initializes views, handles the back button and bottom navigation,
     * retrieves the device ID, and loads the user's registration history.
     *
     * @param savedInstanceState Standard Android saved state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_history); // your XML with historyContainer

        historyContainer = findViewById(R.id.historyContainer);
        tvEmpty = findViewById(R.id.tvEmpty);

        // back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // bottom navigation
        setupBottomNav();

        deviceId = DeviceUtils.getDeviceId(this); // initializing deviceId here

        loadHistory();
    }
    /**
     * Configures the bottom navigation bar. Tapping an icon will navigate to
     * home, waitlist, or notifications.
     */
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            }
            if (id == R.id.nav_waitlist) {
                startActivity(new Intent(this, WaitListActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            }
            if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, EntrantNotificationScreen.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            }
            return false;
        });
    }

    /**
     * Loads all waitlist entries for this device from Firestore. Filters out
     * irrelevant entries (keeps only "selected", "confirmed", or "rejected"),
     * and passes them to {@link #renderHistory} for display.
     */
    private void loadHistory() {
        FirebaseHelper.getInstance().getUserWaitlistEntries(deviceId)
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> allEntries = snap.getDocuments();

                    List<DocumentSnapshot> filtered = new ArrayList<>();
                    for (DocumentSnapshot doc : allEntries) {
                        String status = doc.getString("status");
                        if (status != null && (
                                status.equalsIgnoreCase("selected") ||
                                        status.equalsIgnoreCase("confirmed") ||
                                        status.equalsIgnoreCase("rejected"))) {
                            filtered.add(doc);
                        }
                    }

                    renderHistory(filtered);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load history", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays the filtered registration history in the layout. Each item shows
     * the event number, title, and a colored badge for its status.
     * <p>
     * Clicking an entry opens {@link EventDetailActivity} for that specific event.
     *
     * @param filteredEntries List of filtered Firestore documents to display.
     */
    private void renderHistory(List<DocumentSnapshot> filteredEntries) {
        historyContainer.removeAllViews();

        if (filteredEntries.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        int displayIndex = 0;
        for (DocumentSnapshot doc : filteredEntries) {
            String title = doc.getString("eventTitle");
            String status = doc.getString("status");

            String displayStatus;
            if ("selected".equalsIgnoreCase(status) || "confirmed".equalsIgnoreCase(status)) {
                displayStatus = "Selected";
            } else if ("rejected".equalsIgnoreCase(status)) {
                displayStatus = "Not Selected";
            } else {
                continue;
            }

            // Entry doc ID is "userId_eventId" — strip the userId prefix to get the event ID.
            String docId = doc.getId();
            String eventId = docId.contains("_") ? docId.substring(docId.indexOf('_') + 1) : null;

            displayIndex++;
            View row = inflater.inflate(R.layout.item_waitlist_entry, historyContainer, false);

            ((TextView) row.findViewById(R.id.txtNumber)).setText(displayIndex + ".");
            ((TextView) row.findViewById(R.id.txtEntryTitle)).setText(title != null ? title : "Unknown Event");

            TextView badge = row.findViewById(R.id.txtEntryStatus);
            badge.setText(displayStatus);
            if ("Selected".equals(displayStatus)) {
                badge.setBackgroundResource(R.drawable.bg_status_selected_primary);
                badge.setTextColor(getColor(R.color.text_white));
            } else {
                badge.setBackgroundResource(R.drawable.bg_status_closed);
                badge.setTextColor(getColor(R.color.status_closed_text));
            }

            if (eventId != null) {
                String finalEventId = eventId;
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, EventDetailActivity.class);
                    intent.putExtra("event_id", finalEventId);
                    startActivity(intent);
                });
            }

            historyContainer.addView(row);
        }
    }
}