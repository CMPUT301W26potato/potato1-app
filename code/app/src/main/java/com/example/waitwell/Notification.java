package com.example.waitwell;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * Data model for one notification document from Firestore. This is shared by organizer
 * notification sends and entrant notification screens.
 *
 * Addresses: US 01.05.06 - Entrant: Private Event Invite Notification, US 02.05.01 - Organizer: Notify Chosen Entrants
 *
 * @author Karina Zhang
 * @version 1.0
 * @see NotificationModel
 */
public class Notification implements Serializable {
    /*
     * Used Gemini to figure out how to query Firestore for a specific
     * user's notifications and sort them by timestamp without it getting
     * weird. Also talked through how to handle the UI update when a
     * notification gets tapped and the entrant has already responded.
     *
     *
     * Sites I looked at:
     *
     * Firestore queries - whereEqualTo and orderBy used together:
     * https://firebase.google.com/docs/firestore/query-data/queries
     *
     * RecyclerView with Firestore - how to bind live data to a list:
     * https://developer.android.com/reference/com/firebase/ui/firestore/FirestoreRecyclerAdapter
     *
     * Handling click events inside a RecyclerView adapter:
     * https://developer.android.com/guide/topics/ui/layout/recyclerview#click-listener
     */

    @DocumentId
    private String id;
    private String userId;          // Device ID of the recipient
    private String eventId;         // Event this notification is about
    private String eventName;       // Event title for display
    private String message;         // Notification message content
    private String type;            // "CHOSEN", "NOT_CHOSEN", etc.
    private boolean responded = false;  // Whether user has responded to this notification
    private boolean read = false;       // Whether user has read this notification

    @ServerTimestamp
    private Date createdAt;

    /**
     * Empty constructor required by Firestore object mapping.
     *
     * @author Karina Zhang
     */
    public Notification() {
    }

    /**
     * Creates a new notification payload before writing it to Firestore.
     *
     * @param userId recipient user id
     * @param eventId related event id
     * @param eventName related event title
     * @param message body text shown in card
     * @param type notification type string
     * @author Karina Zhang
     */

    public Notification(String userId, String eventId, String eventName, String message, String type) {
        this.userId = userId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.message = message;
        this.type = type;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isResponded() {
        return responded;
    }

    public void setResponded(boolean responded) {
        this.responded = responded;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * Converts this Firestore model to adapter-friendly `NotificationModel`.
     *
     * @return mapped notification model for RecyclerView rows
     * @author Karina Zhang
     */
    public NotificationModel toNotificationModel() {
        // REHAAN'S ADDITION â€” CO_ORGANIZER type mapping (US 02.09.01 Part 2)
        NotificationModel.NotificationType notifType;
        String buttonLabel;
        if ("CO_ORGANIZER".equals(type)) {
            notifType = NotificationModel.NotificationType.CO_ORGANIZER;
            buttonLabel = "View Invite";
        } else if ("CHOSEN".equals(type)) {
            notifType = NotificationModel.NotificationType.CHOSEN;
            buttonLabel = "View Invitation";
        } else {
            notifType = NotificationModel.NotificationType.NOT_CHOSEN;
            buttonLabel = "View Details";
        }
        // END REHAAN'S ADDITION
        return new NotificationModel(eventId, eventName, message, buttonLabel, notifType);
    }
}
