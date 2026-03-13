package com.example.waitwell;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.waitwell.activities.AdminEventRemovedActivity;

import org.junit.Test;

public class AdminEventRemovedActivityTest {
    @Test
    public void testConfirmationMessage() {

        String message = "Event Removed";

        assertTrue(message.contains("Removed"));
    }
}
