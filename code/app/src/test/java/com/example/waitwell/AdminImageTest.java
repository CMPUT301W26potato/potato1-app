package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for admin permissions related to Images.
 *
 * Verifies that:
 * - Admins can view and delete images
 * - Non-admins cannot perform these actions
 * - Null inputs are handled safely
 */
public class AdminImageTest {

    /**
     * Tests that an admin can view all images.
     */
    @Test
    public void adminCanViewAllImages() {
        assertTrue(ImageUtils.canViewImages("admin"));
    }

    /**
     * Tests that an admin can delete any image.
     */
    @Test
    public void adminCanDeleteAnyImage() {
        assertTrue(ImageUtils.canDeleteImage("admin", "img123"));
    }

    /**
     * Tests that non-admin users cannot view images.
     */
    @Test
    public void nonAdminCannotViewImages() {
        assertFalse(ImageUtils.canViewImages("organizer"));
        assertFalse(ImageUtils.canViewImages("entrant"));
    }

    /**
     * Tests that non-admin users cannot delete images.
     */
    @Test
    public void nonAdminCannotDeleteImages() {
        assertFalse(ImageUtils.canDeleteImage("organizer", "img123"));
        assertFalse(ImageUtils.canDeleteImage("entrant", "img123"));
    }

    /**
     * Tests handling of null inputs.
     */
    @Test
    public void nullInputsCannotViewOrDeleteImages() {
        assertFalse(ImageUtils.canViewImages(null));
        assertFalse(ImageUtils.canDeleteImage(null, "img123"));
        assertFalse(ImageUtils.canDeleteImage("admin", null));
    }
}