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
import java.util.Locale;

public class EntrantChosenAccept extends AppCompatActivity {
    private static final String TAG = "EntrantChosenAccept";

    // UI elements for event details
    private String eventId;
    private String notificationId;
    private ImageView imgEventPoster;
    private TextView txtEventTitle;
    private TextView txtEventLocation;
    private TextView txtEventDateRange;
    private TextView txtEventPrice;
    private TextView messageText;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chosen_notification);

        // Initialize UI elements
        imgEventPoster = findViewById(R.id.imgEventPoster);
        txtEventTitle = findViewById(R.id.eventTitle);
        txtEventLocation = findViewById(R.id.txtEventLocation);
        txtEventDateRange = findViewById(R.id.txtEventDateRange);
        txtEventPrice = findViewById(R.id.txtEventPrice);
        messageText = findViewById(R.id.message);

        // Get data from intent
        eventId = getIntent().getStringExtra("eventId");
        notificationId = getIntent().getStringExtra("notificationId");
        String eventName = getIntent().getStringExtra("eventName");
        String message = getIntent().getStringExtra("message");

        // Set initial values from intent
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


        Button acceptButton = findViewById(R.id.accept);
        acceptButton.setOnClickListener(v -> handleAcceptEvent());

        Button declineButton = findViewById(R.id.decline);
        declineButton.setOnClickListener(v -> handleDeclineEvent());

        //back button to notifications screen
        Button backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EntrantChosenAccept.this, EntrantNotificationScreen.class);
                startActivity(intent);
            }

        });
    }

    /**
     *  load the event details from firestore
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
     *  bind the event details to the ui to be displayed
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

        // Price
        Double priceObj = doc.getDouble("price");
        String priceText;
        if (priceObj == null || priceObj == 0.0) {
            priceText = "FREE";
        } else {
            priceText = String.format(Locale.US, "$%.2f", priceObj);
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
     *  load the event poster image from firestore
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
     *  handle the accept for the entrant  and update their status in the waitlist and firestore
     */
    private void handleAcceptEvent() {
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, "Event information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user ID
        String userId = DeviceUtils.getDeviceId(this);
        String entryId = userId + "_" + eventId;

        // Disable buttons during operation
        findViewById(R.id.accept).setEnabled(false);
        findViewById(R.id.decline).setEnabled(false);

        // Use Firebase transaction for atomic updates
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.runTransaction(transaction -> {
            // Update WaitlistEntry status to "confirmed"
            DocumentReference entryRef = db.collection("waitlist_entries").document(entryId);
            transaction.update(entryRef, "status", "confirmed");

            // Add to AttendingEntrants array in event document
            DocumentReference eventRef = db.collection("events").document(eventId);
            transaction.update(eventRef, "AttendingEntrants", FieldValue.arrayUnion(userId));

            // Remove from waitlistEntrantIds since they're confirmed
            transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(userId));

            return null;
        })
        .addOnSuccessListener(aVoid -> {
            // Mark notification as responded if we have the ID
            if (notificationId != null) {
                FirebaseHelper.getInstance().markNotificationResponded(notificationId, task -> {
                    // Log but don't block on notification update
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Failed to mark notification as responded", task.getException());
                    }
                });
            }

            Toast.makeText(this, "You have successfully accepted the event invitation!", Toast.LENGTH_LONG).show();
            // Navigate back to notifications screen
            Intent intent = new Intent(this, EntrantNotificationScreen.class);
            startActivity(intent);
            finish();
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Failed to accept event", e);
            Toast.makeText(this, "Failed to accept invitation. Please try again.", Toast.LENGTH_SHORT).show();
            // Re-enable buttons on failure
            findViewById(R.id.accept).setEnabled(true);
            findViewById(R.id.decline).setEnabled(true);
        });
    }

    /**
     *  handle the decline for the entrant  and update their status in the waitlist and firestore
     */
    private void handleDeclineEvent() {
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, "Event information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user ID
        String userId = DeviceUtils.getDeviceId(this);
        String entryId = userId + "_" + eventId;

        // Disable buttons during operation
        findViewById(R.id.accept).setEnabled(false);
        findViewById(R.id.decline).setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.runTransaction(transaction -> {
            // Update WaitlistEntry status to "rejected"
            DocumentReference entryRef = db.collection("waitlist_entries").document(entryId);
            transaction.update(entryRef, "status", "cancelled");
            //changing status from "rejected" to "cancelled" to fix bug

            // Remove from waitlistEntrantIds
            DocumentReference eventRef = db.collection("events").document(eventId);
            transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(userId));

            // Do NOT add to AttendingEntrants

            return null;
        })
        .addOnSuccessListener(aVoid -> {
            // Mark notification as responded if we have the ID
            if (notificationId != null) {
                FirebaseHelper.getInstance().markNotificationResponded(notificationId, task -> {
                    // Log but don't block on notification update
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Failed to mark notification as responded", task.getException());
                    }
                });
            }

            Toast.makeText(this, "You have declined the event invitation.", Toast.LENGTH_LONG).show();
            // Navigate back
            Intent intent = new Intent(this, EntrantNotificationScreen.class);
            startActivity(intent);
            finish();
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Failed to decline event", e);
            Toast.makeText(this, "Failed to decline invitation. Please try again.", Toast.LENGTH_SHORT).show();
            // Re-enable buttons on failure
            findViewById(R.id.accept).setEnabled(true);
            findViewById(R.id.decline).setEnabled(true);
        });
    }

}
