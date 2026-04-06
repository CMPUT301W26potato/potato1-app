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
 * Adapter for invited entrants with status pills, checkbox selection, and profile action.
 * This supports organizer selected-group notification flow.
 *
 * Addresses: US 02.05.01 - Organizer: Notify Chosen Entrants, US 02.07.02 - Organizer: Notify All Selected
 *
 * @author Karina Zhang
 * @version 1.0
 * @see InvitedEntrantsActivity
 */
public class InvitedEntrantAdapter extends RecyclerView.Adapter<InvitedEntrantAdapter.Holder> {

    /**
     * Callback contract for row clicks and checkbox state changes.
     *
     * Addresses: US 02.05.01 - Organizer: Notify Chosen Entrants, US 02.07.02 - Organizer: Notify All Selected
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public interface Listener {
        /**
         * Opens profile preview for one invited row.
         *
         * @param item selected row item
         * @author Karina Zhang
         */
        void onViewProfile(@NonNull InvitedEntrantItem item);

        /**
         * Notifies activity when checkbox selection set changed.
         *
         * @author Karina Zhang
         */
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

    /**
     * Creates adapter with listener target.
     *
     * @param listener callback receiver
     * @author Karina Zhang
     */
    public InvitedEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    /**
     * Sets the status constants used to build pills and filters.
     *
     * @param selected selected status value
     * @param confirmed confirmed status value
     * @param cancelled cancelled status value
     * @author Karina Zhang
     */
    public void setStatusConstants(String selected, String confirmed, String cancelled) {
        this.statusSelected = selected;
        this.statusConfirmed = confirmed;
        this.statusCancelled = cancelled;
    }

    /**
     * Replaces current rows and clears checked selections.
     *
     * @param items row models to display
     * @author Karina Zhang
     */
    public void setItems(@NonNull List<InvitedEntrantItem> items) {
        allItems.clear();
        allItems.addAll(items);
        checkedEntryIds.clear();
        applyFilter();
    }

    /**
     * Updates search filter text and refreshes visible list.
     *
     * @param query search text
     * @author Karina Zhang
     */
    public void setFilterQuery(@NonNull String query) {
        filterQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        applyFilter();
    }

    /**
     * Updates status toggles and reapplies filter.
     *
     * @param enrolled show enrolled rows
     * @param cancelled show cancelled rows
     * @param pending show pending rows
     * @author Karina Zhang
     */
    public void setStatusFilters(boolean enrolled, boolean cancelled, boolean pending) {
        this.showEnrolled = enrolled;
        this.showCancelled = cancelled;
        this.showPending = pending;
        applyFilter();
    }

    /**
     * Checks if a row should stay visible under current status toggles.
     *
     * @param item row model to test
     * @return true when row passes selected status filters
     * @author Karina Zhang
     */
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

    /**
     * Rebuilds visible list using status + text filters.
     *
     * @author Karina Zhang
     */
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

    /**
     * Returns all currently checked rows.
     *
     * @return checked row list
     * @author Karina Zhang
     */
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

    /**
     * Alias used by activity for checked rows.
     *
     * @return checked entrant rows
     * @author Karina Zhang
     */
    @NonNull
    public List<InvitedEntrantItem> getSelectedEntrants() {
        return getCheckedItems();
    }

    /**
     * Clears all checkbox selections and refreshes rows.
     *
     * @author Karina Zhang
     */
    public void clearSelections() {
        checkedEntryIds.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    /**
     * Removes one row by waitlist entry id.
     *
     * @param entryDocumentId target entry id
     * @author Karina Zhang
     */
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

    /**
     * Inflates one invited entrant row.
     *
     * @param parent recycler parent
     * @param viewType row type
     * @return holder for row
     * @author Karina Zhang
     */
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invited_entrant_row, parent, false);
        return new Holder(v);
    }

    /**
     * Binds row text, pill style, checkbox state, and click listeners.
     *
     * @param h row holder
     * @param position row index
     * @author Karina Zhang
     */
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

    /**
     * Applies the right status pill look for one row.
     *
     * @param pill status text view
     * @param item row model
     * @author Karina Zhang
     */
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

    /**
     * Returns count of currently visible rows.
     *
     * @return row count
     * @author Karina Zhang
     */
    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    /**
     * Holds views for one invited entrant row.
     *
     * Addresses: US 02.07.02 - Organizer: Notify All Selected
     *
     * @author Karina Zhang
     * @version 1.0
     */
    static final class Holder extends RecyclerView.ViewHolder {
        final AppCompatCheckBox check;
        final ImageButton btnEye;
        final TextView txtName;
        final TextView txtPill;

        /**
         * Maps row ids into holder fields.
         *
         * @param itemView row root
         * @author Karina Zhang
         */
        Holder(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.checkSelect);
            btnEye = itemView.findViewById(R.id.btnViewEntrant);
            txtName = itemView.findViewById(R.id.txtEntrantName);
            txtPill = itemView.findViewById(R.id.txtStatusPill);
        }
    }

    /**
     * Row model for invited entrants list.
     *
     * Addresses: US 02.05.01 - Organizer: Notify Chosen Entrants, US 02.07.02 - Organizer: Notify All Selected
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public static final class InvitedEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;
        public final String firestoreStatus;

        /**
         * Creates one invited entrant row model.
         *
         * @param userId entrant id
         * @param displayName entrant display name
         * @param entryDocumentId waitlist entry doc id
         * @param firestoreStatus status from waitlist entry
         * @author Karina Zhang
         */
        public InvitedEntrantItem(String userId, String displayName, String entryDocumentId, String firestoreStatus) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
            this.firestoreStatus = firestoreStatus;
        }
    }
}
