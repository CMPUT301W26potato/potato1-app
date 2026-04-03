package com.example.waitwell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.activities.WaitListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EntrantNotificationScreen extends AppCompatActivity {
    private static final String TAG = "EntrantNotificationScreen";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notifications;
    private List<String> notificationIds;  // Track notification IDs for marking as responded
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_notification);

        // Initialize lists
        notifications = new ArrayList<>();
        notificationIds = new ArrayList<>();

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifications);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnHamburger).setOnClickListener(v -> finish());

        // Load notifications from Firestore
        loadNotifications();

        //setup the bottom nav bar
        setupBottomNav();
    }

    private void loadNotifications() {
        //check notification preferences before loading
        //these are based on the checkboxes in notification preferences
        SharedPreferences prefs = getSharedPreferences("NotificationPreferences", MODE_PRIVATE);
        boolean accept = prefs.getBoolean("acceptNotifications", true);
        boolean reject = prefs.getBoolean("rejectNotifications", false);

        if (!accept || reject) {
            notifications.clear();
            notificationIds.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Notifications are disabled", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user's device ID
        String userId = DeviceUtils.getDeviceId(this);
        Log.d(TAG, "Loading notifications for Device ID: " + userId);

        // Query notifications from Firestore
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("responded", false)  // Only show unresponded notifications
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        notifications.clear();
                        notificationIds.clear();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<NotificationModel> visibleNotifications = new ArrayList<>();
                    List<String> visibleIds = new ArrayList<>();
                    AtomicInteger pending = new AtomicInteger(querySnapshot.size());

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification == null) {
                            if (pending.decrementAndGet() == 0) {
                                finalizeNotificationLoad(visibleNotifications, visibleIds);
                            }
                            continue;
                        }
                        String eventId = notification.getEventId();
                        if (TextUtils.isEmpty(eventId)) {
                            visibleNotifications.add(notification.toNotificationModel());
                            visibleIds.add(doc.getId());
                            doc.getReference().update("read", true);
                            if (pending.decrementAndGet() == 0) {
                                finalizeNotificationLoad(visibleNotifications, visibleIds);
                            }
                            continue;
                        }
                        FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(eventId)
                                .get()
                                .addOnCompleteListener(task -> {
                                    NotificationModel model = notification.toNotificationModel();
                                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                        DocumentSnapshot eventDoc = task.getResult();
                                        String lifecycle = EventStatusUtils.computeStatus(eventDoc);
                                        if ("completed".equalsIgnoreCase(lifecycle)) {
                                            model.setExpired(true);
                                        }
                                        Date regClose = EventStatusUtils.getRegistrationCloseDate(eventDoc);
                                        boolean inviteStale = "CHOSEN".equals(notification.getType())
                                                && regClose != null
                                                && new Date().after(regClose);
                                        if (inviteStale) {
                                            model.setExpired(true);
                                        }
                                    }
                                    visibleNotifications.add(model);
                                    visibleIds.add(doc.getId());
                                    // Mark as read when it appears in the feed.
                                    doc.getReference().update("read", true);
                                    if (pending.decrementAndGet() == 0) {
                                        finalizeNotificationLoad(visibleNotifications, visibleIds);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }

    private void finalizeNotificationLoad(List<NotificationModel> visibleNotifications, List<String> visibleIds) {
        runOnUiThread(() -> {
            notifications.clear();
            notifications.addAll(visibleNotifications);
            notificationIds.clear();
            notificationIds.addAll(visibleIds);
            adapter.notifyDataSetChanged();

            if (notifications.isEmpty()) {
                Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * setup real-time listener for notifications.
     *  will automatically update the ui when new notifications conme in
     *
     */
    private void setupRealtimeListener() {
        SharedPreferences prefs = getSharedPreferences("NotificationPreferences", MODE_PRIVATE);
        boolean accept = prefs.getBoolean("acceptNotifications", true);
        boolean reject = prefs.getBoolean("rejectNotifications", false);

        if (!accept || reject) {
            notifications.clear();
            notificationIds.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        String userId = DeviceUtils.getDeviceId(this);

        notificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("responded", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        notifications.clear();
                        notificationIds.clear();

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Notification notification = doc.toObject(Notification.class);
                            if (notification != null) {
                                NotificationModel model = notification.toNotificationModel();
                                notifications.add(model);
                                notificationIds.add(doc.getId());
                            }
                        }

                        adapter.notifyDataSetChanged();

                        if (notifications.isEmpty()) {
                            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    /**
     *  Refresh notifications when returning to this screen
     */
    @Override
    protected void onResume() {
        super.onResume();

        loadNotifications();
    }
    /**
     * when the screen is closed shut down all the listeners
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener if using real-time updates
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    /**
     * Get the notification ID at a specific position.
     * Used by NotificationAdapter to mark notifications as responded.
     */
    public String getNotificationId(int position) {
        if (position >= 0 && position < notificationIds.size()) {
            return notificationIds.get(position);
        }
        return null;
    }

    /**
     * Configures the bottom navigation bar for Entrants. Home keeps you
     * on this screen, the waitlist item sends you to {@link WaitListActivity},
     * and the notifications item opens {@link EntrantNotificationScreen}.
     * Keeping this routing in one place makes it easy to see Entrant flows.
     */
    private void setupBottomNav() {

        BottomNavigationView nav = findViewById(R.id.bottomNavigation);

        // Notifications is the primary screen for this activity.
        nav.setSelectedItemId(R.id.nav_notifications);

        nav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, com.example.waitwell.activities.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_waitlist) {
                Intent intent = new Intent(this, com.example.waitwell.activities.WaitListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_notifications) {
                return true; // already here
            }

            return false;
        });
    }
}
