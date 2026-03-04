package com.example.waitwell.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main menu screen matching the "Entrant - Main Menu" mockup.
 * Currently shows a static layout.
 * Event data is hardcoded in the XML for now;
 * this will later be populated from Firebase.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupClickListeners();
        setupBottomNav();
    }

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

        // Hamburger menu
        findViewById(R.id.btnHamburger).setOnClickListener(v ->
                Toast.makeText(this, "Settings menu ", Toast.LENGTH_SHORT).show());

        // "View all" link
        findViewById(R.id.btnViewAll).setOnClickListener(v ->
                Toast.makeText(this, "All Events ", Toast.LENGTH_SHORT).show());

        // Filter tabs – toggle selected state
        TextView tabMostViewed = findViewById(R.id.tabMostViewed);
        TextView tabNearby = findViewById(R.id.tabNearby);
        TextView tabLatest = findViewById(R.id.tabLatest);

        tabMostViewed.setOnClickListener(v -> selectTab(tabMostViewed, tabNearby, tabLatest));
        tabNearby.setOnClickListener(v -> selectTab(tabNearby, tabMostViewed, tabLatest));
        tabLatest.setOnClickListener(v -> selectTab(tabLatest, tabMostViewed, tabNearby));
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
                // already here
                return true;
            } else if (id == R.id.nav_waitlist) {
                Toast.makeText(this, "Wait List ", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
}
