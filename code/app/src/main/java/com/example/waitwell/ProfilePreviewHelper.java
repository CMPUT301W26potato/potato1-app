package com.example.waitwell;

import android.app.Activity;
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

public class ProfilePreviewHelper {

    private ProfilePreviewHelper() {}

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

                // Build a custom view with image + text
                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(48, 32, 48, 16);

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
                layout.addView(textView);

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.profile_preview_title)
                        .setView(layout)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            });
        });
    }
}
