package com.example.waitwell.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.waitwell.R;

/**
 * AdminEventRemovedActivity displays a confirmation screen
 * after an administrator removes an event from the system.
 *
 * The screen simply informs the admin that the event has been
 * successfully deleted and allows them to return to the previous
 * screen using the back button.
 *
 * This activity is launched from AdminEventsActivity after
 * a successful Firestore deletion operation.
 *
 * AI Usage:
 *
 * @author Grace Shin
 */
public class AdminEventRemovedActivity extends AppCompatActivity {
    /**
     * Called when the activity is created.
     * Initializes the confirmation layout and sets up the back button functionality.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the confirmation layout for deleted events
        setContentView(R.layout.activity_admin_event_deleted);
        String imageUrl = getIntent().getStringExtra("image_url");

        ImageView img = findViewById(R.id.imgEvent);

        Glide.with(this)
                .load(imageUrl)
                .onlyRetrieveFromCache(true)
                .into(img);


        // Back button simply closes this activity and returns to the previous admin page
        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
        });
    }
}