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
import com.example.waitwell.ProfilePreviewHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Organizer screen for searching users and sending private-event invites by creating
 * selected waitlist entries plus notifications. This is the organizer side of private invites.
 *
 * Addresses: US 02.01.02 - Organizer: Create Private Event (No QR), US 01.05.06 - Entrant: Private Event Invite Notification
 *
 * @author Karina Zhang
 * @version 1.0
 * @see InvitationResponseActivity
 */
public class InviteEntrantsActivity extends AppCompatActivity {

    /*
     * Used Gemini to get my head around writing notification documents to
     * Firestore in a way the entrant side can actually pick up and display.
     * It helped me figure out what fields to include so we can tell what
     * type of notification it is when the entrant sees it. Wrote everything
     * myself after understanding how it fits together.
     *
     * Sites I looked at:
     *
     * Firestore - adding documents to a collection:
     * https://firebase.google.com/docs/firestore/manage-data/add-data
     *
     * Firestore real-time listeners - how snapshot listeners work:
     * https://firebase.google.com/docs/firestore/query-data/listen
     */
    public static final String EXTRA_EVENT_ID = "event_id";

    /**
     * Search mode for the segmented filter buttons on this screen.
     *
     * Addresses: US 02.01.02 - Organizer: Create Private Event (No QR)
     *
     * @author Karina Zhang
     * @version 1.0
     */
    private enum SearchType { NAME, PHONE, EMAIL }

    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();
    private final Set<String> excludedUserIds = new HashSet<>();
    private final List<InviteUserItem> results = new ArrayList<>();

    private String eventId;
    private String eventTitle = "";
    private SearchType activeType = SearchType.NAME;
    private InviteAdapter adapter;
    private EditText editSearch;
    private Button btnSearchName;
    private Button btnSearchPhone;
    private Button btnSearchEmail;

    /**
     * Wires up UI controls, fetches event context, and starts the invite search flow.
     *
     * @param savedInstanceState activity restore bundle, may be null
     * @author Karina Zhang
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_entrants);

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

        RecyclerView recycler = findViewById(R.id.recyclerInviteEntrants);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InviteAdapter(results, new InviteAdapter.Listener() {
            @Override
            public void onViewProfile(@NonNull InviteUserItem item) {
                ProfilePreviewHelper.showProfileDialog(InviteEntrantsActivity.this, item.userId);
            }

            @Override
            public void onInvite(@NonNull InviteUserItem item) {
                inviteEntrant(item);
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
                startActivity(OrganizerEntryActivity.intentNavigateToMyEvents(this));
                finish();
                return true;
            }
            return false;
        });

        loadEventTitle();
        loadExcludedUserIds();
        setSearchType(SearchType.NAME);
    }

    /**
     * Switches active search type and refreshes results for that mode.
     *
     * @param type selected mode (name, phone, or email)
     * @author Karina Zhang
     */
    private void setSearchType(@NonNull SearchType type) {
        activeType = type;
        btnSearchName.setBackgroundResource(type == SearchType.NAME ? R.drawable.bg_filter_active : R.drawable.bg_filter_inactive);
        btnSearchPhone.setBackgroundResource(type == SearchType.PHONE ? R.drawable.bg_filter_active : R.drawable.bg_filter_inactive);
        btnSearchEmail.setBackgroundResource(type == SearchType.EMAIL ? R.drawable.bg_filter_active : R.drawable.bg_filter_inactive);
        btnSearchName.setTextColor(ContextCompat.getColor(this, type == SearchType.NAME ? R.color.text_white : R.color.text_primary));
        btnSearchPhone.setTextColor(ContextCompat.getColor(this, type == SearchType.PHONE ? R.color.text_white : R.color.text_primary));
        btnSearchEmail.setTextColor(ContextCompat.getColor(this, type == SearchType.EMAIL ? R.color.text_white : R.color.text_primary));
        runSearch();
    }

