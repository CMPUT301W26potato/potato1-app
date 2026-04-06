package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.ProfilePreviewHelper;
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
 * Organizer screen that lists lottery-selected entrants and lets organizer remove a sampled entrant.
 * This screen is used right after lottery selection.
 *
 * Addresses: US 02.05.01 - Organizer: Notify Chosen Entrants
 *
 * @author Karina Zhang
 * @version 1.0
 * @see SamplingConfirmationActivity
 */
public class SampledEntrantsActivity extends OrganizerBaseActivity implements SampledEntrantAdapter.Listener {
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
    private SampledEntrantAdapter adapter;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();

    /**
     * Sets up sampled entrants screen and starts first load.
     *
     * @param savedInstanceState restore bundle, can be null
     * @author Karina Zhang
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sampled_entrants);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupOrganizerDrawer();

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

        RecyclerView recycler = findViewById(R.id.recyclerSampled);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SampledEntrantAdapter(this);
        recycler.setAdapter(adapter);

        findViewById(R.id.btnSelectAll).setVisibility(android.view.View.GONE);
        findViewById(R.id.btnSendNotifications).setVisibility(android.view.View.GONE);

        BottomNavigationView nav = findViewById(R.id.organizerBottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_organizer_bottom_home) {
                Intent intent = new Intent(this, OrganizerEntryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }

            if (id == R.id.nav_organizer_bottom_back) {
                finish();
                return true;
            }
            return false;
        });

        loadSelectedEntrants();
    }

    /**
     * Loads selected entrants and resolves display names.
     *
     * @author Karina Zhang
     */
    private void loadSelectedEntrants() {
        String selectedStatus = getString(R.string.firestore_waitlist_status_selected);
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, selectedStatus)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        adapter.setItems(Collections.emptyList());
                        return;
                    }
                    int total = snapshot.size();
                    AtomicInteger done = new AtomicInteger(0);
                    List<SampledEntrantAdapter.SampledEntrantItem> buf =
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
                            buf.add(new SampledEntrantAdapter.SampledEntrantItem(userId, name, entryDocId));
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
     * Sorts sampled rows by name and updates adapter.
     *
     * @param buf unsorted sampled rows
     * @author Karina Zhang
     */
    private void finishLoad(List<SampledEntrantAdapter.SampledEntrantItem> buf) {
        List<SampledEntrantAdapter.SampledEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

    /**
     * Opens profile preview for selected sampled row.
     *
     * @param item selected row model
     * @author Karina Zhang
     */
    @Override
    public void onViewProfile(@NonNull SampledEntrantAdapter.SampledEntrantItem item) {
        ProfilePreviewHelper.showProfileDialog(this, item.userId);
    }

    // REHAAN'S ADDITION â€” remove a sampled entrant (set to rejected, remove from list)
    /**
     * Shows confirm dialog before removing one sampled entrant.
     *
     * @param item row to remove
     * @author Karina Zhang
     */
    @Override
    public void onRemoveSampledEntrant(@NonNull SampledEntrantAdapter.SampledEntrantItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.sampled_remove_dialog_title))
                .setMessage(getString(R.string.sampled_remove_dialog_message, item.displayName))
                .setPositiveButton(getString(R.string.sampled_remove_dialog_confirm), (dialog, which) ->
                        doRemoveSampledEntrant(item))
                .setNegativeButton(getString(R.string.lottery_dialog_cancel), null)
                .show();
    }

    /**
     * Marks sampled entrant as rejected and removes the row from adapter on success.
     *
     * @param item sampled row to remove
     * @author Karina Zhang
     */
    private void doRemoveSampledEntrant(@NonNull SampledEntrantAdapter.SampledEntrantItem item) {
        db.collection("waitlist_entries")
                .document(item.entryDocumentId)
                .update("status", com.example.waitwell.WaitlistFirestoreStatus.REJECTED)
                .addOnSuccessListener(v -> {
                    adapter.removeEntry(item.entryDocumentId);
                    Toast.makeText(this, R.string.sampled_remove_success, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SampledEntrants", "remove failed", e);
                    Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show();
                });
    }
    // END REHAAN'S ADDITION
}

