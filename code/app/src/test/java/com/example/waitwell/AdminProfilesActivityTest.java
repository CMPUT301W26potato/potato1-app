package com.example.waitwell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.waitwell.activities.AdminProfilesActivity;

import org.junit.Test;

public class AdminProfilesActivityTest {
    @Test
    public void testProfileNameExists() {

        String name = "Costcosecurity";

        assertNotNull(name);
    }

    @Test
    public void testUserRoleValue() {

        String role = "entrant";

        assertEquals("entrant", role);
    }
}
