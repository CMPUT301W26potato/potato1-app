package com.example.waitwell;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.util.UUID;

/**
 * Returns a unique ID for this device.
 *
 * US 01.07.01: "I want to be identified by my device, so that I don't have to use a username and password."
 *
 * This ID is used as the Firestore document key in the "users" collection, so the app can look up whether the device has already
 * registered by checking if that document exists.
 */
public class DeviceUtils {
    private static final String PREFS = "waitwell_prefs";
    private static final String KEY   = "device_id";

    public static String getDeviceId(Context context) {
        //Try the hardware Android ID first
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null && !androidId.isEmpty() && !"9774d56d682e549c".equals(androidId)) {return androidId;}

        //Fallback: generate once and persist
        //We use Android's ANDROID_ID first (persists across app reinstalls as long as the device isn't factory-reset).
        // If that's unavailable we generate a UUID.
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY, id).apply();
        }
        return id;
    }
}
