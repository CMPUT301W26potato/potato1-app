package com.example.waitwell;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, but this file stays in plain JUnit 4.
 *
 * I used Gemini to understand how to mock Firestore snapshot fields in unit tests
 * so status/date logic can be checked without running Android UI.
 *
 * Sites I looked at:
 *
 * Mockito docs:
 * https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
 *
 * Firestore local emulator:
 * https://firebase.google.com/docs/emulator-suite/connect_firestore
 */

import static org.junit.Assert.*;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.*;

import java.util.Calendar;
import java.util.Date;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link EventStatusUtils} with normal, alternative, and boundary cases.
 * Mocks Firestore snapshots so status logic can run in isolation.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see EventStatusUtils
 */
public class EventStatusUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks that past event day returns completed status.
     * This is the happy path for completed events.
     *
     * @author Karina Zhang
     */
    @Test
    public void testComputeStatus_eventDayPassed_returnsCompleted() {
        Calendar now = Calendar.getInstance();
        Calendar event = (Calendar) now.clone();
        event.add(Calendar.DAY_OF_MONTH, -1);
        assertEquals("completed", EventStatusUtils.computeStatus(event.getTime(), null, now.getTime()));
    }

    /**
     * Checks that passed registration close with future event day returns closed.
     * This is the main alternative flow.
     *
     * @author Karina Zhang
     */
    @Test
    public void testComputeStatus_registrationClosed_returnsClosed() {
        Calendar now = Calendar.getInstance();
        Calendar event = (Calendar) now.clone();
        event.add(Calendar.DAY_OF_MONTH, 2);
        Calendar close = (Calendar) now.clone();
        close.add(Calendar.DAY_OF_MONTH, -1);
        assertEquals("closed", EventStatusUtils.computeStatus(event.getTime(), close.getTime(), now.getTime()));
    }

    /**
     * Checks that no blocking dates returns open.
     * This is the happy path for active registration.
     *
     * @author Karina Zhang
     */
    @Test
    public void testComputeStatus_default_returnsOpen() {
        Calendar now = Calendar.getInstance();
        Calendar event = (Calendar) now.clone();
        event.add(Calendar.DAY_OF_MONTH, 4);
        Calendar close = (Calendar) now.clone();
        close.add(Calendar.DAY_OF_MONTH, 1);
        assertEquals("open", EventStatusUtils.computeStatus(event.getTime(), close.getTime(), now.getTime()));
    }

    /**
     * Checks null snapshot input returns closed and does not crash.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testComputeStatus_nullDoc_returnsClosed() {
        assertEquals("closed", EventStatusUtils.computeStatus((DocumentSnapshot) null));
    }

    /**
     * Checks getRegistrationCloseDate reads Date field when present.
     * This is the happy path for Firestore Date mapping.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGetRegistrationCloseDate_readsDateField() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        Date d = new Date();
        when(doc.exists()).thenReturn(true);
        when(doc.getDate(EventStatusUtils.FIELD_REGISTRATION_CLOSE)).thenReturn(d);
        assertEquals(d, EventStatusUtils.getRegistrationCloseDate(doc));
    }
}
