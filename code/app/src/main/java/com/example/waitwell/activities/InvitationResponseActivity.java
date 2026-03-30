package com.example.waitwell.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Entrant screen to accept or decline an event invitation. Firestore "invited" state is {@code selected}
 * (lottery / organizer accept); final state is {@code confirmed}; declined is {@code cancelled}.
 */
public class InvitationResponseActivity extends AppCompatActivity {

    private static final String TAG = "InvitationResponse";

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_NAME = "eventName";
    public static final String EXTRA_EVENT_LOCATION = "eventLocation";
    public static final String EXTRA_EVENT_DATE_RANGE = "eventDateRange";
    public static final String EXTRA_EVENT_PRICE = "eventPrice";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notificationId";

    private String eventId;
    private String notificationId;
    private TextView txtEventTitle;
    private TextView txtEventLocation;
    private TextView txtEventDateRange;
    private TextView txtEventPrice;
    private TextView messageText;

    /**
     * Fills intent extras for location, registration / event date range, and formatted price from a loaded event.
     */
    public static void putEventFieldsFromSnapshot(@NonNull Intent intent, @NonNull DocumentSnapshot doc,
                                                   @NonNull Context ctx) {
        String title = doc.getString("title");
        if (!TextUtils.isEmpty(title)) {
            intent.putExtra(EXTRA_EVENT_NAME, title);
        }

        String location = doc.getString("location");
        if (TextUtils.isEmpty(location)) {
            location = ctx.getString(R.string.event_detail_location_not_specified);
        }
        intent.putExtra(EXTRA_EVENT_LOCATION, location);

        Timestamp openTs = doc.getTimestamp("registrationOpen");
        Timestamp closeTs = doc.getTimestamp("registrationClose");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateText;
        if (openTs != null && closeTs != null) {
            Date open = openTs.toDate();
            Date close = closeTs.toDate();
            dateText = fmt.format(open) + "  -  " + fmt.format(close);
        } else {
            Date eventDate = doc.getDate("eventDate");
            if (eventDate != null) {
                dateText = fmt.format(eventDate);
            } else {
                dateText = ctx.getString(R.string.organizer_date_not_set);
            }
        }
        intent.putExtra(EXTRA_EVENT_DATE_RANGE, dateText);

        Double priceObj = doc.getDouble("price");
        String priceStr;
        if (priceObj == null || priceObj == 0.0) {
            priceStr = ctx.getString(R.string.organizer_price_free);
        } else {
            priceStr = ctx.getString(R.string.invitation_price_formatted,
                    String.format(Locale.US, "%.2f", priceObj));
        }
        intent.putExtra(EXTRA_EVENT_PRICE, priceStr);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_response);

        txtEventTitle = findViewById(R.id.eventTitle);
        txtEventLocation = findViewById(R.id.txtEventLocation);
        txtEventDateRange = findViewById(R.id.txtEventDateRange);
        txtEventPrice = findViewById(R.id.txtEventPrice);
        messageText = findViewById(R.id.message);

        Intent in = getIntent();
        eventId = in.getStringExtra(EXTRA_EVENT_ID);
        notificationId = in.getStringExtra(EXTRA_NOTIFICATION_ID);
        String eventName = in.getStringExtra(EXTRA_EVENT_NAME);
        String message = in.getStringExtra(EXTRA_MESSAGE);

        if (!TextUtils.isEmpty(eventName)) {
            txtEventTitle.setText(eventName);
        }

        boolean preFilled = in.hasExtra(EXTRA_EVENT_LOCATION)
                && in.hasExtra(EXTRA_EVENT_DATE_RANGE)
                && in.hasExtra(EXTRA_EVENT_PRICE);
        if (preFilled) {
            txtEventLocation.setText(in.getStringExtra(EXTRA_EVENT_LOCATION));
            txtEventDateRange.setText(in.getStringExtra(EXTRA_EVENT_DATE_RANGE));
            txtEventPrice.setText(in.getStringExtra(EXTRA_EVENT_PRICE));
        }

