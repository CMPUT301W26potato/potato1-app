package com.example.waitwell.activities;

/**
 * Lightweight display model for one image row in admin image management screens.
 * Wraps the Firestore document id, a human-readable title (typically the event
 * name the image belongs to), and the remote URL used by Glide to load the image.
 * @author Viktoria Lysenko
 */
public class ImageItem {
    /** Firestore document id for the source record (e.g. event id). */
    public String id;
    public String title;
    /** Remote URL of the image to load with Glide. */
    public String imageUrl;

    /**
     * Creates an ImageItem for display in an admin image list.
     *
     * @param id       Firestore document id of the source record
     * @param title    label to display alongside the image
     * @param imageUrl remote URL of the image
     */
    public ImageItem(String id, String title, String imageUrl) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
    }


}