    /**
     * Loads event title used in invitation text and fallback strings.
     *
     * @author Karina Zhang
     */
    private void loadEventTitle() {
        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            String title = doc != null ? doc.getString("title") : null;
            if (!TextUtils.isEmpty(title)) {
                eventTitle = title;
            }
        });
    }

    /**
     * Loads user ids already tied to this event so they do not show in invite search.
     *
     * @author Karina Zhang
     */
    private void loadExcludedUserIds() {
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
                    runSearch();
                });
    }

    /**
     * Runs a simple local filter over queried users based on the active search mode.
     *
     * @author Karina Zhang
     */
    private void runSearch() {
        String query = editSearch.getText() != null ? editSearch.getText().toString().trim() : "";
        if (query.length() < 2) {
            results.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("users")
                .limit(100)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<InviteUserItem> filtered = new ArrayList<>();
                    String q = query.toLowerCase(Locale.US);
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getId();
                        if (excludedUserIds.contains(userId)) {
                            continue;
                        }
                        String name = safe(doc.getString("name"));
                        String email = safe(doc.getString("email"));
                        String phone = safe(doc.getString("phone"));
                        String match = null;

                        if (activeType == SearchType.NAME && name.toLowerCase(Locale.US).startsWith(q)) {
                            match = !TextUtils.isEmpty(email) ? email : phone;
                            if (match == null) {
                                match = "";
                            }
                        } else if (activeType == SearchType.PHONE && phone.startsWith(query)) {
                            match = phone;
                        } else if (activeType == SearchType.EMAIL && email.toLowerCase(Locale.US).startsWith(q)) {
                            match = email;
                        }

                        if (match != null) {
                            filtered.add(new InviteUserItem(userId, name, email, phone, match));
                        }
                    }
                    results.clear();
                    results.addAll(filtered);
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Writes selected status for this user and sends the invite notification.
     *
     * @param item selected user row to invite
     * @author Karina Zhang
     */
    private void inviteEntrant(@NonNull InviteUserItem item) {
        String titleForEntry = TextUtils.isEmpty(eventTitle) ? getString(R.string.app_name) : eventTitle;
        String selectedStatus = getString(R.string.firestore_waitlist_status_selected);
        String entryId = item.userId + "_" + eventId;

        db.runTransaction(transaction -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("userId", item.userId);
            entry.put("eventId", eventId);
            entry.put("eventTitle", titleForEntry);
            entry.put("status", selectedStatus);
            entry.put("joinedAt", FieldValue.serverTimestamp());
            transaction.set(db.collection("waitlist_entries").document(entryId), entry);
            transaction.update(db.collection("events").document(eventId), "waitlistEntrantIds", FieldValue.arrayUnion(item.userId));
            return null;
        }).addOnSuccessListener(v -> {
            String message = getString(R.string.waitlist_chosen_notification_message, titleForEntry);
            FirebaseHelper.getInstance().createNotification(
                    item.userId,
                    eventId,
                    titleForEntry,
                    message,
                    "CHOSEN",
                    task -> {
                        if (task.isSuccessful()) {
                            excludedUserIds.add(item.userId);
                            removeResult(item.userId);
                            Toast.makeText(this, getString(R.string.invite_entrant_toast, item.name), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, R.string.waitlist_status_updated_notify_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
        }).addOnFailureListener(e ->
                Toast.makeText(this, R.string.waitlist_update_failed, Toast.LENGTH_SHORT).show());
    }

    /**
     * Removes a user from visible results after they are invited.
     *
     * @param userId invited user id to remove
     * @author Karina Zhang
     */
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

    /**
     * Trims nullable profile fields to avoid null checks in UI binding.
     *
     * @param value nullable Firestore field value
     * @return non-null trimmed string
     * @author Karina Zhang
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Row model used by the invite search result list.
     *
     * Addresses: US 02.01.02 - Organizer: Create Private Event (No QR), US 01.05.06 - Entrant: Private Event Invite Notification
     *
     * @author Karina Zhang
     * @version 1.0
     */
    static class InviteUserItem {
        final String userId;
        final String name;
        final String email;
        final String phone;
        final String matchedValue;

        /**
         * Creates one invite-result row object.
         *
         * @param userId user document id
         * @param name display name
         * @param email email value
         * @param phone phone value
         * @param matchedValue value currently matched by active filter
         * @author Karina Zhang
         */
        InviteUserItem(String userId, String name, String email, String phone, String matchedValue) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.matchedValue = matchedValue;
        }
    }

    /**
     * Adapter for invite search results with actions for profile view and invite.
     *
     * Addresses: US 02.01.02 - Organizer: Create Private Event (No QR), US 01.05.06 - Entrant: Private Event Invite Notification
     *
     * @author Karina Zhang
     * @version 1.0
     */
    static class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.Holder> {
        /**
         * Callbacks for actions on each invite row.
         *
         * Addresses: US 02.01.02 - Organizer: Create Private Event (No QR)
         *
         * @author Karina Zhang
         * @version 1.0
         */
        interface Listener {
            /**
             * Opens entrant profile preview from an invite row.
             *
             * @param item selected result row
             * @author Karina Zhang
             */
            void onViewProfile(@NonNull InviteUserItem item);
            /**
             * Sends invite action for a selected result row.
             *
             * @param item selected result row
             * @author Karina Zhang
             */
            void onInvite(@NonNull InviteUserItem item);
        }

        private final List<InviteUserItem> items;
        private final Listener listener;

        /**
         * Creates adapter with source list and action callbacks.
         *
         * @param items mutable results list from parent activity
         * @param listener callback receiver for row actions
         * @author Karina Zhang
         */
        InviteAdapter(List<InviteUserItem> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        /**
         * Inflates one invite result row.
         *
         * @param parent recycler parent
         * @param viewType adapter row type
         * @return holder bound to invite row layout
         * @author Karina Zhang
         */
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invite_entrant, parent, false);
            return new Holder(v);
        }

        /**
         * Binds one invite result row with actions.
         *
         * @param h row holder
         * @param position adapter position
         * @author Karina Zhang
         */
        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            InviteUserItem item = items.get(position);
            h.txtName.setText(item.name);
            h.txtMatched.setText(item.matchedValue);
            h.btnView.setOnClickListener(v -> listener.onViewProfile(item));
            h.btnInvite.setOnClickListener(v -> listener.onInvite(item));
        }

        /**
         * Returns number of invite rows currently in the list.
         *
         * @return row count
         * @author Karina Zhang
         */
        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * Holder for invite row view references.
         *
         * Addresses: US 02.01.02 - Organizer: Create Private Event (No QR)
         *
         * @author Karina Zhang
         * @version 1.0
         */
        static class Holder extends RecyclerView.ViewHolder {
            final TextView txtName;
            final TextView txtMatched;
            final ImageButton btnView;
            final ImageButton btnInvite;

            /**
             * Maps invite row view ids into holder fields.
             *
             * @param itemView row root view
             * @author Karina Zhang
             */
            Holder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txtEntrantName);
                txtMatched = itemView.findViewById(R.id.txtMatchedValue);
                btnView = itemView.findViewById(R.id.btnViewEntrant);
                btnInvite = itemView.findViewById(R.id.btnInvite);
            }
        }
    }
}
