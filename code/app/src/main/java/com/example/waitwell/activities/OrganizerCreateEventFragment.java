package com.example.waitwell.activities;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.waitwell.DeviceUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Karina's contribution:
 * Organizer create-event form. US 02.01.01 (Create Event & QR), US 02.01.04 (Registration Period),
 * US 02.02.03 (Geolocation), US 02.03.01 (Waitlist Limit), US 02.04.01/02 (Poster).
 * Isolated in Organizer module; uses Firebase Storage for poster and Firestore for event.
 */
public class OrganizerCreateEventFragment extends Fragment {

    private static final String TAG = "OrganizerCreateEvent";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private EditText editEventName, editLocation, editRegistrationStart, editRegistrationDeadline;
    private EditText editWaitlistLimit, editPrice, editDescription;
    private androidx.appcompat.widget.SwitchCompat switchGeolocation;
    private TextView txtPosterStatus;
    private Uri posterUri;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    posterUri = uri;
                    txtPosterStatus.setVisibility(View.VISIBLE);
                    txtPosterStatus.setText(R.string.organizer_poster_uploaded);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editEventName = view.findViewById(R.id.editEventName);
        editLocation = view.findViewById(R.id.editLocation);
        editRegistrationStart = view.findViewById(R.id.editRegistrationStart);
        editRegistrationDeadline = view.findViewById(R.id.editRegistrationDeadline);
        editWaitlistLimit = view.findViewById(R.id.editWaitlistLimit);
        editPrice = view.findViewById(R.id.editPrice);
        editDescription = view.findViewById(R.id.editDescription);
        switchGeolocation = view.findViewById(R.id.switchGeolocation);
        txtPosterStatus = view.findViewById(R.id.txtPosterStatus);

        view.findViewById(R.id.btnOrganizerBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        view.findViewById(R.id.btnUploadPoster).setOnClickListener(v ->
                pickImage.launch("image/*"));
        view.findViewById(R.id.btnSubmitEvent).setOnClickListener(v -> submitEvent());

        editRegistrationStart.setOnClickListener(v -> showDatePicker(editRegistrationStart));
        editRegistrationDeadline.setOnClickListener(v -> showDatePicker(editRegistrationDeadline));
        editWaitlistLimit.setOnClickListener(v -> showWaitlistLimitPicker());
    }

    private static final int WAITLIST_PICKER_MIN = 0;
    private static final int WAITLIST_PICKER_MAX = 500;

    private void showWaitlistLimitPicker() {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(WAITLIST_PICKER_MIN);
        picker.setMaxValue(WAITLIST_PICKER_MAX);
        String current = editWaitlistLimit.getText().toString().trim();
        int initial = 0;
        if (!current.isEmpty()) {
            try {
                initial = Integer.parseInt(current);
                initial = Math.max(WAITLIST_PICKER_MIN, Math.min(WAITLIST_PICKER_MAX, initial));
            } catch (NumberFormatException ignored) { }
        }
        picker.setValue(initial);
        picker.setWrapSelectorWheel(false);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.organizer_waitlist_limit)
                .setView(picker)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    int value = picker.getValue();
                    editWaitlistLimit.setText(value == 0 ? "" : String.valueOf(value));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void showDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        String existing = target.getText().toString().trim();
        if (!existing.isEmpty()) {
            Date d = parseDate(existing);
            if (d != null) cal.setTime(d);
        }
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog picker = new DatePickerDialog(requireContext(), (view, y, m, dOfMonth) -> {
            cal.set(Calendar.YEAR, y);
            cal.set(Calendar.MONTH, m);
            cal.set(Calendar.DAY_OF_MONTH, dOfMonth);
            String formatted = new SimpleDateFormat(DATE_FORMAT, Locale.US).format(cal.getTime());
            target.setText(formatted);
        }, year, month, day);
        picker.show();
    }

    private void submitEvent() {
        String title = editEventName.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String startStr = editRegistrationStart.getText().toString().trim();
        String deadlineStr = editRegistrationDeadline.getText().toString().trim();
        String waitlistStr = editWaitlistLimit.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (title.isEmpty() || location.isEmpty() || startStr.isEmpty() || deadlineStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(requireContext(), R.string.organizer_error_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), R.string.organizer_error_invalid_price, Toast.LENGTH_SHORT).show();
            return;
        }
        if (price < 0) {
            Toast.makeText(requireContext(), R.string.organizer_error_invalid_price, Toast.LENGTH_SHORT).show();
            return;
        }

        Date registrationOpen = parseDate(startStr);
        Date registrationClose = parseDate(deadlineStr);
        if (registrationOpen == null || registrationClose == null) {
            Toast.makeText(requireContext(), "Use date format: " + DATE_FORMAT, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!registrationClose.after(registrationOpen)) {
            Toast.makeText(requireContext(), R.string.organizer_error_dates, Toast.LENGTH_SHORT).show();
            return;
        }

        Integer waitlistLimit = null;
        if (!waitlistStr.isEmpty()) {
            try {
                int w = Integer.parseInt(waitlistStr);
                if (w >= 0) waitlistLimit = w;
            } catch (NumberFormatException ignored) { }
        }

        String organizerId = DeviceUtils.getDeviceId(requireContext());
        boolean geolocationRequired = switchGeolocation.isChecked();

        if (posterUri != null) {
            uploadPosterThenSaveEvent(organizerId, title, description, location, geolocationRequired,
                    registrationOpen, registrationClose, waitlistLimit, price);
        } else {
            saveEventToFirestore(organizerId, title, description, location, geolocationRequired,
                    registrationOpen, registrationClose, waitlistLimit, price, null);
        }
    }

    private Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return new SimpleDateFormat(DATE_FORMAT, Locale.US).parse(s.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private void uploadPosterThenSaveEvent(String organizerId, String title, String description, String location,
                                           boolean geolocationRequired, Date registrationOpen, Date registrationClose,
                                           Integer waitlistLimit, double price) {
        String path = "posters/" + organizerId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);
        ref.putFile(posterUri)
                .addOnSuccessListener(t -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveEventToFirestore(organizerId, title, description, location,
                                geolocationRequired, registrationOpen, registrationClose, waitlistLimit, price, uri.toString()))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Get download URL failed", e);
                            Toast.makeText(requireContext(), R.string.organizer_error_upload, Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Poster upload failed", e);
                    Toast.makeText(requireContext(), R.string.organizer_error_upload, Toast.LENGTH_SHORT).show();
                });
    }

    private void saveEventToFirestore(String organizerId, String title, String description, String location,
                                      boolean geolocationRequired, Date registrationOpen, Date registrationClose,
                                      Integer waitlistLimit, double price, @Nullable String posterUrl) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("description", description);
        event.put("location", location);
        event.put("geolocationRequired", geolocationRequired);
        event.put("registrationOpen", registrationOpen);
        event.put("registrationClose", registrationClose);
        if (waitlistLimit != null) event.put("waitlistLimit", waitlistLimit);
        event.put("price", price);
        event.put("organizerId", organizerId);
        event.put("createdAt", FieldValue.serverTimestamp());
        event.put("status", "open");
        event.put("waitlistEntrantIds", new ArrayList<String>());
        if (posterUrl != null) event.put("imageUrl", posterUrl);

        FirebaseHelper.getInstance().getDb().collection("events").add(event)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(requireContext(), R.string.organizer_event_created, Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Save event failed", e);
                    Toast.makeText(requireContext(), R.string.organizer_error_save, Toast.LENGTH_SHORT).show();
                });
    }
}
