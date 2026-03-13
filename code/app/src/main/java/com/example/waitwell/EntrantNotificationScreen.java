package com.example.waitwell;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
     * Setup real-time listener for notifications.
     * This will automatically update the UI when new notifications arrive.
     * Uncomment if you want real-time updates.
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notifications when returning to this screen
        loadNotifications();
        // Or use real-time listener: setupRealtimeListener();
    }

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
}
