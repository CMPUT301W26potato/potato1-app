package com.example.waitwell;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.waitwell.activities.MainActivity;
import com.example.waitwell.activities.WaitListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Profile activity allows the user to view and update their profile information.
 * it shows the current name, email, and phone at the top,
 * and lets the user edit them in editable fields below.
 *
 * @author Sarang Kim
 */
public class Profile extends AppCompatActivity {

    private static final String TAG = "Profile";

    // editable fields for user to change their info
    private EditText nameField, emailField, phoneField;
    private Button saveButton; // button to save profile changes

    // textviews to display the current info
    private TextView currentName, currentEmail, currentPhone, joinDate;

    // profile image views
    private ImageView profileImageView;
    private Button btnUploadPhoto, btnRemovePhoto;
    private Uri selectedImageUri;
    private String existingProfileImageUrl;

    // stores the current user's id from shared preferences
    private String userId;

    /**
     * Handles the photo picker interaction when users want to update their profile picture.
     * This launcher opens the system's image gallery and waits for the user to select
     * a photo. Once they pick something (or cancel), it updates the UI accordingly.
     * If a valid image is selected, it shows a preview and enables the remove button.
     *
     * Think of this as the middleman between our app and the photo gallery - it starts
     * the gallery activity and catches whatever the user picks when they come back.
     * @author Nathaniel Chan
     */
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(profileImageView);
                    btnRemovePhoto.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // get the stable device-based user id
        SharedPreferences prefs = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", DeviceUtils.getDeviceId(this));
        // setup top bar back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // go back when pressed

