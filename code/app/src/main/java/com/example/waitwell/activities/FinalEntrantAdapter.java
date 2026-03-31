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
 * Final entrants list: read-only names.
 */
public class FinalEntrantAdapter extends RecyclerView.Adapter<FinalEntrantAdapter.Holder> {
    public interface Listener {
        void onViewProfile(@NonNull FinalEntrantItem item);
    }

    private final Listener listener;
    private final List<FinalEntrantItem> allItems = new ArrayList<>();
    private final List<FinalEntrantItem> visibleItems = new ArrayList<>();
    private String filterQuery = "";

    public FinalEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<FinalEntrantItem> items) {
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
            for (FinalEntrantItem item : allItems) {
                String name = item.displayName != null ? item.displayName.toLowerCase(Locale.getDefault()) : "";
                if (name.contains(filterQuery)) {
                    visibleItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_final_entrant, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        FinalEntrantItem item = visibleItems.get(position);
        h.txtName.setText(item.displayName != null ? item.displayName : "");

        // Defensive guard: hide any legacy action buttons if stale row layouts appear.
        View legacyAccept = h.itemView.findViewById(R.id.btnAccept);
        if (legacyAccept != null) {
            legacyAccept.setVisibility(View.GONE);
        }
        View legacyDecline = h.itemView.findViewById(R.id.btnDecline);
        if (legacyDecline != null) {
            legacyDecline.setVisibility(View.GONE);
        }

        h.btnEye.setOnClickListener(v -> listener.onViewProfile(item));
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final ImageButton btnEye;

        Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtEntrantName);
            btnEye = itemView.findViewById(R.id.btnViewEntrant);
        }
    }

    public static final class FinalEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;

        public FinalEntrantItem(String userId, String displayName, String entryDocumentId) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
        }
    }
}
