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
 * Read-only list of cancelled entrants for organizers.
 */
public class CancelledEntrantAdapter extends RecyclerView.Adapter<CancelledEntrantAdapter.Holder> {

    public interface Listener {
        void onViewProfile(@NonNull CancelledEntrantItem item);
    }

    private final Listener listener;
    private final List<CancelledEntrantItem> allItems = new ArrayList<>();
    private final List<CancelledEntrantItem> visibleItems = new ArrayList<>();
    private String filterQuery = "";

    public CancelledEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<CancelledEntrantItem> items) {
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
            for (CancelledEntrantItem item : allItems) {
                String name = item.displayName != null ? item.displayName.toLowerCase(Locale.getDefault()) : "";
                if (name.contains(filterQuery)) {
                    visibleItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    public List<CancelledEntrantItem> getVisibleItemsSnapshot() {
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
        CancelledEntrantItem item = visibleItems.get(position);
        h.txtName.setText(item.displayName != null ? item.displayName : "");
        h.btnEye.setOnClickListener(v -> listener.onViewProfile(item));
        h.btnAccept.setVisibility(View.GONE);
        h.btnDecline.setVisibility(View.GONE);
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

    public static final class CancelledEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;

        public CancelledEntrantItem(String userId, String displayName, String entryDocumentId) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
        }
    }
}
