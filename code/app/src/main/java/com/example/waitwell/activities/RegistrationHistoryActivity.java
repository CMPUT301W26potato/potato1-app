package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RegistrationHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private TextView tvEmpty;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_history); // your XML with historyContainer

        historyContainer = findViewById(R.id.historyContainer);
        tvEmpty = findViewById(R.id.tvEmpty);

        // back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // bottom navigation
        setupBottomNav();

        deviceId = DeviceUtils.getDeviceId(this); // initializing deviceId here

        loadHistory();
    }
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            }
            if (id == R.id.nav_waitlist) {
                startActivity(new Intent(this, WaitListActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            }
            if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, EntrantNotificationScreen.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            }
            return false;
        });
    }

    private void loadHistory() {
        FirebaseHelper.getInstance().getUserWaitlistEntries(deviceId)
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> allEntries = snap.getDocuments();

                    List<DocumentSnapshot> filtered = new ArrayList<>();
                    for (DocumentSnapshot doc : allEntries) {
                        String status = doc.getString("status");
                        if (status != null && (
                                status.equalsIgnoreCase("selected") ||
                                        status.equalsIgnoreCase("confirmed") ||
                                        status.equalsIgnoreCase("rejected"))) {
                            filtered.add(doc);
                        }
                    }

                    renderHistory(filtered);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load history", Toast.LENGTH_SHORT).show();
                });
    }

    private void renderHistory(List<DocumentSnapshot> filteredEntries) {
        historyContainer.removeAllViews();

        if (filteredEntries.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < filteredEntries.size(); i++) {
            DocumentSnapshot doc = filteredEntries.get(i);
            String title = doc.getString("eventTitle");
            String status = doc.getString("status");

            String displayStatus;
            if ("selected".equalsIgnoreCase(status) || "confirmed".equalsIgnoreCase(status)) {
                displayStatus = "Selected";
            } else if ("rejected".equalsIgnoreCase(status)) {
                displayStatus = "Not Selected";
            } else {
                continue;
            }

            View row = inflater.inflate(R.layout.item_waitlist_entry, historyContainer, false);

            ((TextView) row.findViewById(R.id.txtNumber)).setText((i + 1) + ".");
            ((TextView) row.findViewById(R.id.txtEntryTitle)).setText(title != null ? title : "Unknown Event");

            TextView badge = row.findViewById(R.id.txtEntryStatus);
            badge.setText(displayStatus);
            if ("Selected".equals(displayStatus)) {
                badge.setBackgroundResource(R.drawable.bg_status_selected_primary);
                badge.setTextColor(getColor(R.color.text_white));
            } else {
                badge.setBackgroundResource(R.drawable.bg_status_closed);
                badge.setTextColor(getColor(R.color.status_closed_text));
            }

            historyContainer.addView(row);
        }
    }
}