        applyInvitationMessage(message, eventName);

        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        btnHamburger.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });

        ImageView imgProfile = findViewById(R.id.imgProfileAvatar);
        imgProfile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

        TextView txtBackLink = findViewById(R.id.txtBackLink);
        txtBackLink.setOnClickListener(v -> finish());

        if (!preFilled && !TextUtils.isEmpty(eventId)) {
            loadEvent();
        } else if (!preFilled && TextUtils.isEmpty(eventId)) {
            Log.e(TAG, "No event id and no prefilled card");
            Snackbar.make(findViewById(android.R.id.content), R.string.invitation_error_no_event, Snackbar.LENGTH_LONG).show();
        }

        Button acceptButton = findViewById(R.id.accept);
        acceptButton.setOnClickListener(v -> handleAcceptEvent());

        Button declineButton = findViewById(R.id.decline);
        declineButton.setOnClickListener(v -> handleDeclineEvent());

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v ->
                startActivity(new Intent(this, EntrantNotificationScreen.class)));
    }

    private void applyInvitationMessage(String message, String eventName) {
        if (!TextUtils.isEmpty(message)) {
            messageText.setText(message);
        } else if (!TextUtils.isEmpty(eventName)) {
            messageText.setText(getString(R.string.waitlist_chosen_notification_message, eventName));
        }
    }

    private void loadEvent() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Snackbar.make(findViewById(android.R.id.content),
                                R.string.invitation_error_not_found, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Intent fill = new Intent();
                    putEventFieldsFromSnapshot(fill, doc, this);
                    String title = doc.getString("title");
                    if (!TextUtils.isEmpty(title)) {
                        txtEventTitle.setText(title);
                    }
                    txtEventLocation.setText(fill.getStringExtra(EXTRA_EVENT_LOCATION));
                    txtEventDateRange.setText(fill.getStringExtra(EXTRA_EVENT_DATE_RANGE));
                    txtEventPrice.setText(fill.getStringExtra(EXTRA_EVENT_PRICE));

                    String titleForMessage = txtEventTitle.getText() != null
                            ? txtEventTitle.getText().toString()
                            : "";
                    if (TextUtils.isEmpty(getIntent().getStringExtra(EXTRA_MESSAGE))
                            && !TextUtils.isEmpty(titleForMessage)) {
                        messageText.setText(getString(R.string.waitlist_chosen_notification_message, titleForMessage));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event " + eventId, e);
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.invitation_error_load_details, Snackbar.LENGTH_LONG).show();
                });
    }

    private void handleAcceptEvent() {
        if (TextUtils.isEmpty(eventId)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.invitation_error_no_event, Snackbar.LENGTH_LONG).show();
            return;
        }

        String userId = DeviceUtils.getDeviceId(this);
        String entryId = userId + "_" + eventId;
        String confirmed = getString(R.string.firestore_waitlist_status_confirmed);

        findViewById(R.id.accept).setEnabled(false);
        findViewById(R.id.decline).setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.runTransaction(transaction -> {
            DocumentReference entryRef = db.collection("waitlist_entries").document(entryId);
            transaction.update(entryRef, "status", confirmed);
            DocumentReference eventRef = db.collection("events").document(eventId);
            transaction.update(eventRef, "AttendingEntrants", FieldValue.arrayUnion(userId));
            transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(userId));
            return null;
        })
        .addOnSuccessListener(aVoid -> {
            if (notificationId != null) {
                FirebaseHelper.getInstance().markNotificationResponded(notificationId, task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "markNotificationResponded failed", task.getException());
                    }
                });
            }
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.invitation_accept_success, Snackbar.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateToEntrantHome, 500);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "accept failed", e);
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.invitation_error_accept, Snackbar.LENGTH_LONG).show();
            findViewById(R.id.accept).setEnabled(true);
            findViewById(R.id.decline).setEnabled(true);
        });
    }

    private void navigateToEntrantHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void handleDeclineEvent() {
        if (TextUtils.isEmpty(eventId)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.invitation_error_no_event, Snackbar.LENGTH_LONG).show();
            return;
        }

        String userId = DeviceUtils.getDeviceId(this);
        String entryId = userId + "_" + eventId;
        String cancelled = getString(R.string.firestore_waitlist_status_cancelled);

        findViewById(R.id.accept).setEnabled(false);
        findViewById(R.id.decline).setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.runTransaction(transaction -> {
            DocumentReference entryRef = db.collection("waitlist_entries").document(entryId);
            transaction.update(entryRef, "status", cancelled);
            DocumentReference eventRef = db.collection("events").document(eventId);
            transaction.update(eventRef, "waitlistEntrantIds", FieldValue.arrayRemove(userId));
            return null;
        })
        .addOnSuccessListener(aVoid -> {
            if (notificationId != null) {
                FirebaseHelper.getInstance().markNotificationResponded(notificationId, task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "markNotificationResponded failed", task.getException());
                    }
                });
            }
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.invitation_decline_success, Snackbar.LENGTH_SHORT).show();
            Intent intent = new Intent(this, WaitListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "decline failed", e);
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.invitation_error_decline, Snackbar.LENGTH_LONG).show();
            findViewById(R.id.accept).setEnabled(true);
            findViewById(R.id.decline).setEnabled(true);
        });
    }
}
