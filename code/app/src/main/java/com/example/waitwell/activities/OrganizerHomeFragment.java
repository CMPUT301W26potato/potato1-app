package com.example.waitwell.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.waitwell.DeviceUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

/**
 * Karina's Contribution:
 * Organizer home: "My Events" list and "Create New Event" button.
 * Only used inside OrganizerEntryActivity!
 * User stories: US 02.01.01 (Create Event), US 02.04.01/02 (Poster), etc.
 */
public class OrganizerHomeFragment extends Fragment {

    private static final String TAG = "OrganizerHomeFragment";
    private LinearLayout eventsList;
    private String organizerId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        organizerId = DeviceUtils.getDeviceId(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eventsList = view.findViewById(R.id.organizer_events_list);
        Button btnCreate = view.findViewById(R.id.btnCreateNewEvent);
        btnCreate.setOnClickListener(v -> openCreateEvent());
        loadMyEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyEvents();
    }


    private void openCreateEvent() {
        if (getActivity() instanceof OrganizerEntryActivity) {
            ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(new OrganizerCreateEventFragment());
        }
    }

    private void loadMyEvents() {
        eventsList.removeAllViews();
        Log.d(TAG, "Loading events for organizerId: " + organizerId);
        FirebaseHelper.getInstance().getDb()
                .collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(this::onEventsLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load organizer events", e);
                    Toast.makeText(requireContext(), "Could not load events", Toast.LENGTH_SHORT).show();
                });
    }

    private void onEventsLoaded(QuerySnapshot snapshot) {
        eventsList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String title = doc.getString("title");
            if (title == null) title = "Untitled Event";
            String status = doc.getString("status");
            if (status == null) status = "open";
            String eventId = doc.getId();

            Date registrationClose = doc.getDate("registrationClose");
            Date now = new Date();
            if (registrationClose != null && registrationClose.before(now)) {
                status = "closed";
                doc.getReference().update("status", "closed");
            }

            View row = inflater.inflate(R.layout.item_organizer_event_row, eventsList, false);
            TextView titleView = row.findViewById(R.id.item_organizer_event_title);
            TextView statusBadge = row.findViewById(R.id.item_organizer_event_status);
            Button manageBtn = row.findViewById(R.id.item_organizer_btn_manage);

            titleView.setText(title);
            applyStatusBadge(statusBadge, status);
            manageBtn.setOnClickListener(v -> onManageClicked(eventId));

            eventsList.addView(row);
        }
    }


    private void applyStatusBadge(TextView badge, String status) {
        if ("completed".equalsIgnoreCase(status)) {
            badge.setText(getString(R.string.organizer_status_completed));
            badge.setBackgroundResource(R.drawable.bg_status_completed);
            badge.setTextColor(getResources().getColor(R.color.status_completed_text, null));
        } else if ("closed".equalsIgnoreCase(status)) {
            badge.setText(getString(R.string.organizer_status_closed));
            badge.setBackgroundResource(R.drawable.bg_status_closed);
            badge.setTextColor(getResources().getColor(R.color.status_closed_text, null));
        } else {
            badge.setText(getString(R.string.organizer_status_open));
            badge.setBackgroundResource(R.drawable.bg_status_open);
            badge.setTextColor(getResources().getColor(R.color.status_open_text, null));
        }
    }

    private void onManageClicked(String eventId) {
        if (getActivity() instanceof OrganizerEntryActivity) {
            OrganizerEventDetailFragment fragment = OrganizerEventDetailFragment.newInstance(eventId);
            ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(fragment);
        }
    }
}
