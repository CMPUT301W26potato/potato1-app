package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Entrant-facing main menu screen that shows featured events and navigation.
 * This is the default landing page for the Entrant side of the app and does
 * not mix in Organizer or Admin views. It pulls events from Firestore and
 * renders them as a horizontal card row plus a popular-events list.
 * From a user story point of view this backs the general "browse and
 * discover events" experience before an entrant decides to join.
 * I also leaned on ChatGPT and the Mockito tutorial at
 * https://www.bacancytechnology.com/blog/unit-testing-using-mockito-in-android
 * when designing the small static helper methods that MainActivityTest mocks.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private LinearLayout eventCardsContainer;
    private LinearLayout popularEventsContainer;

    /** All events fetched from Firestore. Kept in memory for quick access. */
    private List<DocumentSnapshot> allEventDocs = new ArrayList<>();

    /**
     * Standard Android lifecycle entry point. Wires up the main layout,
     * grabs references to the key containers, attaches click listeners
     * and bottom navigation, and finally kicks off the initial event load.
     * This assumes the user reached here via the Entrant flow.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        eventCardsContainer = findViewById(R.id.eventCardsContainer);
        popularEventsContainer = findViewById(R.id.popularEventsContainer);

        setupClickListeners();
        setupBottomNav();
        loadEvents();
    }

    /**
     * Helper that mirrors the "title" fallback logic used in {@link #onEventsLoaded},
     * but is kept static and Android-free so we can test it with Mockito.
     */
    static String getEventTitle(DocumentSnapshot doc) {
        String title = doc.getString("title");
        if (title == null) {
            return "Untitled Event";
        }
        return title;
    }

    /**
     * Helper for organizer name fallback. Null organizer becomes an empty string so
     * the UI does not have to handle a null reference every time.
     */
    static String getOrganizerName(DocumentSnapshot doc) {
        String organizer = doc.getString("organizerName");
        if (organizer == null) {
            return "";
        }
        return organizer;
    }

    /**
     * Helper that formats the price from a Firestore document into a small
     * user-facing string. Null price is treated as "Free".
     */
    static String getPriceText(DocumentSnapshot doc) {
        Double priceObj = doc.getDouble("price");
        return priceObj != null ? String.format("$%.2f", priceObj) : "Free";
    }

    /**
     * Determines whether the event should be treated as open based on its
     * stored status string. This keeps the literal "open" comparison in one place.
     */
    static boolean isOpen(DocumentSnapshot doc) {
        String status = doc.getString("status");
        return "open".equals(status);
    }

    /**
     * Starts the async Firestore call to fetch all events. On success we
     * hand off to {@link #onEventsLoaded(QuerySnapshot)}, and on failure
     * we show a simple "could not load events" toast.
     */
    private void loadEvents() {
        FirebaseHelper.getInstance().getAllEvents()
                .addOnSuccessListener(this::onEventsLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events", e);
                    Toast.makeText(this,
                            "Could not load events – check your internet connection",
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Called when Firestore returns the events query.
     * Builds the horizontal card row and the popular-events list rows
     * using a small subset of fields (title, organizer, price, open/closed).
     * This is Entrant-only – organizers have their own home screen that
     * focuses on "My Events" instead.
     */
    private void onEventsLoaded(QuerySnapshot snapshot) {
        allEventDocs = snapshot.getDocuments();
        // Clear any previous views
        eventCardsContainer.removeAllViews();
        popularEventsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : allEventDocs) {
            String title = getEventTitle(doc);
            String organizer = getOrganizerName(doc);
            String priceText = getPriceText(doc);
            boolean isOpen = isOpen(doc);
            String eventId = doc.getId();

            // Build a horizontal card
            View card = inflater.inflate(R.layout.item_event_card, eventCardsContainer, false);
            ((TextView) card.findViewById(R.id.txtCardTitle)).setText(title);
            ((TextView) card.findViewById(R.id.txtCardOrganizer)).setText(organizer);
            ((TextView) card.findViewById(R.id.txtCardPrice)).setText(priceText);
            // TODO: load poster image if imageUrl is set
            card.setOnClickListener(v -> onEventCardClicked(eventId));
            eventCardsContainer.addView(card);

            //Build a popular-event list row
            View row = inflater.inflate(R.layout.item_event_row, popularEventsContainer, false);
            ((TextView) row.findViewById(R.id.txtRowName)).setText(title);

            TextView badge = row.findViewById(R.id.txtRowStatus);
            if (isOpen) {
                badge.setText("Open");
                badge.setBackgroundResource(R.drawable.bg_status_open);
                badge.setTextColor(getColor(R.color.status_open_text));
            } else {
                badge.setText("Closed");
                badge.setBackgroundResource(R.drawable.bg_status_closed);
                badge.setTextColor(getColor(R.color.status_closed_text));
            }
            row.setOnClickListener(v -> onEventCardClicked(eventId));
            popularEventsContainer.addView(row);
        }

        if (allEventDocs.isEmpty()) {
            Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Called when the user taps an event card or row.
     * We launch {@link EventDetailActivity} with the selected id so the
     * entrant can see the full description and decide whether to join.
     */
    private void onEventCardClicked(String eventId) {
        Intent i = new Intent(this, EventDetailActivity.class);
        i.putExtra("event_id", eventId);
        startActivity(i);
    }



    /**
     * Wires up all of the tappable UI pieces on the main screen –
     * search bar, history chip, QR scanner button, "view all" link,
     * and the three little filter tabs. Right now most of these just
     * show toasts or simple navigation hooks.
     */
    private void setupClickListeners() {
        // Search bar tap
        findViewById(R.id.searchBar).setOnClickListener(v ->
                Toast.makeText(this, "Search ", Toast.LENGTH_SHORT).show());

        // History chip
        findViewById(R.id.chipHistory).setOnClickListener(v ->
                Toast.makeText(this, "History", Toast.LENGTH_SHORT).show());

        // Scan QR Code
        Button btnScan = findViewById(R.id.btnScanQr);
        btnScan.setOnClickListener(v ->
                Toast.makeText(this, "QR Scanner", Toast.LENGTH_SHORT).show());

        // Hamburger menu -> small overflow with Log out
        View hamburger = findViewById(R.id.btnHamburger);
        hamburger.setOnClickListener(this::showHamburgerMenu);

        // "View all" link
        findViewById(R.id.btnViewAll).setOnClickListener(v ->
                startActivity(new Intent(this, AllEventsActivity.class)));

        // Filter tabs – toggle selected state
        TextView tabMostViewed = findViewById(R.id.tabMostViewed);
        TextView tabNearby = findViewById(R.id.tabNearby);
        TextView tabLatest = findViewById(R.id.tabLatest);

        tabMostViewed.setOnClickListener(v -> selectTab(tabMostViewed, tabNearby, tabLatest));
        tabNearby.setOnClickListener(v -> selectTab(tabNearby, tabMostViewed, tabLatest));
        tabLatest.setOnClickListener(v -> selectTab(tabLatest, tabMostViewed, tabNearby));
    }

    /**
     * Shows the overflow (hamburger) menu for the entrant home screen.
     * Right now it mainly exposes a "Log out" action that kicks the user
     * back to {@link RegisterActivity} so they can pick their role again.
     */
    private void showHamburgerMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_main_hamburger, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }
    /**
     * Visually selects one filter tab and deselects the others.
     * This is purely styling – the actual event filtering behaviour
     * can be layered on top later without changing this helper.
     */
    private void selectTab(TextView selected, TextView... others) {
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(getColor(R.color.text_white));
        for (TextView t : others) {
            t.setBackgroundResource(R.drawable.bg_chip);
            t.setTextColor(getColor(R.color.text_hint));
        }
    }

    /**
     * Configures the bottom navigation bar for Entrants. Home keeps you
     * on this screen, the waitlist item sends you to {@link WaitListActivity},
     * and the notifications item opens {@link EntrantNotificationScreen}.
     * Keeping this routing in one place makes it easy to see Entrant flows.
     */
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_waitlist) {
                startActivity(new Intent(this, WaitListActivity.class));
                return true;
            } else if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();

                //go to the notifications screen
                //use intent to do this
                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                startActivity(intent);

                return true;
            }
            return false;
        });
    }
}
