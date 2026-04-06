package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.ProfilePreviewHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Organizer screen that lists cancelled entrants and supports bulk notify for that group.
 * This is used when organizers follow up with cancelled entrants.
 *
 * Addresses: US 02.06.04 - Organizer: View Enrolled Entrants, US 02.07.03 - Organizer: Notify All Cancelled
 *
 * @author Karina Zhang
 * @version 1.0
 * @see CancelledEntrantAdapter
 */
public class CancelledEntrantsActivity extends AppCompatActivity implements CancelledEntrantAdapter.Listener {
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

    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private CancelledEntrantAdapter adapter;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();
    private String statusCancelled;
    private String eventTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelled_entrants);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            eventId = getIntent().getStringExtra("event_id");
        }
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        statusCancelled = getString(R.string.firestore_waitlist_status_cancelled);

        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        ImageView imgProfile = findViewById(R.id.imgProfileAvatar);
        btnHamburger.setOnClickListener(v -> finish());
        imgProfile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

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

        RecyclerView recycler = findViewById(R.id.recyclerCancelledEntrants);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CancelledEntrantAdapter(this);
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

        findViewById(R.id.btnSendNotifications).setOnClickListener(v -> sendCancelledNotifications());

        loadCancelledEntrants();
        refreshEventTitleIfNeeded(null);
    }

    private void loadCancelledEntrants() {
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, statusCancelled)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        adapter.setItems(Collections.emptyList());
                        return;
                    }
                    int total = snapshot.size();
                    AtomicInteger done = new AtomicInteger(0);
                    List<CancelledEntrantAdapter.CancelledEntrantItem> buf =
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
                            buf.add(new CancelledEntrantAdapter.CancelledEntrantItem(userId, name, entryDocId));
                            if (done.incrementAndGet() == total) {
                                finishLoad(buf);
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show());
    }

    private void finishLoad(List<CancelledEntrantAdapter.CancelledEntrantItem> buf) {
        List<CancelledEntrantAdapter.CancelledEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

    @Override
    public void onViewProfile(@NonNull CancelledEntrantAdapter.CancelledEntrantItem item) {
        ProfilePreviewHelper.showProfileDialog(this, item.userId);
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

    private void sendCancelledNotifications() {
        List<CancelledEntrantAdapter.CancelledEntrantItem> visible = adapter.getVisibleItemsSnapshot();
        if (visible.isEmpty()) {
            Toast.makeText(this, R.string.waitlist_no_entrants_to_notify, Toast.LENGTH_SHORT).show();
            return;
        }
        refreshEventTitleIfNeeded(() -> {
            AtomicInteger remaining = new AtomicInteger(visible.size());
            final boolean[] hadFailure = {false};
            String message = getString(R.string.invited_notify_cancelled_message, eventTitle);
            for (CancelledEntrantAdapter.CancelledEntrantItem item : visible) {
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
                                        : R.string.invited_notifications_sent_success;
                                Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}

