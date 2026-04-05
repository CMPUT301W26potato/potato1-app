package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Registration screen for first-time users.
 *   1. Validate: name and email are required, phone is optional
 *   2. Get this device's unique ID via DeviceUtils
 *   3. Save a document to Firestore at "users/{deviceId}" with:
 *   - name, email, phone, role ("entrant" or "organizer")
 *   - the deviceId itself
 *   - a createdAt timestamp
 * Admin accounts:
 * Admins are added manually in the Firebase Console by creating a user document with role = "admin".
 * Troubleshoot with the help from Claude (claude.ai)
 */
public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private EditText editName, editEmail, editPhone;
    private Spinner spinnerType;
    private Button btnSignUp, btnAlreadyHave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        spinnerType = findViewById(R.id.spinnerAccountType);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnAlreadyHave = findViewById(R.id.btnAlreadyHaveAccount);
        setupSpinner();

        btnSignUp.setOnClickListener(v -> attemptRegister());
        btnAlreadyHave.setOnClickListener(v -> checkExistingAccount());
    }
    /**
     * Only "Entrant" and "Organizer" are offered.
     * Admin is added manually in the Firebase.
     */

    private void setupSpinner() {
        String[] items = {"Type of Account", "Entrant", "Organizer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items) {
            @Override
            public boolean isEnabled(int position) {
                // Position 0 is the placeholder - it can't be re-selected from the dropdown
                return position != 0;
            }
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                android.widget.TextView tv = (android.widget.TextView) view;
                if (position == 0) {
                    tv.setTextColor(android.graphics.Color.GRAY);
                } else {
                    tv.setTextColor(android.graphics.Color.BLACK);
                }
                return view;
            }
        };
        spinnerType.setAdapter(adapter);
        spinnerType.setSelection(0);// show "Type of Account" initially
    }

    private void attemptRegister() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String role = spinnerType.getSelectedItem().toString().toLowerCase();

        if (spinnerType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select an account type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name)) {
            editName.setError("Name is required");
            editName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email is required");
            editEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Enter a valid email");
            editEmail.requestFocus();
            return;
        }

        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating account...");

        //build the user document
        String deviceId = DeviceUtils.getDeviceId(this);
        Map<String, Object> user = new HashMap<>();
        user.put("deviceId", deviceId);
        user.put("name", name);
        user.put("email", email);
        user.put("role", role);
        user.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // optional phone number
        if (!TextUtils.isEmpty(phone)) {
            user.put("phone", phone);
        }

        //Save to Firestore
        // Document ID = deviceId for easier management
        // Save to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if ("entrant".equalsIgnoreCase(role)) {
            // entrant: 1 per device
            user.put("deviceId", deviceId);
            db.collection("users")
                    .whereEqualTo("deviceId", deviceId)
                    .whereEqualTo("role", role)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            Toast.makeText(this, "An entrant account already exists on this device", Toast.LENGTH_LONG).show();
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign up");
                            return;
                        }
                        db.collection("users")
                                .document(deviceId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User registered: " + deviceId);
                                    getSharedPreferences("WaitWellPrefs", MODE_PRIVATE)
                                            .edit().putString("userId", deviceId)
                                            .apply();
                                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                                    if ("organizer".equalsIgnoreCase(role)) {
                                        startActivity(new Intent(this, OrganizerEntryActivity.class));
                                    } else {
                                        startActivity(new Intent(this, MainActivity.class));
                                    }
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Registration failed", e);
                                    Toast.makeText(this, "Registration failed – check your connection", Toast.LENGTH_LONG).show();
                                    btnSignUp.setEnabled(true);
                                    btnSignUp.setText("Sign up");
                                });
                    });
        } else if ("organizer".equalsIgnoreCase(role)) {
            // if organizer it can create just 1 other entrant account
            db.collection("users")
                    .whereEqualTo("createdByOrganizerDeviceId", deviceId)
                    .whereEqualTo("role", "entrant")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            Toast.makeText(this, "Organizer can only create one extra entrant account", Toast.LENGTH_LONG).show();
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign up");
                            return;
                        }

                        // add irganizer created entrant with auto id
                        user.put("deviceId", deviceId); // optional reference
                        user.put("createdByOrganizerDeviceId", deviceId);
                        db.collection("users")
                                .add(user)
                                .addOnSuccessListener(docRef -> {
                                    Log.d(TAG, "Organizer created new entrant: " + docRef.getId());
                                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, OrganizerEntryActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Registration failed", e);
                                    Toast.makeText(this, "Registration failed – check your connection", Toast.LENGTH_LONG).show();
                                    btnSignUp.setEnabled(true);
                                    btnSignUp.setText("Sign up");
                                });
                    });
        }
    }

    /**
     *
     * "Already have an account" button:
     *  Checks if a user document already exists for this device.
     *  If yes - goes to MainActivity. If no - tells them to register.
     *
     */
    private void checkExistingAccount() {
        btnAlreadyHave.setEnabled(true);
        btnAlreadyHave.setText("Already have an account");

        showRoleSelectionDialog();
    }

    private void showRoleSelectionDialog() {

        String[] roles = {"Entrant", "Organizer", "Admin"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select account type")
                .setItems(roles, (dialog, which) -> {

                    String selectedRole = roles[which].toLowerCase();
                    checkRoleAndLogin(selectedRole);

                })
                .show();
    }

    private void checkRoleAndLogin(String role) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String deviceId = DeviceUtils.getDeviceId(this);

        btnAlreadyHave.setEnabled(false);
        btnAlreadyHave.setText("Checking...");

        db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(snapshot -> {




                    if (!snapshot.isEmpty()) {

                        // if found...
                        if ("organizer".equalsIgnoreCase(role)) {
                            startActivity(new Intent(this, OrganizerEntryActivity.class));
                        } else if ("admin".equalsIgnoreCase(role)) {
                            startActivity(new Intent(this, AdminMainMenuActivity.class));
                        } else {
                            startActivity(new Intent(this, MainActivity.class));
                        }

                        finish();

                    } else {
                        Toast.makeText(this,
                                "No " + role + " account found for this device",
                                Toast.LENGTH_LONG).show();

                        btnAlreadyHave.setEnabled(true);
                        btnAlreadyHave.setText("Already have an account");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not check – try again", Toast.LENGTH_SHORT).show();
                    btnAlreadyHave.setEnabled(true);
                    btnAlreadyHave.setText("Already have an account");
                });
    }
}
