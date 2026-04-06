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
import androidx.core.util.Pair;

import com.google.android.material.datepicker.MaterialDatePicker;

import com.example.waitwell.EventStatusUtils;
import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
 *
 * @author Viktoria
 *
 *  Logic written with help from Claude (claude.ai)
 */
public class AllEventsActivity extends AppCompatActivity {
    private static final String TAG = "AllEventsActivity";
    private static final String[] CAPACITY_RANGES = {"Small (1-20)", "Medium (21-50)", "Large (51+)"};

    private EditText editSearch;
    private LinearLayout eventsListContainer;
    private LinearLayout emptyState;
    private LinearLayout categoryIndicator;
    private LinearLayout eventCapacityIndicator;
    private LinearLayout dateRangeIndicator;
    private ScrollView scrollEvents;
    private TextView txtResultCount, txtEmptyMessage, txtActiveCategory, txtEventCapacity, txtDateRange;
    private TextView chipAll, chipOpen, chipCategory, btnClearCategory, chipDateRange, chipEventCapacity, btnClearEventCapacity, btnClearDateRange;

    private List<DocumentSnapshot> allDocs = new ArrayList<>();
    /** Categories collected from event data. */
    private List<String> availableCategories = new ArrayList<>();
    /** Current filter mode: "all", "open", or "category" */
    private String filterMode = "all";
    /** If filterMode == "category", which one. */
//    private String selectedCategory = null;
    private Set<String> selectedCategories = new LinkedHashSet<>();

    /** If filterMode == "event_capacity" we need to select which range. */
    private String selectedCapacityRange = null;
    /** if filterMode == "date_range" start and end in millis. */
    private Long startDateMillis = null, endDateMillis = null;
    private final String[] allCategories = new String[] {
            "Sports", "Music", "Art", "Technology", "Education",
            "Health", "Kids", "Beginner", "Advanced"
    };



