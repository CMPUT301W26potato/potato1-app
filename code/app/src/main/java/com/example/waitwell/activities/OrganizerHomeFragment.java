package com.example.waitwell.activities;

import android.content.Intent;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

/**
 * Fragment that acts as the home screen for organizers.
 * This is Organizer-only and lives inside {@link OrganizerEntryActivity},
 * showing the organizer's events plus a button to create new ones.
 * It supports user stories around listing and managing events, including
 * creating new events and seeing their current status (US 02.01.01,
 * 02.03.01, 02.04.01, 02.04.02, and friends).
 */
public class OrganizerHomeFragment extends Fragment {

    private static final String TAG = "OrganizerHomeFragment";
    private LinearLayout eventsList;
    private String organizerId;

    /**
     * Grabs the device based organizer id once when the fragment is created.
     * We assume DeviceUtils is already wired to give a stable id per device,
     * and that this fragment is only used for organizer accounts.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        organizerId = DeviceUtils.getDeviceId(requireContext());
    }

    @Nullable
    /**
     * Inflates the organizer home layout which holds the "My Events" list
     * and the actions row (create button + hamburger).
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_home, container, false);
    }

    /**
     * Wires up click listeners and does the first load for the organizer's events.
     * This is the main entry point once the view hierarchy is ready.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eventsList = view.findViewById(R.id.organizer_events_list);
        Button btnCreate = view.findViewById(R.id.btnCreateNewEvent);
        btnCreate.setOnClickListener(v -> openCreateEvent());

        View hamburger = view.findViewById(R.id.btnHamburger);
        hamburger.setOnClickListener(this::showHamburgerMenu);

        loadMyEvents();
    }

    /**
     * Refreshes the events list whenever the fragment comes back into view.
     * Nice for when an organizer edits an event and returns to this screen.
     */
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

    private void showHamburgerMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_main_hamburger, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                logoutToRegister();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void logoutToRegister() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), RegisterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void loadMyEvents() {
        eventsList.removeAllViews();
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
