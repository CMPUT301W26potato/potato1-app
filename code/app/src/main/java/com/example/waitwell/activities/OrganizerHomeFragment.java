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
import com.example.waitwell.EventStatusUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Karina's features:
 * stories: creating new events and seeing their current status (US 02.01.01,
 * 02.03.01, 02.04.01, 02.04.02).
 * Fragment that acts as the home screen for organizers.
 * This is Organizer-only and lives inside {@link OrganizerEntryActivity},
 * showing the organizer's events plus a button to create new ones.
 * It supports user stories around listing and managing events, including
 * *
 * Citation will be gray inline comments at where the referenced code begins.
 *
 * Modified by Rehaan: Added bridge logic to connect the hamburger button to the
 * Activity-level Navigation Drawer.
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

        // ---------------------------------------------------------
        // REHAAN'S ADDITION START
        // Bridging Fragment UI to Activity Drawer:
        // The hamburger button physically exists in this fragment's XML,
        // but the DrawerLayout logic belongs to OrganizerEntryActivity.
        // ---------------------------------------------------------
        View btnHamburger = view.findViewById(R.id.btnHamburger);
        if (btnHamburger != null) {
            btnHamburger.setOnClickListener(v -> {
                // Call the helper method in the parent activity to slide the drawer open
                if (getActivity() instanceof OrganizerEntryActivity) {
                    ((OrganizerEntryActivity) getActivity()).openDrawer();
                } else {
                    Log.w(TAG, "getActivity() is not an instance of OrganizerEntryActivity");
                }
            });
        }
        // REHAAN'S ADDITION END
        // ---------------------------------------------------------

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

    /**
     * Navigates to the organizer create‑event form while staying inside
     * the organizer‑only entry activity / back stack.
     */
    private void openCreateEvent() {
        if (getActivity() instanceof OrganizerEntryActivity) {
            ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(new OrganizerCreateEventFragment());
        }
    }

    /**
     * Loads only events created by this organizer from Firestore and feeds
     * the result into {@link #onEventsLoaded(QuerySnapshot)} for rendering.
     */
    private void loadMyEvents() {
        // ... (Karina's original code remains unchanged)
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

    /**
     * Renders all organizer events into the vertical list, applying
     * status badges and wiring the Manage button for each row.
     * Also auto‑closes events whose registration deadline has passed.
     */
    private void onEventsLoaded(QuerySnapshot snapshot) {
        // ... (Karina's original code remains unchanged)
        eventsList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String title = doc.getString("title");
            if (title == null) title = "Untitled Event";

            String status = doc.getString("status");
            if (status == null) status = "open";

            String eventId = doc.getId();

            String computed = EventStatusUtils.computeStatus(doc);
            if (!computed.equalsIgnoreCase(status)) {
                status = computed;
                doc.getReference().update("status", computed);
            }

            View row = inflater.inflate(R.layout.item_organizer_event_row, eventsList, false);

            TextView titleView = row.findViewById(R.id.item_organizer_event_title);
            TextView dateView = row.findViewById(R.id.item_organizer_event_date);
            TextView statusBadge = row.findViewById(R.id.item_organizer_event_status);
            Button manageBtn = row.findViewById(R.id.item_organizer_btn_manage);

            titleView.setText(title);

            Date eventDate = doc.getDate("eventDate");
            if (eventDate != null) {
                SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault());
                dateView.setText(fmt.format(eventDate));
                dateView.setVisibility(View.VISIBLE);
            } else {
                dateView.setVisibility(View.GONE);
            }

            applyStatusBadge(statusBadge, status);

            manageBtn.setOnClickListener(v -> onManageClicked(eventId));

            eventsList.addView(row);
        }
    }

    /**
     * Converts the raw status string into the styled pill shown in the UI.
     * Status values are stored as plain strings in Firestore (open/closed/completed).
     */
    private void applyStatusBadge(TextView badge, String status) {
        // ... (Karina's original code remains unchanged)
        if ("completed".equalsIgnoreCase(status)) {
            badge.setText(getString(R.string.organizer_status_completed));
            badge.setBackgroundResource(R.drawable.bg_status_completed);
            badge.setTextColor(getResources().getColor(R.color.status_completed_text, null));
        }
        else if ("closed".equalsIgnoreCase(status)) {
            badge.setText(getString(R.string.organizer_status_closed));
            badge.setBackgroundResource(R.drawable.bg_status_closed);
            badge.setTextColor(getResources().getColor(R.color.status_closed_text, null));
        }
        else {
            badge.setText(getString(R.string.organizer_status_open));
            badge.setBackgroundResource(R.drawable.bg_status_open);
            badge.setTextColor(getResources().getColor(R.color.status_open_text, null));
        }
    }

    /**
     * Handles the "Manage" button for a specific event by pushing the
     * organizer‑only event detail / manage fragment onto the stack.
     */
    private void onManageClicked(String eventId) {
        if (getActivity() instanceof OrganizerEntryActivity) {
            OrganizerEventDetailFragment fragment = OrganizerEventDetailFragment.newInstance(eventId);
            ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(fragment);
        }
    }
}