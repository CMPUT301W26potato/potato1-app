package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class ProfileUtils {

    public static Map<String, Object> buildProfileMap(String name, String email, String phone, String profileImageUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone);
        if (profileImageUrl != null) {
            map.put("profileImageUrl", profileImageUrl);
        }
        return map;
    }

    public static String getNameFromDoc(DocumentSnapshot doc) {
        if (doc == null) return null;
        return doc.getString("name");
    }

    public static String getEmailFromDoc(DocumentSnapshot doc) {
        if (doc == null) return null;
        return doc.getString("email");
    }

    public static String getPhoneFromDoc(DocumentSnapshot doc) {
        if (doc == null) return null;
        return doc.getString("phone");
    }
}