    /**
     * Inflates the layout and wires up views, filter chips, search box, and
     * bottom navigation. Event data is loaded later in {@link #onResume()}.
     *
     * @param savedInstanceState previously saved instance state, or {@code null} on first launch
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_events);
        initViews();
        setupChips();
        setupSearch();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        chipDateRange = findViewById(R.id.chipDateRange);
        chipEventCapacity = findViewById(R.id.chipEventCapacity);
        btnClearEventCapacity = findViewById(R.id.btnClearEventCapacity);
        eventCapacityIndicator = findViewById(R.id.eventCapacityIndicator);
        txtEventCapacity = findViewById(R.id.txtEventCapacity);
        dateRangeIndicator = findViewById(R.id.dateRangeIndicator);
        txtDateRange = findViewById(R.id.txtDateRange);
        btnClearDateRange = findViewById(R.id.btnClearDateRange);


        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        //Tapping x clears the category and reverts to "All"
        btnClearCategory.setOnClickListener(v -> {
            selectedCategories.clear();

            filterMode = "all";
            updateChipStyles();
            applyFilters();
        });
        btnClearDateRange.setOnClickListener(v -> {
            startDateMillis = null;
            endDateMillis = null;
            filterMode = "all";
            updateChipStyles();
            applyFilters();
        });
        //do the same thing for event capacity
        btnClearEventCapacity.setOnClickListener(v -> {
            selectedCapacityRange = null;
            filterMode = "all";
            updateChipStyles();
            applyFilters();
        });

    }
    private void setupChips() {
        chipAll.setOnClickListener(v -> {
            filterMode = "all";
            selectedCategories.clear();

            selectedCapacityRange = null;
            startDateMillis = null;
            endDateMillis = null;
            updateChipStyles();
            applyFilters();
        });

        chipOpen.setOnClickListener(v -> {
            filterMode = "open";
            selectedCategories.clear();

            selectedCapacityRange = null;
            startDateMillis = null;
            endDateMillis = null;
            updateChipStyles();
            applyFilters();
        });

        chipDateRange.setOnClickListener(v -> showDateRangePicker());

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
        chipDateRange.setBackgroundResource(R.drawable.bg_filter_dropdown);
        chipDateRange.setTextColor(getColor(R.color.primary));
        chipDateRange.setText("Date Range ▾");
        chipEventCapacity.setBackgroundResource(R.drawable.bg_filter_dropdown);
        chipEventCapacity.setTextColor(getColor(R.color.primary));
        chipEventCapacity.setText("Event Capacity ▾");


        switch (filterMode) {
            case "all":
                chipAll.setBackgroundResource(R.drawable.bg_filter_active);
                chipAll.setTextColor(getColor(R.color.text_white));
                categoryIndicator.setVisibility(View.GONE);
                eventCapacityIndicator.setVisibility(View.GONE);
                dateRangeIndicator.setVisibility(View.GONE);
                break;

            case "open":
                chipOpen.setBackgroundResource(R.drawable.bg_filter_active);
                chipOpen.setTextColor(getColor(R.color.text_white));
                categoryIndicator.setVisibility(View.GONE);
                eventCapacityIndicator.setVisibility(View.GONE);
                dateRangeIndicator.setVisibility(View.GONE);
                break;

            case "category":
                chipCategory.setBackgroundResource(R.drawable.bg_filter_dropdown_active);
                chipCategory.setTextColor(getColor(R.color.text_white));
                String display = String.join(", ", selectedCategories);

                chipCategory.setText(display + " ▾");
                txtActiveCategory.setText(display);

                categoryIndicator.setVisibility(View.VISIBLE);

                eventCapacityIndicator.setVisibility(View.GONE);
                dateRangeIndicator.setVisibility(View.GONE);
                break;

            case "date_range":
                chipDateRange.setBackgroundResource(R.drawable.bg_filter_dropdown_active);
                chipDateRange.setTextColor(getColor(R.color.text_white));
                SimpleDateFormat fmt = new SimpleDateFormat("MMM d", Locale.getDefault());
                String rangeText = fmt.format(new Date(startDateMillis)) + " – " + fmt.format(new Date(endDateMillis));
                chipDateRange.setText(rangeText + " ▾");
                categoryIndicator.setVisibility(View.GONE);
                eventCapacityIndicator.setVisibility(View.GONE);
                dateRangeIndicator.setVisibility(View.VISIBLE);
                txtDateRange.setText(rangeText);
                break;

            case "event_capacity":
                chipEventCapacity.setBackgroundResource(R.drawable.bg_filter_dropdown_active);
                chipEventCapacity.setTextColor(getColor(R.color.text_white));
                chipEventCapacity.setText(selectedCapacityRange + " ▾");
                categoryIndicator.setVisibility(View.GONE);
                eventCapacityIndicator.setVisibility(View.VISIBLE);
                dateRangeIndicator.setVisibility(View.GONE);
                txtEventCapacity.setText(selectedCapacityRange);
                break;
        }
    }

    /**
     * Shows a dialog listing all categories found in the event data.
     * The list is built dynamically — whatever "category" fields exist in Firestore appear here.
     */
    private void showCategoryDialog() {
        if (availableCategories.isEmpty()) {
            Toast.makeText(this, "No categories found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = allCategories;

        boolean[] checkedItems = new boolean[items.length];

        // pre-select already selected ones
        for (int i = 0; i < items.length; i++) {
            checkedItems[i] = selectedCategories.contains(items[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("Filter by categories")
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedCategories.add(items[which]);
                    } else {
                        selectedCategories.remove(items[which]);
                    }
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    if (selectedCategories.isEmpty()) {
                        filterMode = "all";
                    } else {
                        filterMode = "category";
                    }
                    updateChipStyles();
                    applyFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    /**
     * Displays a Material Design date range picker dialog for filtering events by date.
     *
     * Opens a date range selection dialog that allows users to pick a start and end date.
     * Once a range is selected, it updates the filter mode to "date_range", stores the
     * selected dates in milliseconds, refreshes the UI chip styles to show the active
     * date range, and applies the filter to the events list.
     *
     * The selected date range is displayed in "MMM d – MMM d" format on the filter chip.
     */
    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select date range")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            startDateMillis = selection.first;
            endDateMillis = selection.second;
            filterMode = "date_range";
            updateChipStyles();
            applyFilters();
        });

        picker.show(getSupportFragmentManager(), "dateRangePicker");
    }

    private void showEventCapacityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Filter by event capacity")
                .setItems(CAPACITY_RANGES, (dialog, which) -> {
                    selectedCapacityRange = CAPACITY_RANGES[which];
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
            List<String> list = (List<String>) doc.get("categories");
            if (list != null) {
                for (String cat : list) {
                    if (cat != null && !cat.trim().isEmpty()) {
                        cats.add(cat.trim());
                    }
                }
            }
        }

        availableCategories = new ArrayList<>(cats);
    }


    //Filter
    private void applyFilters() {
        String query = editSearch.getText().toString().trim().toLowerCase();
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allDocs) {
            if (Boolean.TRUE.equals(doc.getBoolean("isPrivate"))) {
                continue;
            }
            String title    = doc.getString("title");
            List<String> categories = (List<String>) doc.get("categories");

            String description = doc.getString("description");
            String location = doc.getString("location");

            if (title == null) title = "";
            if (categories == null) categories = new ArrayList<>();;
            if (description == null) description = "";
            if (location == null) location = "";

            //text search across multiple fields, ie title, description, category, location
            if (!query.isEmpty()) {
                boolean matchFound = title.toLowerCase().contains(query) ||
                                   description.toLowerCase().contains(query) ||
                        (categories != null && categories.toString().toLowerCase().contains(query)) ||
                location.toLowerCase().contains(query);

                if (!matchFound) {
                    continue;
                }
            }
            switch (filterMode) {
                case "open":
                    if (!"open".equals(EventStatusUtils.computeStatus(doc))) continue;
                    break;
                case "category":
                    if (categories == null) categories = new ArrayList<>();;

                    boolean match = false;
                    for (String cat : categories) {
                        if (selectedCategories.contains(cat)) {
                            match = true;
                            break;
                        }
                    }

                    if (!match) continue;
                    break;

                case "date_range":
                    Date eventDate = doc.getDate("eventDate");
                    if (eventDate == null) continue;
                    if (startDateMillis != null && eventDate.getTime() < startDateMillis) continue;
                    if (endDateMillis != null && eventDate.getTime() > endDateMillis + 86400000L) continue;
                    break;

                case "event_capacity":
                    Long capVal = doc.getLong("waitlistLimit");
                    int c = (capVal != null) ? capVal.intValue() : 0;
                    if ("Small (1-20)".equals(selectedCapacityRange) && (c < 1 || c > 20)) continue;
                    if ("Medium (21-50)".equals(selectedCapacityRange) && (c < 21 || c > 50)) continue;
                    if ("Large (51+)".equals(selectedCapacityRange) && c < 51) continue;
                    break;
                // "all" -no filtering
            }

            filtered.add(doc);
        }
        renderList(filtered);
    }

    /**
     * Renders the filtered list of event documents into the events container,
     * updating the result count and showing the empty state when no events match.
     *
     * @param docs the filtered list of event documents to display
     */
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

            Date eventDate = doc.getDate("eventDate");
            TextView txtDate = row.findViewById(R.id.txtEventDate);
            if (eventDate != null) {
                SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault());
                txtDate.setText(fmt.format(eventDate));
                txtDate.setVisibility(View.VISIBLE);
            } else {
                txtDate.setVisibility(View.GONE);
            }

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
            row.setAlpha(isOpen ? 1.0f : 0.5f);

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
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_waitlist) {
                Intent intent = new Intent(this, WaitListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_notifications) {
                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
