package com.example.waitwell;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class for displaying user profile previews in dialog format.
 *
 * Provides a utility method to fetch and display user information including
 * profile picture, name, and join date in a custom AlertDialog. Designed
 * as a utility class with only static methods and a private constructor
 * to prevent instantiation.
 *
 * @author Nathaniel Chan 
 */
public class ProfilePreviewHelper {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ProfilePreviewHelper() {}

    /**
     * Displays a user's profile information in a custom AlertDialog.
     *
     * Fetches user data from Firebase, creates a custom dialog layout with:
     * - Centered title "User Profile"
     * - Profile image (if available) displayed as a circular image
     * - User's name and join date formatted as "Name: [name]\nJoined: [date]"
     *
     * Handles edge cases including missing data, activity lifecycle states,
     * and Firebase fetch failures. All UI updates are performed on the main thread.
     *
     * @param activity The activity context for displaying the dialog and loading images.
     *                 Must not be finishing or destroyed.
     * @param userId   The Firebase user ID to fetch profile information for
     */
    public static void showProfileDialog(Activity activity, String userId) {
        FirebaseHelper.getInstance().fetchUserDocumentForWaitlistUserId(userId, task -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;

            activity.runOnUiThread(() -> {
                if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                    Toast.makeText(activity, R.string.profile_preview_fetch_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = task.getResult().getString("name");
                if (name == null || name.isEmpty()) {
                    name = "Unknown";
                }

                Date joinDate = task.getResult().getDate("createdAt");
                String joinDateStr;
                if (joinDate != null) {
                    joinDateStr = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(joinDate);
                } else {
                    joinDateStr = "Unknown";
                }

                String profileImageUrl = task.getResult().getString("profileImageUrl");

                // Build a custom view with title + image + text
                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(48, 32, 48, 16);

                // Title as part of layout so we can center it
                TextView titleView = new TextView(activity);
                titleView.setText(R.string.profile_preview_title);
                titleView.setGravity(Gravity.CENTER);
                titleView.setTextSize(20);
                titleView.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                titleParams.bottomMargin = 24;
                titleView.setLayoutParams(titleParams);
                layout.addView(titleView);

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    ImageView imageView = new ImageView(activity);
                    int size = (int) (100 * activity.getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                    params.bottomMargin = 24;
                    imageView.setLayoutParams(params);
                    Glide.with(activity).load(profileImageUrl).circleCrop().into(imageView);
                    layout.addView(imageView);
                }

                TextView textView = new TextView(activity);
                textView.setText(activity.getString(R.string.profile_preview_body, name, joinDateStr));
                textView.setGravity(Gravity.CENTER);
                layout.addView(textView);

                new AlertDialog.Builder(activity)
                        .setView(layout)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            });
        });
    }
}
