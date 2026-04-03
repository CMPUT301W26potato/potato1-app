package com.example.waitwell.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.Notification;
import com.example.waitwell.NotificationAdapter;
import com.example.waitwell.NotificationModel;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminNotificationsActivity extends AppCompatActivity {

    private static final String TAG = "AdminNotifications";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notifications);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifications);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadAllNotifications();
    }

    private void loadAllNotifications() {

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    notifications.clear();

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No notifications found", Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        Notification notification = doc.toObject(Notification.class);

                        if (notification != null) {
                            NotificationModel model = notification.toNotificationModel();
                            //show user id
                            model.setMessage(
                                    model.getMessage() + "\n(User: " + notification.getUserId() + ")"
                            );

                            notifications.add(model);
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }
}