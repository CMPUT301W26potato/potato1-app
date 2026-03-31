package com.example.waitwell.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class RegistrationAdapter extends RecyclerView.Adapter<RegistrationAdapter.ViewHolder> {

    private final List<DocumentSnapshot> registrations;

    public RegistrationAdapter(List<DocumentSnapshot> registrations) {
        this.registrations = registrations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registration_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = registrations.get(position);

        String rawStatus = doc.getString("status");

        // Only display selected or rejected events
        if (rawStatus == null ||
                (!rawStatus.equalsIgnoreCase("selected") && !rawStatus.equalsIgnoreCase("confirmed") &&
                        !rawStatus.equalsIgnoreCase("rejected"))) {
            // Hide this row by setting visibility to GONE (optional) or skip adding to list
            holder.itemView.setVisibility(View.GONE);
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
        }

        // Event title
        String title = doc.getString("eventTitle");
        holder.txtEventTitle.setText(title != null ? title : "Unknown Event");

        // Map Firestore status to display string
        String displayStatus = rawStatus.equalsIgnoreCase("rejected") ? "Not Selected" : "Selected";
        holder.txtStatus.setText(displayStatus);
    }

    @Override
    public int getItemCount() {
        return registrations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtEventTitle, txtStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtEventTitle = itemView.findViewById(R.id.txtEventTitle);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}