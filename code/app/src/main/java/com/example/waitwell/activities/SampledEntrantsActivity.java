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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
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
 * Lists entrants with lottery status {@code selected} for an event (same field as sampling).
 */
public class SampledEntrantsActivity extends AppCompatActivity implements SampledEntrantAdapter.Listener {

    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private SampledEntrantAdapter adapter;
    private Button btnSelectAll;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();

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
                updateSelectAllButtonLabel();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        RecyclerView recycler = findViewById(R.id.recyclerSampled);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SampledEntrantAdapter(this);
        recycler.setAdapter(adapter);

        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnSelectAll.setOnClickListener(v -> {
            adapter.toggleSelectAll();
            updateSelectAllButtonLabel();
        });

        Button btnSend = findViewById(R.id.btnSendNotifications);
        btnSend.setOnClickListener(v -> Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show());

        BottomNavigationView nav = findViewById(R.id.organizerBottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_organizer_bottom_back) {
                finish();
                return true;
            }
            if (id == R.id.nav_organizer_bottom_home) {
                Intent i = new Intent(this, OrganizerEntryActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
                return true;
            }
            return false;
        });

        loadSelectedEntrants();
    }

    private void updateSelectAllButtonLabel() {
        if (adapter.areAllChecked()) {
            btnSelectAll.setText(R.string.sampled_deselect_all);
        } else {
            btnSelectAll.setText(R.string.sampled_select_all);
        }
    }

    private void loadSelectedEntrants() {
        String selectedStatus = getString(R.string.firestore_waitlist_status_selected);
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, selectedStatus)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        adapter.setItems(Collections.emptyList());
                        updateSelectAllButtonLabel();
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

    private void finishLoad(List<SampledEntrantAdapter.SampledEntrantItem> buf) {
        List<SampledEntrantAdapter.SampledEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
            updateSelectAllButtonLabel();
        });
    }

    @Override
    public void onConfirm(@NonNull SampledEntrantAdapter.SampledEntrantItem item) {
        String confirmed = getString(R.string.firestore_waitlist_status_confirmed);
        db.runTransaction(transaction -> {
            DocumentReference entryRef = db.collection("waitlist_entries").document(item.entryDocumentId);
            transaction.update(entryRef, "status", confirmed);
            DocumentReference eventRef = db.collection("events").document(eventId);
            transaction.update(eventRef, "AttendingEntrants", FieldValue.arrayUnion(item.userId));
            transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(item.userId));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, R.string.sampled_confirm_success, Toast.LENGTH_SHORT).show();
            adapter.removeEntry(item.entryDocumentId);
        }).addOnFailureListener(e ->
                Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRemoveFromSampled(@NonNull SampledEntrantAdapter.SampledEntrantItem item) {
        String waiting = getString(R.string.firestore_waitlist_status_waiting);
        db.collection("waitlist_entries")
                .document(item.entryDocumentId)
                .update("status", waiting)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.sampled_remove_success, Toast.LENGTH_SHORT).show();
                    adapter.removeEntry(item.entryDocumentId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSelectionChanged() {
        runOnUiThread(this::updateSelectAllButtonLabel);
    }
}
