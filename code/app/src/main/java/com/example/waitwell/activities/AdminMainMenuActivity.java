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
 * AdminMainMenuActivity represents the main dashboard screen for administrators.
 *
 * This activity allows admins to navigate to different management sections of the app,
 * including events, user profiles, images, and notifications.
 *
 * It also includes a navigation drawer that allows switching between roles
 * (entrant and organizer) based on the current device.
 *
 * This class mainly handles UI interactions and redirects users to the appropriate screens.
 *
 * @author Grace Shin
 */
public class AdminMainMenuActivity extends AppCompatActivity {

    // buttons for navigating to different admin sections
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

        // link UI elements from XML layout
        btnEvents = findViewById(R.id.btnAllEvents);
        btnProfiles = findViewById(R.id.btnAllProfiles);
        btnNotifications = findViewById(R.id.btnAllNotifications);
        btnImages = findViewById(R.id.btnAllImages);

        // navigate to events management screen
        btnEvents.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminEventsActivity.class));
        });
        // navigate to profiles management screen
        btnProfiles.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminProfilesActivity.class));
        });
        // navigate to images management screen
        btnImages.setOnClickListener((v -> {
            startActivity(new Intent(this, AdminImagesActivity.class));
        }));
        // navigate to notifications screen
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminNotificationsActivity.class));
        });

        DrawerLayout drawerLayout = findViewById(R.id.admin_drawer_layout);
        NavigationView navigationView = findViewById(R.id.admin_navigation_view);

        // open drawer on hamburger click
        findViewById(R.id.btnHamburger).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        // handle navigation menu item clicks
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

    /**
     * Switches the current user to the entrant role.
     *
     * Checks Firebase to see if a user with this device ID already exists
     * as an entrant:
     * - If yes, go to MainActivity (entrant home)
     * - If no, go to RegisterActivity to create entrant profile
     */
    private void switchToEntrant() {
        // get unique device ID for this phone
        String deviceId = DeviceUtils.getDeviceId(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // query users collection for matching device ID and entrant role
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

    /**
     * Switches the current user to the organizer role.
     *
     * Checks Firebase to see if a user with this device ID already exists
     * as an organizer:
     * - If yes, go to OrganizerEntryActivity
     * - If no, go to RegisterActivity to create organizer profile
     */
    private void switchToOrganizer() {
        // get unique device ID for this phone
        String deviceId = DeviceUtils.getDeviceId(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // query users collection for matching device ID and organizer role
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
