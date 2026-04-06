package com.example.waitwell;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a notification in the Wait Well system.
 * Firestore collection: "notifications"
 * Notifications are created when:
 * - User is selected in lottery (CHOSEN)
 * - User is not selected in lottery (NOT_CHOSEN)
 * - Other event updates occur
 */
public class Notification implements Serializable {

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

    // Default constructor for Firestore
    public Notification() {
    }

    /**
     *  Constructor for creating new notifications
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
     * Helper method to convert to NotificationModel for adapter
     */
    public NotificationModel toNotificationModel() {
        // REHAAN'S ADDITION — CO_ORGANIZER type mapping (US 02.09.01 Part 2)
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
        NotificationModel model = new NotificationModel(eventId, eventName, message, buttonLabel, notifType);
        model.setResponded(responded);
        return model;
    }
}