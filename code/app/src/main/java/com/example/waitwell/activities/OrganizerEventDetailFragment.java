package com.example.waitwell.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.waitwell.EventDeletionHelper;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment that lets organizers peek at all the details for a single event.
 * This is Organizer-only and is fed an event id through arguments, which it
 * then uses to pull data from Firestore and show things like dates, price,
 * and poster. It ties into stories about managing existing events and running
 * extra flows like the lottery (US 02.05.02 plus earlier organizer stories).
 * Karina's Contribution:
 * Organizer-only event detail / manage screen.
 * Keep in mind there is eventId via arguments, loads from Firestore
 * Displays organizer actions like Edit / Delete / View
 * Entrants, etc.
 * *
 * Rehaan added: lottery sampling (02.05.02), draw replacement (02.05.03),
 * view invited (02.06.01), view cancelled (02.06.02).
 * Javadoc written with help from Claude (claude.ai)
 * *
 * Citation will be gray inline comments at where the referenced code begins.
 * *
 */
public class OrganizerEventDetailFragment extends Fragment {

    private static final String TAG = "OrganizerEventDetail";
    private static final String ARG_EVENT_ID = "event_id";

    private String eventId;

    private ImageView imgPoster;
    private TextView txtTitle;
    private TextView txtLocation;
    private TextView txtDateRange;
    private TextView txtBannerEventDate;
    private TextView txtBannerEventTime;
    private TextView txtPrice;
    private Button btnInviteEntrants;
    private Button btnShare;
    private Button btnDelete;

