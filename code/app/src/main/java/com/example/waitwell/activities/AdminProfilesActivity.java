package com.example.waitwell.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
/**
 * AdminProfilesActivity allows administrators to view
 * and manage all user profiles stored in Firestore.
 *
 * Profiles are displayed in a RecyclerView grid.
 * Each profile card shows:
 * - name
 * - role
 * - email
 * - Firestore document ID
 *
 * Administrators can remove profiles from the system.
 *
 * AI Usage:
 *
 * @author Grace Shin
 */
public class AdminProfilesActivity extends AppCompatActivity {

    RecyclerView recyclerProfiles;
    private AdminProfilesAdapter adapter;
    private List<DocumentSnapshot> profiles;
    /**
     * Initializes the activity and loads profile data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profiles);

        recyclerProfiles = findViewById(R.id.recyclerProfiles);
        // Display profiles in 2 column grid
        recyclerProfiles.setLayoutManager(new GridLayoutManager(this, 2));

        loadProfiles();
        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
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
    /**
     * Retrieves all user profiles from Firestore and populates the RecyclerView.
     */
    private void loadProfiles() {

        FirebaseHelper.getInstance()
                .getAllProfiles()
                .addOnSuccessListener(snapshot -> {

                    profiles = snapshot.getDocuments();

// Load events too
                    FirebaseHelper.getInstance().getAllEvents()
                            .addOnSuccessListener(eventSnapshot -> {

                                List<DocumentSnapshot> events = eventSnapshot.getDocuments();

                                adapter = new AdminProfilesAdapter(
                                        this,
                                        profiles,
                                        events,
                                        this::removeProfile
                                );

                                recyclerProfiles.setAdapter(adapter);

                                setupSearch();
                                setupFilters();
                            });

                    recyclerProfiles.setAdapter(adapter);

                    setupSearch();
                    setupFilters();
                });
    }
    private void setupSearch() {
        EditText search = findViewById(R.id.searchProfiles);

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filterSearch(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }
    private void setupFilters() {

        Button btnAll = findViewById(R.id.btnAll);
        Button btnEntrants = findViewById(R.id.btnEntrants);
        Button btnOrganizers = findViewById(R.id.btnOrganizers);
        Button btnAdmins = findViewById(R.id.btnAdmins);


        btnAll.setOnClickListener(v -> adapter.filterRole("All"));
        btnEntrants.setOnClickListener(v -> adapter.filterRole("entrant"));
        btnOrganizers.setOnClickListener(v -> adapter.filterRole("organizer"));
        btnAdmins.setOnClickListener(v -> adapter.filterRole("admin"));


    }
    /**
     * Removes a user profile from Firestore.
     */
    private void removeProfile(String userId) {

        FirebaseHelper.getInstance()
                .removeProfile(userId)
                .addOnSuccessListener(unused -> {
                    // Navigate to confirmation page
                    Intent intent = new Intent(
                            this,
                            AdminProfileRemovedActivity.class
                    );

                    startActivity(intent);
                });
    }
}
