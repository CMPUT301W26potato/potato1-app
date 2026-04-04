package com.example.waitwell.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.waitwell.EventStatusUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Karina's features:
 * It mainly covers user stories 02.01.01 (Create Event & QR), 02.01.04 (Registration Period),
 * 02.02.03 (Geolocation), 02.03.01 (Waitlist Limit), and 02.04.01/02 (Poster uploads).
 * *
 * Fragment that handles the "create or edit event" form for organizers.
 * This lives completely inside the Organizer module and never shows up
 * for entrants or admins. It wires together title/location/date fields,
 * geolocation and waitlist toggles, and poster upload, then saves everything
 * into Firestore/Firebase Storage.
 * *
 * Citation will be gray inline comments at where the referenced code begins.

 */
public class OrganizerCreateEventFragment extends Fragment {

    private static final String TAG = "OrganizerCreateEvent";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String ARG_EVENT_ID = "event_id";

    private EditText editEventName, editLocation, editRegistrationStart, editRegistrationDeadline, editEventDate, editEventTime;
    /** Picked event time; {@code -1} means not chosen yet. */
    private int eventTimeHour = -1;
    private int eventTimeMinute;
    private EditText editWaitlistLimit, editPrice, editDescription;
    private androidx.appcompat.widget.SwitchCompat switchGeolocation;
    private androidx.appcompat.widget.SwitchCompat switchPrivateEvent;
    private TextView txtPosterStatus;
    private Uri posterUri;

    // I used ChatGPT to better understand how we can treat this device based
    // identifier as the organizer's "account id" and reuse it consistently.
    private String eventIdToEdit;
    private String existingPosterUrl;
    private TextView txtCategories;
    private ArrayList<String> selectedCategories = new ArrayList<>();
    private final String[] allCategories = new String[] {
            "Sports", "Music", "Art", "Technology", "Education", "Health", "Kids", "Beginner", "Advanced"
    };

    private void showCategoryPicker() {
        boolean[] checkedItems = new boolean[allCategories.length];
        for (int i = 0; i < allCategories.length; i++) {
            checkedItems[i] = selectedCategories.contains(allCategories[i]);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select categories")
                .setMultiChoiceItems(allCategories, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        if (!selectedCategories.contains(allCategories[which])) {
                            selectedCategories.add(allCategories[which]);
                        }
                    } else {
                        selectedCategories.remove(allCategories[which]);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    txtCategories.setText(selectedCategories.isEmpty() ? "Select categories" :
                            String.join(", ", selectedCategories));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    posterUri = uri;
                    txtPosterStatus.setVisibility(View.VISIBLE);
                    txtPosterStatus.setText(R.string.organizer_poster_uploaded);
                }
            });

