package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Event detail screen (US 01.01.01 – join waitlist).
 *
 * Receives "event_id" via Intent extra, loads the event from Firestore,
 * and shows all details. The "Join Waitlist Now" button calls
 * FirebaseHelper.joinWaitlist() then navigates to the confirmation screen.
 *
 * If the user is already on this event's waitlist, the button is hidden and a message is shown instead.
 */
public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";

    private TextView txtTitle, txtLocation, txtPrice, txtRegistered;
    private TextView txtTimeRemaining, txtRating, txtDescription;
    private View btnJoin;
    private String eventId, deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        deviceId = DeviceUtils.getDeviceId(this);
        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null) { finish(); return; }

        initViews();
        loadEvent();
        setupBottomNav();
    }

    private void initViews() {
        txtTitle= findViewById(R.id.txtTitle);
        txtLocation = findViewById(R.id.txtLocation);
        txtPrice = findViewById(R.id.txtPrice);
        txtRegistered = findViewById(R.id.txtRegistered);
        txtTimeRemaining = findViewById(R.id.txtTimeRemaining);
        txtRating = findViewById(R.id.txtRating);
        txtDescription = findViewById(R.id.txtDescription);
        btnJoin = findViewById(R.id.btnJoinWaitlist);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnJoin.setOnClickListener(v -> joinWaitlist());
    }

    private void loadEvent() {
        FirebaseHelper.getInstance().getEvent(eventId)
                .addOnSuccessListener(this::populateUI)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event", e);
                    Toast.makeText(this, "Could not load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @SuppressWarnings("unchecked")
    private void populateUI(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = doc.getString("title");
        String location = doc.getString("location");
        String desc = doc.getString("description");
        String status = doc.getString("status");
        Double price = doc.getDouble("price");
        Double rating = doc.getDouble("rating");
        List<String> waitlist = (List<String>) doc.get("waitlistEntrantIds");

        txtTitle.setText(title != null ? title : "");
        txtLocation.setText(location != null ? location : "");
        txtDescription.setText(desc != null ? desc : "");
        txtPrice.setText(price != null ? String.format("$%.0f", price) : "Free");
        txtRating.setText(rating != null ? String.format("%.1f", rating) : "—");

        int count = (waitlist != null) ? waitlist.size() : 0;
        txtRegistered.setText(count + " Registered");

        boolean isOpen = "open".equals(status);
        if (isOpen) {
            txtTimeRemaining.setText("Open to register");
        } else {
            txtTimeRemaining.setText("Registration closed");
        }

        //Check if user is already on the waitlist
        boolean alreadyJoined = waitlist != null && waitlist.contains(deviceId);

        if (alreadyJoined || !isOpen) {
            // Hide join button, show status text instead
            findViewById(R.id.joinButtonContainer).setVisibility(View.GONE);
            //Maybe add message that "You already joined the waitlist"
            //TODO
        }
    }

    // Join Waitlist (US 01.01.01)
    private void joinWaitlist() {
        btnJoin.setEnabled(false);
        String title = txtTitle.getText().toString();
        FirebaseHelper.getInstance().joinWaitlist(
                deviceId, eventId, title,
                task -> {
                    if (task.isSuccessful()) {
                        // Go to confirmation screen
                        Intent intent = new Intent(this, ConfirmationActivity.class);
                        intent.putExtra("event_title", title);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show();btnJoin.setEnabled(true);}
                });
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { finish(); return true; }
            if (id == R.id.nav_waitlist) {
                startActivity(new Intent(this, WaitListActivity.class));
                return true;
            }
            if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications ", Toast.LENGTH_SHORT).show();
                //todo
                return true;
            }
            return false;
        });
    }

}
