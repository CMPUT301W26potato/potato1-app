package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// REHAAN'S ADDITION — US 02.09.01 + US 01.09.01
/**
 * AssignCoOrganizerActivity.java
 * Lets an organizer search for entrants by name, phone, or email and assign
 * one of them as a co-organizer for the current event (US 02.09.01).
 * When assigned, the entrant is added to the event's coOrganizerIds array,
 * removed from the waitlist pool if present, and sent a CO_ORGANIZER
 * notification so they know they were invited (US 01.09.01).
 * Javadoc written with help from Claude (claude.ai)
 */
public class AssignCoOrganizerActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private enum SearchType { NAME, PHONE, EMAIL }

    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();

    // users already assigned as co-organizer or on waitlist — excluded from results
    private final Set<String> excludedUserIds = new HashSet<>();
    private final List<CoOrganizerUserItem> results = new ArrayList<>();

    private String eventId;
    private String eventTitle = "";
    private SearchType activeType = SearchType.NAME;
    private CoOrganizerAdapter adapter;
    private EditText editSearch;
    private Button btnSearchName;
    private Button btnSearchPhone;
    private Button btnSearchEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_co_organizer);

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

        btnSearchName = findViewById(R.id.btnSearchName);
        btnSearchPhone = findViewById(R.id.btnSearchPhone);
        btnSearchEmail = findViewById(R.id.btnSearchEmail);
        editSearch = findViewById(R.id.editSearch);

        RecyclerView recycler = findViewById(R.id.recyclerCoOrganizers);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CoOrganizerAdapter(results, new CoOrganizerAdapter.Listener() {
            @Override
            public void onViewProfile(@NonNull CoOrganizerUserItem item) {
                Toast.makeText(AssignCoOrganizerActivity.this,
                        R.string.waitlist_profile_preview_placeholder, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAssign(@NonNull CoOrganizerUserItem item) {
                assignCoOrganizer(item);
            }
        });
        recycler.setAdapter(adapter);

        btnSearchName.setOnClickListener(v -> setSearchType(SearchType.NAME));
        btnSearchPhone.setOnClickListener(v -> setSearchType(SearchType.PHONE));
        btnSearchEmail.setOnClickListener(v -> setSearchType(SearchType.EMAIL));

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                runSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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

        loadEventTitle();
        loadExcludedUserIds();
        setSearchType(SearchType.NAME);
    }

    private void setSearchType(@NonNull SearchType type) {
        activeType = type;
        btnSearchName.setBackgroundResource(
                type == SearchType.NAME ? R.drawable.bg_filter_active : R.drawable.bg_filter_inactive);
        btnSearchPhone.setBackgroundResource(
                type == SearchType.PHONE ? R.drawable.bg_filter_active : R.drawable.bg_filter_inactive);
        btnSearchEmail.setBackgroundResource(
                type == SearchType.EMAIL ? R.drawable.bg_filter_active : R.drawable.bg_filter_inactive);
        btnSearchName.setTextColor(ContextCompat.getColor(this,
                type == SearchType.NAME ? R.color.text_white : R.color.text_primary));
        btnSearchPhone.setTextColor(ContextCompat.getColor(this,
                type == SearchType.PHONE ? R.color.text_white : R.color.text_primary));
        btnSearchEmail.setTextColor(ContextCompat.getColor(this,
                type == SearchType.EMAIL ? R.color.text_white : R.color.text_primary));
        runSearch();
    }

    private void loadEventTitle() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null) {
                        String title = doc.getString("title");
                        if (!TextUtils.isEmpty(title)) {
                            eventTitle = title;
                        }
                    }
                });
    }

    /**
     * Loads the set of user IDs that should not appear in search results.
     * This includes anyone already on the waitlist and anyone already
     * assigned as a co-organizer for this event.
     */
    private void loadExcludedUserIds() {
        // exclude anyone already on the waitlist_entries for this event
        db.collection("waitlist_entries")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    excludedUserIds.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (!TextUtils.isEmpty(userId)) {
                            excludedUserIds.add(userId);
                        }
                    }
                    // also exclude anyone already a co-organizer on this event
                    db.collection("events").document(eventId).get()
                            .addOnSuccessListener(eventDoc -> {
                                if (eventDoc != null && eventDoc.exists()) {
                                    Object raw = eventDoc.get("coOrganizerIds");
                                    List<String> existing = new ArrayList<>();
                                    if (raw instanceof List) {
                                        for (Object o : (List<?>) raw) {
                                            if (o instanceof String) {
                                                existing.add((String) o);
                                            }
                                        }
                                    }
                                    if (existing != null) {
                                        excludedUserIds.addAll(existing);
                                    }
                                }
                                runSearch();
                            });
                });
    }

    private void runSearch() {
        String query = editSearch.getText() != null
                ? editSearch.getText().toString().trim() : "";
        if (query.length() < 2) {
            results.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("users")
                .limit(100)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<CoOrganizerUserItem> filtered = new ArrayList<>();
                    String q = query.toLowerCase(Locale.US);
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getId();
                        if (excludedUserIds.contains(userId)) {
                            continue;
                        }
                        String name = safe(doc.getString("name"));
                        String email = safe(doc.getString("email"));
                        String phone = safe(doc.getString("phone"));

                        boolean matches = false;
                        String match = "";

                        if (activeType == SearchType.NAME
                                && name.toLowerCase(Locale.US).startsWith(q)) {
                            match = !TextUtils.isEmpty(email) ? email : phone;
                            if (match == null) match = "";
                            matches = true;
                        } else if (activeType == SearchType.PHONE
                                && phone.startsWith(query)) {
                            match = phone;
                            matches = true;
                        } else if (activeType == SearchType.EMAIL
                                && email.toLowerCase(Locale.US).startsWith(q)) {
                            match = email;
                            matches = true;
                        }

                        if (matches) {
                            filtered.add(new CoOrganizerUserItem(userId, name, email, phone, match));
                        }
                    }
                    results.clear();
                    results.addAll(filtered);
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Assigns the selected user as a co-organizer for this event (US 02.09.01).
     * Writes their userId into the event's coOrganizerIds array.
     * Also removes them from waitlistEntrantIds and deletes their
     * waitlist_entries doc if one exists, so they cannot be drawn in the lottery.
     * Then fires the CO_ORGANIZER notification (US 01.09.01).
     */
    private void assignCoOrganizer(@NonNull CoOrganizerUserItem item) {
        String titleForMsg = TextUtils.isEmpty(eventTitle)
                ? getString(R.string.app_name) : eventTitle;
        String entryDocId = item.userId + "_" + eventId;

        db.runTransaction(transaction -> {
            // add to coOrganizerIds array on the event
            com.google.firebase.firestore.DocumentReference eventRef =
                    db.collection("events").document(eventId);
            transaction.update(eventRef, "coOrganizerIds",
                    com.google.firebase.firestore.FieldValue.arrayUnion(item.userId));
            // remove from waitlist array in case they were on it
            transaction.update(eventRef, "waitlistEntrantIds",
                    com.google.firebase.firestore.FieldValue.arrayRemove(item.userId));
            // delete their waitlist_entries doc so they are excluded from sampling
            com.google.firebase.firestore.DocumentReference entryRef =
                    db.collection("waitlist_entries").document(entryDocId);
            transaction.delete(entryRef);
            return null;
        }).addOnSuccessListener(v -> {
            // US 01.09.01 — notify the entrant they have been invited as co-organizer
            String message = getString(R.string.co_organizer_notification_message, titleForMsg);
            FirebaseHelper.getInstance().createNotification(
                    item.userId,
                    eventId,
                    titleForMsg,
                    message,
                    "CO_ORGANIZER",
                    task -> {
                        if (task.isSuccessful()) {
                            excludedUserIds.add(item.userId);
                            removeResult(item.userId);
                            Toast.makeText(this,
                                    getString(R.string.co_organizer_assigned_toast, item.name),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // assignment succeeded but notification failed — still show partial success
                            excludedUserIds.add(item.userId);
                            removeResult(item.userId);
                            Toast.makeText(this,
                                    R.string.waitlist_status_updated_notify_failed,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }).addOnFailureListener(e ->
                Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show());
    }

    private void removeResult(@NonNull String userId) {
        for (int i = 0; i < results.size(); i++) {
            if (userId.equals(results.get(i).userId)) {
                results.remove(i);
                adapter.notifyItemRemoved(i);
                return;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // simple data holder for a search result row
    public static class CoOrganizerUserItem {
        public final String userId;
        public final String name;
        public final String email;
        public final String phone;
        public final String matchedValue;

       public CoOrganizerUserItem(String userId, String name, String email,
                            String phone, String matchedValue) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.matchedValue = matchedValue;
        }
    }

    // inline adapter — matches InviteAdapter pattern exactly
    public static class CoOrganizerAdapter extends RecyclerView.Adapter<CoOrganizerAdapter.Holder> {

        interface Listener {
            void onViewProfile(@NonNull CoOrganizerUserItem item);
            void onAssign(@NonNull CoOrganizerUserItem item);
        }

        private final List<CoOrganizerUserItem> items;
        private final Listener listener;

        CoOrganizerAdapter(List<CoOrganizerUserItem> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_assign_co_organizer_row, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            CoOrganizerUserItem item = items.get(position);
            h.txtName.setText(item.name);
            h.txtMatched.setText(item.matchedValue);
            h.btnView.setOnClickListener(v -> listener.onViewProfile(item));
            h.btnAssign.setOnClickListener(v -> listener.onAssign(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView txtName;
            final TextView txtMatched;
            final ImageButton btnView;
            final ImageButton btnAssign;

            Holder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txtEntrantName);
                txtMatched = itemView.findViewById(R.id.txtMatchedValue);
                btnView = itemView.findViewById(R.id.btnViewEntrant);
                btnAssign = itemView.findViewById(R.id.btnAssignCoOrganizer);
            }
        }
    }
}
// END REHAAN'S ADDITION — US 02.09.01 + US 01.09.01