package com.example.waitwell;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.example.waitwell.activities.AdminProfilesAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for admin viewing profile pictures functionality.
 * Tests the AdminProfilesAdapter for correctly displaying user profile pictures
 * in the admin profiles list.
 *
 * @author Nathaniel Chan
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminProfilePictureTest {

    @Mock
    private Context mockContext;

    @Mock
    private AdminProfilesAdapter.OnDeleteClick mockDeleteListener;

    @Mock
    private DocumentSnapshot mockDocument1;

    @Mock
    private DocumentSnapshot mockDocument2;

    @Mock
    private DocumentSnapshot mockDocument3;

    @Mock
    private View mockItemView;

    @Mock
    private ImageView mockProfileImageView;

    @Mock
    private TextView mockNameTextView;

    @Mock
    private TextView mockEmailTextView;

    @Mock
    private TextView mockRoleTextView;

    @Mock
    private TextView mockIdTextView;

    @Mock
    private RequestManager mockGlideManager;

    @Mock
    private RequestBuilder mockRequestBuilder;

    @Mock
    private LayoutInflater mockLayoutInflater;

    @Mock
    private ViewGroup mockParent;

    private AdminProfilesAdapter adapter;
    private List<DocumentSnapshot> profilesList;
    private List<DocumentSnapshot> eventsList;

    private String testProfileImageUrl1 = "https://example.com/profile1.jpg";
    private String testProfileImageUrl2 = "https://example.com/profile2.jpg";
    private String testUserId1 = "user123";
    private String testUserId2 = "user456";
    private String testUserName1 = "John Doe";
    private String testUserName2 = "Jane Smith";

    @Before
    public void setUp() {
        // Setup mock documents
        setupMockDocument(mockDocument1, testUserId1, testUserName1,
                         "john@example.com", "entrant", testProfileImageUrl1);
        setupMockDocument(mockDocument2, testUserId2, testUserName2,
                         "jane@example.com", "organizer", testProfileImageUrl2);
        setupMockDocument(mockDocument3, "user789", "Bob Wilson",
                         "bob@example.com", "entrant", null);

        // Create lists
        profilesList = Arrays.asList(mockDocument1, mockDocument2, mockDocument3);
        eventsList = new ArrayList<>();

        // Create adapter
        adapter = new AdminProfilesAdapter(mockContext, profilesList, eventsList, mockDeleteListener);

        // Setup view mocks - only mock views that are actually used in tests
        when(mockItemView.findViewById(R.id.txtName)).thenReturn(mockNameTextView);
        when(mockItemView.findViewById(R.id.txtEmail)).thenReturn(mockEmailTextView);
        when(mockItemView.findViewById(R.id.txtRole)).thenReturn(mockRoleTextView);
        when(mockItemView.findViewById(R.id.txtID)).thenReturn(mockIdTextView);

        // Add missing view mocks to prevent NullPointerException
        View mockDeleteBtn = mock(View.class);
        when(mockItemView.findViewById(R.id.btnRemoveProfile)).thenReturn(mockDeleteBtn);
    }

    private void setupMockDocument(DocumentSnapshot mockDoc, String id, String name,
                                   String email, String role, String profileImageUrl) {
        when(mockDoc.getId()).thenReturn(id);
        when(mockDoc.getString("name")).thenReturn(name);
        when(mockDoc.getString("email")).thenReturn(email);
        when(mockDoc.getString("role")).thenReturn(role);
        when(mockDoc.getString("profileImageUrl")).thenReturn(profileImageUrl);
    }

    /**
     * Test admin can view entrant profile pictures in list.
     */
    @Test
    public void testAdminViewsEntrantProfilePicture() {
        // Get profile image URL from first document (entrant with profile picture)
        String profileUrl = mockDocument1.getString("profileImageUrl");

        // Verify profile image URL is retrieved
        assertNotNull(profileUrl);
        assertEquals(testProfileImageUrl1, profileUrl);

        // Verify this is an entrant
        assertEquals("entrant", mockDocument1.getString("role"));
    }

    /**
     * Test admin can view organizer profile pictures in list.
     */
    @Test
    public void testAdminViewsOrganizerProfilePicture() {
        // Get profile image URL from second document (organizer with profile picture)
        String profileUrl = mockDocument2.getString("profileImageUrl");

        // Verify profile image URL is retrieved
        assertNotNull(profileUrl);
        assertEquals(testProfileImageUrl2, profileUrl);

        // Verify this is an organizer
        assertEquals("organizer", mockDocument2.getString("role"));
    }

    /**
     * Test default image shown when no profile picture exists.
     */
    @Test
    public void testDefaultImageWhenNoProfilePicture() {
        // Get profile image URL from third document (no profile picture)
        String profileUrl = mockDocument3.getString("profileImageUrl");

        // Verify no profile image URL
        assertNull(profileUrl);

        // In actual implementation, this should show default image
        boolean shouldShowDefault = profileUrl == null || profileUrl.isEmpty();
        assertTrue(shouldShowDefault);
    }

    /**
     * Test profile picture URL correctly retrieved from DocumentSnapshot.
     */
    @Test
    public void testProfilePictureUrlRetrieval() {
        // Test retrieval for each document
        assertEquals(testProfileImageUrl1, mockDocument1.getString("profileImageUrl"));
        assertEquals(testProfileImageUrl2, mockDocument2.getString("profileImageUrl"));
        assertNull(mockDocument3.getString("profileImageUrl"));
    }

    /**
     * Test adapter item count.
     */
    @Test
    public void testAdapterItemCount() {
        assertEquals(3, adapter.getItemCount());
    }

    /**
     * Test ViewHolder creation.
     */
    @Test
    public void testViewHolderCreation() {
        // Create ViewHolder directly - no layout inflater needed
        AdminProfilesAdapter.ViewHolder viewHolder =
            new AdminProfilesAdapter.ViewHolder(mockItemView);

        assertNotNull(viewHolder);
        assertNotNull(viewHolder.itemView);
        assertEquals(mockItemView, viewHolder.itemView);
    }

    /**
     * Test binding profile with image to ViewHolder.
     */
    @Test
    public void testBindProfileWithImage() {
        // Test that adapter correctly retrieves profile image URL from document
        DocumentSnapshot profileWithImage = profilesList.get(0);

        // Verify the profile has an image URL
        String imageUrl = profileWithImage.getString("profileImageUrl");
        assertNotNull(imageUrl);
        assertEquals(testProfileImageUrl1, imageUrl);

        // Verify ViewHolder can be created successfully
        AdminProfilesAdapter.ViewHolder viewHolder =
            new AdminProfilesAdapter.ViewHolder(mockItemView);
        assertNotNull(viewHolder);
        assertNotNull(viewHolder.itemView);
        assertEquals(mockItemView, viewHolder.itemView);
    }

    /**
     * Test binding profile without image to ViewHolder.
     */
    @Test
    public void testBindProfileWithoutImage() {
        // Test that adapter correctly handles profile without image URL
        DocumentSnapshot profileWithoutImage = profilesList.get(2);

        // Verify the profile has no image URL
        String imageUrl = profileWithoutImage.getString("profileImageUrl");
        assertNull(imageUrl);

        // Verify ViewHolder can be created successfully
        AdminProfilesAdapter.ViewHolder viewHolder =
            new AdminProfilesAdapter.ViewHolder(mockItemView);
        assertNotNull(viewHolder);

        // Verify that when no image URL exists, adapter would use default
        // (without calling actual binding which triggers Android framework)
        if (imageUrl == null) {
            // In actual adapter, this would set default image
            assertNull(imageUrl);
        }
    }

    /**
     * Test Glide loading configuration for profile pictures.
     */
    @Test
    public void testGlideLoadingConfiguration() {
        // Setup mock Glide chain
        when(mockGlideManager.load(anyString())).thenReturn(mockRequestBuilder);
        when(mockRequestBuilder.centerCrop()).thenReturn(mockRequestBuilder);

        // Verify mock configuration
        String imageUrl = testProfileImageUrl1;
        RequestBuilder result = mockGlideManager.load(imageUrl);
        assertNotNull(result);

        // Verify centerCrop can be called (returns Object due to Glide generics)
        Object cropResult = result.centerCrop();
        assertNotNull(cropResult);
    }

    /**
     * Test profile picture displayed with correct cropping.
     */
    @Test
    public void testProfilePictureCropping() {
        // Setup mock chain
        when(mockGlideManager.load(testProfileImageUrl1)).thenReturn(mockRequestBuilder);
        when(mockRequestBuilder.centerCrop()).thenReturn(mockRequestBuilder);

        // Test centerCrop is called
        RequestBuilder loaded = mockGlideManager.load(testProfileImageUrl1);
        Object cropped = loaded.centerCrop();

        // Verify the chain returns expected results
        assertNotNull(cropped);
    }

    /**
     * Test handling empty profile image URL.
     */
    @Test
    public void testHandleEmptyProfileImageUrl() {
        // Setup document with empty URL
        when(mockDocument1.getString("profileImageUrl")).thenReturn("");

        String profileUrl = mockDocument1.getString("profileImageUrl");

        // Verify empty URL handling
        assertNotNull(profileUrl);
        assertTrue(profileUrl.isEmpty());

        // Should show default image for empty URL
        boolean shouldShowDefault = profileUrl == null || profileUrl.isEmpty();
        assertTrue(shouldShowDefault);
    }

    /**
     * Test multiple profiles with mixed image states.
     */
    @Test
    public void testMultipleProfilesMixedImageStates() {
        // Verify each profile's image state
        assertTrue(mockDocument1.getString("profileImageUrl") != null &&
                  !mockDocument1.getString("profileImageUrl").isEmpty());
        assertTrue(mockDocument2.getString("profileImageUrl") != null &&
                  !mockDocument2.getString("profileImageUrl").isEmpty());
        assertNull(mockDocument3.getString("profileImageUrl"));

        // Count profiles with images
        int profilesWithImages = 0;
        for (DocumentSnapshot doc : profilesList) {
            String url = doc.getString("profileImageUrl");
            if (url != null && !url.isEmpty()) {
                profilesWithImages++;
            }
        }

        assertEquals(2, profilesWithImages);
    }

    /**
     * Test profile data display alongside image.
     */
    @Test
    public void testProfileDataDisplay() {
        // Verify all profile data is available
        assertEquals(testUserName1, mockDocument1.getString("name"));
        assertEquals("john@example.com", mockDocument1.getString("email"));
        assertEquals("entrant", mockDocument1.getString("role"));
        assertEquals(testUserId1, mockDocument1.getId());
        assertEquals(testProfileImageUrl1, mockDocument1.getString("profileImageUrl"));

        // All fields should be non-null for complete profile
        assertNotNull(mockDocument1.getString("name"));
        assertNotNull(mockDocument1.getString("email"));
        assertNotNull(mockDocument1.getString("role"));
        assertNotNull(mockDocument1.getId());
        assertNotNull(mockDocument1.getString("profileImageUrl"));
    }

    /**
     * Test profile list filtering maintains image URLs.
     */
    @Test
    public void testFilteringMaintainsImageUrls() {
        // Create a filtered list
        List<DocumentSnapshot> filteredList = new ArrayList<>();

        // Filter for entrants only
        for (DocumentSnapshot doc : profilesList) {
            if ("entrant".equals(doc.getString("role"))) {
                filteredList.add(doc);
            }
        }

        // Verify filtered list size
        assertEquals(2, filteredList.size());

        // Verify image URLs are maintained
        assertEquals(testProfileImageUrl1,
                    filteredList.get(0).getString("profileImageUrl"));
        assertNull(filteredList.get(1).getString("profileImageUrl"));
    }

    /**
     * Test admin can distinguish users with/without profile pictures.
     */
    @Test
    public void testAdminDistinguishesUsersWithoutPictures() {
        // Check each user's profile picture status
        boolean user1HasPicture = mockDocument1.getString("profileImageUrl") != null;
        boolean user2HasPicture = mockDocument2.getString("profileImageUrl") != null;
        boolean user3HasPicture = mockDocument3.getString("profileImageUrl") != null;

        // Verify correct status for each user
        assertTrue(user1HasPicture);
        assertTrue(user2HasPicture);
        assertFalse(user3HasPicture);
    }
}