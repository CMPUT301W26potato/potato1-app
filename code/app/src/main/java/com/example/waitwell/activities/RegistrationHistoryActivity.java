package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class RegistrationHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_history);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        historyContainer = findViewById(R.id.historyContainer);
        tvEmpty = findViewById(R.id.tvEmpty);

        // bottom navigation setup i got from event details
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
        String userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        loadRegistrationHistory(userId);
    }

    private void loadRegistrationHistory(String userId) {
        FirebaseHelper.getInstance()
                .getUserRegistrations(userId)
                .addOnSuccessListener(this::displayHistory)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load registration history",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void displayHistory(List<DocumentSnapshot> regDocs) {

        historyContainer.removeAllViews();

        if (regDocs.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : regDocs) {

            String eventTitle = doc.getString("eventTitle");
            String status = doc.getString("status");

            View row = inflater.inflate(
                    R.layout.item_registration,
                    historyContainer,
                    false
            );

            TextView txtEventTitle = row.findViewById(R.id.txtEventTitle);
            TextView txtStatus = row.findViewById(R.id.txtStatus);

            txtEventTitle.setText(eventTitle != null ? eventTitle : "Unknown Event");
            txtStatus.setText(status != null ? status : "Unknown");

            historyContainer.addView(row);
        }
    }
}