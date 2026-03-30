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
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
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
 * Confirmed (final) entrants for an event.
 */
public class FinalEntrantsActivity extends AppCompatActivity implements FinalEntrantAdapter.Listener {

    public static final String EXTRA_EVENT_ID = "event_id";

    private String eventId;
    private FinalEntrantAdapter adapter;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();
    private String statusConfirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_entrants);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        statusConfirmed = getString(R.string.firestore_waitlist_status_confirmed);

        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        ImageView imgProfile = findViewById(R.id.imgProfileAvatar);
        btnHamburger.setOnClickListener(v -> finish());
        imgProfile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

        RecyclerView recycler = findViewById(R.id.recyclerFinalEntrants);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FinalEntrantAdapter(this);
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

        AppCompatButton btnRemoveSelected = findViewById(R.id.btnRemoveSelected);
        btnRemoveSelected.setVisibility(android.view.View.GONE);

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

        loadFinalEntrants();
    }

    private void loadFinalEntrants() {
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, statusConfirmed)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        adapter.setItems(Collections.emptyList());
                        return;
                    }
                    int total = snapshot.size();
                    AtomicInteger done = new AtomicInteger(0);
                    List<FinalEntrantAdapter.FinalEntrantItem> buf =
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
                                    buf.add(new FinalEntrantAdapter.FinalEntrantItem(userId, name, entryDocId));
                                    if (done.incrementAndGet() == total) {
                                        finishLoad(buf);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show());
    }

    private void finishLoad(List<FinalEntrantAdapter.FinalEntrantItem> buf) {
        List<FinalEntrantAdapter.FinalEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

    @Override
    public void onViewProfile(@NonNull FinalEntrantAdapter.FinalEntrantItem item) {
        Toast.makeText(this, R.string.waitlist_profile_preview_placeholder, Toast.LENGTH_SHORT).show();
    }
}
