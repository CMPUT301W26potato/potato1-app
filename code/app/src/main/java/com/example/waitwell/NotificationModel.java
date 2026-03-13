package com.example.waitwell;

/**
 * Model class representing a notification for event selection results.
 * This class event information,
 * message content, and the selection status of the user.
 *
 * @author Nathaniel Chan

 */
public class NotificationModel {

    /**
     * Enumeration defining the types of notifications based on lottery selection results.
     */
    public enum NotificationType {
        /** User was chosen in the lottery */
        CHOSEN,
        /** User was not chosen in the lottery */
        NOT_CHOSEN
    }

    private String eventId;
    private String eventName;
    private String message;
    private String buttonLabel;
    private NotificationType type;

    /**
     * Constructs a new NotificationModel with the specified details.
     *
     * @param eventId unique identifier of the event
     * @param eventName name of the event
     * @param message notification message to display to the user
     * @param buttonLabel label for the action button in the notification
     * @param type type of notification (CHOSEN or NOT_CHOSEN)
     */
    public NotificationModel(String eventId, String eventName, String message, String buttonLabel, NotificationType type){
        this.eventId = eventId;
        this.eventName = eventName;
        this.message = message;
        this.buttonLabel = buttonLabel;
        this.type = type;
    }

    /**
     * Gets the unique identifier of the event.
     *
     * @return the event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the name of the event.
     *
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Gets the notification message.
     *
     * @return the message to display to the user
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the label for the notification action button.
     *
     * @return the button label text
     */
    public String getButtonLabel() {
        return buttonLabel;
    }

    /**
     * Gets the type of notification.
     *
     * @return the notification type (CHOSEN or NOT_CHOSEN)
     */
    public NotificationType getType() {
        return type;
    }

}
