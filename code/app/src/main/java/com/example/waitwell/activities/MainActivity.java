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
 * Main menu screen – now loads events from Firestore.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private LinearLayout eventCardsContainer;
    private LinearLayout popularEventsContainer;

    /** All events fetched from Firestore. Kept in memory. */
    private List<DocumentSnapshot> allEventDocs = new ArrayList<>();

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

    //  Firebase: Load events
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
     * Builds the horizontal card row and the popular-events list rows.
     */
    private void onEventsLoaded(QuerySnapshot snapshot) {
        allEventDocs = snapshot.getDocuments();
        // Clear any previous views
        eventCardsContainer.removeAllViews();
        popularEventsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : allEventDocs) {
            String title = doc.getString("title");
            String organizer = doc.getString("organizerName");
            Double priceObj = doc.getDouble("price");
            String status = doc.getString("status");
            String eventId = doc.getId();

            if (title == null) title = "Untitled Event";
            if (organizer == null) organizer = "";
            String priceText = priceObj != null ? String.format("$%.2f", priceObj) : "Free";
            boolean isOpen = "open".equals(status);

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


    /** Called when user taps an event card or row. */
    private void onEventCardClicked(String eventId) {
        Intent i = new Intent(this, EventDetailActivity.class);
        i.putExtra("event_id", eventId);
        startActivity(i);
    }



    private void setupClickListeners() {
        // Search bar tap
        findViewById(R.id.searchBar).setOnClickListener(v ->
                Toast.makeText(this, "Search ", Toast.LENGTH_SHORT).show());

        // History chip
        findViewById(R.id.chipHistory).setOnClickListener(v -> openRegistrationHistory());

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

    /** Shows a popup anchored to the hamburger with a Log out action. */
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
     * Visually selects one tab and deselects the others.
     */
    private void selectTab(TextView selected, TextView... others) {
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(getColor(R.color.text_white));
        for (TextView t : others) {
            t.setBackgroundResource(R.drawable.bg_chip);
            t.setTextColor(getColor(R.color.text_hint));
        }
    }

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
    private void showDeleteProfileDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteUserProfile();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void deleteUserProfile() {
        // get stored user id
        SharedPreferences prefs = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "No user profile found", Toast.LENGTH_SHORT).show();

            // navigate to registeractivity anyway
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // delete user from firestore
        FirebaseHelper.getInstance().deleteUser(userId)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();

                    // clear stored user ID so SplashActivity doesnt auto login
                    prefs.edit().remove("userId").apply();

                    // send user back to register screen
                    Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }
    // Add this inside MainActivity
    private void logoutToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    private void openRegistrationHistory() {
        // Get the user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "No user profile found", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }

        Intent intent = new Intent(this, RegistrationHistoryActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}
