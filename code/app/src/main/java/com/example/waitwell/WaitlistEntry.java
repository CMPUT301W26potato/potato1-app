package com.example.waitwell;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

/**
 * Tracks an entrant's waitlist entry for a specific event.
 * Firestore collection: "waitlist_entries"
 * Document ID format: "{userId}_{eventId}"
 */
public class WaitlistEntry implements Serializable {

    @DocumentId
    private String id;
    private String userId;
    private String eventId;
    private String eventTitle;
    private String status; // "waiting", "selected", "confirmed", "rejected", "cancelled"
    private double joinLatitude;
    private double joinLongitude;

    @ServerTimestamp
    private Date joinedAt;

    public WaitlistEntry() {}

    public WaitlistEntry(String userId, String eventId, String eventTitle) {
        this.userId = userId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.status = "waiting";
    }

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

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getJoinLatitude() {
        return joinLatitude;
    }

    public void setJoinLatitude(double joinLatitude) {
        this.joinLatitude = joinLatitude;
    }

    public double getJoinLongitude() {
        return joinLongitude;
    }

    public void setJoinLongitude(double joinLongitude) {
        this.joinLongitude = joinLongitude;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }
}