package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.waitwell.EntrantNotificationOptions;
import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class EntrantLotteryCriteria extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_lottery_criteria);

        // Drawer setup for the hamburger menu
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.btnHamburger).setOnClickListener(v -> finish());

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_notification_options) {
                startActivity(new Intent(this, EntrantNotificationOptions.class));
            } else if (id == R.id.nav_delete_profile) {
                // handle delete profile if needed
            } else if (id == R.id.nav_lottery_selection_criteria) {
                // already on this screen
            } else if (id == R.id.nav_logout) {
                startActivity(new Intent(this, RegisterActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // bottom navbar setup!!
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_waitlist) {
                Intent intent = new Intent(this, WaitListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_notifications) {
                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
