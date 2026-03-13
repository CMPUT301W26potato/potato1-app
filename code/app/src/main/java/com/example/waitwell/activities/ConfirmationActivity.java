package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.R;

/**
 * Shown after successfully joining a waitlist.
 * Displays the event name and "You're on the waiting list!"
 * with a button to go to the WaitListActivity.
 */
public class ConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        String eventTitle = getIntent().getStringExtra("event_title");
        if (eventTitle == null) eventTitle = "Event";

        ((TextView) findViewById(R.id.txtEventName)).setText(eventTitle);
        findViewById(R.id.btnMyWaitlist).setOnClickListener(v -> {
            startActivity(new Intent(this, WaitListActivity.class));
            finish();
        });

        // Back arrow - go back to main
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
