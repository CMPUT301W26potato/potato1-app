package com.example.waitwell.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * List adapter for organizer "View Requests" waiting-list rows.
 * Filters by display name (case-insensitive) via {@link #setFilterQuery(String)}.
 */
public class WaitlistEntrantAdapter extends RecyclerView.Adapter<WaitlistEntrantAdapter.Holder> {

    public interface Listener {
        void onViewProfile(@NonNull WaitlistEntrantItem item);

        void onAccept(@NonNull WaitlistEntrantItem item);

        void onDecline(@NonNull WaitlistEntrantItem item);
    }

    private final Listener listener;
    private final List<WaitlistEntrantItem> allItems = new ArrayList<>();
    private final List<WaitlistEntrantItem> visibleItems = new ArrayList<>();
    private String filterQuery = "";

    public WaitlistEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<WaitlistEntrantItem> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    public void setFilterQuery(@NonNull String query) {
        filterQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        applyFilter();
    }

    private void applyFilter() {
        visibleItems.clear();
        if (filterQuery.isEmpty()) {
            visibleItems.addAll(allItems);
        } else {
            for (WaitlistEntrantItem item : allItems) {
                String name = item.displayName != null ? item.displayName.toLowerCase(Locale.getDefault()) : "";
                if (name.contains(filterQuery)) {
                    visibleItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    public List<WaitlistEntrantItem> getVisibleItemsSnapshot() {
        return new ArrayList<>(visibleItems);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waitlist_entrant_request, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        WaitlistEntrantItem item = visibleItems.get(position);
        h.txtName.setText(item.displayName != null ? item.displayName : "");
        h.btnEye.setOnClickListener(v -> listener.onViewProfile(item));
        h.btnAccept.setOnClickListener(v -> listener.onAccept(item));
        h.btnDecline.setOnClickListener(v -> listener.onDecline(item));
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final ImageButton btnEye;
        final ImageButton btnAccept;
        final ImageButton btnDecline;

        Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtEntrantName);
            btnEye = itemView.findViewById(R.id.btnViewEntrant);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    /** One row: Firestore userId, display name, composite waitlist_entries document id. */
    public static final class WaitlistEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;

        public WaitlistEntrantItem(String userId, String displayName, String entryDocumentId) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
        }
    }
}
