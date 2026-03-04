package com.example.waitwell;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents an event in the Wait Well lottery system.
 * Firestore collection: "events"
 */
public class Event implements Serializable {
    @DocumentId
    private String id;
    private String title;
    private String description;
    private String organizerId;
    private String organizerName;
    private String location;
    private String imageUrl;
    private double price;
    private int capacity;
    private int currentRegistered;
    private Date eventDate;
    private Date registrationOpen;
    private Date registrationClose;
    private boolean geolocationRequired;
    private String status; // "open" or "closed"
    private double rating;
    private List<String> waitlistEntrantIds;
    private Integer waitlistLimit; // null = no limit
    private String qrCodeData;

    @ServerTimestamp
    private Date createdAt;

    public Event() {
        this.waitlistEntrantIds = new ArrayList<>();
    }

    // Getters / Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentRegistered() {
        return currentRegistered;
    }

    public void setCurrentRegistered(int currentRegistered) {
        this.currentRegistered = currentRegistered;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public Date getRegistrationOpen() {
        return registrationOpen;
    }

    public void setRegistrationOpen(Date registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    public Date getRegistrationClose() {
        return registrationClose;
    }

    public void setRegistrationClose(Date registrationClose) {
        this.registrationClose = registrationClose;
    }

    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public List<String> getWaitlistEntrantIds() {
        return waitlistEntrantIds;
    }

    public void setWaitlistEntrantIds(List<String> waitlistEntrantIds) {
        this.waitlistEntrantIds = waitlistEntrantIds;
    }

    public Integer getWaitlistLimit() {
        return waitlistLimit;
    }

    public void setWaitlistLimit(Integer waitlistLimit) {
        this.waitlistLimit = waitlistLimit;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }


    // Helpers

    /** Check if registration is currently open based on dates and status. */
    public boolean isRegistrationOpen() {
        if (registrationOpen == null || registrationClose == null) return "open".equals(status);
        Date now = new Date();
        return now.after(registrationOpen) && now.before(registrationClose) && "open".equals(status);
    }

    /** Days remaining until registration closes. */
    public int getDaysUntilClose() {
        if (registrationClose == null) return 0;
        long diff = registrationClose.getTime() - new Date().getTime();
        return Math.max(0, (int) (diff / (1000 * 60 * 60 * 24)));
    }

    /** Total entrants currently on the waiting list. */
    public int getWaitlistCount() {
        return waitlistEntrantIds != null ? waitlistEntrantIds.size() : 0;
    }

    /** Whether the waitlist has hit its cap (if one exists). */
    public boolean isWaitlistFull() {
        if (waitlistLimit == null) return false;
        return getWaitlistCount() >= waitlistLimit;
    }

    /** Check whether a given user is already on this event's waitlist. */
    public boolean isUserOnWaitlist(String userId) {
        return waitlistEntrantIds != null && waitlistEntrantIds.contains(userId);
    }
}
