package com.example.waitwell.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
/**
 * AdminProfilesActivity allows administrators to view
 * and manage all user profiles stored in Firestore.
 *
 * Profiles are displayed in a RecyclerView grid.
 * Each profile card shows:
 * - name
 * - role
 * - email
 * - Firestore document ID
 *
 * Administrators can remove profiles from the system.
 *
 * AI Usage:
 *
 * @author Grace Shin
 */
public class AdminProfilesActivity extends AppCompatActivity {

    RecyclerView recyclerProfiles;
    /**
     * Initializes the activity and loads profile data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profiles);

        recyclerProfiles = findViewById(R.id.recyclerProfiles);
        // Display profiles in 2 column grid
        recyclerProfiles.setLayoutManager(new GridLayoutManager(this, 2));

        loadProfiles();
        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
        });
    }
    /**
     * Retrieves all user profiles from Firestore and populates the RecyclerView.
     */
    private void loadProfiles() {

        FirebaseHelper.getInstance()
                .getAllProfiles()
                .addOnSuccessListener(snapshot -> {

                    List<DocumentSnapshot> profiles = snapshot.getDocuments();

                    RecyclerView.Adapter adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                        @Override
                        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                            // Inflate profile card layout
                            View view = LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.item_admin_profile, parent, false);

                            return new RecyclerView.ViewHolder(view) {};
                        }

                        @Override
                        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                            DocumentSnapshot doc = profiles.get(position);

                            String name = doc.getString("name");
                            String role = doc.getString("role");
                            String email = doc.getString("email");

                            String userId = doc.getId();

                            ((TextView) holder.itemView.findViewById(R.id.txtName)).setText(name);
                            ((TextView) holder.itemView.findViewById(R.id.txtRole)).setText(role);
                            ((TextView) holder.itemView.findViewById(R.id.txtEmail)).setText(email);
                            ((TextView) holder.itemView.findViewById(R.id.txtID)).setText(userId);

                            holder.itemView.findViewById(R.id.btnRemoveProfile)
                                    .setOnClickListener(v -> removeProfile(userId));
                        }

                        @Override
                        public int getItemCount() {
                            return profiles.size();
                        }
                    };

                    recyclerProfiles.setAdapter(adapter);
                });
    }
    /**
     * Removes a user profile from Firestore.
     */
    private void removeProfile(String userId) {

        FirebaseHelper.getInstance()
                .removeProfile(userId)
                .addOnSuccessListener(unused -> {
                    // Navigate to confirmation page
                    Intent intent = new Intent(
                            this,
                            AdminProfileRemovedActivity.class
                    );

                    startActivity(intent);
                });
    }
}
