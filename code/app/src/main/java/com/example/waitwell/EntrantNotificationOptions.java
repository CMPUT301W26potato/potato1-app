package com.example.waitwell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.activities.WaitListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class EntrantNotificationOptions extends AppCompatActivity {
    //the user can accept or not accept notifications

    private CheckBox acceptNotificationCheckBox;
    private CheckBox rejectNotificationCheckBox;

    private SharedPreferences prefs;

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
            prefs.edit().putBoolean("checkbox1", isChecked).apply();
        });

        rejectNotificationCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("checkbox2", isChecked).apply();
        });

        //setup the bottom nav bar
        setupBottomNav();
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
