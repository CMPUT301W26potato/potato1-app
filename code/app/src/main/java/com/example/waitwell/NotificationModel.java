package com.example.waitwell;

public class NotificationModel {
    public enum NotificationType {
        CHOSEN,
        NOT_CHOSEN
    }
    //fixed set of named constants, public accessibility

    private String eventId;
    private String eventName;
    private String message;
    private String buttonLabel;
    private NotificationType type;
    private boolean expired;
    /** True once we have started an async waitlist status fetch for invitation UI. */
    private boolean inviteWaitlistStatusFetchStarted;
    /** True when waitlist entry is already confirmed or cancelled (invitation already handled). */
    private boolean inviteAlreadyResolvedOnWaitlist;

    /**
     *  create a new notificationmodel object to be used in the adapter
     */
    public NotificationModel(String eventId, String eventName, String message, String buttonLabel, NotificationType type){
        this.eventId = eventId;
        this.eventName = eventName;
        this.message = message;
        this.buttonLabel = buttonLabel;
        this.type = type;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getMessage() {
        return message;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public NotificationType getType() {
        return type;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isInviteWaitlistStatusFetchStarted() {
        return inviteWaitlistStatusFetchStarted;
    }

    public void setInviteWaitlistStatusFetchStarted(boolean inviteWaitlistStatusFetchStarted) {
        this.inviteWaitlistStatusFetchStarted = inviteWaitlistStatusFetchStarted;
    }

    public boolean isInviteAlreadyResolvedOnWaitlist() {
        return inviteAlreadyResolvedOnWaitlist;
    }

    public void setInviteAlreadyResolvedOnWaitlist(boolean inviteAlreadyResolvedOnWaitlist) {
        this.inviteAlreadyResolvedOnWaitlist = inviteAlreadyResolvedOnWaitlist;
    }
}
