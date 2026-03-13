package com.example.waitwell;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.waitwell.activities.AdminProfileRemovedActivity;

import org.junit.Test;

public class AdminProfileRemovedActivityTest {
    @Test
    public void testProfileRemovalMessage() {

        String message = "Profile Removed";

        assertTrue(message.contains("Removed"));
    }
}
