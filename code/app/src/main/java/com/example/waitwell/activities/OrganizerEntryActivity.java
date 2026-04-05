package com.example.waitwell.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;

// ---------------------------------------------------------
// REHAAN'S ADDITIONS START
// Added imports to support side-drawer navigation logic
// ---------------------------------------------------------
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

    /**
     * When set on an intent to this activity, clears the organizer fragment back stack and shows
     * {@link OrganizerHomeFragment} (My Events). Needed because child activities (e.g. View Requests)
     * sit above this activity while detail fragments remain on the stack underneath.
     */
    public static final String EXTRA_NAVIGATE_TO_MY_EVENTS = "navigate_to_my_events";

    // REHAAN'S ADDITION — US 02.09.01 Part 2 — open detail directly for a specific event (co-organizer Manage button)
    public static final String EXTRA_OPEN_EVENT_ID = "open_event_id";
    // END REHAAN'S ADDITION

    public static Intent intentNavigateToMyEvents(Context context) {
        Intent i = new Intent(context, OrganizerEntryActivity.class);
        i.putExtra(EXTRA_NAVIGATE_TO_MY_EVENTS, true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }

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
                    startActivity(new Intent(OrganizerEntryActivity.this, RegisterActivity.class));
                } else if (id == R.id.nav_delete_profile) {
                    showDeleteProfileDialog();
                }
                drawerLayout.closeDrawers();
                return true;
            });
        }
        // REHAAN'S ADDITION END
        // ---------------------------------------------------------

        boolean navigateMyEvents = getIntent().getBooleanExtra(EXTRA_NAVIGATE_TO_MY_EVENTS, false);
        // REHAAN'S ADDITION — open detail fragment directly for co-organizer Manage button
        String openEventId = getIntent().getStringExtra(EXTRA_OPEN_EVENT_ID);
        // END REHAAN'S ADDITION
        if (navigateMyEvents) {
            navigateToMyEventsClearingBackStack();
            getIntent().removeExtra(EXTRA_NAVIGATE_TO_MY_EVENTS);
        } else if (openEventId != null && savedInstanceState == null) {
            // REHAAN'S ADDITION — go straight to the event detail fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.organizer_fragment_container,
                            OrganizerEventDetailFragment.newInstance(openEventId))
                    .commit();
            // END REHAAN'S ADDITION
        } else if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.organizer_fragment_container, new OrganizerHomeFragment())
                    .commit();
        }
    }

    private void showDeleteProfileDialog() { // taken from entrant code to ensure consistency

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        deleteUserProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserProfile() { // taken from entrant code to ensure consistency

        // get stored user id
        SharedPreferences prefs = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId == null) {

            Toast.makeText(this, "No user profile found", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(OrganizerEntryActivity.this, RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
            return;
        }

        // delete user from firestore
        FirebaseHelper.getInstance().deleteUser(userId)

                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();

                    prefs.edit().remove("userId").apply();

                    Intent intent = new Intent(OrganizerEntryActivity.this, RegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                })

                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_NAVIGATE_TO_MY_EVENTS, false)) {
            navigateToMyEventsClearingBackStack();
            intent.removeExtra(EXTRA_NAVIGATE_TO_MY_EVENTS);
        }
    }

    private void navigateToMyEventsClearingBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }
        fm.beginTransaction()
                .replace(R.id.organizer_fragment_container, new OrganizerHomeFragment())
                .commit();
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

    public void goToHomeFragment() {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE); // clear back stack
        fm.beginTransaction()
                .replace(R.id.organizer_fragment_container, new OrganizerHomeFragment())
                .commit();
    }

    /** Go back one fragment in the back stack, or go home if nothing left */
    public void goBack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            goToHomeFragment();
        }
    }

}