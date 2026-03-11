package com.example.waitwell;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

public class EntrantNotificationOptions extends AppCompatActivity {
    //the user can accept or not accept notifications

    private CheckBox acceptNotificationCheckBox;
    private CheckBox rejectNotificationCheckBox;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_options);

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
    }

}
