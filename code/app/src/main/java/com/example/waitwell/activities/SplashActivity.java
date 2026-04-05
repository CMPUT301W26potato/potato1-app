package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.R;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Splash screen shown on app launch.
 * Displays the Wait Well logo and tagline for 2 seconds.
 * Checks if this device already has a user profile.
 * 2. Query Firestore: does "users/{deviceId}" exist?
 * YES -> go to MainActivity (returning user)
 * NO -> go to RegisterActivity (first-time user)
 * ERROR -> go to RegisterActivity (safe fallback)
 *
 *  The 2-second delay runs in parallel with the Firestore query,
 *  * so whichever finishes last triggers the navigation. This means
 *  * the user always sees the splash for at least 2 seconds but never
 *  * waits longer than necessary.
 */
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 2000;

    /** Becomes true once the Firestore check completes. */
    private boolean checkDone = false;
    /** Becomes true once the 2-second timer fires. */
    private boolean timerDone = false;
    /** The destination decided by the Firestore check. */
    private Class<?> destination = RegisterActivity.class;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //the minimum-display timer
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            timerDone = true;
            navigateIfReady();
        }, SPLASH_DELAY_MS);

        // the Firestore check in parallel
        checkDeviceRegistered();
    }

    private void checkDeviceRegistered() {
        String deviceId = DeviceUtils.getDeviceId(this);
        Log.d(TAG, "deviceId=" + deviceId);
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        String role = snapshot.getDocuments().get(0).getString("role");
                        if ("organizer".equalsIgnoreCase(role)) {
                            destination = OrganizerEntryActivity.class;
                        } else if ("admin".equalsIgnoreCase(role)) {
                            destination = AdminMainMenuActivity.class;
                        } else {
                            destination = MainActivity.class;
                        }
                    } else {
                        destination = RegisterActivity.class;
                    }
                    checkDone = true;
                    navigateIfReady();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore check failed", e);
                    destination = RegisterActivity.class;
                    checkDone = true;
                    navigateIfReady();
                });
    }

    /**
     * navigates once BOTH the timer and the Firestore check
     * have finished
     */
    private void navigateIfReady() {
        if (timerDone && checkDone) {
            startActivity(new Intent(this, destination));
            finish();
        }
    }
}