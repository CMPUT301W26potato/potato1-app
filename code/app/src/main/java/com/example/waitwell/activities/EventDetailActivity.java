package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.EventStatusUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
    private View btnJoin;
    private String eventId, deviceId;
    private EditText editComment;
    private View btnPostComment;
    private LinearLayout commentsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        deviceId = DeviceUtils.getDeviceId(this);
        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null) { finish(); return; }

        initViews();
        loadEvent();
        loadComments();
        setupBottomNav();
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
        editComment = findViewById(R.id.editComment);
        btnPostComment = findViewById(R.id.btnPostComment);
        commentsContainer = findViewById(R.id.commentsContainer);
        btnPostComment.setOnClickListener(v -> postComment());

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
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = doc.getString("title");
        String location = doc.getString("location");
        String desc = doc.getString("description");
        Double price = doc.getDouble("price");
        Double rating = doc.getDouble("rating");
        List<String> waitlist = (List<String>) doc.get("waitlistEntrantIds");

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

        //Check if user is already on the waitlist
        boolean alreadyJoined = waitlist != null && waitlist.contains(deviceId);

        if (alreadyJoined || !isOpen) {
            // Hide join button, show status text instead
            findViewById(R.id.joinButtonContainer).setVisibility(View.GONE);
            //Maybe add message that "You already joined the waitlist"
            //TODO
        }
    }

    // Join Waitlist (US 01.01.01)
    private void joinWaitlist() {
        btnJoin.setEnabled(false);
        String title = txtTitle.getText().toString();
        FirebaseHelper.getInstance().joinWaitlist(
                deviceId, eventId, title,
                task -> {
                    if (task.isSuccessful()) {
                        // Go to confirmation screen
                        Intent intent = new Intent(this, ConfirmationActivity.class);
                        intent.putExtra("event_title", title);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show();btnJoin.setEnabled(true);}
                });
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { finish(); return true; }
            if (id == R.id.nav_waitlist) {
                startActivity(new Intent(this, WaitListActivity.class));
                return true;
            }
            if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications ", Toast.LENGTH_SHORT).show();
                //todo
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
                    if (!"rejected".equals(status)) {
                        return;
                    }
                    View anchor = findViewById(R.id.bottomNavigation);
                    Snackbar sb = Snackbar.make(
                            anchor,
                            R.string.event_detail_registration_not_accepted,
                            Snackbar.LENGTH_LONG);
                    sb.setAnchorView(anchor);
                    sb.setBackgroundTint(ContextCompat.getColor(this, R.color.bg_white));
                    sb.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    sb.show();
                })
                .addOnFailureListener(e -> Log.w(TAG, "waitlist status check failed", e));
    }
    private void postComment() {
        String commentText = editComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch username from users collection
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
    private void loadComments() {
        commentsContainer.removeAllViews();

        FirebaseHelper.getInstance().getDb()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String text = doc.getString("text");
                        String userId = doc.getString("userId");
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

                                    // create comment TextView
                                    TextView commentView = new TextView(this);
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
