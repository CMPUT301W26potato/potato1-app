package com.example.waitwell;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.activities.MainActivity;
import com.example.waitwell.activities.RegisterActivity;
import com.example.waitwell.activities.WaitListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Profile activity allows the user to view and update their profile information.
 * it shows the current name, email, and phone at the top,
 * and lets the user edit them in editable fields below.
 *
 * author: Sarang Kim
 */
public class Profile extends AppCompatActivity {

    // editable fields for user to change their info
    private EditText nameField, emailField, phoneField;
    private Button saveButton; // button to save profile changes

    // textviews to display the current info
    private TextView currentName, currentEmail, currentPhone;

    // stores the current user's id from shared preferences
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // get stored user id from shared preferences
        userId = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE)
                .getString("userId", null);

        // if no user id is found, redirect to register screen
        if (userId == null) {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }

        // setup top bar back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // go back when pressed

        // initialize editable fields
        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        phoneField = findViewById(R.id.phoneField);
        saveButton = findViewById(R.id.saveButton);

        // initialize current info labels
        currentName = findViewById(R.id.currentName);
        currentEmail = findViewById(R.id.currentEmail);
        currentPhone = findViewById(R.id.currentPhone);

        // load the user's profile from firestore and display
        loadProfile();

        // save button click listener to update profile
        saveButton.setOnClickListener(v -> saveProfile());

        // setup bottom navigation listener
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_waitlist) startActivity(new Intent(this, WaitListActivity.class));
            else if (id == R.id.nav_notifications)
                startActivity(new Intent(this, EntrantNotificationScreen.class));
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
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // get the values or set empty string if null
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");

                        // populate editable fields
                        nameField.setText(name != null ? name : "");
                        emailField.setText(email != null ? email : "");
                        phoneField.setText(phone != null ? phone : "");

                        // populate current info labels
                        currentName.setText("Current Name: " + (name != null ? name : "Not set"));
                        currentEmail.setText("Current Email: " + (email != null ? email : "Not set"));
                        currentPhone.setText("Current Phone: " + (phone != null ? phone : "Not set"));
                    } else {
                        // if document not found, show 'not found'
                        currentName.setText("Current Name: Not found");
                        currentEmail.setText("Current Email: Not found");
                        currentPhone.setText("Current Phone: Not found");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
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
}