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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
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
 * Organizer waiting-list / "View Requests" screen (US: manage requests).
 * Loads {@code waitlist_entries} with status {@code waiting}, accepts map to {@code selected}
 * (same as lottery) with CHOSEN notification; declines set {@code rejected} and NOT_CHOSEN notification.
 */
public class ViewRequestsActivity extends AppCompatActivity implements WaitlistEntrantAdapter.Listener {

    private String eventId;
    private String eventTitle;

    private WaitlistEntrantAdapter adapter;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();

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

        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        ImageView imgProfile = findViewById(R.id.imgProfileAvatar);
        btnHamburger.setOnClickListener(v -> finish());
        imgProfile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

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
        btnViewMap.setOnClickListener(v -> Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show());
        btnNotifyAll.setOnClickListener(v -> notifyAllWaitingEntrants());
        btnSample.setOnClickListener(v -> showLotteryDialog());

        loadWaitingEntrants();
    }

    private void loadWaitingEntrants() {
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, "waiting")
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

                        db.collection("users").document(userId).get()
                                .addOnCompleteListener(task -> {
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

    private void finishLoad(List<WaitlistEntrantAdapter.WaitlistEntrantItem> buf) {
        List<WaitlistEntrantAdapter.WaitlistEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

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

    @Override
    public void onViewProfile(@NonNull WaitlistEntrantAdapter.WaitlistEntrantItem item) {
        Toast.makeText(this, R.string.waitlist_profile_preview_placeholder, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccept(@NonNull WaitlistEntrantAdapter.WaitlistEntrantItem item) {
        refreshEventTitleIfNeeded(() -> {
            DocumentReference entryRef = db.collection("waitlist_entries").document(item.entryDocumentId);
            db.runTransaction(transaction -> {
                transaction.update(entryRef, "status", "selected");
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

    @Override
    public void onDecline(@NonNull WaitlistEntrantAdapter.WaitlistEntrantItem item) {
        refreshEventTitleIfNeeded(() -> {
            DocumentReference entryRef = db.collection("waitlist_entries").document(item.entryDocumentId);
            DocumentReference eventRef = db.collection("events").document(eventId);
            db.runTransaction(transaction -> {
                transaction.update(entryRef, "status", "rejected");
                transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(item.userId));
                return null;
            }).addOnSuccessListener(v -> {
                String message = getString(R.string.event_detail_registration_not_accepted);
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

    private void runLottery(int sampleSize) {
        Toast.makeText(this, getString(R.string.lottery_running), Toast.LENGTH_SHORT).show();
        FirebaseHelper.getInstance().executeLotterySampling(eventId, sampleSize, (task, actualSampledCount) -> {
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
