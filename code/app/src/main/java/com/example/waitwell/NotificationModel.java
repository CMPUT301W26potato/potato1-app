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

}
