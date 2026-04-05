package com.example.waitwell.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminProfilesAdapter extends RecyclerView.Adapter<AdminProfilesAdapter.ViewHolder> {

    private Context context;
    private List<DocumentSnapshot> originalList; // full list
    private List<DocumentSnapshot> filteredList; // filtered list
    List<DocumentSnapshot> allEvents;
    java.util.Map<String, Integer> eventCountCache = new java.util.HashMap<>();

    public interface OnDeleteClick {
        void onDelete(String userId);
    }

    private OnDeleteClick deleteListener;

    public AdminProfilesAdapter(Context context,
                                List<DocumentSnapshot> profiles,
                                List<DocumentSnapshot> events,
                                OnDeleteClick listener) {

        this.context = context;
        this.originalList = profiles;
        this.filteredList = new ArrayList<>(profiles);
        this.allEvents = events;
        this.deleteListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, role, email, id;
        View deleteBtn;

        public ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.txtName);
            role = v.findViewById(R.id.txtRole);
            email = v.findViewById(R.id.txtEmail);
            id = v.findViewById(R.id.txtID);
            deleteBtn = v.findViewById(R.id.btnRemoveProfile);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DocumentSnapshot doc = filteredList.get(position);

        String name = doc.getString("name");
        String email = doc.getString("email");
        String role = doc.getString("role");
        String userId = doc.getId();

// Name
        ((TextView) holder.itemView.findViewById(R.id.txtName))
                .setText("Name: " + (name != null ? name : "Unknown"));

// Email
        ((TextView) holder.itemView.findViewById(R.id.txtEmail))
                .setText("Email: " + (email != null ? email : "Unknown"));

// Role
        ((TextView) holder.itemView.findViewById(R.id.txtRole))
                .setText("Role: " + (role != null ? role : "Unknown"));

// ID
        ((TextView) holder.itemView.findViewById(R.id.txtID))
                .setText("ID: " + userId);

// Profile image
        String profileImageUrl = doc.getString("profileImageUrl");
        ImageView profileImage = holder.itemView.findViewById(R.id.profileImage);
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(context).load(profileImageUrl).centerCrop().into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.waitwell_logo);
        }

        String id = doc.getId();



        holder.deleteBtn.setOnClickListener(v -> showDeleteDialog(id));

        TextView eventsView = holder.itemView.findViewById(R.id.txtEventsJoined);

        if (eventCountCache.containsKey(userId)) {
            eventsView.setText("Events Joined: " + eventCountCache.get(userId));
        } else {

            int count = 0;

            for (DocumentSnapshot event : allEvents) {

                java.util.List<String> entrants =
                        (java.util.List<String>) event.get("waitlistEntrantIds"); // or attendingEntrantIds

                if (entrants != null && entrants.contains(userId)) {
                    count++;
                }
            }

            eventCountCache.put(userId, count);
            eventsView.setText("Events Joined: " + count);
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }


    public void filterSearch(String text) {
        filteredList.clear();

        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            text = text.toLowerCase();

            for (DocumentSnapshot doc : originalList) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String id = doc.getId();

                if ((name != null && name.toLowerCase().contains(text)) ||
                        (email != null && email.toLowerCase().contains(text)) ||
                        id.toLowerCase().contains(text)) {
                    filteredList.add(doc);
                }
            }
        }

        notifyDataSetChanged();
    }


    public void filterRole(String roleFilter) {
        filteredList.clear();

        if (roleFilter.equals("All")) {
            filteredList.addAll(originalList);
        } else {
            for (DocumentSnapshot doc : originalList) {
                String role = doc.getString("role");

                if (role != null && role.equalsIgnoreCase(roleFilter)) {
                    filteredList.add(doc);
                }
            }
        }

        notifyDataSetChanged();
    }


    private void showDeleteDialog(String userId) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (deleteListener != null) {
                        deleteListener.onDelete(userId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}