    /**
     * Inflates the create/edit event layout that contains all of the form fields
     * the organizer can interact with. This is purely Organizer UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_create_event, container, false);
    }

    /**
     * Binds all views, hooks up click listeners, and optionally loads an
     * existing event into the form if an {@code event_id} argument is present.
     * Assumes this fragment is only hosted by {@link OrganizerEntryActivity}
     * inside the Organizer flow.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editEventName = view.findViewById(R.id.editEventName);
        editLocation = view.findViewById(R.id.editLocation);
        editRegistrationStart = view.findViewById(R.id.editRegistrationStart);
        editRegistrationDeadline = view.findViewById(R.id.editRegistrationDeadline);
        editEventDate = view.findViewById(R.id.editEventDate);
        editEventTime = view.findViewById(R.id.editEventTime);
        editWaitlistLimit = view.findViewById(R.id.editWaitlistLimit);
        editPrice = view.findViewById(R.id.editPrice);
        editDescription = view.findViewById(R.id.editDescription);
        switchGeolocation = view.findViewById(R.id.switchGeolocation);
        switchPrivateEvent = view.findViewById(R.id.switchPrivateEvent);
        txtPosterStatus = view.findViewById(R.id.txtPosterStatus);
        txtCategories = view.findViewById(R.id.txtCategories);
        txtCategories.setOnClickListener(v -> showCategoryPicker());


        // Top bar back button.
        view.findViewById(R.id.btnTopBarBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // When the organizer taps this, we launch the system picker for an image banner.
        view.findViewById(R.id.btnUploadPoster).setOnClickListener(v ->
                pickImage.launch("image/*"));
        // Main "Save" action for both creating and editing events.
        view.findViewById(R.id.btnSubmitEvent).setOnClickListener(v -> submitEvent());

        // These fields are all read-only text inputs that open pickers instead of keyboards.
        // I used ChatGPT to learn, step by step, how to swap a plain text/date input
        // for a proper Calendar style DatePicker so organizers choose real dates.
        editRegistrationStart.setOnClickListener(v -> showDatePicker(editRegistrationStart));
        editRegistrationDeadline.setOnClickListener(v -> showDatePicker(editRegistrationDeadline));
        editEventDate.setOnClickListener(v -> showDatePicker(editEventDate));
        editEventTime.setOnClickListener(v -> showEventTimePicker());
        editWaitlistLimit.setOnClickListener(v -> showWaitlistLimitPicker());

        Bundle args = getArguments();
        if (args != null) {
            eventIdToEdit = args.getString(ARG_EVENT_ID);
            if (eventIdToEdit != null && !eventIdToEdit.trim().isEmpty()) {
                loadEventForEdit(eventIdToEdit);
            }
        }
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

        // an AlertDialog shell that just wraps the number picker.
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
        if (target == editRegistrationStart) {
            picker.getDatePicker().setMinDate(EventStatusUtils.startOfToday().getTime());
        } else if (target == editRegistrationDeadline) {
            Date start = parseDate(editRegistrationStart.getText().toString().trim());
            long minMillis = EventStatusUtils.startOfToday().getTime();
            if (start != null) {
                minMillis = Math.max(minMillis, EventStatusUtils.startOfDay(start).getTime());
            }
            picker.getDatePicker().setMinDate(minMillis);
        } else if (target == editEventDate) {
            Date deadline = parseDate(editRegistrationDeadline.getText().toString().trim());
            if (deadline != null) {
                picker.getDatePicker().setMinDate(EventStatusUtils.startOfDay(deadline).getTime());
            }
        }
        picker.show();
    }

    private void showEventTimePicker() {
        int h = eventTimeHour >= 0 ? eventTimeHour : Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int m = eventTimeHour >= 0 ? eventTimeMinute : 0;
        boolean is24h = android.text.format.DateFormat.is24HourFormat(requireContext());
        new TimePickerDialog(requireContext(), (tp, hourOfDay, minute) -> {
            eventTimeHour = hourOfDay;
            eventTimeMinute = minute;
            editEventTime.setText(formatTimeLabel(hourOfDay, minute));
        }, h, m, is24h).show();
    }

    private String formatTimeLabel(int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(c.getTime());
    }

    private void submitEvent() {
        String title = editEventName.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String startStr = editRegistrationStart.getText().toString().trim();
        String deadlineStr = editRegistrationDeadline.getText().toString().trim();
        String eventDateStr = editEventDate.getText().toString().trim();
        String waitlistStr = editWaitlistLimit.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        // Basic "all required fields filled in" check for the path.
        if (title.isEmpty() || location.isEmpty() || startStr.isEmpty() || deadlineStr.isEmpty()
                || eventDateStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(requireContext(), R.string.organizer_error_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (eventTimeHour < 0) {
            Toast.makeText(requireContext(), R.string.organizer_error_event_time, Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), R.string.organizer_error_invalid_price, Toast.LENGTH_SHORT).show();
            return;
        }
        // We don’t allow negative prices – a free event is just 0.
        if (price < 0) {
            Toast.makeText(requireContext(), R.string.organizer_error_invalid_price, Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse dates from the string fields using a consistent yyyy-MM-dd format.
        Date registrationOpen = parseDate(startStr);
        Date registrationClose = parseDate(deadlineStr);
        Date eventDate = parseDate(eventDateStr);
        if (registrationOpen == null || registrationClose == null || eventDate == null) {
            Toast.makeText(requireContext(), "Use date format: " + DATE_FORMAT, Toast.LENGTH_SHORT).show();
            return;
        }
        if (EventStatusUtils.isCalendarDayBefore(registrationOpen, EventStatusUtils.startOfToday())) {
            Toast.makeText(requireContext(), R.string.organizer_error_registration_start_past, Toast.LENGTH_SHORT).show();
            return;
        }
        if (EventStatusUtils.isCalendarDayBefore(registrationClose, registrationOpen)) {
            Toast.makeText(requireContext(), R.string.organizer_error_dates, Toast.LENGTH_SHORT).show();
            return;
        }
        if (EventStatusUtils.isCalendarDayBefore(eventDate, registrationClose)) {
            Toast.makeText(requireContext(), R.string.organizer_error_event_after_deadline, Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(eventDate);
        eventCal.set(Calendar.HOUR_OF_DAY, eventTimeHour);
        eventCal.set(Calendar.MINUTE, eventTimeMinute);
        eventCal.set(Calendar.SECOND, 0);
        eventCal.set(Calendar.MILLISECOND, 0);
        Date eventDateTime = eventCal.getTime();

        if (selectedCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one category", Toast.LENGTH_SHORT).show();
            return;
        }


        Integer waitlistLimit = null;
        if (!waitlistStr.isEmpty()) {
            try {
                int w = Integer.parseInt(waitlistStr);
                if (w >= 0) waitlistLimit = w;
            } catch (NumberFormatException ignored) { }
        }

        // Device id plays the role of "organizer account" for this project.
        String organizerId = DeviceUtils.getDeviceId(requireContext());
        boolean geolocationRequired = switchGeolocation.isChecked();
        boolean isPrivate = switchPrivateEvent.isChecked();

            // After running into issues with a full Firestore-driven image
            // subscription approach, I asked ChatGPT to walk me through a
            // simpler alternative. This version just stores the local URI
            // string for the banner image instead of wiring up a complex
            // upload/subscription flow.
        // Decide which banner image (if any) we should persist with this event.
        String posterUrlForSave = com.example.waitwell.OrganizerPosterUtils.resolvePosterUrl(
                posterUri != null ? posterUri.toString() : null,
                (eventIdToEdit != null && !eventIdToEdit.trim().isEmpty()) ? existingPosterUrl : null
        );

        // If we already have an id we treat this as an edit, otherwise it’s a brand new event.
        if (eventIdToEdit != null && !eventIdToEdit.trim().isEmpty()) {
            updateEventInFirestore(eventIdToEdit, organizerId, title, description, location,
                    geolocationRequired, isPrivate, registrationOpen, registrationClose, eventDateTime, waitlistLimit, price, posterUrlForSave);
        } else {
            saveEventToFirestore(organizerId, title, description, location, geolocationRequired, isPrivate,
                    registrationOpen, registrationClose, eventDateTime, waitlistLimit, price, posterUrlForSave);
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

    private void saveEventToFirestore(String organizerId, String title, String description, String location,
                                      boolean geolocationRequired, boolean isPrivate, Date registrationOpen, Date registrationClose,
                                      Date eventDate,
                                      Integer waitlistLimit, double price, @Nullable String posterUrl) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("description", description);
        event.put("location", location);
        event.put("geolocationRequired", geolocationRequired);
        event.put("registrationOpen", registrationOpen);
        event.put("registrationClose", registrationClose);
        event.put("eventDate", eventDate);
        if (waitlistLimit != null) event.put("waitlistLimit", waitlistLimit);
        event.put("price", price);
        event.put("organizerId", organizerId);
        event.put("createdAt", FieldValue.serverTimestamp());
        event.put("status", "open");
        event.put("isPrivate", isPrivate);
        event.put("waitlistEntrantIds", new ArrayList<String>());
        event.put("categories", selectedCategories);

        if (posterUrl != null) event.put("imageUrl", posterUrl); // optional banner image

        // New document in the "events" collection – Firestore will generate the id.
        FirebaseHelper.getInstance().getDb().collection("events").add(event)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(requireContext(), R.string.organizer_event_created, Toast.LENGTH_SHORT).show();
                    String newEventId = docRef.getId();
                    // Drop the organizer into the little "event created" confirmation fragment.
                    Fragment fragment = OrganizerEventCreatedFragment.newInstance(
                            newEventId,
                            title,
                            posterUrl,
                            isPrivate
                    );
                    if (getActivity() instanceof OrganizerEntryActivity) {
                        ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(fragment);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Save event failed", e);
                    Toast.makeText(requireContext(), R.string.organizer_error_save, Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEventInFirestore(String eventId, String organizerId, String title, String description, String location,
                                        boolean geolocationRequired, boolean isPrivate, Date registrationOpen, Date registrationClose,
                                        Date eventDate,
                                        Integer waitlistLimit, double price, @Nullable String posterUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("location", location);
        updates.put("geolocationRequired", geolocationRequired);
        updates.put("registrationOpen", registrationOpen);
        updates.put("registrationClose", registrationClose);
        updates.put("eventDate", eventDate);
        updates.put("categories", selectedCategories.isEmpty() ? null : selectedCategories);

        if (waitlistLimit != null) {
            updates.put("waitlistLimit", waitlistLimit);
        } else {
            updates.put("waitlistLimit", null);
        }
        updates.put("price", price);
        updates.put("organizerId", organizerId);
        updates.put("isPrivate", isPrivate);
        if (posterUrl != null) {
            updates.put("imageUrl", posterUrl);
        }

        // Update the existing document for this event rather than creating a new one.
        FirebaseHelper.getInstance().getDb().collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), R.string.organizer_event_created, Toast.LENGTH_SHORT).show();
                    // On a successful edit we just pop back to wherever the organizer came from.
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Update event failed", e);
                    Toast.makeText(requireContext(), R.string.organizer_error_save, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadEventForEdit(@NonNull String eventId) {
        FirebaseHelper.getInstance().getDb()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        return;
                    }
                    String title = doc.getString("title");
                    String location = doc.getString("location");
                    String description = doc.getString("description");
                    Boolean geoRequired = doc.getBoolean("geolocationRequired");
                    Boolean privateEvent = doc.getBoolean("isPrivate");
                    Date regOpen = doc.getDate("registrationOpen");
                    Date regClose = doc.getDate("registrationClose");
                    Number waitlist = doc.getLong("waitlistLimit");
                    Double price = doc.getDouble("price");
                    existingPosterUrl = doc.getString("imageUrl");

                    SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT, Locale.US);

                    if (title != null) editEventName.setText(title);
                    if (location != null) editLocation.setText(location);
                    if (description != null) editDescription.setText(description);
                    if (geoRequired != null) switchGeolocation.setChecked(geoRequired);
                    switchPrivateEvent.setChecked(privateEvent != null && privateEvent);
                    if (regOpen != null) editRegistrationStart.setText(fmt.format(regOpen));
                    if (regClose != null) editRegistrationDeadline.setText(fmt.format(regClose));
                    Date evDate = doc.getDate("eventDate");
                    if (evDate != null) {
                        editEventDate.setText(fmt.format(evDate));
                        Calendar tc = Calendar.getInstance();
                        tc.setTime(evDate);
                        eventTimeHour = tc.get(Calendar.HOUR_OF_DAY);
                        eventTimeMinute = tc.get(Calendar.MINUTE);
                        editEventTime.setText(formatTimeLabel(eventTimeHour, eventTimeMinute));
                    } else {
                        eventTimeHour = -1;
                        editEventTime.setText("");
                    }
                    if (waitlist != null) {
                        int w = waitlist.intValue();
                        if (w > 0) {
                            editWaitlistLimit.setText(String.valueOf(w));
                        }
                    }
                    if (price != null) {
                        editPrice.setText(String.format(Locale.US, "%.2f", price));
                    }
                    // If the event already had a banner we show a little "uploaded" hint.
                    if (existingPosterUrl != null) {
                        txtPosterStatus.setVisibility(View.VISIBLE);
                        txtPosterStatus.setText(R.string.organizer_poster_uploaded);
                    }
                    List<String> categories = (List<String>) doc.get("categories");
                    if (categories != null) {
                        selectedCategories.clear();
                        selectedCategories.addAll(categories);
                        txtCategories.setText(String.join(", ", selectedCategories));
                    } else {
                        txtCategories.setText("Select categories");
                    }

                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load event for edit", e));
    }


}
