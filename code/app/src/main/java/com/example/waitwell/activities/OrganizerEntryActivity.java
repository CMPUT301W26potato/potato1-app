package com.example.waitwell.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.waitwell.R;

/**
 * Karina's Contribution:
 * Organizer-only!
 * the app shows only Organizer views (OrganizerHomeFragment, OrganizerCreateEventFragment).
 * User stories: US 02.01.01, US 02.01.04, US 02.02.03, US 02.03.01, US 02.04.01, US 02.04.02.
 */
public class OrganizerEntryActivity extends AppCompatActivity {

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

    public void replaceWithOrganizerFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.organizer_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
