package com.example.waitwell;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Notification} class.
 *
 * Tests the creation, manipulation, and conversion of Notification objects,
 * including constructors, getters/setters, and the conversion to NotificationModel.
 * Validates proper handling of different notification types (CHOSEN, NOT_CHOSEN)
 * and edge cases including null values and unknown types.
 *
 * @author Nathan
 */
public class NotificationTest {

    private Notification notification;

    /**
     * Sets up test fixture before each test method.
     * Creates a sample notification with predefined test data.
     */
    @Before
    public void setUp() {
        notification = new Notification("user123", "event456", "Test Event",
                "You have been selected!", "CHOSEN");
    }

    /**
     * Tests the default constructor of Notification class.
     * Verifies that a notification created with no parameters has all fields set to null.
     */
    @Test
    public void testDefaultConstructor() {
        Notification defaultNotification = new Notification();
        assertNotNull(defaultNotification);
        assertNull(defaultNotification.getId());
        assertNull(defaultNotification.getUserId());
        assertNull(defaultNotification.getEventId());
    }

    /**
     * Tests the parameterized constructor of Notification class.
     * Verifies that all fields are correctly set through the constructor.
     */
    @Test
    public void testParameterizedConstructor() {
        assertEquals("user123", notification.getUserId());
        assertEquals("event456", notification.getEventId());
        assertEquals("Test Event", notification.getEventName());
        assertEquals("You have been selected!", notification.getMessage());
        assertEquals("CHOSEN", notification.getType());
    }

    /**
     * Tests all getter and setter methods for the Notification class.
     * Verifies that values can be set and retrieved correctly for all fields.
     */
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

    /**
     * Tests conversion to NotificationModel for CHOSEN type notifications.
     * Verifies correct button label and type mapping for chosen notifications.
     */
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

    /**
     * Tests conversion to NotificationModel for NOT_CHOSEN type notifications.
     * Verifies correct button label and type mapping for not chosen notifications.
     */
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

    /**
     * Tests conversion to NotificationModel for unknown notification types.
     * Verifies that unknown types default to NOT_CHOSEN with appropriate button label.
     */
    @Test
    public void testToNotificationModelUnknownType() {
        notification.setType("UNKNOWN");
        NotificationModel model = notification.toNotificationModel();

        assertNotNull(model);
        // Should default to NOT_CHOSEN for unknown types
        assertEquals(NotificationModel.NotificationType.NOT_CHOSEN, model.getType());
        assertEquals("View Details", model.getButtonLabel());
    }

    /**
     * Tests handling of null values in Notification fields.
     * Verifies that the class gracefully handles null inputs without throwing exceptions
     * and that toNotificationModel() provides sensible defaults for null types.
     */
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