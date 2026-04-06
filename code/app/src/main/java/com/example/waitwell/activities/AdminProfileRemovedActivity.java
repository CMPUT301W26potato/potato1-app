package com.example.waitwell.activities;



import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.waitwell.R;

/**
 * AdminProfileRemovedActivity displays a confirmation screen
 * after an administrator removes a user profile.
 *
 * The page confirms the deletion and allows the admin
 * to return to the profiles list.
 *
 * @author Grace Shin
 */
public class AdminProfileRemovedActivity extends AppCompatActivity {
    /**
     * Initializes the profile deletion confirmation screen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile_deleted);

        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
        });
    }
}