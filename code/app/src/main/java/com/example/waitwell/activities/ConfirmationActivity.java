package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.R;

/**
 * Entrant confirmation screen shown after joining a waitlist, with quick navigation
 * back to waitlist or home.
 *
 * Addresses: US 01.05.07 - Entrant: Accept/Decline Private Event
 *
 * @author Karina Zhang
 * @version 1.0
 * @see WaitListActivity
 */
public class ConfirmationActivity extends AppCompatActivity {

    /**
     * Shows confirmation details and wires navigation buttons back to waitlist/home.
     *
     * @param savedInstanceState restore bundle, can be null
     * @author Karina Zhang
     */
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
