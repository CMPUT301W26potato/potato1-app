package com.example.waitwell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.activities.WaitListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Entrant settings screen for notification preferences and bottom-nav routing.
 * This supports the entrant notification and response flow settings.
 *
 * Addresses: US 01.05.06 - Entrant: Private Event Invite Notification, US 01.05.07 - Entrant: Accept/Decline Private Event
 *
 * @author Karina Zhang
 * @version 1.0
 * @see EntrantNotificationScreen
 */
public class EntrantNotificationOptions extends AppCompatActivity {
    /*
     * Used Gemini to figure out how to query Firestore for a specific
     * user's notifications and sort them by timestamp without it getting
     * weird. Also talked through how to handle the UI update when a
     * notification gets tapped and the entrant has already responded.
     *
     *
     * Sites I looked at:
     *
     * Firestore queries - whereEqualTo and orderBy used together:
     * https://firebase.google.com/docs/firestore/query-data/queries
     *
     * RecyclerView with Firestore - how to bind live data to a list:
     * https://developer.android.com/reference/com/firebase/ui/firestore/FirestoreRecyclerAdapter
     *
     * Handling click events inside a RecyclerView adapter:
     * https://developer.android.com/guide/topics/ui/layout/recyclerview#click-listener
     */
    //the user can accept or not accept notifications

    private CheckBox acceptNotificationCheckBox;
    private CheckBox rejectNotificationCheckBox;

    private SharedPreferences prefs;

    /**
     * Sets up notification option toggles and bottom navigation.
     *
     * @param savedInstanceState restore bundle, can be null
     * @author Karina Zhang
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_options);
        //do the checkboxes, make them persistent on launch and exit
        prefs = getSharedPreferences("NotificationPreferences",MODE_PRIVATE);
        acceptNotificationCheckBox = findViewById(R.id.acceptNotificationCheckBox);
        rejectNotificationCheckBox = findViewById(R.id.rejectNotificationCheckBox);
        //load saved state
        acceptNotificationCheckBox.setChecked(prefs.getBoolean("acceptNotifications",true));
        rejectNotificationCheckBox.setChecked(prefs.getBoolean("rejectNotifications",false));

        //save on toggle
        acceptNotificationCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("acceptNotifications", isChecked).apply();
        });

        rejectNotificationCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("rejectNotifications", isChecked).apply();
        });

        findViewById(R.id.btnHamburger).setOnClickListener(v -> finish());

        //setup the bottom nav bar
        setupBottomNav();
    }


    /**
     * Configures the bottom navigation bar for Entrants. Home keeps you
     * on this screen, the waitlist item sends you to {@link WaitListActivity},
     * and the notifications item opens {@link EntrantNotificationScreen}.
     * Keeping this routing in one place makes it easy to see Entrant flows.
     *
     * @author Karina Zhang
     */
    private void setupBottomNav() {

        BottomNavigationView nav = findViewById(R.id.bottomNavigation);

        nav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, com.example.waitwell.activities.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_waitlist) {
                Intent intent = new Intent(this, WaitListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_notifications) {
                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }
}

