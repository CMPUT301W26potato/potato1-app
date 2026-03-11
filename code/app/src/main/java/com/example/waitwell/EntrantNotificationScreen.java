package com.example.waitwell;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EntrantNotificationScreen extends AppCompatActivity {
    //setup a list of notifications
    //create ui elements based on the notifications
    //link to chosen or not chosen screens for all notifications

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_notification);

        //list rn, firestore later

        List<NotificationModel> notifications = new ArrayList<>();
        //add notifications here
        notifications.add(new NotificationModel("Event 1", "You have been chosen to attend the event", "Accept", NotificationModel.NotificationType.CHOSEN));


        //connect to the recycler view
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new NotificationAdapter(notifications));


    }
}
