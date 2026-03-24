package com.example.waitwell.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.waitwell.R;

// ---------------------------------------------------------
// REHAAN'S ADDITIONS START
// Added imports to support side-drawer navigation logic
// ---------------------------------------------------------
import android.content.Intent;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.example.waitwell.Profile;
import com.example.waitwell.EntrantNotificationOptions;
// REHAAN'S ADDITIONS END
// ---------------------------------------------------------

/**Karina's features:
 * In terms of user stories, this is basically the entry point that lets
 * organizers reach create, manage, and QR flows (US 02.01.01, 02.01.04,
 * 02.02.03, 02.03.01, 02.04.01, 02.04.02, etc.).
 * *
 * Activity that bootstraps the whole Organizer side of the app.
 * This screen never shows entrant/admin UI – it only hosts Organizer fragments
 * like {@link OrganizerHomeFragment} and {@link OrganizerCreateEventFragment}.
 * *
 * Citation will be gray inline comments at where the referenced code begins.
 *
 * Modified by Rehaan: Integrated DrawerLayout logic to bridge Karina's
 * Organizer shell with Sarang's Navigation Drawer structure.
 */
public class OrganizerEntryActivity extends AppCompatActivity {

    // ---------------------------------------------------------
    // REHAAN'S ADDITION: Reference for the drawer mechanic
    // ---------------------------------------------------------
    private DrawerLayout drawerLayout;

    /**
     * Sets up the Organizer shell layout and, on first creation,
     * drops the organizer on the home fragment that lists their events.
     * Assumes this activity is only launched from the role-selection
     * flow as the Organizer option, not from entrant/admin paths.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entry);

        // ---------------------------------------------------------
        // REHAAN'S ADDITION START
        // Initializing the Drawer and Navigation View.
        // Logic adapted from Sarang's MainActivity to ensure UI consistency.
        // ---------------------------------------------------------
        drawerLayout = findViewById(R.id.organizer_drawer_layout);
        NavigationView navigationView = findViewById(R.id.organizer_navigation_view);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, Profile.class));
                } else if (id == R.id.nav_notification_options) {
                    startActivity(new Intent(this, EntrantNotificationOptions.class));
                } else if (id == R.id.nav_logout) {
                    // Navigate back to registration to switch roles
                    Intent intent = new Intent(this, RegisterActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                drawerLayout.closeDrawers();
                return true;
            });
        }
        // REHAAN'S ADDITION END
        // ---------------------------------------------------------

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.organizer_fragment_container, new OrganizerHomeFragment())
                    .commit();
        }
    }

    // ---------------------------------------------------------
    // REHAAN'S ADDITION START
    /**
     * Helper method to allow OrganizerHomeFragment to trigger the drawer.
     * This bridges Karina's fragment-based UI with the Activity-level DrawerLayout.
     */
    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
    // REHAAN'S ADDITION END
    // ---------------------------------------------------------

    /**
     * Swaps the current Organizer fragment with another one and
     * adds the transaction to the back stack for normal Android back behaviour.
     * This is only meant for Organizer fragments so we keep their navigation
     * isolated from entrant/admin flows.
     *
     * @param fragment the new Organizer-only fragment to display
     */
    public void replaceWithOrganizerFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.organizer_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}