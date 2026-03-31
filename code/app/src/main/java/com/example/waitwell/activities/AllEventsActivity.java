package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.waitwell.EventStatusUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * All Events screen with search and filter.
 *
 * US 01.01.03 – entrant can see a list of events to join
 * US 01.01.04 – entrant can filter events by interests/availability
 *
 * Search matches against event title (case-insensitive).
 * Both search and filter are applied together.
 *
 Category dropdown:
 *   Reads the "category" field from every event in Firestore and builds the list of options dynamically.
 *   So if an organizer creates an event with category "Piano Lessons", it automatically
 *   appears in the dropdown.
 *
 * Each event document in Firestore should have a "category" field (string), e.g.:
 *   category: "Swimming"
 *   category: "Dance"
 * The search bar and the active filter combine — the entrant can pick "Swimming" and then type "beginner" to narrow down their search.
 *  Logic written with help from Claude (claude.ai)
 */
public class AllEventsActivity extends AppCompatActivity {
    private static final String TAG = "AllEventsActivity";

    private EditText editSearch;
    private LinearLayout eventsListContainer;
    private LinearLayout emptyState;
    private LinearLayout categoryIndicator;
    private ScrollView scrollEvents;
    private TextView txtResultCount, txtEmptyMessage, txtActiveCategory;
    private TextView chipAll, chipOpen, chipCategory, btnClearCategory,chipAvailability,chipEventCapacity, btnClearEventCapacity;