        // initialize profile image views
        profileImageView = findViewById(R.id.profileImageView);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);

        btnUploadPhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        btnRemovePhoto.setOnClickListener(v -> removeProfilePhoto());

        // initialize editable fields
        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        phoneField = findViewById(R.id.phoneField);
        saveButton = findViewById(R.id.saveButton);

        // initialize current info labels
        currentName = findViewById(R.id.currentName);
        currentEmail = findViewById(R.id.currentEmail);
        currentPhone = findViewById(R.id.currentPhone);
        joinDate = findViewById(R.id.joinDate);

        // load the user's profile from firestore and display
        loadProfile();

        // save button click listener to update profile
        saveButton.setOnClickListener(v -> saveProfile());

        // setup bottom navigation listener
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else if (id == R.id.nav_waitlist) {
                Intent intent = new Intent(this, WaitListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else if (id == R.id.nav_notifications) {
                Intent intent = new Intent(this, EntrantNotificationScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            return true; // always return true so the item shows as selected
        });
    }

    /**
     * loads the user's profile from firestore and populates both the
     * editable fields and the current info labels.
     */
    private void loadProfile() {
        // get the document for the current user id
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("deviceId", DeviceUtils.getDeviceId(this))
                .whereEqualTo("role", "entrant")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);

                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        Date joinDate_date = doc.getDate("createdAt");

                        // populate editable fields
                        nameField.setText(name != null ? name : "");
                        emailField.setText(email != null ? email : "");
                        phoneField.setText(phone != null ? phone : "");

                        // populate current info labels
                        currentName.setText("Current Name: " + (name != null ? name : "Not set"));
                        currentEmail.setText("Current Email: " + (email != null ? email : "Not set"));
                        currentPhone.setText("Current Phone: " + (phone != null ? phone : "Not set"));

                        if (joinDate_date != null) {
                            joinDate.setText("Joined: " + joinDate_date.toString());
                        } else {
                            joinDate.setText("No join date found");
                        }

                        // load profile image, so the admin can view entrant pfps
                        existingProfileImageUrl = doc.getString("profileImageUrl");
                        if (existingProfileImageUrl != null && !existingProfileImageUrl.isEmpty()) {
                            Glide.with(Profile.this)
                                    .load(existingProfileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.waitwell_logo)
                                    .into(profileImageView);
                            btnRemovePhoto.setVisibility(View.VISIBLE);
                        } else {
                            profileImageView.setImageResource(R.drawable.waitwell_logo);
                            btnRemovePhoto.setVisibility(View.GONE);
                        }

                        // IMPORTANT: update userId so saveProfile works correctly
                        userId = doc.getId();

                    } else {
                        currentName.setText("Current Name: Not found");
                        currentEmail.setText("Current Email: Not found");
                        currentPhone.setText("Current Phone: Not found");
                        joinDate.setText("No join date found");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * saves the user's edited profile information to firestore.
     * uses set with merge to safely create or update fields.
     */
    private void saveProfile() {
        // get values from editable fields
        String name = nameField.getText().toString();
        String email = emailField.getText().toString();
        String phone = phoneField.getText().toString();

        // validate name and email
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email required", Toast.LENGTH_SHORT).show();
            return;
        }

        // build map to save
        Map<String, Object> updatedUser = new HashMap<>();
        updatedUser.put("name", name);
        updatedUser.put("email", email);
        updatedUser.put("phone", phone);

        if (selectedImageUri != null) {
            // upload new image first, then save profile
            saveButton.setEnabled(false);
            uploadProfileImageThenSave(updatedUser);
        } else {
            // no new image, save directly
            saveToFirestore(updatedUser);
        }
    }

    /**
     * Uploads a user's selected profile image to Firebase Storage and then saves the complete
     * profile data to Firestore, including the image URL.
     *
     *
     *   Uploads the image file stored in {@code selectedImageUri} to Firebase Storage
     *       under the path "profile_images/{userId}""
     *   Retrieves the download URL for the uploaded image and adds it to the user data
     *   Saves the complete user profile data to Firestore with the image URL included
     * 
     *   On success: Updates the profile image URL, clears the selected image URI,
     *       re-enables the save button, and proceeds to save to Firestore
     *   On failure: Re-enables the save button, logs the error, and shows an error
     *       message to the user via Toast
     * 
     *
     * @param updatedUser A map containing the user profile fields to be saved to Firestore
     *                    (name, email, phone). The profileImageUrl field will be added
     *                    to this map after successful upload.
     *
     * @see #saveToFirestore(Map) for the actual Firestore save operation
     * @see #saveProfile() for the method that initiates this upload process
     */
    private void uploadProfileImageThenSave(Map<String, Object> updatedUser) {
        String fileName = "profile_images/" + userId;
        StorageReference ref = FirebaseStorage.getInstance().getReference(fileName);
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    String url = downloadUri.toString();
                                    updatedUser.put("profileImageUrl", url);
                                    existingProfileImageUrl = url;
                                    selectedImageUri = null;
                                    saveButton.setEnabled(true);
                                    saveToFirestore(updatedUser);
                                })
                                .addOnFailureListener(e -> {
                                    saveButton.setEnabled(true);
                                    Log.e(TAG, "Failed to get download URL", e);
                                    Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                                }))
                .addOnFailureListener(e -> {
                    saveButton.setEnabled(true);
                    Log.e(TAG, "Profile image upload failed", e);
                    Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore(Map<String, Object> updatedUser) {
        // save to firestore, merge so it doesn't overwrite other fields
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(updatedUser, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Removes the user's profile photo from both Firebase Storage and Firestore.
     *
     * Performs a two-step deletion process:
     * 1. Deletes the image file from Firebase Storage at "profile_images/{userId}"
     * 2. Removes the profileImageUrl field from the user's Firestore document
     *
     * On success, resets the UI to show the default logo and hides the remove button.
     * Storage deletion failures are logged but don't block the Firestore update.
     * Returns early if userId is null.
     *
     * @see #uploadProfileImageThenSave(Map) for adding a profile photo
     */
    private void removeProfilePhoto() {
        if (userId == null) return;

        // delete from storage
        String fileName = "profile_images/" + userId;
        StorageReference ref = FirebaseStorage.getInstance().getReference(fileName);
        ref.delete().addOnFailureListener(e ->
                Log.w(TAG, "Failed to delete profile image from storage", e));

        // remove URL from firestore
        Map<String, Object> update = new HashMap<>();
        update.put("profileImageUrl", FieldValue.delete());
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                    existingProfileImageUrl = null;
                    selectedImageUri = null;
                    profileImageView.setImageResource(R.drawable.waitwell_logo);
                    btnRemovePhoto.setVisibility(View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove photo", Toast.LENGTH_SHORT).show());
    }
}