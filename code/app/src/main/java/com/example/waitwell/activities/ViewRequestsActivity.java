package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.ProfilePreviewHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.example.waitwell.WaitlistFirestoreStatus;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Organizer waitlist requests screen where each waiting entrant can be accepted or declined,
 * plus bulk notify and lottery actions.
 *
 * Addresses: US 02.02.01 - Organizer: View Waitlist Entrants, US 02.05.01 - Organizer: Notify Chosen Entrants, US 02.07.01 - Organizer: Notify All on Waitlist
 *
 * @author Karina Zhang
 * @version 1.0
 * @see WaitlistEntrantAdapter
 */
public class ViewRequestsActivity extends OrganizerBaseActivity implements WaitlistEntrantAdapter.Listener {
    /*
     * Asked Gemini how to structure notification documents in Firestore so
     * the entrant side can read them and figure out what type they are. It
     * helped me think through what fields to include and how to trigger the
     * write at the right point in the flow.
     * getting the concept down.
     *
     * Sites I looked at:
     *
     * Firestore - writing documents to a collection:
     * https://firebase.google.com/docs/firestore/manage-data/add-data
     *
     * Firestore real-time listeners - snapshot listeners for live updates:
     * https://firebase.google.com/docs/firestore/query-data/listen
     */

    private String eventId;
    private String eventTitle;

    private WaitlistEntrantAdapter adapter;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();

