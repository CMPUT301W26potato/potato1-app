package com.example.waitwell;

import android.app.Activity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

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

                String message = activity.getString(R.string.profile_preview_body, name, joinDateStr);

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.profile_preview_title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            });
        });
    }
}
