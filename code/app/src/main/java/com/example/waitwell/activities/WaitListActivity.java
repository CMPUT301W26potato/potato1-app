package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
 * "Your WaitList" screen – shows every event the user has joined.
 *
 * US 01.01.01 – see events you joined
 * US 01.01.02 – leave a waitlist via "Quit Waiting List"
 *
 * Each entry shows its status: Waiting / Selected / Rejected / Confirmed.
 * Tapping a row navigates to that event's detail screen.
 * The "Quit Waiting List" button lets the user pick which event to leave.
 */
public class WaitListActivity extends AppCompatActivity {

    private static final String TAG = "WaitListActivity";

    private LinearLayout entriesContainer;
    private LinearLayout emptyState;
    private ScrollView scrollEntries;
    private View btnQuit;
    private String deviceId;

    private List<DocumentSnapshot> entryDocs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist);

        deviceId = DeviceUtils.getDeviceId(this);

        entriesContainer = findViewById(R.id.entriesContainer);
        emptyState = findViewById(R.id.emptyState);
        scrollEntries = findViewById(R.id.scrollEntries);
        btnQuit = findViewById(R.id.btnQuit);

        btnQuit.setOnClickListener(v -> showQuitDialog());
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEntries();
    }

    private void loadEntries() {
        FirebaseHelper.getInstance().getUserWaitlistEntries(deviceId)
                .addOnSuccessListener(snap -> {
                    entryDocs = snap.getDocuments();
                    renderEntries();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load waitlist", e);
                    Toast.makeText(this, "Could not load waitlist",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void renderEntries() {
        entriesContainer.removeAllViews();
        if (entryDocs.isEmpty()) {
            scrollEntries.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            btnQuit.setVisibility(View.GONE);
            return;
        }

        scrollEntries.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        btnQuit.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < entryDocs.size(); i++) {
            DocumentSnapshot doc = entryDocs.get(i);

            View row = inflater.inflate(R.layout.item_waitlist_entry, entriesContainer, false);

            String title = doc.getString("eventTitle");
            String status = doc.getString("status");
            String eventId = doc.getString("eventId");

            if (title == null) {
                title = getString(R.string.waitlist_unknown_event);
            }
            if (status == null) {
                status = getString(R.string.firestore_waitlist_status_waiting);
            }

            ((TextView) row.findViewById(R.id.txtNumber)).setText((i + 1) + ".");
            ((TextView) row.findViewById(R.id.txtEntryTitle)).setText(title);

            // Status badge with colour
            TextView badge = row.findViewById(R.id.txtEntryStatus);
            applyStatusStyle(badge, status);

            final String eid = eventId;
            final String entryTitle = title;
            final String entryStatus = status;
            row.setOnClickListener(v -> {
                if (eid == null) {
                    return;
                }
                String selectedStatus = getString(R.string.firestore_waitlist_status_selected);
                if (selectedStatus.equals(entryStatus)) {
                    FirebaseHelper.getInstance().getEvent(eid)
                            .addOnSuccessListener(eventDoc -> {
                                if (eventDoc == null || !eventDoc.exists()) {
                                    Toast.makeText(this, R.string.invitation_error_not_found,
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Intent invitation = new Intent(this, InvitationResponseActivity.class);
                                invitation.putExtra(InvitationResponseActivity.EXTRA_EVENT_ID, eid);
                                invitation.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, entryTitle);
                                InvitationResponseActivity.putEventFieldsFromSnapshot(invitation, eventDoc, this);
                                invitation.putExtra(InvitationResponseActivity.EXTRA_MESSAGE,
                                        getString(R.string.waitlist_chosen_notification_message, entryTitle));
                                startActivity(invitation);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, R.string.invitation_error_load_details,
                                            Toast.LENGTH_SHORT).show());
                    return;
                }
                Intent intent = new Intent(this, EventDetailActivity.class);
                intent.putExtra("event_id", eid);
                startActivity(intent);
            });
            entriesContainer.addView(row);
        }
    }

    /**
     * Applies the correct text, colour, and background to a status badge
     * based on the entry's status string from Firestore.
     */
    private void applyStatusStyle(TextView badge, String status) {
        String waiting = getString(R.string.firestore_waitlist_status_waiting);
        String selected = getString(R.string.firestore_waitlist_status_selected);
        String confirmed = getString(R.string.firestore_waitlist_status_confirmed);
        String rejected = getString(R.string.firestore_waitlist_status_rejected);
        String cancelled = getString(R.string.firestore_waitlist_status_cancelled);
        String s = status != null ? status : waiting;

        if (waiting.equals(s) || getString(R.string.waitlist_entry_status_pending_alias).equals(s)) {
            badge.setText(R.string.waitlist_status_display_waiting);
            badge.setBackgroundResource(R.drawable.bg_status_waiting);
            badge.setTextColor(getColor(R.color.status_waiting_text));
        } else if (selected.equals(s)) {
            badge.setText(R.string.waitlist_status_display_selected);
            badge.setBackgroundResource(R.drawable.bg_status_selected_primary);
            badge.setTextColor(getColor(R.color.text_white));
        } else if (confirmed.equals(s)) {
            badge.setText(R.string.waitlist_status_display_confirmed);
            badge.setBackgroundResource(R.drawable.bg_status_selected_primary);
            badge.setTextColor(getColor(R.color.text_white));
        } else if (rejected.equals(s)) {
            badge.setText(R.string.waitlist_status_label_declined);
            badge.setBackgroundResource(R.drawable.bg_status_closed);
            badge.setTextColor(getColor(R.color.status_closed_text));
        } else if (cancelled.equals(s)) {
            badge.setText(R.string.waitlist_status_display_cancelled);
            badge.setBackgroundResource(R.drawable.bg_status_closed);
            badge.setTextColor(getColor(R.color.status_closed_text));
        } else {
            badge.setText(R.string.waitlist_status_display_waiting);
            badge.setBackgroundResource(R.drawable.bg_status_waiting);
            badge.setTextColor(getColor(R.color.status_waiting_text));
        }
    }

    //Quit waitlist (US 01.01.02)
    /**
     * Shows a dialog listing only "waiting" entries — those are the
     * ones the user can quit. Selected/confirmed have a different flow.
     */
    private void showQuitDialog() {
        List<DocumentSnapshot> quittable = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (DocumentSnapshot doc : entryDocs) {
            if ("waiting".equals(doc.getString("status"))) {
                quittable.add(doc);
                String t = doc.getString("eventTitle");
                names.add(t != null ? t : getString(R.string.waitlist_unknown_event));
            }
        }

        if (quittable.isEmpty()) {
            Toast.makeText(this, "No active waitlists to leave", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Quit Waiting List")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    DocumentSnapshot chosen = quittable.get(which);
                    confirmQuit(chosen);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmQuit(DocumentSnapshot entry) {
        String title = entry.getString("eventTitle");
        String eid   = entry.getString("eventId");

        new AlertDialog.Builder(this)
                .setTitle("Leave " + title + "?")
                .setMessage("Are you sure you want to leave this waitlist?")
                .setPositiveButton("Leave", (d, w) -> {
                    FirebaseHelper.getInstance().leaveWaitlist(deviceId, eid,
                            task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Left " + title,
                                            Toast.LENGTH_SHORT).show();
                                    loadEntries(); // refresh
                                } else {
                                    Toast.makeText(this, "Failed to leave",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        // Highlight the waitlist tab (middle)
        nav.setSelectedItemId(R.id.nav_waitlist);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_waitlist) return true; // already here
            if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, EntrantNotificationScreen.class));
                return true;
            }
            return false;
        });
    }
}
