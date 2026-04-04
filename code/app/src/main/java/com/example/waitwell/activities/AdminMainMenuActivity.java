package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * AdminMainMenuActivity is the main dashboard for administrators.
 *
 * It provides navigation to administrative tools such as:
 * - Viewing all events
 * - Viewing all user profiles
 *
 * Each option is represented by a clickable layout element.
 *
 * AI Usage:
 *
 * @author Grace Shin
 */
public class AdminMainMenuActivity extends AppCompatActivity {

    LinearLayout btnEvents;
    LinearLayout btnProfiles;
    LinearLayout btnImages;
    LinearLayout btnNotifications;

    /**
     * Initializes the admin menu and sets navigation listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main_menu);

        btnEvents = findViewById(R.id.btnAllEvents);
        btnProfiles = findViewById(R.id.btnAllProfiles);
        btnNotifications = findViewById(R.id.btnAllNotifications);


        btnImages = findViewById(R.id.btnAllImages);
        // Navigate to events management screen
        btnEvents.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminEventsActivity.class));
        });
        // Navigate to profiles management screen
        btnProfiles.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminProfilesActivity.class));
        });
        // Navigate to images management screen
        btnImages.setOnClickListener((v -> {
            startActivity(new Intent(this, AdminImagesActivity.class));
        }));
        // Navigate to notifications screen
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminNotificationsActivity.class));
        });

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
}
