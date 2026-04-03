package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.waitwell.Profile;
import com.example.waitwell.R;

/**
 * Shown after {@link com.example.waitwell.FirebaseHelper#executeLotterySampling} succeeds.
 */
public class SamplingConfirmationActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_SAMPLED_COUNT = "sampled_count";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sampling_confirmation);

        final String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        int sampledCount = getIntent().getIntExtra(EXTRA_SAMPLED_COUNT, 0);

        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        ImageButton btnBack = findViewById(R.id.btnBackBelowToolbar);
        ImageView imgProfile = findViewById(R.id.imgProfileAvatar);
        TextView txtCountLine = findViewById(R.id.txtSampledCountLine);

        txtCountLine.setText(getResources().getQuantityString(
                R.plurals.sampling_confirmation_count_line, sampledCount, sampledCount));

        btnHamburger.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
        imgProfile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

        findViewById(R.id.btnViewSampledEntrants).setOnClickListener(v -> {
            if (eventId == null || eventId.isEmpty()) {
                return;
            }
            Intent i = new Intent(this, SampledEntrantsActivity.class);
            i.putExtra(SampledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });

        BottomNavigationView nav = findViewById(R.id.organizerBottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_organizer_bottom_back) {
                finish();
                return true;
            }
            if (id == R.id.nav_organizer_bottom_home) {
                startActivity(OrganizerEntryActivity.intentNavigateToMyEvents(this));
                finish();
                return true;
            }
            return false;
        });
    }
}
