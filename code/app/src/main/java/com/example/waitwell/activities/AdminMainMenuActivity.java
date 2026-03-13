package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.R;

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

    /**
     * Initializes the admin menu and sets navigation listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main_menu);

        btnEvents = findViewById(R.id.btnAllEvents);
        btnProfiles = findViewById(R.id.btnAllProfiles);
        // Navigate to events management screen
        btnEvents.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminEventsActivity.class));
        });
        // Navigate to events management screen
        btnProfiles.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminProfilesActivity.class));
        });
    }
}
