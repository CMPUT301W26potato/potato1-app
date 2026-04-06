// REHAAN'S ADDITION — US 02.09.01 Part 2
package com.example.waitwell.activities;

/**
 * CoOrganizerInviteResponseActivity.java
 * Shown when an entrant taps "View Invite" on a CO_ORGANIZER notification.
 * Lets them accept or decline the co-organizer invitation for an event.
 * Accepting moves their userId from pendingCoOrganizerIds to coOrganizerIds.
 * Declining removes them from pendingCoOrganizerIds.
 * Javadoc written with help from Claude (claude.ai)
 */
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.FieldValue;

public class CoOrganizerInviteResponseActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID       = "event_id";
    public static final String EXTRA_EVENT_NAME     = "event_name";
    public static final String EXTRA_MESSAGE        = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    /** Pass "accepted" or "declined" to show read-only mode (already responded). */
    public static final String EXTRA_ALREADY_RESPONDED = "alreadyResponded";

    private String eventId;
    private String notificationId;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_co_organizer_invite_response);

        eventId        = getIntent().getStringExtra(EXTRA_EVENT_ID);
        notificationId = getIntent().getStringExtra(EXTRA_NOTIFICATION_ID);
        deviceId       = DeviceUtils.getDeviceId(this);

        String eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);
        String message   = getIntent().getStringExtra(EXTRA_MESSAGE);

        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.invitation_error_no_event, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtCoOrgInviteTitle)).setText(
                TextUtils.isEmpty(eventName) ? getString(R.string.app_name) : eventName);

        TextView messageView = findViewById(R.id.txtCoOrgInviteMessage);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Button btnAccept  = findViewById(R.id.btnAcceptCoOrg);
        Button btnDecline = findViewById(R.id.btnDeclineCoOrg);

        String alreadyResponded = getIntent().getStringExtra(EXTRA_ALREADY_RESPONDED);
        if (alreadyResponded != null) {
            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            if ("accepted".equals(alreadyResponded)) {
                messageView.setText(R.string.co_organizer_already_accepted);
            } else {
                messageView.setText(R.string.co_organizer_already_declined);
            }
        } else {
            messageView.setText(
                    TextUtils.isEmpty(message) ? getString(R.string.co_organizer_notification_message, eventName) : message);
            btnAccept.setOnClickListener(v -> handleAccept(btnAccept, btnDecline));
            btnDecline.setOnClickListener(v -> handleDecline(btnAccept, btnDecline));
        }
    }

    private void handleAccept(Button btnAccept, Button btnDecline) {
        btnAccept.setEnabled(false);
        btnDecline.setEnabled(false);

        FirebaseHelper.getInstance().getDb()
                .collection("events").document(eventId)
                .update(
                        "pendingCoOrganizerIds", FieldValue.arrayRemove(deviceId),
                        "coOrganizerIds",        FieldValue.arrayUnion(deviceId)
                )
                .addOnSuccessListener(v -> {
                    markNotificationResponded();
                    Toast.makeText(this,
                            R.string.co_organizer_invite_accept_success,
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAccept.setEnabled(true);
                    btnDecline.setEnabled(true);
                    Toast.makeText(this, R.string.invitation_error_accept, Toast.LENGTH_SHORT).show();
                });
    }

    private void handleDecline(Button btnAccept, Button btnDecline) {
        btnAccept.setEnabled(false);
        btnDecline.setEnabled(false);

        FirebaseHelper.getInstance().getDb()
                .collection("events").document(eventId)
                .update("pendingCoOrganizerIds", FieldValue.arrayRemove(deviceId))
                .addOnSuccessListener(v -> {
                    markNotificationResponded();
                    Toast.makeText(this,
                            R.string.co_organizer_invite_decline_success,
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAccept.setEnabled(true);
                    btnDecline.setEnabled(true);
                    Toast.makeText(this, R.string.invitation_error_decline, Toast.LENGTH_SHORT).show();
                });
    }

    private void markNotificationResponded() {
        if (!TextUtils.isEmpty(notificationId)) {
            FirebaseHelper.getInstance().markNotificationResponded(notificationId, null);
        }
    }
}
// END REHAAN'S ADDITION — US 02.09.01 Part 2