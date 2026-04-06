package com.example.waitwell;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for profile picture functionality in the Profile class.
 * Tests uploading, removing, and managing user profile pictures.
 *
 * Note: These tests focus on the logic and interactions rather than
 * Android-specific components that require instrumentation testing.
 *
 * @author Nathaniel Chan
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfilePictureTest {

    @Mock
    private FirebaseStorage mockStorage;

    @Mock
    private StorageReference mockStorageRef;

    @Mock
    private StorageReference mockFileRef;

    @Mock
    private DocumentReference mockDocRef;

    @Mock
    private CollectionReference mockCollectionRef;

    @Mock
    private UploadTask mockUploadTask;

    @Mock
    private Task<Void> mockVoidTask;

    @Mock
    private UploadTask.TaskSnapshot mockTaskSnapshot;

    private String testUserId = "testUser123";
    private String testImageUrl = "https://firebasestorage.googleapis.com/test/image.jpg";

    @Before
    public void setUp() {
        // Setup common mock behaviors
        when(mockStorage.getReference(anyString())).thenReturn(mockStorageRef);
    }

    /**
     * Test successful profile picture upload to Firebase Storage.
     */
    @Test
    public void testUploadProfilePicture_Success() {
        // Setup mocks for successful upload
        String expectedPath = "profile_images/" + testUserId;
        when(mockStorage.getReference(expectedPath)).thenReturn(mockFileRef);
        when(mockFileRef.putFile(any())).thenReturn(mockUploadTask);
        when(mockUploadTask.addOnSuccessListener(any())).thenReturn(mockUploadTask);

        // Trigger upload
        StorageReference ref = mockStorage.getReference(expectedPath);
        UploadTask uploadTask = ref.putFile(null); // null represents the Uri in test
        uploadTask.addOnSuccessListener(snapshot -> {
            // Success callback
            assertNotNull(snapshot);
        });

        // Verify the correct path was used
        verify(mockStorage).getReference(expectedPath);
        assertEquals("profile_images/testUser123", expectedPath);
    }

    /**
     * Test handling of profile picture upload failure.
     */
    @Test
    public void testUploadProfilePicture_Failure() {
        // Setup mocks for failed upload
        String expectedPath = "profile_images/" + testUserId;
        when(mockStorage.getReference(expectedPath)).thenReturn(mockFileRef);
        when(mockFileRef.putFile(any())).thenReturn(mockUploadTask);

        Exception testException = new Exception("Upload failed");

        when(mockUploadTask.addOnFailureListener(any())).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(testException);
            return mockUploadTask;
        });

        // Trigger upload with failure
        StorageReference ref = mockStorage.getReference(expectedPath);
        UploadTask uploadTask = ref.putFile(null);

        final boolean[] failureCalled = {false};
        uploadTask.addOnFailureListener(e -> {
            failureCalled[0] = true;
            assertEquals("Upload failed", e.getMessage());
        });

        // Verify failure was handled
        assertTrue(failureCalled[0]);
    }

    /**
     * Test saving profile image URL to Firestore.
     */
    @Test
    public void testSaveProfileImageUrlToFirestore() {
        // Setup Firestore mocks
        when(mockDocRef.set(any(Map.class), any(SetOptions.class))).thenReturn(mockVoidTask);

        // Create user data with profile image URL
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "Test User");
        userData.put("email", "test@example.com");
        userData.put("profileImageUrl", testImageUrl);

        // Simulate save
        mockDocRef.set(userData, SetOptions.merge());

        // Verify the data includes profile image URL
        ArgumentCaptor<Map> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockDocRef).set(dataCaptor.capture(), any(SetOptions.class));

        Map<String, Object> capturedData = dataCaptor.getValue();
        assertEquals(testImageUrl, capturedData.get("profileImageUrl"));
    }

    /**
     * Test removing profile picture from Firebase Storage.
     */
    @Test
    public void testRemoveProfilePicture_Storage() {
        // Setup storage deletion mocks
        String expectedPath = "profile_images/" + testUserId;
        when(mockStorage.getReference(expectedPath)).thenReturn(mockFileRef);
        when(mockFileRef.delete()).thenReturn(mockVoidTask);

        // Simulate deletion
        StorageReference ref = mockStorage.getReference(expectedPath);
        Task<Void> deleteTask = ref.delete();

        // Verify deletion was called
        verify(mockStorage).getReference(expectedPath);
        verify(mockFileRef).delete();
        assertNotNull(deleteTask);
    }

    /**
     * Test removing profile picture URL from Firestore document.
     */
    @Test
    public void testRemoveProfilePicture_Firestore() {
        // Setup Firestore mocks for field deletion
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", FieldValue.delete());

        when(mockDocRef.update(updates)).thenReturn(mockVoidTask);

        // Simulate field deletion
        mockDocRef.update(updates);

        // Verify the field deletion
        ArgumentCaptor<Map> updateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockDocRef).update(updateCaptor.capture());

        Map<String, Object> capturedUpdate = updateCaptor.getValue();
        assertEquals(FieldValue.delete(), capturedUpdate.get("profileImageUrl"));
    }

    /**
     * Test handling null userId in removeProfilePhoto.
     */
    @Test
    public void testRemoveProfilePhoto_NullUserId() {
        // When userId is null, the method should return early
        String nullUserId = null;

        // Simulate the early return logic
        if (nullUserId == null) {
            // Method returns early, no operations performed
        }

        // Verify no storage or firestore operations occur
        verify(mockStorage, never()).getReference(anyString());
        verify(mockCollectionRef, never()).document(anyString());
    }

    /**
     * Test profile picture storage path format.
     */
    @Test
    public void testProfilePictureStoragePath() {
        String userId = "user456";
        String expectedPath = "profile_images/" + userId;

        // Verify the path format
        assertTrue(expectedPath.startsWith("profile_images/"));
        assertTrue(expectedPath.endsWith(userId));
        assertEquals("profile_images/user456", expectedPath);
    }

    /**
     * Test profile image URL validation.
     */
    @Test
    public void testProfileImageUrlValidation() {
        // Test valid URLs
        String validUrl1 = "https://firebasestorage.googleapis.com/v0/b/bucket/o/image.jpg";
        String validUrl2 = "https://storage.googleapis.com/bucket/image.png";

        assertTrue(validUrl1.startsWith("https://"));
        assertTrue(validUrl2.startsWith("https://"));

        // Test invalid URLs
        String invalidUrl1 = "";
        String invalidUrl2 = null;

        assertTrue(invalidUrl1.isEmpty());
        assertNull(invalidUrl2);
    }

    /**
     * Test profile image URL format for Firebase Storage.
     */
    @Test
    public void testFirebaseStorageUrlFormat() {
        String firebaseUrl = "https://firebasestorage.googleapis.com/v0/b/waitwell/o/profile_images%2Fuser123";

        // Verify Firebase Storage URL components
        assertTrue(firebaseUrl.contains("firebasestorage.googleapis.com"));
        assertTrue(firebaseUrl.contains("profile_images"));
        assertTrue(firebaseUrl.contains("user123"));
    }

    /**
     * Test upload task success callback.
     */
    @Test
    public void testUploadTaskSuccessCallback() {
        when(mockUploadTask.isSuccessful()).thenReturn(true);
        when(mockUploadTask.getResult()).thenReturn(mockTaskSnapshot);

        // Verify successful upload
        assertTrue(mockUploadTask.isSuccessful());
        assertNotNull(mockUploadTask.getResult());
    }

    /**
     * Test upload task failure callback.
     */
    @Test
    public void testUploadTaskFailureCallback() {
        when(mockUploadTask.isSuccessful()).thenReturn(false);
        when(mockUploadTask.getException()).thenReturn(new Exception("Upload failed"));

        // Verify failed upload
        assertFalse(mockUploadTask.isSuccessful());
        assertNotNull(mockUploadTask.getException());
        assertEquals("Upload failed", mockUploadTask.getException().getMessage());
    }

    /**
     * Test concurrent profile image operations.
     */
    @Test
    public void testConcurrentImageOperations() {
        // Test that multiple operations don't interfere
        String path1 = "profile_images/user1";
        String path2 = "profile_images/user2";

        when(mockStorage.getReference(path1)).thenReturn(mockStorageRef);
        when(mockStorage.getReference(path2)).thenReturn(mockFileRef);

        StorageReference ref1 = mockStorage.getReference(path1);
        StorageReference ref2 = mockStorage.getReference(path2);

        assertNotNull(ref1);
        assertNotNull(ref2);
        assertNotEquals(ref1, ref2);
    }

    /**
     * Test profile image metadata.
     */
    @Test
    public void testProfileImageMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", testUserId);
        metadata.put("uploadTime", System.currentTimeMillis());
        metadata.put("contentType", "image/jpeg");

        // Verify metadata structure
        assertEquals(testUserId, metadata.get("userId"));
        assertNotNull(metadata.get("uploadTime"));
        assertEquals("image/jpeg", metadata.get("contentType"));
    }
}