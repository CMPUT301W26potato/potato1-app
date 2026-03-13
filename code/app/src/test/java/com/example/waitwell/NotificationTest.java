package com.example.waitwell;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * nathans unit tests for the Notification class
 */
public class NotificationTest {

    private Notification notification;

    //manually add a notification
    @Before
    public void setUp() {
        notification = new Notification("user123", "event456", "Test Event",
                "You have been selected!", "CHOSEN");
    }

    @Test
    public void testDefaultConstructor() {
        Notification defaultNotification = new Notification();
        assertNotNull(defaultNotification);
        assertNull(defaultNotification.getId());
        assertNull(defaultNotification.getUserId());
        assertNull(defaultNotification.getEventId());
    }

    @Test
    public void testParameterizedConstructor() {
        assertEquals("user123", notification.getUserId());
        assertEquals("event456", notification.getEventId());
        assertEquals("Test Event", notification.getEventName());
        assertEquals("You have been selected!", notification.getMessage());
        assertEquals("CHOSEN", notification.getType());
    }

    @Test
    public void testGettersAndSetters() {
        // Test ID
        notification.setId("notif789");
        assertEquals("notif789", notification.getId());

        // Test UserId
        notification.setUserId("newUser456");
        assertEquals("newUser456", notification.getUserId());

        // Test EventId
        notification.setEventId("newEvent789");
        assertEquals("newEvent789", notification.getEventId());

        // Test EventName
        notification.setEventName("New Event Name");
        assertEquals("New Event Name", notification.getEventName());

        // Test Message
        notification.setMessage("New message content");
        assertEquals("New message content", notification.getMessage());

        // Test Type
        notification.setType("NOT_CHOSEN");
        assertEquals("NOT_CHOSEN", notification.getType());

        // Test CreatedAt
        Date testDate = new Date();
        notification.setCreatedAt(testDate);
        assertEquals(testDate, notification.getCreatedAt());
    }

    @Test
    public void testToNotificationModelChosen() {
        notification.setType("CHOSEN");
        NotificationModel model = notification.toNotificationModel();

        assertNotNull(model);
        assertEquals("event456", model.getEventId());
        assertEquals("Test Event", model.getEventName());
        assertEquals("You have been selected!", model.getMessage());
        assertEquals("View Invitation", model.getButtonLabel());
        assertEquals(NotificationModel.NotificationType.CHOSEN, model.getType());
    }

    @Test
    public void testToNotificationModelNotChosen() {
        notification.setType("NOT_CHOSEN");
        NotificationModel model = notification.toNotificationModel();

        assertNotNull(model);
        assertEquals("event456", model.getEventId());
        assertEquals("Test Event", model.getEventName());
        assertEquals("You have been selected!", model.getMessage());
        assertEquals("View Details", model.getButtonLabel());
        assertEquals(NotificationModel.NotificationType.NOT_CHOSEN, model.getType());
    }

    @Test
    public void testToNotificationModelUnknownType() {
        notification.setType("UNKNOWN");
        NotificationModel model = notification.toNotificationModel();

        assertNotNull(model);
        // Should default to NOT_CHOSEN for unknown types
        assertEquals(NotificationModel.NotificationType.NOT_CHOSEN, model.getType());
        assertEquals("View Details", model.getButtonLabel());
    }

    @Test
    public void testNullFieldHandling() {
        Notification nullNotification = new Notification(null, null, null, null, null);

        assertNull(nullNotification.getUserId());
        assertNull(nullNotification.getEventId());
        assertNull(nullNotification.getEventName());
        assertNull(nullNotification.getMessage());
        assertNull(nullNotification.getType());

        // Should handle null type gracefully
        NotificationModel model = nullNotification.toNotificationModel();
        assertEquals(NotificationModel.NotificationType.NOT_CHOSEN, model.getType());
        assertEquals("View Details", model.getButtonLabel());
    }
}