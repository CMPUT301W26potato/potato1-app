package com.example.waitwell;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Profile extends AppCompatActivity {
    EditText nameField;
    EditText emailField;
    EditText phoneField;
    Button saveButton;

    DatabaseReference databaseUsers;

    String userId; //storing user's id
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get stored user ID
        userId = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE)
                .getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish(); // go back to login/register
            return;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        phoneField = findViewById(R.id.phoneField);
        saveButton = findViewById(R.id.saveButton);

        //firebase
        databaseUsers = FirebaseDatabase.getInstance().getReference("users");

        loadProfile(); //loading existing profile (if exist)
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        databaseUsers.child(userId).get().addOnSuccessListener(dataSnapshot -> {
            User user = dataSnapshot.getValue(User.class);
            if (user != null) {
                nameField.setText(user.name);
                emailField.setText(user.email);
                phoneField.setText(user.phone);
            }
        });
    }
    private void saveProfile() {

        String name = nameField.getText().toString();
        String email = emailField.getText().toString();
        String phone = phoneField.getText().toString();

        // Example validation
        if(name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email required", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(name, email, phone);

        // save or update profile in firebase
        databaseUsers.child(userId).setValue(user);


        Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();

        finish(); // return to profile page
    }
}

