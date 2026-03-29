package com.example.waitwell.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Organizer list of lottery-sampled entrants (Firestore status {@code selected}).
 */
public class SampledEntrantAdapter extends RecyclerView.Adapter<SampledEntrantAdapter.Holder> {

    public interface Listener {
        void onConfirm(@NonNull SampledEntrantItem item);

        void onRemoveFromSampled(@NonNull SampledEntrantItem item);

        void onSelectionChanged();
    }

    private final Listener listener;
    private final List<SampledEntrantItem> allItems = new ArrayList<>();
    private final List<SampledEntrantItem> visibleItems = new ArrayList<>();
    private String filterQuery = "";

    public SampledEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<SampledEntrantItem> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    public void setFilterQuery(@NonNull String query) {
        filterQuery = query.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    /**
     * Drops an entrant from the list after Firestore updates (status no longer {@code selected}).
     */
    public void removeEntry(@NonNull String entryDocumentId) {
        SampledEntrantItem toRemove = null;
        for (SampledEntrantItem item : allItems) {
            if (entryDocumentId.equals(item.entryDocumentId)) {
                toRemove = item;
                break;
            }
        }
        if (toRemove == null) {
            return;
        }
        allItems.remove(toRemove);
        visibleItems.remove(toRemove);
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    private void applyFilter() {
        visibleItems.clear();
        if (filterQuery.isEmpty()) {
            visibleItems.addAll(allItems);
        } else {
            for (SampledEntrantItem item : allItems) {
                String name = item.displayName != null ? item.displayName.toLowerCase(Locale.getDefault()) : "";
                if (name.contains(filterQuery)) {
                    visibleItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    public void setAllChecked(boolean checked) {
        for (SampledEntrantItem item : allItems) {
            item.checked = checked;
        }
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    public boolean areAllChecked() {
        if (allItems.isEmpty()) {
            return false;
        }
        for (SampledEntrantItem item : allItems) {
            if (!item.checked) {
                return false;
            }
        }
        return true;
    }

    public void toggleSelectAll() {
        setAllChecked(!areAllChecked());
    }

    /**
     * Returns a snapshot of all items currently checked by the organizer.
     * Used by SampledEntrantsActivity to know who to notify.
     *
     * @return list of checked SampledEntrantItem objects
     */
    public java.util.List<SampledEntrantItem> getCheckedItems() {
        java.util.List<SampledEntrantItem> checked = new java.util.ArrayList<>();
        for (SampledEntrantItem item : allItems) {
            if (item.checked) {
                checked.add(item);
            }
        }
        return checked;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sampled_entrant_row, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        SampledEntrantItem item = visibleItems.get(position);
        h.txtName.setText(item.displayName != null ? item.displayName : "");

        h.checkbox.setOnCheckedChangeListener(null);
        h.checkbox.setChecked(item.checked);
        h.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.checked = isChecked;
            listener.onSelectionChanged();
        });

        h.btnConfirm.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onConfirm(visibleItems.get(pos));
            }
        });
        h.btnRemove.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onRemoveFromSampled(visibleItems.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final AppCompatCheckBox checkbox;
        final TextView txtName;
        final ImageButton btnConfirm;
        final ImageButton btnRemove;

        Holder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkboxSelect);
            txtName = itemView.findViewById(R.id.txtEntrantName);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnRemove = itemView.findViewById(R.id.btnRemoveFromSampled);
        }
    }

    public static final class SampledEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;
        public boolean checked;

        public SampledEntrantItem(String userId, String displayName, String entryDocumentId) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
            this.checked = false;
        }
    }
}
