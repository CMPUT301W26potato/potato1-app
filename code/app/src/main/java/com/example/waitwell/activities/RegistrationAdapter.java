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
        holder.txtEventTitle.setText(doc.getString("eventTitle"));
        holder.txtStatus.setText(doc.getString("status") != null ? doc.getString("status") : "Unknown");
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