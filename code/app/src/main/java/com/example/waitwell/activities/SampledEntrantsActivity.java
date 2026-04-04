package com.example.waitwell.activities;

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
 * Lists entrants with lottery status {@code selected} for an event (same field as sampling).
 */
public class SampledEntrantsActivity extends OrganizerBaseActivity implements SampledEntrantAdapter.Listener {

    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private SampledEntrantAdapter adapter;
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

        loadSelectedEntrants();
    }

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

    private void finishLoad(List<SampledEntrantAdapter.SampledEntrantItem> buf) {
        List<SampledEntrantAdapter.SampledEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

    @Override
    public void onViewProfile(@NonNull SampledEntrantAdapter.SampledEntrantItem item) {
        Toast.makeText(this, R.string.waitlist_profile_preview_placeholder, Toast.LENGTH_SHORT).show();
    }
}