    private List<DocumentSnapshot> allDocs = new ArrayList<>();
    /** Categories collected from event data. */
    private List<String> availableCategories = new ArrayList<>();
    /** Current filter mode: "all", "open", or "category" */
    private String filterMode = "all";
    /** If filterMode == "category", which one. */
    private String selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_events);
        initViews();
        setupChips();
        setupSearch();
        setupBottomNav();
        loadEvents();
    }

    private void initViews() {
        editSearch = findViewById(R.id.editSearch);
        eventsListContainer = findViewById(R.id.eventsListContainer);
        emptyState = findViewById(R.id.emptyState);
        scrollEvents = findViewById(R.id.scrollEvents);
        categoryIndicator = findViewById(R.id.categoryIndicator);
        txtResultCount = findViewById(R.id.txtResultCount);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);
        txtActiveCategory = findViewById(R.id.txtActiveCategory);
        btnClearCategory = findViewById(R.id.btnClearCategory);
        chipAll = findViewById(R.id.chipAll);
        chipOpen = findViewById(R.id.chipOpen);
        chipCategory = findViewById(R.id.chipCategory);
        chipAvailability = findViewById(R.id.chipAvailability);
        chipEventCapacity = findViewById(R.id.chipEventCapacity);
        btnClearEventCapacity = findViewById(R.id.btnClearEventCapacity);


        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        //Tapping x clears the category and reverts to "All"
        btnClearCategory.setOnClickListener(v -> {
            selectedCategory = null;
            filterMode = "all";
            updateChipStyles();
            applyFilters();
        });
        //do the same thing for event capacity
        btnClearEventCapacity.setOnClickListener(v -> {
            selectedCategory = null;
            filterMode = "all";
            updateChipStyles();
            applyFilters();
        });

    }
    private void setupChips() {
        chipAll.setOnClickListener(v -> {
            filterMode = "all";
            selectedCategory = null;
            updateChipStyles();
            applyFilters();
        });

        chipOpen.setOnClickListener(v -> {
            filterMode = "open";
            selectedCategory = null;
            updateChipStyles();
            applyFilters();
        });

        chipAvailability.setOnClickListener(v -> {
            filterMode = "availability";
            selectedCategory = null;
            updateChipStyles();
            applyFilters();
        });

        chipCategory.setOnClickListener(v -> showCategoryDialog());
        chipEventCapacity.setOnClickListener(v -> showEventCapacityDialog());
    }

    /**
     * Updates the visual state of all three chips based on the current filterMode and selectedCategory.
     */
    private void updateChipStyles() {
        //Reset all to inactive
        chipAll.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipAll.setTextColor(getColor(R.color.primary));
        chipOpen.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipOpen.setTextColor(getColor(R.color.primary));
        chipCategory.setBackgroundResource(R.drawable.bg_filter_dropdown);
        chipCategory.setTextColor(getColor(R.color.primary));
        chipCategory.setText("Category ▾");
        chipAvailability.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipAvailability.setTextColor(getColor(R.color.primary));
        chipEventCapacity.setBackgroundResource(R.drawable.bg_filter_dropdown);
        chipEventCapacity.setTextColor(getColor(R.color.primary));
        chipEventCapacity.setText("Event Capacity ▾");


        switch (filterMode) {
            case "all":
                chipAll.setBackgroundResource(R.drawable.bg_filter_active);
                chipAll.setTextColor(getColor(R.color.text_white));
                categoryIndicator.setVisibility(View.GONE);
                break;

            case "open":
                chipOpen.setBackgroundResource(R.drawable.bg_filter_active);
                chipOpen.setTextColor(getColor(R.color.text_white));
                categoryIndicator.setVisibility(View.GONE);
                break;

            case "category":
                chipCategory.setBackgroundResource(R.drawable.bg_filter_dropdown_active);
                chipCategory.setTextColor(getColor(R.color.text_white));
                chipCategory.setText(selectedCategory + " ▾");
                categoryIndicator.setVisibility(View.VISIBLE);
                txtActiveCategory.setText(selectedCategory);
                break;
            case "availability":
                chipAvailability.setBackgroundResource(R.drawable.bg_filter_active);
                chipAvailability.setTextColor(getColor(R.color.text_white));
                categoryIndicator.setVisibility(View.GONE);
                break;

            case "event_capacity":
                chipCategory.setBackgroundResource(R.drawable.bg_filter_dropdown_active);
                chipCategory.setTextColor(getColor(R.color.text_white));
                chipCategory.setText(selectedCategory + " ▾");
                categoryIndicator.setVisibility(View.VISIBLE);
                txtActiveCategory.setText(selectedCategory);
                break;
        }
    }

    /**
     * Shows a dialog listing all categories found in the event data.
     * The list is built dynamically — whatever "category" fields exist in Firestore appear here.
     */
    private void showCategoryDialog() {
        if (availableCategories.isEmpty()) {
            Toast.makeText(this, "No categories found - add a \"category\" field to your events in Firestore", Toast.LENGTH_LONG).show();
            return;
        }

        String[] items = availableCategories.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Filter by interest")
                .setItems(items, (dialog, which) -> {
                    selectedCategory = items[which];
                    filterMode = "category";
                    updateChipStyles();
                    applyFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEventCapacityDialog() {
        if (availableCategories.isEmpty()) {
            Toast.makeText(this, "No categories found - add a \"category\" field to your events in Firestore", Toast.LENGTH_LONG).show();
            return;
        }

        String[] items = availableCategories.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Filter by event capacity")
                .setItems(items, (dialog, which) -> {
                    selectedCategory = items[which];
                    filterMode = "event_capacity";
                    updateChipStyles();
                    applyFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSearch() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

   //load from Firestore
    private void loadEvents() {
        FirebaseHelper.getInstance().getAllEvents()
                .addOnSuccessListener(snap -> {
                    allDocs = snap.getDocuments();
                    extractCategories();
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events", e);
                    Toast.makeText(this, "Could not load events",
                            Toast.LENGTH_SHORT).show();
                });
    }

    //Scans all event documents and collects unique category values.
    private void extractCategories() {
        Set<String> cats = new LinkedHashSet<>();
        for (DocumentSnapshot doc : allDocs) {
            String cat = doc.getString("category");
            if (cat != null && !cat.trim().isEmpty()) {
                cats.add(cat.trim());
            }
        }
        availableCategories = new ArrayList<>(cats);
    }

    //Filter
    private void applyFilters() {
        String query = editSearch.getText().toString().trim().toLowerCase();
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allDocs) {
            String title    = doc.getString("title");
            String category = doc.getString("category");

            if (title == null) title = "";
            if (category == null) category = "";

            //text search on title
            if (!query.isEmpty() && !title.toLowerCase().contains(query)) {
                continue;
            }
            switch (filterMode) {
                case "open":
                    if (!"open".equals(EventStatusUtils.computeStatus(doc))) continue;
                    break;
                case "category":
                    if (!category.equalsIgnoreCase(selectedCategory)) continue;
                    break;
                case "availability":
                    //if the capacity is greater than 0 its open
                    List<String> ids = (List<String>) doc.get("waitlistEntrantIds");
                    Long cap = doc.getLong("capacity");
                    int enrolled = (ids != null) ? ids.size() : 0;
                    int maxCap = (cap != null) ? cap.intValue() : 0;
                    if (maxCap > 0 && enrolled >= maxCap) continue;
                    break;

                case "event_capacity":

                    break;
                // "all" -no filtering
            }

            filtered.add(doc);
        }
        renderList(filtered);
    }

    private void renderList(List<DocumentSnapshot> docs) {
        eventsListContainer.removeAllViews();

        String countText = docs.size() + (docs.size() == 1 ? " event" : " events");
        txtResultCount.setText(countText);

        if (docs.isEmpty()) {
            scrollEvents.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            if (editSearch.getText().toString().trim().isEmpty()) {
                txtEmptyMessage.setText("No events in this category");
            } else {txtEmptyMessage.setText("No events match \"" + editSearch.getText().toString().trim() + "\"");
            }
            return;
        }

        scrollEvents.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : docs) {
            View row = inflater.inflate(R.layout.item_all_events_row, eventsListContainer, false);

            String title= doc.getString("title");
            String location = doc.getString("location");
            Double price = doc.getDouble("price");
            String eventId = doc.getId();

            if (title == null) title = "Untitled";
            if (location == null) location = "";
            if (price == null) price = 0.0;

            String lifecycle = EventStatusUtils.computeStatus(doc);
            boolean isOpen = "open".equals(lifecycle);

            ((TextView) row.findViewById(R.id.txtEventTitle)).setText(title);
            ((TextView) row.findViewById(R.id.txtEventLocation)).setText(location);

            //price pill
            TextView txtPrice = row.findViewById(R.id.txtEventPrice);
            if (price == 0) {
                txtPrice.setText("Free");
            } else {
                txtPrice.setText(String.format("$%.2f", price));
            }

            //status badge
            TextView badge = row.findViewById(R.id.txtEventStatus);
            if ("completed".equals(lifecycle)) {
                badge.setText(R.string.organizer_status_completed);
                badge.setBackgroundResource(R.drawable.bg_status_completed);
                badge.setTextColor(getColor(R.color.status_completed_text));
            } else if (isOpen) {
                badge.setText("Open");
                badge.setBackgroundResource(R.drawable.bg_status_open);
                badge.setTextColor(getColor(R.color.status_open_text));
            } else {
                badge.setText("Closed");
                badge.setBackgroundResource(R.drawable.bg_status_closed);
                badge.setTextColor(getColor(R.color.status_closed_text));
            }

            View root = row.findViewById(R.id.rowRoot);
            root.setBackgroundResource(isOpen ? R.drawable.bg_event_row : R.drawable.bg_event_row_closed);
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, EventDetailActivity.class);
                i.putExtra("event_id", eventId);
                startActivity(i);
            });
            eventsListContainer.addView(row);
        }
    }

    //Bottom Navigation
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { finish(); return true; }
            if (id == R.id.nav_waitlist) {
                startActivity(new Intent(this, WaitListActivity.class));
                return true;
            }
            if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications ", Toast.LENGTH_SHORT).show();
                //todo
                return true;
            }
            return false;
        });
    }
}
