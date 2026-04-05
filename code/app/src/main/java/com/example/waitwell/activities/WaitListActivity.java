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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.EntrantNotificationOptions;
import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.example.waitwell.WaitlistFirestoreStatus;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
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

    // REHAAN'S ADDITION — US 02.09.01 Part 2 — co-organizer events the entrant accepted
    private List<DocumentSnapshot> coOrganizerEventDocs = new ArrayList<>();
    // END REHAAN'S ADDITION

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist);

        deviceId = DeviceUtils.getDeviceId(this);

        entriesContainer = findViewById(R.id.entriesContainer);
        emptyState = findViewById(R.id.emptyState);
        scrollEntries = findViewById(R.id.scrollEntries);
        btnQuit = findViewById(R.id.btnQuit);

        setupDrawer();
        btnQuit.setOnClickListener(v -> showQuitDialog());
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEntries();
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        if (nav != null) nav.setSelectedItemId(R.id.nav_waitlist);
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
            // Don't show emptyState yet — co-organizer events may still populate the list.
            // loadCoOrganizerEvents() will decide final empty state.
            scrollEntries.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            btnQuit.setVisibility(View.GONE);
            // REHAAN'S ADDITION — always check for co-organizer events too
            loadCoOrganizerEvents();
            // END REHAAN'S ADDITION
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
        // REHAAN'S ADDITION — append co-organizer events after waitlist entries
        loadCoOrganizerEvents();
        // END REHAAN'S ADDITION
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
            badge.setText(R.string.waitlist_status_display_not_selected);
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

    // REHAAN'S ADDITION — US 02.09.01 Part 2 — co-organizer events section

    /**
     * Queries events where the current device's userId is in coOrganizerIds
     * (i.e. they accepted a co-organizer invite) and renders them below the
     * waitlist entries with a Manage button.
     */
    private void loadCoOrganizerEvents() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("events")
                .whereArrayContains("coOrganizerIds", deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    coOrganizerEventDocs = snapshot.getDocuments();
                    renderCoOrganizerEvents();
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to load co-organizer events", e));
    }

    /**
     * Appends a "Events I Co-Organize" section to entriesContainer.
     * If co-organizer events exist, the ScrollView is forced visible even if
     * the entrant has no waitlist entries.
     */
    private void renderCoOrganizerEvents() {
        if (coOrganizerEventDocs.isEmpty()) {
            // If also no waitlist entries, emptyState is already visible — nothing to do.
            return;
        }

        // Co-organizer events exist: make sure the scroll container is shown.
        scrollEntries.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        // Section header
        TextView header = new TextView(this);
        header.setText(getString(R.string.co_organizer_event_section_header));
        header.setTextColor(getColor(R.color.text_secondary));
        header.setTextSize(13f);
        android.view.ViewGroup.MarginLayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        ((android.widget.LinearLayout.LayoutParams) lp).setMargins(0, 16, 0, 8);
        header.setLayoutParams(lp);
        entriesContainer.addView(header);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocumentSnapshot doc : coOrganizerEventDocs) {
            String title   = doc.getString("title");
            String eventId = doc.getId();
            if (title == null) title = getString(R.string.waitlist_unknown_event);

            View row = inflater.inflate(R.layout.item_co_organizer_event_row, entriesContainer, false);
            ((TextView) row.findViewById(R.id.txtCoOrganizerEventTitle)).setText(title);

            final String eid        = eventId;
            row.findViewById(R.id.btnManageCoOrganizerEvent).setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerEntryActivity.class);
                intent.putExtra(OrganizerEntryActivity.EXTRA_OPEN_EVENT_ID, eid);
                startActivity(intent);
            });

            entriesContainer.addView(row);
        }
    }
    // END REHAAN'S ADDITION

    //Quit waitlist (US 01.01.02)
    /**
     * Shows a dialog listing only "waiting" entries — those are the
     * ones the user can quit. Selected/confirmed have a different flow.
     */
    private void showQuitDialog() {
        List<DocumentSnapshot> quittable = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (DocumentSnapshot doc : entryDocs) {
            if (WaitlistFirestoreStatus.WAITING.equals(doc.getString("status"))) {
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

    private void setupDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.btnHamburger).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_delete_profile) {
                showDeleteProfileDialog();
            } else if (id == R.id.nav_notification_options) {
                startActivity(new Intent(this, EntrantNotificationOptions.class));
            } else if (id == R.id.nav_lottery_selection_criteria) {
                startActivity(new Intent(this, EntrantLotteryCriteria.class));
            } else if (id == R.id.nav_logout) {
                startActivity(new Intent(this, RegisterActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        // Highlight the waitlist tab (middle)
        nav.setSelectedItemId(R.id.nav_waitlist);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_waitlist) return true; // already here
            if (id == R.id.nav_notifications) {
                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void showDeleteProfileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserProfile() {
        String userId = DeviceUtils.getDeviceId(this);
        FirebaseHelper.getInstance().deleteUser(userId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("waitwell_prefs", MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences("WaitWellPrefs", MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(this, RegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }
}
