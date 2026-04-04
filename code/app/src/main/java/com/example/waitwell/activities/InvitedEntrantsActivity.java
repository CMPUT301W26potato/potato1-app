package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.ProfilePreviewHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Invited / enrolled / declined entrants for an event (status {@code selected}, {@code confirmed}, {@code cancelled}).
 */
public class InvitedEntrantsActivity extends OrganizerBaseActivity implements InvitedEntrantAdapter.Listener {

    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private InvitedEntrantAdapter adapter;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();

    private String statusSelected;
    private String statusConfirmed;
    private String statusCancelled;
    private String eventTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invited_entrants);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            eventId = getIntent().getStringExtra("event_id");
        }
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        statusSelected = getString(R.string.firestore_waitlist_status_selected);
        statusConfirmed = getString(R.string.firestore_waitlist_status_confirmed);
        statusCancelled = getString(R.string.firestore_waitlist_status_cancelled);

        setupOrganizerDrawer();

        RecyclerView recycler = findViewById(R.id.recyclerInvited);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvitedEntrantAdapter(this);
        adapter.setStatusConstants(statusSelected, statusConfirmed, statusCancelled);
        recycler.setAdapter(adapter);

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

        CheckBox checkEnrolled = findViewById(R.id.checkFilterEnrolled);
        CheckBox checkCancelled = findViewById(R.id.checkFilterCancelled);
        CheckBox checkPending = findViewById(R.id.checkFilterPending);
        Runnable applyFilters = () -> adapter.setStatusFilters(
                checkEnrolled.isChecked(),
                checkCancelled.isChecked(),
                checkPending.isChecked());
        checkEnrolled.setOnCheckedChangeListener((b, c) -> applyFilters.run());
        checkCancelled.setOnCheckedChangeListener((b, c) -> applyFilters.run());
        checkPending.setOnCheckedChangeListener((b, c) -> applyFilters.run());

        findViewById(R.id.btnSendNotifications).setOnClickListener(v -> sendStatusAwareNotifications());

        findViewById(R.id.btnRemoveApplicants).setOnClickListener(v -> removeSelectedApplicants());

        BottomNavigationView nav = findViewById(R.id.organizerBottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_organizer_bottom_back) {
                finish();
                return true;
            }
            if (id == R.id.nav_organizer_bottom_home) {
                startActivity(OrganizerEntryActivity.intentNavigateToMyEvents(this));
                finish();
                return true;
            }
            return false;
        });

        loadInvitedEntrants();
        refreshEventTitleIfNeeded(null);
    }

    private void refreshEventTitleIfNeeded(Runnable then) {
        if (!TextUtils.isEmpty(eventTitle)) {
            if (then != null) then.run();
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
                    if (then != null) then.run();
                })
                .addOnFailureListener(e -> {
                    eventTitle = getString(R.string.app_name);
                    if (then != null) then.run();
                });
    }

    private void sendStatusAwareNotifications() {
        List<InvitedEntrantAdapter.InvitedEntrantItem> selected = adapter.getSelectedEntrants();
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.invited_select_entrants_for_notifications, Toast.LENGTH_SHORT).show();
            return;
        }
        refreshEventTitleIfNeeded(() -> {
            AtomicInteger remaining = new AtomicInteger(selected.size());
            final boolean[] hadFailure = {false};
            for (InvitedEntrantAdapter.InvitedEntrantItem item : selected) {
                String message;
                String type;
                if (statusSelected.equals(item.firestoreStatus)) {
                    message = getString(R.string.invited_notify_pending_message, eventTitle);
                    type = "CHOSEN";
                } else if (statusConfirmed.equals(item.firestoreStatus)) {
                    message = getString(R.string.invited_notify_confirmed_message, eventTitle);
                    type = "CHOSEN";
                } else {
                    message = getString(R.string.invited_notify_cancelled_message, eventTitle);
                    type = "NOT_CHOSEN";
                }
                FirebaseHelper.getInstance().createNotification(
                        item.userId,
                        eventId,
                        eventTitle,
                        message,
                        type,
                        task -> {
                            if (!task.isSuccessful()) {
                                hadFailure[0] = true;
                            }
                            if (remaining.decrementAndGet() == 0) {
                                int toast = hadFailure[0]
                                        ? R.string.waitlist_status_updated_notify_failed
                                        : R.string.invited_notifications_sent_success;
                                Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void loadInvitedEntrants() {
        List<String> statuses = Arrays.asList(statusSelected, statusConfirmed, statusCancelled);
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatuses(eventId, statuses)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        adapter.setItems(Collections.emptyList());
                        return;
                    }
                    int total = snapshot.size();
                    AtomicInteger done = new AtomicInteger(0);
                    List<InvitedEntrantAdapter.InvitedEntrantItem> buf =
                            Collections.synchronizedList(new ArrayList<>());

                    for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
                        String userId = entryDoc.getString("userId");
                        String fsStatus = entryDoc.getString("status");
                        if (userId == null || fsStatus == null) {
                            if (done.incrementAndGet() == total) {
                                finishLoad(buf);
                            }
                            continue;
                        }
                        String entryDocId = entryDoc.getId();
                        FirebaseHelper.getInstance().fetchUserDocumentForWaitlistUserId(userId, task -> {
                            String name = getString(R.string.unknown_user);
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                String n = task.getResult().getString("name");
                                if (!TextUtils.isEmpty(n)) {
                                    name = n;
                                }
                            }
                            buf.add(new InvitedEntrantAdapter.InvitedEntrantItem(userId, name, entryDocId, fsStatus));
                            if (done.incrementAndGet() == total) {
                                finishLoad(buf);
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show());
    }

    private void finishLoad(List<InvitedEntrantAdapter.InvitedEntrantItem> buf) {
        List<InvitedEntrantAdapter.InvitedEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

    private void removeSelectedApplicants() {
        List<InvitedEntrantAdapter.InvitedEntrantItem> selected = adapter.getSelectedEntrants();
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.invited_select_applicants_first, Toast.LENGTH_SHORT).show();
            return;
        }
        AtomicInteger remaining = new AtomicInteger(selected.size());
        for (InvitedEntrantAdapter.InvitedEntrantItem item : selected) {
            db.runTransaction(transaction -> {
                DocumentReference entryRef = db.collection("waitlist_entries").document(item.entryDocumentId);
                transaction.update(entryRef, "status", statusCancelled);
                DocumentReference eventRef = db.collection("events").document(eventId);
                transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(item.userId));
                transaction.update(eventRef, "AttendingEntrants", FieldValue.arrayRemove(item.userId));
                return null;
            }).addOnSuccessListener(v -> {
                adapter.removeByEntryId(item.entryDocumentId);
                if (remaining.decrementAndGet() == 0) {
                    Toast.makeText(this, R.string.waitlist_declined_sent, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onViewProfile(@NonNull InvitedEntrantAdapter.InvitedEntrantItem item) {
        ProfilePreviewHelper.showProfileDialog(this, item.userId);
    }

    @Override
    public void onSelectionChanged() {
        // no-op
    }
}
