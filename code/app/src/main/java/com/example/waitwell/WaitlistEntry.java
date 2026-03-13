package com.example.waitwell;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;



public class WaitlistEntry implements Serializable {
    /**
     * WaitlistEntry.java
     * Model class for a single waitlist entry in the "waitlist_entries" Firestore collection.
     * Document ID format is userId_eventId.
     * Status can be: "waiting", "selected", "confirmed", "rejected", or "cancelled".
     * Javadoc written with help from Claude (claude.ai)
     */

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

    public WaitlistEntry() {
        /*
          Creates a new waitlist entry with status set to "waiting" by default.

          @param userId     device ID of the entrant
         * @param eventId    Firestore document ID of the event
         * @param eventTitle title of the event, stored so we don't need a second query
         */
    }

    public WaitlistEntry(String userId, String eventId, String eventTitle) {
        this.userId = userId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.status = "waiting";
    }
    /** @return Firestore document ID for this entry */
    public String getId() {
        return id;
    }
    /** @param id Firestore document ID */
    public void setId(String id) {
        this.id = id;
    }
      /** @return device ID of the entrant */
    public String getUserId() {
        return userId;
    }
    /** @param userId device ID of the entrant */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    /** @return Firestore document ID of the event */
    public String getEventId() {
        return eventId;
    }
    /**  Firestore document ID of the event */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    /** @return title of the event */
    public String getEventTitle() {
        return eventTitle;
    }


    /** @param eventTitle title of the event */

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }
    /** @return current status: "waiting", "selected", "confirmed", "rejected", or "cancelled" */
    public String getStatus() {
        return status;
    }
    /** @param status one of "waiting", "selected", "confirmed", "rejected", "cancelled" */
    public void setStatus(String status) {
        this.status = status;
    }
    /** @return latitude where the entrant joined the waitlist from */
    public double getJoinLatitude() {
        return joinLatitude;
    }
    /** @param joinLatitude latitude where the entrant joined from */
    public void setJoinLatitude(double joinLatitude) {
        this.joinLatitude = joinLatitude;
    }

    /** @return longitude where the entrant joined the waitlist from */
    public double getJoinLongitude() {
        return joinLongitude;
    }

    /** @param joinLongitude longitude where the entrant joined from */
    public void setJoinLongitude(double joinLongitude) {
        this.joinLongitude = joinLongitude;
    }
    /** @return timestamp of when the entrant joined the waitlist */
    public Date getJoinedAt() {
        return joinedAt;
    }
    /** @param joinedAt timestamp of when the entrant joined */
    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }
}