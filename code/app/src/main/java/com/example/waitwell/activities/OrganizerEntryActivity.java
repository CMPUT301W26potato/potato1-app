package com.example.waitwell.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.waitwell.R;

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

 */
public class OrganizerEntryActivity extends AppCompatActivity {

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

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.organizer_fragment_container, new OrganizerHomeFragment())
                    .commit();
        }
    }

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
