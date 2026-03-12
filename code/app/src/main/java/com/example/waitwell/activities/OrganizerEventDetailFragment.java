package com.example.waitwell.activities;

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
import androidx.fragment.app.Fragment;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**Karina's Contribution:
 * Organizer-only event detail / manage screen.
 * Keep in mind there is eventId via arguments, loads from Firestore
 * Displays organizer actions like Edit / Delete / View Entrants, etc.
 */
public class OrganizerEventDetailFragment extends Fragment {

    private static final String TAG = "OrganizerEventDetail";
    private static final String ARG_EVENT_ID = "event_id";

    private String eventId;

    private ImageView imgPoster;
    private TextView txtTitle;
    private TextView txtLocation;
    private TextView txtDateRange;
    private TextView txtPrice;

    public static OrganizerEventDetailFragment newInstance(@NonNull String eventId) {
        OrganizerEventDetailFragment fragment = new OrganizerEventDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgPoster = view.findViewById(R.id.imgEventPoster);
        txtTitle = view.findViewById(R.id.txtEventTitle);
        txtLocation = view.findViewById(R.id.txtEventLocation);
        txtDateRange = view.findViewById(R.id.txtEventDateRange);
        txtPrice = view.findViewById(R.id.txtEventPrice);

        Button btnDelete = view.findViewById(R.id.btnDeleteEvent);
        Button btnEdit = view.findViewById(R.id.btnEditEvent);
        Button btnShare = view.findViewById(R.id.btnShare);
        Button btnViewRequests = view.findViewById(R.id.btnViewRequests);
        Button btnViewFinalEntrants = view.findViewById(R.id.btnViewFinalEntrants);
        Button btnViewCanceledEntrants = view.findViewById(R.id.btnViewCanceledEntrants);
        Button btnViewInvitedEntrants = view.findViewById(R.id.btnViewInvitedEntrants);
        Button btnViewSampledEntrants = view.findViewById(R.id.btnViewSampledEntrants);
        View btnBack = view.findViewById(R.id.btnOrganizerBack);

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID);
        }

        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onViewCreated: eventId missing in arguments");
            return;
        }

        btnDelete.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show());
        btnShare.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show());
        btnViewRequests.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show());
        btnViewFinalEntrants.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show());
        btnViewCanceledEntrants.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show());
        btnViewInvitedEntrants.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show());
//        Rehaan's addition for 02.05.02
        btnViewSampledEntrants.setOnClickListener(v -> showLotteryDialog());
//        btnViewSampledEntrants.setOnClickListener(v ->
//                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show());

        btnEdit.setOnClickListener(v -> openEditEvent());

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

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
    }

    private void loadPosterImage(@NonNull String url) {
        // If it's a local content/file URI, load directly from the device.
        if (url.startsWith("content:") || url.startsWith("file:")) {
            try {
                imgPoster.setImageURI(android.net.Uri.parse(url));
            } catch (Exception e) {
                Log.w(TAG, "Failed to load local poster URI: " + url, e);
            }
            return;
        }

        // Fallback for remote storage URLs (legacy behavior).
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
     * Rehaan's addition for 02.05.02
     * Shows a dialog prompting the organizer to enter how many entrants to sample.
     * On confirm, executes the lottery and updates Firestore.
     */
    private void showLotteryDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint(getString(R.string.lottery_dialog_hint));
        input.setPadding(48, 24, 48, 24);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.lottery_dialog_title))
                .setMessage(getString(R.string.lottery_dialog_message))
                .setView(input)
                .setPositiveButton(getString(R.string.lottery_dialog_confirm), (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.lottery_error_enter_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int sampleSize = Integer.parseInt(raw);
                    if (sampleSize <= 0) {
                        Toast.makeText(requireContext(), getString(R.string.lottery_error_positive), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    runLottery(sampleSize);
                })
                .setNegativeButton(getString(R.string.lottery_dialog_cancel), null)
                .show();
    }

    /**
     * Calls FirebaseHelper to execute lottery sampling for this event.
     * Shows a loading toast while running, then success or failure feedback.
     * @param sampleSize number of entrants the organizer wants to select
     */
    private void runLottery(int sampleSize) {
        Toast.makeText(requireContext(), getString(R.string.lottery_running), Toast.LENGTH_SHORT).show();

        FirebaseHelper.getInstance().executeLotterySampling(eventId, sampleSize, task -> {
            if (!isAdded()) return;
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(),
                        getString(R.string.lottery_success, sampleSize),
                        Toast.LENGTH_LONG).show();
            } else {
                Exception e = task.getException();
                android.util.Log.e("LotteryDebug", "Lottery failed", e);
                Toast.makeText(requireContext(),
                        getString(R.string.lottery_error_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}

