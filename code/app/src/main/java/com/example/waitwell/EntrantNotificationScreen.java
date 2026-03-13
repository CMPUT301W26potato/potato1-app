package com.example.waitwell;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.activities.WaitListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

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

        // Load notifications from Firestore
        loadNotifications();

        //setup the bottom nav bar
        setupBottomNav();
    }

    private void loadNotifications() {
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
                    notifications.clear();
                    notificationIds.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            // Convert to NotificationModel for the adapter
                            NotificationModel model = notification.toNotificationModel();
                            notifications.add(model);
                            notificationIds.add(doc.getId());

                            // Mark as read
                            doc.getReference().update("read", true);
                        }
                    }

                    // Update UI
                    adapter.notifyDataSetChanged();

                    // Show empty state if no notifications
                    if (notifications.isEmpty()) {
                        Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * setup real-time listener for notifications.
     *  will automatically update the ui when new notifications conme in
     *
     */
    private void setupRealtimeListener() {
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

        nav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            }

            else if (id == R.id.nav_waitlist) {
                startActivity(new Intent(this, WaitListActivity.class));
                return true;
            }

            else if (id == R.id.nav_notifications) {

                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                startActivity(intent);

                return true;
            }

            return false;
        });
    }
}
