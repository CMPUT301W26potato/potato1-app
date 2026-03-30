package com.example.waitwell.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Organizer invited-entrants list: filters by name and by status pills (enrolled / cancelled / pending).
 */
public class InvitedEntrantAdapter extends RecyclerView.Adapter<InvitedEntrantAdapter.Holder> {

    public interface Listener {
        void onViewProfile(@NonNull InvitedEntrantItem item);

        void onSelectionChanged();
    }

    private final Listener listener;
    private final List<InvitedEntrantItem> allItems = new ArrayList<>();
    private final List<InvitedEntrantItem> visibleItems = new ArrayList<>();
    private final Set<String> checkedEntryIds = new HashSet<>();

    private String filterQuery = "";
    private boolean showEnrolled = true;
    private boolean showCancelled = true;
    private boolean showPending = true;

    private String statusSelected;
    private String statusConfirmed;
    private String statusCancelled;

    public InvitedEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setStatusConstants(String selected, String confirmed, String cancelled) {
        this.statusSelected = selected;
        this.statusConfirmed = confirmed;
        this.statusCancelled = cancelled;
    }

    public void setItems(@NonNull List<InvitedEntrantItem> items) {
        allItems.clear();
        allItems.addAll(items);
        checkedEntryIds.clear();
        applyFilter();
    }

    public void setFilterQuery(@NonNull String query) {
        filterQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        applyFilter();
    }

    public void setStatusFilters(boolean enrolled, boolean cancelled, boolean pending) {
        this.showEnrolled = enrolled;
        this.showCancelled = cancelled;
        this.showPending = pending;
        applyFilter();
    }

    private boolean passesStatusFilter(@NonNull InvitedEntrantItem item) {
        if (statusConfirmed != null && statusConfirmed.equals(item.firestoreStatus)) {
            return showEnrolled;
        }
        if (statusCancelled != null && statusCancelled.equals(item.firestoreStatus)) {
            return showCancelled;
        }
        if (statusSelected != null && statusSelected.equals(item.firestoreStatus)) {
            return showPending;
        }
        return true;
    }

    private void applyFilter() {
        visibleItems.clear();
        for (InvitedEntrantItem item : allItems) {
            if (!passesStatusFilter(item)) {
                continue;
            }
            if (filterQuery.isEmpty()) {
                visibleItems.add(item);
                continue;
            }
            String name = item.displayName != null ? item.displayName.toLowerCase(Locale.getDefault()) : "";
            if (name.contains(filterQuery)) {
                visibleItems.add(item);
            }
        }
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    @NonNull
    public List<InvitedEntrantItem> getCheckedItems() {
        List<InvitedEntrantItem> out = new ArrayList<>();
        for (InvitedEntrantItem item : allItems) {
            if (checkedEntryIds.contains(item.entryDocumentId)) {
                out.add(item);
            }
        }
        return out;
    }

    public void clearSelections() {
        checkedEntryIds.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    public void removeByEntryId(@NonNull String entryDocumentId) {
        InvitedEntrantItem toRemove = null;
        for (InvitedEntrantItem item : allItems) {
            if (entryDocumentId.equals(item.entryDocumentId)) {
                toRemove = item;
                break;
            }
        }
        if (toRemove != null) {
            allItems.remove(toRemove);
            checkedEntryIds.remove(entryDocumentId);
            applyFilter();
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invited_entrant_row, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        InvitedEntrantItem item = visibleItems.get(position);
        h.txtName.setText(item.displayName != null ? item.displayName : "");

        applyPill(h.txtPill, item);

        h.check.setOnCheckedChangeListener(null);
        h.check.setChecked(checkedEntryIds.contains(item.entryDocumentId));
        h.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkedEntryIds.add(item.entryDocumentId);
            } else {
                checkedEntryIds.remove(item.entryDocumentId);
            }
            listener.onSelectionChanged();
        });

        h.btnEye.setOnClickListener(v -> listener.onViewProfile(item));
    }

    private void applyPill(TextView pill, InvitedEntrantItem item) {
        if (statusConfirmed != null && statusConfirmed.equals(item.firestoreStatus)) {
            pill.setText(pill.getContext().getString(R.string.invited_status_enrolled));
            pill.setBackgroundResource(R.drawable.bg_pill_enrolled);
            pill.setTextColor(ContextCompat.getColor(pill.getContext(), R.color.text_white));
            return;
        }
        if (statusCancelled != null && statusCancelled.equals(item.firestoreStatus)) {
            pill.setText(pill.getContext().getString(R.string.invited_status_cancelled_list));
            pill.setBackgroundResource(R.drawable.bg_pill_cancelled_invited);
            pill.setTextColor(ContextCompat.getColor(pill.getContext(), R.color.status_closed_text));
            return;
        }
        if (statusSelected != null && statusSelected.equals(item.firestoreStatus)) {
            pill.setText(pill.getContext().getString(R.string.invited_status_pending));
            int ctxColor = ContextCompat.getColor(pill.getContext(), R.color.status_waiting_text);
            pill.setBackgroundResource(R.drawable.bg_pill_pending_invited);
            pill.setTextColor(ctxColor);
        }
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final AppCompatCheckBox check;
        final ImageButton btnEye;
        final TextView txtName;
        final TextView txtPill;

        Holder(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.checkSelect);
            btnEye = itemView.findViewById(R.id.btnViewEntrant);
            txtName = itemView.findViewById(R.id.txtEntrantName);
            txtPill = itemView.findViewById(R.id.txtStatusPill);
        }
    }

    public static final class InvitedEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;
        public final String firestoreStatus;

        public InvitedEntrantItem(String userId, String displayName, String entryDocumentId, String firestoreStatus) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
            this.firestoreStatus = firestoreStatus;
        }
    }
}