    /**
     * Sets up the waitlist requests screen, wires buttons, and kicks off the first load.
     *
     * @param savedInstanceState activity restore bundle, can be null
     * @author Karina Zhang
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        eventId = getIntent().getStringExtra("event_id");
        eventTitle = getIntent().getStringExtra("event_title");
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (eventTitle == null) {
            eventTitle = "";
        }

        setupOrganizerDrawer();

        BottomNavigationView nav = findViewById(R.id.organizerBottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_organizer_bottom_back) {
                finish();
                return true;
            }
            if (id == R.id.nav_organizer_bottom_home) {
                Intent intent = new Intent(this, OrganizerEntryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
            return false;
        });

        EditText editSearch = findViewById(R.id.editSearch);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setFilterQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        RecyclerView recycler = findViewById(R.id.recyclerWaitlist);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitlistEntrantAdapter(this);
        recycler.setAdapter(adapter);

        Button btnViewMap = findViewById(R.id.btnViewMap);
        Button btnNotifyAll = findViewById(R.id.btnNotifyAll);
        Button btnSample = findViewById(R.id.btnSampleAttendees);
        // REHAAN'S ADDITION â€” US 02.02.02
        btnViewMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, WaitlistMapActivity.class);
            mapIntent.putExtra(WaitlistMapActivity.EXTRA_EVENT_ID, eventId);
            startActivity(mapIntent);
        });
        // END REHAAN'S ADDITION
        btnNotifyAll.setOnClickListener(v -> notifyAllWaitingEntrants());
        btnSample.setOnClickListener(v -> showLotteryDialog());

        loadWaitingEntrants();
    }

    /**
     * Loads waiting entrants for this event and resolves each user name for display.
     *
     * @author Karina Zhang
     */
    private void loadWaitingEntrants() {
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, WaitlistFirestoreStatus.WAITING)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        adapter.setItems(Collections.emptyList());
                        return;
                    }
                    int total = snapshot.size();
                    AtomicInteger done = new AtomicInteger(0);
                    List<WaitlistEntrantAdapter.WaitlistEntrantItem> buf =
                            Collections.synchronizedList(new ArrayList<>());

                    for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
                        String userId = entryDoc.getString("userId");
                        if (userId == null) {
                            if (done.incrementAndGet() == total) {
                                finishLoad(buf);
                            }
                            continue;
                        }
                        String entryDocId = entryDoc.getId();

                        FirebaseHelper.getInstance().fetchUserDocumentForWaitlistUserId(userId, task -> {
                            String name = getString(R.string.unknown_user);
                            if (task.isSuccessful() && task.getResult() != null
                                    && task.getResult().exists()) {
                                String n = task.getResult().getString("name");
                                if (!TextUtils.isEmpty(n)) {
                                    name = n;
                                }
                            }
                            buf.add(new WaitlistEntrantAdapter.WaitlistEntrantItem(userId, name, entryDocId));
                            if (done.incrementAndGet() == total) {
                                finishLoad(buf);
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show());
    }

    /**
     * Sorts fetched rows by name and pushes them into the adapter.
     *
     * @param buf unsorted rows built from Firestore responses
     * @author Karina Zhang
     */
    private void finishLoad(List<WaitlistEntrantAdapter.WaitlistEntrantItem> buf) {
        List<WaitlistEntrantAdapter.WaitlistEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

    /**
     * Fetches event title once if missing, then runs the callback.
     *
     * @param then callback to run after title is available
     * @author Karina Zhang
     */
    private void refreshEventTitleIfNeeded(Runnable then) {
        if (!TextUtils.isEmpty(eventTitle)) {
            then.run();
            return;
        }
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String t = doc.getString("title");
                        eventTitle = t != null ? t : getString(R.string.app_name);
                    } else {
                        eventTitle = getString(R.string.app_name);
                    }
                    then.run();
                })
                .addOnFailureListener(e -> {
                    eventTitle = getString(R.string.app_name);
                    then.run();
                });
    }

    /**
     * Opens the selected entrant profile preview dialog.
     *
     * @param item selected row item
     * @author Karina Zhang
     */
    @Override
    public void onViewProfile(@NonNull WaitlistEntrantAdapter.WaitlistEntrantItem item) {
        ProfilePreviewHelper.showProfileDialog(this, item.userId);
    }

    /**
     * Marks one waiting entrant as selected and sends a chosen notification.
     *
     * @param item row item to accept
     * @author Karina Zhang
     */
    @Override
    public void onAccept(@NonNull WaitlistEntrantAdapter.WaitlistEntrantItem item) {
        refreshEventTitleIfNeeded(() -> {
            DocumentReference entryRef = db.collection("waitlist_entries").document(item.entryDocumentId);
            db.runTransaction(transaction -> {
                transaction.update(entryRef, "status", WaitlistFirestoreStatus.SELECTED);
                return null;
            }).addOnSuccessListener(v -> {
                String message = getString(R.string.waitlist_chosen_notification_message, eventTitle);
                FirebaseHelper.getInstance().createNotification(
                        item.userId,
                        eventId,
                        eventTitle,
                        message,
                        "CHOSEN",
                        task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, R.string.waitlist_accepted_sent, Toast.LENGTH_SHORT).show();
                                loadWaitingEntrants();
                            } else {
                                Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
            }).addOnFailureListener(e ->
                    Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show());
        });
    }

    /**
     * Marks one waiting entrant as rejected, removes them from waitlist ids, and sends not-chosen notification.
     *
     * @param item row item to decline
     * @author Karina Zhang
     */
    @Override
    public void onDecline(@NonNull WaitlistEntrantAdapter.WaitlistEntrantItem item) {
        refreshEventTitleIfNeeded(() -> {
            DocumentReference entryRef = db.collection("waitlist_entries").document(item.entryDocumentId);
            DocumentReference eventRef = db.collection("events").document(eventId);
            db.runTransaction(transaction -> {
                transaction.update(entryRef, "status", WaitlistFirestoreStatus.REJECTED);
                transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(item.userId));
                return null;
            }).addOnSuccessListener(v -> {
                String message = getString(R.string.notification_entrant_not_selected, eventTitle);
                FirebaseHelper.getInstance().createNotification(
                        item.userId,
                        eventId,
                        eventTitle,
                        message,
                        "NOT_CHOSEN",
                        task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, R.string.waitlist_declined_sent, Toast.LENGTH_SHORT).show();
                                loadWaitingEntrants();
                            } else {
                                Toast.makeText(this, R.string.waitlist_status_updated_notify_failed, Toast.LENGTH_SHORT).show();
                                loadWaitingEntrants();
                            }
                        });
            }).addOnFailureListener(e ->
                    Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show());
        });
    }

    /**
     * Shows the lottery sample-size input dialog.
     *
     * @author Karina Zhang
     */
    private void showLotteryDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint(getString(R.string.lottery_dialog_hint));
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.lottery_dialog_title))
                .setMessage(getString(R.string.lottery_dialog_message))
                .setView(input)
                .setPositiveButton(getString(R.string.lottery_dialog_confirm), (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) {
                        Toast.makeText(this, getString(R.string.lottery_error_enter_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int sampleSize = Integer.parseInt(raw);
                    if (sampleSize <= 0) {
                        Toast.makeText(this, getString(R.string.lottery_error_positive), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    runLottery(sampleSize);
                })
                .setNegativeButton(getString(R.string.lottery_dialog_cancel), null)
                .show();
    }

    /**
     * Runs lottery sampling for this event and opens the confirmation screen on success.
     *
     * @param sampleSize number of entrants to sample
     * @author Karina Zhang
     */
    private void runLottery(int sampleSize) {
        Toast.makeText(this, getString(R.string.lottery_running), Toast.LENGTH_SHORT).show();
        FirebaseHelper.getInstance().executeLotterySampling(this, eventId, sampleSize, true, (task, actualSampledCount) -> {
            if (task.isSuccessful()) {
                Intent i = new Intent(this, SamplingConfirmationActivity.class);
                i.putExtra(SamplingConfirmationActivity.EXTRA_EVENT_ID, eventId);
                i.putExtra(SamplingConfirmationActivity.EXTRA_SAMPLED_COUNT, actualSampledCount);
                startActivity(i);
                loadWaitingEntrants();
            } else {
                Toast.makeText(this, getString(R.string.lottery_error_failed), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sends a bulk notification to all currently visible waiting entrants.
     *
     * @author Karina Zhang
     */
    private void notifyAllWaitingEntrants() {
        List<WaitlistEntrantAdapter.WaitlistEntrantItem> visible = adapter.getVisibleItemsSnapshot();
        if (visible.isEmpty()) {
            Toast.makeText(this, R.string.waitlist_no_entrants_to_notify, Toast.LENGTH_SHORT).show();
            return;
        }
        refreshEventTitleIfNeeded(() -> {
            AtomicInteger remaining = new AtomicInteger(visible.size());
            final boolean[] hadFailure = {false};
            String message = getString(R.string.waitlist_notify_all_message, eventTitle);
            for (WaitlistEntrantAdapter.WaitlistEntrantItem item : visible) {
                FirebaseHelper.getInstance().createNotification(
                        item.userId,
                        eventId,
                        eventTitle,
                        message,
                        "NOT_CHOSEN",
                        task -> {
                            if (!task.isSuccessful()) {
                                hadFailure[0] = true;
                            }
                            if (remaining.decrementAndGet() == 0) {
                                int toast = hadFailure[0]
                                        ? R.string.waitlist_status_updated_notify_failed
                                        : R.string.waitlist_notifications_sent;
                                Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}

