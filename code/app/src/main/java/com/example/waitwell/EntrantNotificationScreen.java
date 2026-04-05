package com.example.waitwell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.activities.EntrantLotteryCriteria;
import com.example.waitwell.activities.MainActivity;
import com.example.waitwell.activities.RegisterActivity;
import com.example.waitwell.activities.WaitListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
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
    private TextView txtEmptyNotifications;
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
        txtEmptyNotifications = findViewById(R.id.txtEmptyNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifications);
        recyclerView.setAdapter(adapter);

        setupDrawer();

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
            updateNotificationEmptyState(true);
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
                        updateNotificationEmptyState(true);
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
            updateNotificationEmptyState(notifications.isEmpty());

            if (notifications.isEmpty()) {
                Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNotificationEmptyState(boolean empty) {
        if (txtEmptyNotifications == null) {
            return;
        }
        txtEmptyNotifications.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
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
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        if (nav != null) nav.setSelectedItemId(R.id.nav_notifications);
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

    private void setupDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.btnHamburger).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_delete_profile) {
                showDeleteProfileDialog();
            } else if (id == R.id.nav_notification_options) {
                startActivity(new Intent(this, EntrantNotificationOptions.class));
            } else if (id == R.id.nav_lottery_selection_criteria) {
                startActivity(new Intent(this, EntrantLotteryCriteria.class));
            } else if (id == R.id.nav_logout) {
                startActivity(new Intent(this, RegisterActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });
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

    private void showDeleteProfileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserProfile() {
        String userId = DeviceUtils.getDeviceId(this);
        FirebaseHelper.getInstance().deleteUser(userId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("waitwell_prefs", MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences("WaitWellPrefs", MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(this, RegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }
}
