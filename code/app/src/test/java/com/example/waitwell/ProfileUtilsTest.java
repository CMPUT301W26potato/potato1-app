package com.example.waitwell;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Test;

import java.util.Map;

/**
 * Unit tests for {@link ProfileUtils}.
 * Verifies profile map creation and field retrieval from Firestore documents.
 * @author Sarang Kim
 */
public class ProfileUtilsTest {

    /** Tests that buildProfileMap correctly constructs a map with all fields. */
    @Test
    public void testBuildProfileMap() {
        Map<String, Object> map = ProfileUtils.buildProfileMap("Alice", "alice@test.com", "1234567890", "url");
        assertEquals("Alice", map.get("name"));
        assertEquals("alice@test.com", map.get("email"));
        assertEquals("1234567890", map.get("phone"));
        assertEquals("url", map.get("profileImageUrl"));
    }

    /** Tests that getNameFromDoc, getEmailFromDoc, and getPhoneFromDoc return correct values. */
    @Test
    public void testGetFieldsFromDoc() {
        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        when(mockDoc.getString("name")).thenReturn("Bob");
        when(mockDoc.getString("email")).thenReturn("bob@test.com");
        when(mockDoc.getString("phone")).thenReturn("9876543210");

        assertEquals("Bob", ProfileUtils.getNameFromDoc(mockDoc));
        assertEquals("bob@test.com", ProfileUtils.getEmailFromDoc(mockDoc));
        assertEquals("9876543210", ProfileUtils.getPhoneFromDoc(mockDoc));
    }
}