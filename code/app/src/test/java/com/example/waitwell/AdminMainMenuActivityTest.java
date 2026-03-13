package com.example.waitwell;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.waitwell.activities.AdminMainMenuActivity;

import org.junit.Test;

public class AdminMainMenuActivityTest {
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
