package com.example.waitwell;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EntrantNotChosenScreen extends AppCompatActivity {
    private static final String TAG = "EntrantNotChosenScreen";

    // UI elements for event details
    private String eventId;
    private String notificationId;  // To mark notification as responded
    private ImageView imgEventPoster;
    private TextView txtEventTitle;
    private TextView txtEventLocation;
    private TextView txtEventDateRange;
    private TextView txtEventPrice;
    private TextView messageText;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.not_chosen_notification);

        // Initialize UI elements
        imgEventPoster = findViewById(R.id.imgEventPoster);
        txtEventTitle = findViewById(R.id.eventTitle);
        txtEventLocation = findViewById(R.id.txtEventLocation);
        txtEventDateRange = findViewById(R.id.txtEventDateRange);
        txtEventPrice = findViewById(R.id.txtEventPrice);
        messageText = findViewById(R.id.message);

        // get data from intent
        eventId = getIntent().getStringExtra("eventId");
        notificationId = getIntent().getStringExtra("notificationId");
        String eventName = getIntent().getStringExtra("eventName");
        String message = getIntent().getStringExtra("message");

        // set initial values from intent
        if (eventName != null) {
            txtEventTitle.setText(eventName);
        }
        if (message != null) {
            messageText.setText(message);
        }

        // Load full event details from Firebase
        if (!TextUtils.isEmpty(eventId)) {
            loadEvent();
        } else {
            Log.e(TAG, "No eventId provided in intent");
            Toast.makeText(this, "Event information not available", Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.btnHamburger).setOnClickListener(v -> finish());
        findViewById(R.id.btnOrganizerBack).setOnClickListener(v -> finish());

        Button backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());

        Button redrawButton = findViewById(R.id.entrantRedraw);
        redrawButton.setOnClickListener(v -> handleEnterRedraw());
    }

    /**
     * load details from firestore for events, to be displayed on the ui
     */
    private void loadEvent() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event " + eventId, e);
                    Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     *  this is for setting the event to the ui
     */
    private void bindEvent(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Title - already set from intent, but update if needed
        String title = doc.getString("title");
        if (title != null && !title.trim().isEmpty()) {
            txtEventTitle.setText(title);
        }

        // Location
        String location = doc.getString("location");
        if (location == null || location.trim().isEmpty()) {
            location = "Location not specified";
        }
        txtEventLocation.setText(location);

        // Date range
        Timestamp openTs = doc.getTimestamp("registrationOpen");
        Timestamp closeTs = doc.getTimestamp("registrationClose");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateText;
        if (openTs != null && closeTs != null) {
            Date open = openTs.toDate();
            Date close = closeTs.toDate();
            dateText = fmt.format(open) + "  -  " + fmt.format(close);
        } else {
            dateText = "Dates not specified";
        }
        txtEventDateRange.setText(dateText);

        Double priceObj = doc.getDouble("price");
        String priceText;

        if (priceObj == null || priceObj == 0.0) {
            priceText = "FREE";
        } else {
            priceText = String.format(Locale.US, "$%.2f", priceObj); //2 decimal places
        }
        txtEventPrice.setText(priceText);

        // Event poster image
        String imageUrl = doc.getString("imageUrl");
        if (imageUrl == null) {
            imageUrl = doc.getString("posterUrl");
        }
        if (!TextUtils.isEmpty(imageUrl)) {
            loadPosterImage(imageUrl);
        }
    }

    /**
     *  load the poster image for the ui
     */
    private void loadPosterImage(String url) {
        // If it's a local content/file URI, load directly from the device
        if (url.startsWith("content:") || url.startsWith("file:")) {
            try {
                imgEventPoster.setImageURI(android.net.Uri.parse(url));
            } catch (Exception e) {
                Log.w(TAG, "Failed to load local poster URI: " + url, e);
            }
            return;
        }

        // Fallback for remote storage URLs
        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            final long ONE_MB = 1024 * 1024;
            ref.getBytes(ONE_MB)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            imgEventPoster.setImageBitmap(bitmap);
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Failed to load poster image", e));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid poster URL: " + url, e);
        }
    }

    /**
     *  Handle the redraw for the entrant ,. put them back into the pool of entrants
     *  updates their status in the firestore collection back to waiting and adds them backj to the waitlistentrantids
     */
    private void handleEnterRedraw() {
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, "Event information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user ID
        String userId = DeviceUtils.getDeviceId(this);
        String entryId = userId + "_" + eventId;

        // Disable button during operation
        findViewById(R.id.entrantRedraw).setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First check if user already has an entry
        db.collection("waitlist_entries")
                .document(entryId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        db.collection("events")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (isEventFullFromSnapshot(eventDoc)) {
                                        Toast.makeText(this, R.string.event_detail_event_full, Toast.LENGTH_LONG).show();
                                        findViewById(R.id.entrantRedraw).setEnabled(true);
                                        return;
                                    }
                                    // Update existing entry back to "waiting" status
                                    enterRedrawAfterCapacityOk(doc, db, userId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to load event for redraw check", e);
                                    Toast.makeText(this, "Failed to enter redraw. Please try again.", Toast.LENGTH_SHORT).show();
                                    findViewById(R.id.entrantRedraw).setEnabled(true);
                                });
                    } else {
                        // Create new waitlist entry (shouldn't normally happen, but handle it)
                        createNewWaitlistEntry(userId, eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check waitlist entry", e);
                    Toast.makeText(this, "Error checking waitlist status", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.entrantRedraw).setEnabled(true);
                });
    }

    @SuppressWarnings("unchecked")
    private static boolean isEventFullFromSnapshot(DocumentSnapshot eventDoc) {
        if (eventDoc == null || !eventDoc.exists()) {
            return true;
        }
        Long limitVal = eventDoc.getLong("waitlistLimit");
        if (limitVal == null || limitVal <= 0) {
            return false;
        }
        List<String> attending = (List<String>) eventDoc.get("AttendingEntrants");
        int confirmed = attending != null ? attending.size() : 0;
        return confirmed >= limitVal.intValue();
    }

    private void enterRedrawAfterCapacityOk(DocumentSnapshot entryDoc, FirebaseFirestore db, String userId) {
        entryDoc.getReference().update("status", "waiting")
                .addOnSuccessListener(aVoid -> {
                    db.collection("events")
                            .document(eventId)
                            .update("waitlistEntrantIds", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener(unused -> {
                                if (notificationId != null) {
                                    FirebaseHelper.getInstance().markNotificationResponded(notificationId, task -> {
                                        if (!task.isSuccessful()) {
                                            Log.w(TAG, "Failed to mark notification as responded", task.getException());
                                        }
                                    });
                                }
                                Toast.makeText(this, "You have re-entered the waitlist for the redraw!", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update event waitlist", e);
                                Toast.makeText(this, "Failed to enter redraw. Please try again.", Toast.LENGTH_SHORT).show();
                                findViewById(R.id.entrantRedraw).setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update waitlist entry", e);
                    Toast.makeText(this, "Failed to enter redraw. Please try again.", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.entrantRedraw).setEnabled(true);
                });
    }

    /**
     *  create a new waitlist entry for the entrant
     */
    private void createNewWaitlistEntry(String userId, String eventId) {
        // Get event title for the entry
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (isEventFullFromSnapshot(eventDoc)) {
                        Toast.makeText(this, R.string.event_detail_event_full, Toast.LENGTH_LONG).show();
                        findViewById(R.id.entrantRedraw).setEnabled(true);
                        return;
                    }
                    String eventTitle = eventDoc.getString("title");
                    if (eventTitle == null) {
                        eventTitle = "Unknown Event";
                    }

                    // Use existing FirebaseHelper method to join waitlist
                    FirebaseHelper.getInstance().joinWaitlist(userId, eventId, eventTitle, task -> {
                        if (task.isSuccessful()) {
                            // Mark notification as responded if we have the ID
                            if (notificationId != null) {
                                FirebaseHelper.getInstance().markNotificationResponded(notificationId, markTask -> {
                                    // Log but don't block on notification update
                                    if (!markTask.isSuccessful()) {
                                        Log.w(TAG, "Failed to mark notification as responded", markTask.getException());
                                    }
                                });
                            }
                            Toast.makeText(this, "You have entered the waitlist for the redraw!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Log.e(TAG, "Failed to join waitlist", task.getException());
                            if (isEventFullFailure(task.getException())) {
                                Toast.makeText(this, R.string.event_detail_event_full, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Failed to enter redraw. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                            findViewById(R.id.entrantRedraw).setEnabled(true);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get event details", e);
                    Toast.makeText(this, "Failed to enter redraw. Please try again.", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.entrantRedraw).setEnabled(true);
                });
    }

    private static boolean isEventFullFailure(Throwable e) {
        while (e != null) {
            if (e instanceof IllegalStateException
                    && FirebaseHelper.EVENT_FULL_MESSAGE.equals(e.getMessage())) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }
}
