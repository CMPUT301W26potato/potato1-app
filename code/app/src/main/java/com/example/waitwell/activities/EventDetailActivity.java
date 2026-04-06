package com.example.waitwell.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.TextViewCompat;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.EventStatusUtils;
import com.example.waitwell.EntrantNotificationScreen;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.example.waitwell.WaitlistFirestoreStatus;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
// REHAAN'S ADDITION — US 02.02.02
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

// END REHAAN'S ADDITION






/**
 * Event detail screen (US 01.01.01 – join waitlist).
 *
 * Receives "event_id" via Intent extra, loads the event from Firestore,
 * and shows all details. The "Join Waitlist Now" button calls
 * FirebaseHelper.joinWaitlist() then navigates to the confirmation screen.
 *
 * If the user is already on this event's waitlist, the button is hidden and a message is shown instead.
 */
public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";

    private TextView txtTitle, txtLocation, txtPrice, txtRegistered;
    private TextView txtEventDate, txtEventTime, txtTimeRemaining, txtRating, txtDescription;
    private ImageView imgEventPoster;
    private AppCompatButton btnJoin;
    private TextView txtJoinBlockedMessage;
    private View joinButtonContainer;
    private String eventId, deviceId;
    private boolean shownWaitlistStatusSnack;
    private EditText editComment;
    private TextView txtCategory;
    private TextView txtEventClosed;


    // REHAAN'S ADDITION — US 02.02.02
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private String pendingJoinTitle;
    private boolean pendingGeolocationRequired;
    // END REHAAN'S ADDITION
    private View btnPostComment;
    private LinearLayout commentsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        deviceId = DeviceUtils.getDeviceId(this);
        // REHAAN'S ADDITION — US 02.02.02
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // END REHAAN'S ADDITION
        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null) { finish(); return; }
        txtCategory = findViewById(R.id.txtCategory);


        initViews();
        loadEvent();
        loadComments();
        setupBottomNav();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        shownWaitlistStatusSnack = false;
        loadEvent();
    }

    private void initViews() {
        txtTitle= findViewById(R.id.txtTitle);
        txtLocation = findViewById(R.id.txtLocation);
        txtPrice = findViewById(R.id.txtPrice);
        txtRegistered = findViewById(R.id.txtRegistered);
        txtEventDate = findViewById(R.id.txtEventDate);
        txtEventTime = findViewById(R.id.txtEventTime);
        txtTimeRemaining = findViewById(R.id.txtTimeRemaining);
        txtRating = findViewById(R.id.txtRating);
        txtDescription = findViewById(R.id.txtDescription);
        btnJoin = findViewById(R.id.btnJoinWaitlist);
        txtJoinBlockedMessage = findViewById(R.id.txtJoinBlockedMessage);
        joinButtonContainer = findViewById(R.id.joinButtonContainer);
        imgEventPoster = findViewById(R.id.imgEventPoster);
        editComment = findViewById(R.id.editComment);
        btnPostComment = findViewById(R.id.btnPostComment);
        commentsContainer = findViewById(R.id.commentsContainer);
        btnPostComment.setOnClickListener(v -> postComment());
        txtEventClosed = findViewById(R.id.txtEventClosed);


        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnJoin.setOnClickListener(v -> joinWaitlist());
    }

    private void loadEvent() {
        FirebaseHelper.getInstance().getEvent(eventId)
                .addOnSuccessListener(this::populateUI)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event", e);
                    Toast.makeText(this, "Could not load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @SuppressWarnings("unchecked")
    private void populateUI(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, R.string.event_no_longer_available, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = doc.getString("title");
        String location = doc.getString("location");
        String desc = doc.getString("description");
        Double price = doc.getDouble("price");
        Double rating = doc.getDouble("rating");
        List<String> waitlist = (List<String>) doc.get("waitlistEntrantIds");
        List<String> attending = (List<String>) doc.get("AttendingEntrants");
        List<String> category = (List<String>) doc.get("categories");

        if (category != null && !category.isEmpty()) {
            txtCategory.setText(String.join(", ", category));
        } else {
            txtCategory.setVisibility(View.GONE);
        }


        String imageUrl = doc.getString("imageUrl");
        if (!TextUtils.isEmpty(imageUrl) && !imageUrl.startsWith("content:") && !imageUrl.startsWith("file:")) {
            loadPosterImage(imageUrl);
        }

        txtTitle.setText(title != null ? title : "");
        txtLocation.setText(location != null ? location : "");
        txtDescription.setText(desc != null ? desc : "");
        txtPrice.setText(price != null ? String.format("$%.0f", price) : "Free");
        txtRating.setText(rating != null ? String.format("%.1f", rating) : "—");

        int count = (waitlist != null) ? waitlist.size() : 0;
        txtRegistered.setText(count + " Registered");

        Date eventDate = doc.getDate("eventDate");
        if (eventDate != null) {
            txtEventDate.setText(new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(eventDate));
            txtEventTime.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(eventDate));
        } else {
            txtEventDate.setText(R.string.event_detail_event_date_not_set);
            txtEventTime.setText(R.string.event_detail_event_date_not_set);
        }

        String lifecycle = EventStatusUtils.computeStatus(doc);
        if ("open".equals(lifecycle)) {
            Date registrationClose = doc.getDate("registrationClose");
            if (registrationClose != null) {
                long diffMs = registrationClose.getTime() - System.currentTimeMillis();
                if (diffMs <= 0) {
                    txtTimeRemaining.setText(R.string.event_detail_closes_today);
                } else {
                    long days = TimeUnit.MILLISECONDS.toDays(diffMs);
                    if (days == 0) {
                        txtTimeRemaining.setText(R.string.event_detail_closes_today);
                    } else if (days == 1) {
                        txtTimeRemaining.setText(R.string.event_detail_open_one_day);
                    } else {
                        txtTimeRemaining.setText(getString(R.string.event_detail_open_for_days, days));
                    }
                }
            } else {
                txtTimeRemaining.setText("Open to register");
            }
        } else if ("closed".equals(lifecycle)) {
            txtTimeRemaining.setText("Registration closed");
        } else {
            txtTimeRemaining.setText(R.string.event_detail_status_completed);
        }

        boolean isOpen = "open".equals(lifecycle);


        if (!isOpen) {
            txtEventClosed.setVisibility(View.VISIBLE);
            btnJoin.setEnabled(false);
            btnJoin.setText("Event Closed");
            btnJoin.setAlpha(0.6f); // optional: grey out
            txtJoinBlockedMessage.setVisibility(View.VISIBLE);
            txtJoinBlockedMessage.setText("This event has already ended.");
        } else {
            txtEventClosed.setVisibility(View.GONE);
            txtJoinBlockedMessage.setVisibility(View.GONE);
            btnJoin.setEnabled(true);
            btnJoin.setAlpha(1f);
        }


        boolean alreadyJoined = waitlist != null && waitlist.contains(deviceId);
        boolean alreadyFinalEntrant = attending != null && attending.contains(deviceId);
        Long waitlistLimitVal = doc.getLong("waitlistLimit");
        int limit = waitlistLimitVal != null ? waitlistLimitVal.intValue() : 0;
        int confirmedCount = attending != null ? attending.size() : 0;
        boolean eventFullByConfirmed = limit > 0 && confirmedCount >= limit;

        String entryDocId = deviceId + "_" + eventId;
        FirebaseFirestore.getInstance()
                .collection("waitlist_entries")
                .document(entryDocId)
                .get()
                .addOnSuccessListener(entryDoc -> {
                    String entryStatus = entryDoc.exists() ? entryDoc.getString("status") : null;
                    applyJoinAvailability(isOpen, alreadyJoined, alreadyFinalEntrant, entryStatus, eventFullByConfirmed);
                    maybeShowWaitlistStatusSnack(entryStatus);
                })
                .addOnFailureListener(e -> applyJoinAvailability(isOpen, alreadyJoined, alreadyFinalEntrant, null, eventFullByConfirmed));
    }

    /**
     * Firestore statuses that mean the entrant must not join the waitlist again from this screen.
     * Uses {@link WaitlistFirestoreStatus} values: invited/final/declined/not-selected entrants cannot rejoin.
     */
    private static boolean blocksWaitlistRejoin(String status) {
        return WaitlistFirestoreStatus.SELECTED.equals(status)
                || WaitlistFirestoreStatus.CONFIRMED.equals(status)
                || WaitlistFirestoreStatus.REJECTED.equals(status)
                || WaitlistFirestoreStatus.CANCELLED.equals(status);
    }

    private void applyJoinAvailability(boolean isOpen, boolean alreadyOnWaitlistArray,
                                       boolean alreadyInAttendingList, String entryStatus,
                                       boolean eventFullByConfirmedLimit) {
        if (joinButtonContainer == null) {
            return;
        }
        if (!isOpen) {
            joinButtonContainer.setVisibility(View.VISIBLE);
            btnJoin.setVisibility(View.VISIBLE);
            return;

        }
        if (alreadyOnWaitlistArray || alreadyInAttendingList) {
            joinButtonContainer.setVisibility(View.GONE);
            return;
        }
        joinButtonContainer.setVisibility(View.VISIBLE);
        if (blocksWaitlistRejoin(entryStatus)) {
            btnJoin.setVisibility(View.GONE);
            txtJoinBlockedMessage.setVisibility(View.VISIBLE);
            if (WaitlistFirestoreStatus.REJECTED.equals(entryStatus)) {
                txtJoinBlockedMessage.setText(R.string.event_detail_not_selected_for_event);
            } else {
                txtJoinBlockedMessage.setText(R.string.event_detail_already_registered);
            }
        } else if (eventFullByConfirmedLimit) {
            txtJoinBlockedMessage.setVisibility(View.GONE);
            btnJoin.setVisibility(View.VISIBLE);
            styleJoinButtonEventFull();
        } else {
            txtJoinBlockedMessage.setVisibility(View.GONE);
            btnJoin.setVisibility(View.VISIBLE);
            styleJoinButtonNormal();
        }
    }

    /** Grayed-out CTA when confirmed (final) entrants reached {@code waitlistLimit}. */
    private void styleJoinButtonEventFull() {
        btnJoin.setEnabled(false);
        btnJoin.setText(R.string.event_detail_event_full);
        btnJoin.setBackgroundResource(R.drawable.bg_event_detail_cta_disabled);
        btnJoin.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(btnJoin, null, null, null, null);
    }

    private void styleJoinButtonNormal() {
        btnJoin.setEnabled(true);
        btnJoin.setText(R.string.event_detail_join_waitlist);
        btnJoin.setBackgroundResource(R.drawable.bg_event_detail_cta);
        btnJoin.setTextColor(ContextCompat.getColor(this, R.color.text_white));
        Drawable end = AppCompatResources.getDrawable(this, R.drawable.ic_send_small);
        if (end != null) {
            end = DrawableCompat.wrap(end.mutate());
            DrawableCompat.setTint(end, ContextCompat.getColor(this, R.color.text_white));
        }
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(btnJoin, null, null, end, null);
        int pad = (int) (10 * getResources().getDisplayMetrics().density + 0.5f);
        btnJoin.setCompoundDrawablePadding(pad);
    }

    private void maybeShowWaitlistStatusSnack(String entryStatus) {
        if (shownWaitlistStatusSnack) {
            return;
        }
        if (WaitlistFirestoreStatus.REJECTED.equals(entryStatus)) {
            shownWaitlistStatusSnack = true;
            showWaitlistMessageSnack(R.string.event_detail_not_selected_for_event);
        } else if (WaitlistFirestoreStatus.CANCELLED.equals(entryStatus)) {
            shownWaitlistStatusSnack = true;
            showWaitlistMessageSnack(R.string.event_detail_invitation_declined);
        }
    }

    private void showWaitlistMessageSnack(int messageRes) {
        View anchor = findViewById(R.id.bottomNavigation);
        Snackbar sb = Snackbar.make(anchor, messageRes, Snackbar.LENGTH_LONG);
        sb.setAnchorView(anchor);
        sb.setBackgroundTint(ContextCompat.getColor(this, R.color.bg_white));
        sb.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        sb.show();
    }

    // Join Waitlist (US 01.01.01)
    private void joinWaitlist() {
        btnJoin.setEnabled(false);
        String title = txtTitle.getText().toString();
        String entryDocId = deviceId + "_" + eventId;
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    @SuppressWarnings("unchecked")
                    List<String> attending = eventDoc.exists()
                            ? (List<String>) eventDoc.get("AttendingEntrants")
                            : null;
                    if (attending != null && attending.contains(deviceId)) {
                        Toast.makeText(this, R.string.event_detail_already_registered, Toast.LENGTH_LONG).show();
                        btnJoin.setEnabled(true);
                        applyJoinAvailability(true, false, true, "confirmed", false);
                        return;
                    }
                    FirebaseFirestore.getInstance()
                .collection("waitlist_entries")
                .document(entryDocId)
                .get()
                .addOnSuccessListener(entryDoc -> {
                    String st = entryDoc.exists() ? entryDoc.getString("status") : null;
                    if (blocksWaitlistRejoin(st)) {
                        int msg = WaitlistFirestoreStatus.REJECTED.equals(st)
                                ? R.string.event_detail_not_selected_for_event
                                : R.string.event_detail_already_registered;
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        btnJoin.setEnabled(true);
                        applyJoinAvailability(true, false, false, st, false);
                        return;
                    }
                    runJoinWaitlist(title);
                })
                .addOnFailureListener(e -> {
                    btnJoin.setEnabled(true);
                    Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                });
                })
                .addOnFailureListener(e -> {
                    btnJoin.setEnabled(true);
                    Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                });
    }

    // REHAAN'S ADDITION — US 02.02.02 (modified runJoinWaitlist to capture location)
    private void runJoinWaitlist(String title) {
        FirebaseHelper.getInstance().getEvent(eventId).addOnSuccessListener(eventDoc -> {
            boolean geolocationRequired = Boolean.TRUE.equals(eventDoc.getBoolean("geolocationRequired"));
            FirebaseHelper.getInstance().joinWaitlist(
                    deviceId, eventId, title,
                    task -> {
                        if (task.isSuccessful()) {
                            if (geolocationRequired) {
                                pendingJoinTitle = title;
                                pendingGeolocationRequired = true;
                                captureAndSaveLocation();
                                // navigation deferred to captureAndSaveLocation flow
                            } else {
                                navigateToConfirmation(title);
                            }
                        } else {
                            Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                            btnJoin.setEnabled(true);
                        }
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show();
            btnJoin.setEnabled(true);
        });
    }
    // END REHAAN'S ADDITION
    /**
     * Requests location permission if needed, then saves the device's
     * last known location to the waitlist_entries document.
     * Best-effort — join already succeeded, so failure here is silent.
     * Javadoc written with help from Claude (claude.ai)
     */
// REHAAN'S ADDITION — US 02.02.02
    private void captureAndSaveLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not yet granted — request it; navigation happens in onRequestPermissionsResult
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        // Permission already granted — write location then navigate
        writeLocationToEntryThenNavigate();
    }
    // END REHAAN'S ADDITION

    /**
     * Reads last known location from FusedLocationProviderClient and
     * writes it to the waitlist_entries document for this user+event.
     */
    // REHAAN'S ADDITION — US 02.02.02
    private void writeLocationToEntryThenNavigate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            navigateToConfirmation(pendingJoinTitle);
            return;
        }
        String entryDocId = deviceId + "_" + eventId;
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        FirebaseHelper.getInstance().updateEntryLocation(
                                entryDocId,
                                location.getLatitude(),
                                location.getLongitude(),
                                null);
                    }
                    // Navigate regardless — location write is best-effort
                    navigateToConfirmation(pendingJoinTitle);
                })
                .addOnFailureListener(e -> navigateToConfirmation(pendingJoinTitle));
    }

    private void navigateToConfirmation(String title) {
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra("event_title", title != null ? title : "");
        startActivity(intent);
        finish();
    }
    // END REHAAN'S ADDITION

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // REHAAN'S ADDITION — US 02.02.02
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && pendingGeolocationRequired) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                writeLocationToEntryThenNavigate();
            } else {
                // Denied — join already succeeded, navigate without location
                navigateToConfirmation(pendingJoinTitle);
            }
        }
        // END REHAAN'S ADDITION
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

    /**
     * Sets up the bottom navigation bar for the activity. Each menu item
     * navigates to a different screen: Home, Waitlist, or Notifications.
     * Uses FLAG_ACTIVITY_CLEAR_TOP and FLAG_ACTIVITY_SINGLE_TOP to prevent
     * multiple instances of the same activity in the back stack.
     */
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_waitlist) {
                Intent intent = new Intent(this, WaitListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_notifications) {
                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    /**
     * If this user has a waitlist entry for the event with status {@code rejected} (organizer decline),
     * show an in-screen message on the event detail view.
     */
    private void checkWaitlistRejectedAndNotify() {
        String entryId = deviceId + "_" + eventId;
        FirebaseHelper.getInstance().getDb()
                .collection("waitlist_entries")
                .document(entryId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }
                    String status = doc.getString("status");
                    if (!WaitlistFirestoreStatus.REJECTED.equals(status)) {
                        return;
                    }
                    View anchor = findViewById(R.id.bottomNavigation);
                    Snackbar sb = Snackbar.make(
                            anchor,
                            R.string.event_detail_not_selected_for_event,
                            Snackbar.LENGTH_LONG);
                    sb.setAnchorView(anchor);
                    sb.setBackgroundTint(ContextCompat.getColor(this, R.color.bg_white));
                    sb.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    sb.show();
                })
                .addOnFailureListener(e -> Log.w(TAG, "waitlist status check failed", e));
    }
    /**
     * Posts a comment entered by the user. Validates input,
     * fetches username from 'users' collection, generates a unique
     * comment ID, and saves the comment to Firestore.
     * Refreshes the comment list after posting.
     */
    private void postComment() {
        String commentText = editComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // fetch username from users collection
        FirebaseHelper.getInstance().getDb()
                .collection("users")
                .document(deviceId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String username = "Anonymous";
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        if (name != null && !name.isEmpty()) username = name;
                    }

                    String commentId = FirebaseHelper.getInstance().getDb()
                            .collection("events")
                            .document(eventId)
                            .collection("comments")
                            .document()
                            .getId();

                    java.util.Map<String, Object> comment = new java.util.HashMap<>();
                    comment.put("text", commentText);
                    comment.put("userId", deviceId);
                    comment.put("username", username); // store username directly
                    comment.put("timestamp", new java.util.Date());

                    FirebaseHelper.getInstance().getDb()
                            .collection("events")
                            .document(eventId)
                            .collection("comments")
                            .document(commentId)
                            .set(comment)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show();
                                editComment.setText("");
                                loadComments(); // refresh
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show());
                });
    }
    /**
     * Loads the event poster from a Firebase Storage URL
     * and displays it in the ImageView. Handles errors for
     * invalid URLs or failed downloads.
     *
     * @param url the URL of the poster image in Firebase Storage
     */
    private void loadPosterImage(String url) {
        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            ref.getBytes(1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            imgEventPoster.setImageBitmap(bitmap);
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to load poster image", e));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid poster URL: " + url, e);
        }
    }

    /**
     * Loads all comments for the current event from Firestore,
     * fetches the username for each comment, and dynamically
     * creates a TextView for each. Comments are displayed in
     * descending order by timestamp. Organizer comments are
     * labeled accordingly.
     */
    private void loadComments() {
        commentsContainer.removeAllViews();

        FirebaseHelper.getInstance().getDb()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING) // latest first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String text = doc.getString("text");
                        String userId = doc.getString("userId");
                        String role = doc.getString("role"); // gonna use for organizer role differentiation display
                        if (text == null || userId == null) continue;

                        // fetch the user's name from 'users' collection
                        FirebaseHelper.getInstance().getDb()
                                .collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = "User"; // default
                                    if (userDoc.exists()) {
                                        String n = userDoc.getString("name");
                                        if (n != null && !n.isEmpty()) name = n;
                                    }

                                    // only show role for organizers
                                    if ("organizer".equalsIgnoreCase(role)) {
                                        name = "[Organizer] " +name;
                                    }

                                    // create comment TextView
                                    TextView commentView = new TextView(this);
                                    commentView.setTypeface(ResourcesCompat.getFont(this, R.font.inter));
                                    commentView.setText(name + ": " + text);
                                    commentView.setTextSize(14);
                                    commentView.setPadding(16, 16, 16, 16);
                                    commentView.setBackgroundResource(R.drawable.bg_comment_box);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );
                                    params.setMargins(0, 8, 0, 8);
                                    commentView.setLayoutParams(params);

                                    commentsContainer.addView(commentView);
                                });
                    }
                });
    }

}
