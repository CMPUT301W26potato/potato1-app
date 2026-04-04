package com.example.waitwell.activities;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.EntrantNotificationOptions;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.navigation.NavigationView;

/**
 * Base activity for organizer screens that need a hamburger drawer.
 * Subclasses call {@link #setupOrganizerDrawer()} from their onCreate after
 * setContentView. The layout must contain ids: drawer_layout, navigation_view,
 * and btnHamburger.
 */
public abstract class OrganizerBaseActivity extends AppCompatActivity {

    protected void setupOrganizerDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.btnHamburger).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_notification_options) {
                startActivity(new Intent(this, EntrantNotificationOptions.class));
            } else if (id == R.id.nav_lottery_selection_criteria) {
                startActivity(new Intent(this, EntrantLotteryCriteria.class));
            } else if (id == R.id.nav_delete_profile) {
                showDeleteProfileDialog();
            } else if (id == R.id.nav_logout) {
                startActivity(new Intent(this, RegisterActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
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
                    Intent intent = new Intent(this, RegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }
}