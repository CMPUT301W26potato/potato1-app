package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the User model class with profileImageUrl field.
 * Tests User class functionality including the new profile picture URL field,
 * serialization/deserialization, and backward compatibility.
 ** Written with help from Claude (claude.ai)
 * @author Nathaniel Chan
 */
@RunWith(MockitoJUnitRunner.class)
public class UserModelTest {

    @Mock
    private DocumentSnapshot mockDocument;

    private User user;

    private String testName = "John Doe";
    private String testEmail = "john@example.com";
    private String testPhone = "+1234567890";
    private String testProfileImageUrl = "https://example.com/profile.jpg";

    @Before
    public void setUp() {
        user = new User();
    }

    /**
     * Test default User constructor.
     */
    @Test
    public void testDefaultConstructor() {
        User defaultUser = new User();

        assertNotNull(defaultUser);
        assertNull(defaultUser.name);
        assertNull(defaultUser.email);
        assertNull(defaultUser.phone);
        assertNull(defaultUser.profileImageUrl);
    }

    /**
     * Test parameterized User constructor (original without profileImageUrl).
     */
    @Test
    public void testParameterizedConstructor() {
        User paramUser = new User(testName, testEmail, testPhone);

        assertNotNull(paramUser);
        assertEquals(testName, paramUser.name);
        assertEquals(testEmail, paramUser.email);
        assertEquals(testPhone, paramUser.phone);
        assertNull(paramUser.profileImageUrl); // Should be null by default
    }

    /**
     * Test setting and getting profileImageUrl.
     */
    @Test
    public void testProfileImageUrlGetterSetter() {
        user.profileImageUrl = testProfileImageUrl;

        assertEquals(testProfileImageUrl, user.profileImageUrl);
        assertNotNull(user.profileImageUrl);
    }

    /**
     * Test User with all fields including profileImageUrl.
     */
    @Test
    public void testCompleteUserWithProfileImage() {
        user.name = testName;
        user.email = testEmail;
        user.phone = testPhone;
        user.profileImageUrl = testProfileImageUrl;

        assertEquals(testName, user.name);
        assertEquals(testEmail, user.email);
        assertEquals(testPhone, user.phone);
        assertEquals(testProfileImageUrl, user.profileImageUrl);

        // Verify all fields are non-null
        assertNotNull(user.name);
        assertNotNull(user.email);
        assertNotNull(user.phone);
        assertNotNull(user.profileImageUrl);
    }

    /**
     * Test User serialization to Map (for Firebase).
     */
    @Test
    public void testUserSerializationToMap() {
        user.name = testName;
        user.email = testEmail;
        user.phone = testPhone;
        user.profileImageUrl = testProfileImageUrl;

        // Convert to Map for Firebase
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", user.name);
        userMap.put("email", user.email);
        userMap.put("phone", user.phone);
        userMap.put("profileImageUrl", user.profileImageUrl);

        assertEquals(testName, userMap.get("name"));
        assertEquals(testEmail, userMap.get("email"));
        assertEquals(testPhone, userMap.get("phone"));
        assertEquals(testProfileImageUrl, userMap.get("profileImageUrl"));
    }

    /**
     * Test User deserialization from DocumentSnapshot.
     */
    @Test
    public void testUserDeserializationFromDocument() {
        // Setup mock document
        when(mockDocument.getString("name")).thenReturn(testName);
        when(mockDocument.getString("email")).thenReturn(testEmail);
        when(mockDocument.getString("phone")).thenReturn(testPhone);
        when(mockDocument.getString("profileImageUrl")).thenReturn(testProfileImageUrl);

        // Create User from document data
        User userFromDoc = new User();
        userFromDoc.name = mockDocument.getString("name");
        userFromDoc.email = mockDocument.getString("email");
        userFromDoc.phone = mockDocument.getString("phone");
        userFromDoc.profileImageUrl = mockDocument.getString("profileImageUrl");

        assertEquals(testName, userFromDoc.name);
        assertEquals(testEmail, userFromDoc.email);
        assertEquals(testPhone, userFromDoc.phone);
        assertEquals(testProfileImageUrl, userFromDoc.profileImageUrl);
    }

    /**
     * Test backward compatibility - User without profileImageUrl.
     */
    @Test
    public void testBackwardCompatibility() {
        // Setup mock document without profileImageUrl (legacy user)
        when(mockDocument.getString("name")).thenReturn(testName);
        when(mockDocument.getString("email")).thenReturn(testEmail);
        when(mockDocument.getString("phone")).thenReturn(testPhone);
        when(mockDocument.getString("profileImageUrl")).thenReturn(null);

        // Create User from legacy document
        User legacyUser = new User();
        legacyUser.name = mockDocument.getString("name");
        legacyUser.email = mockDocument.getString("email");
        legacyUser.phone = mockDocument.getString("phone");
        legacyUser.profileImageUrl = mockDocument.getString("profileImageUrl");

        // Verify basic fields work
        assertEquals(testName, legacyUser.name);
        assertEquals(testEmail, legacyUser.email);
        assertEquals(testPhone, legacyUser.phone);

        // Verify profileImageUrl is null for legacy users
        assertNull(legacyUser.profileImageUrl);
    }

