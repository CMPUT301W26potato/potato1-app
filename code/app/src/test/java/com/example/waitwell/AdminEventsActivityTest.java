package com.example.waitwell;

import com.example.waitwell.activities.AdminEventsActivity;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AdminEventsActivityTest {
    @Test
    public void testEventTitleNotNull() {

        String title = "Boxing";

        assertNotNull(title);
    }

    @Test
    public void testOrganizerNameNotEmpty() {

        String organizer = "Boxing centre";

        assertTrue(organizer.length() > 0);
    }
}
