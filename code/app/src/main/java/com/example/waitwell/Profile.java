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

public class Profile extends AppCompatActivity {

    EditText nameField, emailField, phoneField;
    Button saveButton;
    TextView currentName, currentEmail, currentPhone;

    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get stored user ID
        userId = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE)
                .getString("userId", null);

        if (userId == null) {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }

        // Top bar
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Fields
        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        phoneField = findViewById(R.id.phoneField);
        saveButton = findViewById(R.id.saveButton);

        currentName = findViewById(R.id.currentName);
        currentEmail = findViewById(R.id.currentEmail);
        currentPhone = findViewById(R.id.currentPhone);

        loadProfile();

        saveButton.setOnClickListener(v -> saveProfile());

        // Bottom navigation
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.nav_waitlist) startActivity(new Intent(this, WaitListActivity.class));
            else if (id == R.id.nav_notifications)
                startActivity(new Intent(this, EntrantNotificationScreen.class));
            return true;
        });
    }

    private void loadProfile() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");

                        // Editable fields
                        nameField.setText(name != null ? name : "");
                        emailField.setText(email != null ? email : "");
                        phoneField.setText(phone != null ? phone : "");

                        // Current info labels
                        currentName.setText("Current Name: " + (name != null ? name : "Not set"));
                        currentEmail.setText("Current Email: " + (email != null ? email : "Not set"));
                        currentPhone.setText("Current Phone: " + (phone != null ? phone : "Not set"));
                    } else {
                        currentName.setText("Current Name: Not found");
                        currentEmail.setText("Current Email: Not found");
                        currentPhone.setText("Current Phone: Not found");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String name = nameField.getText().toString();
        String email = emailField.getText().toString();
        String phone = phoneField.getText().toString();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedUser = new HashMap<>();
        updatedUser.put("name", name);
        updatedUser.put("email", email);
        updatedUser.put("phone", phone);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(updatedUser, SetOptions.merge()) // safer: create or update
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show());
    }
}