    /**
     * Factory method for creating a detail fragment for a specific event.
     * We wrap the event id in a Bundle so Android can recreate the fragment
     * cleanly across config changes.
     *
     * @param eventId Firestore id for the event the organizer wants to view
     * @return a new {@link OrganizerEventDetailFragment} with arguments set
     */
    public static OrganizerEventDetailFragment newInstance(@NonNull String eventId) {
        OrganizerEventDetailFragment fragment = new OrganizerEventDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Fires up the Organizer event detail layout which shows poster,
     * title, location, date range, and pricing for a single event.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_event_detail, container, false);
    }

    /**
     * Wires up the UI widgets, resolves the event id from arguments,
     * hooks up click listeners for all the organizer actions, and finally
     * triggers the initial load from Firestore.
     * Assumes that a valid {@code event_id} argument was provided.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgPoster = view.findViewById(R.id.imgEventPoster);
        txtTitle = view.findViewById(R.id.txtEventTitle);
        txtLocation = view.findViewById(R.id.txtEventLocation);
        txtDateRange = view.findViewById(R.id.txtEventDateRange);
        txtBannerEventDate = view.findViewById(R.id.txtBannerEventDate);
        txtBannerEventTime = view.findViewById(R.id.txtBannerEventTime);
        txtPrice = view.findViewById(R.id.txtEventPrice);

        btnDelete = view.findViewById(R.id.btnDeleteEvent);
        Button btnEdit = view.findViewById(R.id.btnEditEvent);
        btnShare = view.findViewById(R.id.btnShare);
        Button btnViewRequests = view.findViewById(R.id.btnViewRequests);
        Button btnViewFinalEntrants = view.findViewById(R.id.btnViewFinalEntrants);
        Button btnViewCanceledEntrants = view.findViewById(R.id.btnViewCanceledEntrants);
        Button btnViewInvitedEntrants = view.findViewById(R.id.btnViewInvitedEntrants);
        btnInviteEntrants = view.findViewById(R.id.btnInviteEntrants);
        Button btnViewSampledEntrants = view.findViewById(R.id.btnViewSampledEntrants);
        Button btnViewComments = view.findViewById(R.id.btnViewComments);
        //View btnBack = view.findViewById(R.id.btnOrganizerBack);
        BottomNavigationView nav = view.findViewById(R.id.organizerBottomNavigation);

        View btnHamburger = view.findViewById(R.id.btnHamburger);
        if (btnHamburger != null && getActivity() instanceof OrganizerEntryActivity) {
            btnHamburger.setOnClickListener(v ->
                    ((OrganizerEntryActivity) getActivity()).openDrawer());
        }

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID);
        }

        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onViewCreated: eventId missing in arguments");
            return;
        }

        btnDelete.setOnClickListener(v -> showDeleteEventConfirmationDialog());
        btnShare.setOnClickListener(v -> openEventQrShareFromManage());
        btnViewRequests.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), ViewRequestsActivity.class);
            i.putExtra("event_id", eventId);
            i.putExtra("event_title", txtTitle.getText() != null ? txtTitle.getText().toString() : "");
            startActivity(i);
        });

        btnViewFinalEntrants.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), FinalEntrantsActivity.class);
            i.putExtra(FinalEntrantsActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });

        btnViewCanceledEntrants.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), CancelledEntrantsActivity.class);
            i.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });

        btnViewInvitedEntrants.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), InvitedEntrantsActivity.class);
            i.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });
        btnInviteEntrants.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), InviteEntrantsActivity.class);
            i.putExtra(InviteEntrantsActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });

        btnViewSampledEntrants.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), SampledEntrantsActivity.class);
            i.putExtra(SampledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });

        Button btnDrawReplacement = view.findViewById(R.id.btnDrawReplacement);
        btnDrawReplacement.setOnClickListener(v -> showDrawReplacementDialog());
        btnEdit.setOnClickListener(v -> openEditEvent());


        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (getActivity() instanceof OrganizerEntryActivity) {
                    OrganizerEntryActivity activity = (OrganizerEntryActivity) getActivity();
                    FragmentManager fm = activity.getSupportFragmentManager();

                    // check if the top fragment is already homefragment
                    Fragment current = fm.findFragmentById(R.id.organizer_fragment_container);
                    if (!(current instanceof OrganizerHomeFragment)) {
                        // clear back stack
                        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                        // replace with home fragment
                        activity.replaceWithOrganizerFragment(new OrganizerHomeFragment());
                    }

                } else {
                    // not in entryactivity then start fresh
                    Intent intent = new Intent(getContext(), OrganizerEntryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }

                return true;
            }
            return false;
        });

        btnViewComments.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), OrganizerCommentsActivity.class);
            i.putExtra("event_id", eventId);
            startActivity(i);
        });

        // REHAAN'S ADDITION — US 02.09.01
        Button btnAssignCoOrganizer = view.findViewById(R.id.btnAssignCoOrganizer);
        btnAssignCoOrganizer.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), AssignCoOrganizerActivity.class);
            i.putExtra(AssignCoOrganizerActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });
// END REHAAN'S ADDITION

        loadEvent();
    }

    private void loadEvent() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event " + eventId, e);
                    Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
                });
    }

    private void bindEvent(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = doc.getString("title");
        if (title == null || title.trim().isEmpty()) {
            title = "Untitled Event";
        }
        txtTitle.setText(title);

        String location = doc.getString("location");
        if (location == null) {
            location = "";
        }
        txtLocation.setText(location);

        Timestamp openTs = doc.getTimestamp("registrationOpen");
        Timestamp closeTs = doc.getTimestamp("registrationClose");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateText;
        if (openTs != null && closeTs != null) {
            Date open = openTs.toDate();
            Date close = closeTs.toDate();
            dateText = fmt.format(open) + "  -  " + fmt.format(close);
        } else {
            dateText = getString(R.string.organizer_date_not_set);
        }
        txtDateRange.setText(dateText);

        Date eventWhen = doc.getDate("eventDate");
        SimpleDateFormat eventDayFmt = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat eventTimeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
        if (eventWhen != null) {
            txtBannerEventDate.setText(eventDayFmt.format(eventWhen));
            txtBannerEventTime.setText(eventTimeFmt.format(eventWhen));
        } else {
            txtBannerEventDate.setText(getString(R.string.event_detail_event_date_not_set));
            txtBannerEventTime.setText("");
        }

        Double priceObj = doc.getDouble("price");
        String priceText;
        if (priceObj == null || priceObj == 0.0) {
            priceText = getString(R.string.organizer_price_free);
        } else {
            priceText = String.format(Locale.US, "$%.2f", priceObj);
        }
        txtPrice.setText(priceText);

        String imageUrl = doc.getString("imageUrl");
        if (imageUrl == null) {
            imageUrl = doc.getString("posterUrl");
        }
        if (!TextUtils.isEmpty(imageUrl)) {
            loadPosterImage(imageUrl);
        }
        Boolean privateEvent = doc.getBoolean("isPrivate");
        boolean isPrivate = Boolean.TRUE.equals(privateEvent);
        View spacer = requireView().findViewById(R.id.spaceBetweenButtons);
        btnInviteEntrants.setVisibility(Boolean.TRUE.equals(privateEvent) ? View.VISIBLE : View.GONE);
        btnShare.setVisibility(Boolean.TRUE.equals(privateEvent) ? View.GONE : View.VISIBLE);
        spacer.setVisibility(isPrivate ? View.GONE : View.VISIBLE);
    }

    private void openEventQrShareFromManage() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(requireContext(), R.string.invitation_error_not_found, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (Boolean.TRUE.equals(doc.getBoolean("isPrivate"))) {
                        return;
                    }
                    String title = doc.getString("title");
                    if (title == null) {
                        title = "";
                    }
                    String posterUrl = doc.getString("imageUrl");
                    if (posterUrl == null) {
                        posterUrl = doc.getString("posterUrl");
                    }
                    boolean isPrivate = Boolean.TRUE.equals(doc.getBoolean("isPrivate"));
                    Fragment fragment = OrganizerEventCreatedFragment.newInstance(
                            eventId,
                            title,
                            posterUrl,
                            isPrivate,
                            true);
                    if (getActivity() instanceof OrganizerEntryActivity) {
                        ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(fragment);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load event for share", e);
                    Toast.makeText(requireContext(), R.string.invitation_error_load_details, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPosterImage(@NonNull String url) {
        // Load a local image from the android device (Not sure if it works yet)
        if (url.startsWith("content:") || url.startsWith("file:")) {
            try {
                imgPoster.setImageURI(android.net.Uri.parse(url));
            } catch (Exception e) {
                Log.w(TAG, "Failed to load local poster URI: " + url, e);
            }
            return;
        }

        // In case the first solution doens't work
        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            final long ONE_MB = 1024 * 1024;
            ref.getBytes(ONE_MB)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            imgPoster.setImageBitmap(bitmap);
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Failed to load poster image", e));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid poster URL: " + url, e);
        }
    }

    private void openEditEvent() {
        Fragment fragment = new OrganizerCreateEventFragment();
        Bundle args = new Bundle();
        args.putString("event_id", eventId);
        fragment.setArguments(args);

        if (getActivity() instanceof OrganizerEntryActivity) {
            ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(fragment);
        }
    }

    /**
     * Shows a confirmation dialog before drawing a replacement (US 02.05.03).
     * Organizer uses this when a selected entrant cancelled or rejected.
     */
    private void showDrawReplacementDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.replacement_dialog_title))
                .setMessage(getString(R.string.replacement_dialog_message))
                .setPositiveButton(getString(R.string.replacement_dialog_confirm), (dialog, which) ->
                        drawReplacement())
                .setNegativeButton(getString(R.string.lottery_dialog_cancel), null)
                .show();
    }

    /**
     * Calls FirebaseHelper to draw one replacement applicant (US 02.05.03).
     * Shows a toast result so the organizer knows if it worked.
     */

    private void drawReplacement() {
        Toast.makeText(requireContext(), getString(R.string.replacement_running), Toast.LENGTH_SHORT).show();
        FirebaseHelper.getInstance().drawReplacementApplicant(eventId, task -> {
            if (!isAdded()) return;
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(),
                        getString(R.string.replacement_success),
                        Toast.LENGTH_LONG).show();
            } else {
                android.util.Log.e(TAG, "drawReplacement failed", task.getException());
                Toast.makeText(requireContext(),
                        getString(R.string.replacement_error_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateBackFromManage() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private void showDeleteEventConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.organizer_delete_event_title)
                .setMessage(R.string.organizer_delete_event_message)
                .setPositiveButton(R.string.organizer_delete_confirm, (d, which) -> {
                    btnDelete.setEnabled(false);
                    EventDeletionHelper.deleteEvent(eventId, success -> {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(requireContext(),
                                        R.string.organizer_event_deleted_success,
                                        Toast.LENGTH_SHORT).show();
                                navigateBackFromManage();
                            } else {
                                btnDelete.setEnabled(true);
                                Toast.makeText(requireContext(),
                                        R.string.organizer_event_delete_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton(R.string.lottery_dialog_cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (pos != null) {
                pos.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_closed_text));
            }
        });
        dialog.show();
    }
}