    /**
     * Test handling null profileImageUrl.
     */
    @Test
    public void testNullProfileImageUrl() {
        user.name = testName;
        user.email = testEmail;
        user.phone = testPhone;
        user.profileImageUrl = null;

        assertNull(user.profileImageUrl);

        // Verify other fields are unaffected
        assertEquals(testName, user.name);
        assertEquals(testEmail, user.email);
        assertEquals(testPhone, user.phone);
    }

    /**
     * Test handling empty profileImageUrl.
     */
    @Test
    public void testEmptyProfileImageUrl() {
        user.profileImageUrl = "";

        assertNotNull(user.profileImageUrl);
        assertTrue(user.profileImageUrl.isEmpty());
    }

    /**
     * Test updating profileImageUrl.
     */
    @Test
    public void testUpdateProfileImageUrl() {
        // Set initial URL
        user.profileImageUrl = testProfileImageUrl;
        assertEquals(testProfileImageUrl, user.profileImageUrl);

        // Update to new URL
        String newUrl = "https://example.com/new-profile.jpg";
        user.profileImageUrl = newUrl;
        assertEquals(newUrl, user.profileImageUrl);

        // Clear URL
        user.profileImageUrl = null;
        assertNull(user.profileImageUrl);
    }

    /**
     * Test User equality with profileImageUrl.
     */
    @Test
    public void testUserEqualityWithProfileImage() {
        User user1 = new User(testName, testEmail, testPhone);
        user1.profileImageUrl = testProfileImageUrl;

        User user2 = new User(testName, testEmail, testPhone);
        user2.profileImageUrl = testProfileImageUrl;

        // Users with same data should be considered equal
        assertEquals(user1.name, user2.name);
        assertEquals(user1.email, user2.email);
        assertEquals(user1.phone, user2.phone);
        assertEquals(user1.profileImageUrl, user2.profileImageUrl);
    }

    /**
     * Test User with different profile image URLs.
     */
    @Test
    public void testUsersWithDifferentProfileImages() {
        User user1 = new User(testName, testEmail, testPhone);
        user1.profileImageUrl = "https://example.com/profile1.jpg";

        User user2 = new User(testName, testEmail, testPhone);
        user2.profileImageUrl = "https://example.com/profile2.jpg";

        // Same user data but different profile images
        assertEquals(user1.name, user2.name);
        assertEquals(user1.email, user2.email);
        assertEquals(user1.phone, user2.phone);
        assertNotEquals(user1.profileImageUrl, user2.profileImageUrl);
    }

    /**
     * Test valid profile image URL formats.
     */
    @Test
    public void testValidProfileImageUrlFormats() {
        // Test various valid URL formats
        String[] validUrls = {
            "https://firebasestorage.googleapis.com/v0/b/app/o/image.jpg",
            "https://storage.googleapis.com/bucket/image.png",
            "https://example.com/profile.jpg",
            "http://localhost:8080/image.gif"
        };

        for (String url : validUrls) {
            user.profileImageUrl = url;
            assertEquals(url, user.profileImageUrl);
            assertNotNull(user.profileImageUrl);
        }
    }

    /**
     * Test Firebase Storage URL format for profile images.
     */
    @Test
    public void testFirebaseStorageUrlFormat() {
        String firebaseUrl = "https://firebasestorage.googleapis.com/v0/b/waitwell-app/o/profile_images%2Fuser123?alt=media&token=abc123";
        user.profileImageUrl = firebaseUrl;

        assertTrue(user.profileImageUrl.contains("firebasestorage.googleapis.com"));
        assertTrue(user.profileImageUrl.contains("profile_images"));
        assertEquals(firebaseUrl, user.profileImageUrl);
    }

    /**
     * Test User creation with partial data including profileImageUrl.
     */
    @Test
    public void testPartialUserWithProfileImage() {
        // User with only name and profileImageUrl
        user.name = testName;
        user.profileImageUrl = testProfileImageUrl;

        assertEquals(testName, user.name);
        assertNull(user.email);
        assertNull(user.phone);
        assertEquals(testProfileImageUrl, user.profileImageUrl);
    }

    /**
     * Test profileImageUrl field independence from other fields.
     */
    @Test
    public void testProfileImageUrlFieldIndependence() {
        // Set profileImageUrl first
        user.profileImageUrl = testProfileImageUrl;

        // Then set other fields
        user.name = testName;
        user.email = testEmail;
        user.phone = testPhone;

        // Verify profileImageUrl is unaffected
        assertEquals(testProfileImageUrl, user.profileImageUrl);

        // Clear other fields
        user.name = null;
        user.email = null;
        user.phone = null;

        // Verify profileImageUrl remains
        assertEquals(testProfileImageUrl, user.profileImageUrl);
    }
}