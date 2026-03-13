package com.example.waitwell;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a notification in the Wait Well system.
 * Firestore collection: "notifications"
 *
 * Notifications are created when:
 * - User is selected in lottery (CHOSEN)
 * - User is not selected in lottery (NOT_CHOSEN)
 * - Other event updates occur
 */
public class Notification implements Serializable {

    @DocumentId
    private String id;
    private String userId;
    private String eventId;
    private String eventName;
    private String message;
    private String type;

    @ServerTimestamp
    private Date createdAt;


    public Notification() {
    }

    //creating a new notification object
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

    // this is a helper method to convert to NotificationModel for adapter
    public NotificationModel toNotificationModel() {
        NotificationModel.NotificationType notifType =
            "CHOSEN".equals(type) ? NotificationModel.NotificationType.CHOSEN
                                  : NotificationModel.NotificationType.NOT_CHOSEN;

        String buttonLabel = "CHOSEN".equals(type) ? "View Invitation" : "View Details";

        return new NotificationModel(eventId, eventName, message, buttonLabel, notifType);
    }
}