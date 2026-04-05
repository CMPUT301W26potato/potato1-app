package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.waitwell.DeviceUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * AdminEventsActivity allows administrators to view
 * and manage all events stored in Firestore.
 */
public class AdminEventsActivity extends AppCompatActivity {

    RecyclerView recyclerEvents;
    List<DocumentSnapshot> allEvents;
    RecyclerView.Adapter adapter;
    java.util.Map<String, String> organizerCache = new java.util.HashMap<>();
    List<DocumentSnapshot> filteredEvents;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_events);

        recyclerEvents = findViewById(R.id.recyclerEvents);
        recyclerEvents.setLayoutManager(new GridLayoutManager(this, 2));

        loadEvents();

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        DrawerLayout drawerLayout = findViewById(R.id.admin_drawer_layout);
        NavigationView navigationView = findViewById(R.id.admin_navigation_view);

// open drawer on hamburger click
        findViewById(R.id.btnHamburger).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_switch_entrant) {
                switchToEntrant();
            }
            else if (id == R.id.nav_switch_organizer) {
                switchToOrganizer();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }
    private void switchToEntrant() {

        String deviceId = DeviceUtils.getDeviceId(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("role", "entrant")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (!queryDocumentSnapshots.isEmpty()) {
                        startActivity(new Intent(this, MainActivity.class));

                    } else {
                        Intent intent = new Intent(this, RegisterActivity.class);
                        intent.putExtra("role", "entrant");
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking entrant role", Toast.LENGTH_SHORT).show();
                });
    }
    private void switchToOrganizer() {

        String deviceId = DeviceUtils.getDeviceId(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("role", "organizer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (!queryDocumentSnapshots.isEmpty()) {
                        startActivity(new Intent(this, OrganizerEntryActivity.class));

                    } else {
                        Intent intent = new Intent(this, RegisterActivity.class);
                        intent.putExtra("role", "organizer");
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking organizer role", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Load events from Firestore
     */
    private void loadEvents() {

        FirebaseHelper.getInstance().getAllEvents()
                .addOnSuccessListener(snapshot -> {

                    allEvents = snapshot.getDocuments();
                    filteredEvents = new java.util.ArrayList<>(allEvents);

                    setupAdapter();
                    setupSearch();
                });
    }

    /**
     * Setup RecyclerView adapter
     */
    private void setupAdapter() {

        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_admin_event, parent, false);

                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                DocumentSnapshot doc = filteredEvents.get(position);
                java.util.List<String> entrants = (java.util.List<String>) doc.get("AttendingEntrants");
                TextView entrantsView = holder.itemView.findViewById(R.id.txtEntrants);
                int count = (entrants != null) ? entrants.size() : 0;
                entrantsView.setText("Entrants: " + count);

// Status
                String status = doc.getString("status");
                TextView statusView = holder.itemView.findViewById(R.id.txtStatus);

                statusView.setText("Status: " + (status != null ? status : "Unknown"));

                String title = doc.getString("title");
                String organizerId = doc.getString("organizerId");
                TextView organizerView = holder.itemView.findViewById(R.id.txtOrganizer);
                String imageUrl = doc.getString("imageUrl");

                ImageView imgPoster = holder.itemView.findViewById(R.id.eventImage);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .into(imgPoster);
                }


                if (organizerId == null) {
                    organizerView.setText("Unknown");
                } else if (organizerCache.containsKey(organizerId)) {
                    organizerView.setText(organizerCache.get(organizerId));
                } else {
                    FirebaseHelper.getInstance().getUserById(organizerId)
                            .addOnSuccessListener(userDoc -> {

                                String name = userDoc.getString("name");
                                if (name == null) name = "Unknown";

                                organizerCache.put(organizerId, name);
                                organizerView.setText(name);
                            })
                            .addOnFailureListener(e -> organizerView.setText("Unknown"));
                }

                ((TextView) holder.itemView.findViewById(R.id.txtEventName))
                        .setText(title != null ? title : "No Title");



                // Delete button (with confirmation)
                holder.itemView.findViewById(R.id.btnRemoveEvent)
                        .setOnClickListener(v -> {

                            showDeleteDialog(doc.getId(), imageUrl);
                        });

                // Open comments screen
                holder.itemView.findViewById(R.id.btnRemoveComments)
                        .setOnClickListener(v -> {
                            Intent intent = new Intent(v.getContext(), AdminCommentsActivity.class);
                            intent.putExtra("event_id", doc.getId());
                            v.getContext().startActivity(intent);
                        });
            }

            @Override
            public int getItemCount() {
                return filteredEvents.size();
            }
        };

        recyclerEvents.setAdapter(adapter);
    }

    /**
     * Delete confirmation dialog
     */
    private void showDeleteDialog(String eventId, String imageUrl) {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> removeEvent(eventId, imageUrl))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Remove event from Firestore
     */
    private void removeEvent(String eventId, String imageUrl) {

        FirebaseHelper.getInstance()
                .removeEvent(eventId)
                .addOnSuccessListener(unused -> {

                    Intent intent = new Intent(this, AdminEventRemovedActivity.class);
                    intent.putExtra("image_url", imageUrl);
                    startActivity(intent);
                });
    }

    /**
     * Setup search bar
     */
    private void setupSearch() {

        android.widget.EditText search = findViewById(R.id.searchEvents);

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    /**
     * Filter logic (search + buttons)
     */
    private void filterEvents(String query) {

        if (allEvents == null) return;

        filteredEvents.clear();

        for (DocumentSnapshot doc : allEvents) {

            String title = doc.getString("title");
            String organizerId = doc.getString("organizerId");
            String organizer = organizerCache.get(organizerId);

            boolean matchesSearch =
                    (title != null && title.toLowerCase().contains(query.toLowerCase())) ||
                            (organizer != null && organizer.toLowerCase().contains(query.toLowerCase())) ||
                            doc.getId().toLowerCase().contains(query.toLowerCase());

            if (matchesSearch) {
                filteredEvents.add(doc);
            }
        }

        adapter.notifyDataSetChanged();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadEvents(); // reload so that prev screen doesn't display old display
    }

}