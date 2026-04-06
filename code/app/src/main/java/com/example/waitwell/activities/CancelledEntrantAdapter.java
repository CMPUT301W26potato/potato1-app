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
 * Adapter for the organizer cancelled-entrant list so we can filter names and keep the row
 * layout consistent with other entrant lists. This is part of the notify-cancelled flow.
 *
 * Addresses: US 02.07.03 - Organizer: Notify All Cancelled
 *
 * @author Karina Zhang
 * @version 1.0
 * @see CancelledEntrantsActivity
 */
public class CancelledEntrantAdapter extends RecyclerView.Adapter<CancelledEntrantAdapter.Holder> {

    /**
     * Click callbacks from each cancelled-entrant row.
     *
     * Addresses: US 02.07.03 - Organizer: Notify All Cancelled
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public interface Listener {
        /**
         * Opens the profile preview for a cancelled entrant.
         *
         * @param item row item with user id and display name
         * @author Karina Zhang
         */
        void onViewProfile(@NonNull CancelledEntrantItem item);
    }

    private final Listener listener;
    private final List<CancelledEntrantItem> allItems = new ArrayList<>();
    private final List<CancelledEntrantItem> visibleItems = new ArrayList<>();
    private String filterQuery = "";

    /**
     * Creates the adapter with the activity callback target.
     *
     * @param listener callback receiver for row actions
     * @author Karina Zhang
     */
    public CancelledEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the backing list and reapplies the current search text.
     *
     * @param items new cancelled entrants from Firestore
     * @author Karina Zhang
     */
    public void setItems(@NonNull List<CancelledEntrantItem> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    /**
     * Updates the name filter used on cancelled entrants.
     *
     * @param query search text from the UI
     * @author Karina Zhang
     */
    public void setFilterQuery(@NonNull String query) {
        filterQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        applyFilter();
    }

    /**
     * Rebuilds the visible list based on the current filter string.
     *
     * @author Karina Zhang
     */
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

    /**
     * Returns a copy of currently visible rows for batch notification actions.
     *
     * @return copy of visible cancelled entrants
     * @author Karina Zhang
     */
    @NonNull
    public List<CancelledEntrantItem> getVisibleItemsSnapshot() {
        return new ArrayList<>(visibleItems);
    }

    @NonNull
    @Override
    /**
     * Inflates one cancelled-entrant row view.
     *
     * @param parent recycler parent view group
     * @param viewType adapter row type
     * @return holder bound to the cancelled row layout
     * @author Karina Zhang
     */
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waitlist_entrant_request, parent, false);
        return new Holder(v);
    }

    @Override
    /**
     * Binds row text and hides accept/decline buttons for cancelled rows.
     *
     * @param h row holder to bind
     * @param position adapter position
     * @author Karina Zhang
     */
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CancelledEntrantItem item = visibleItems.get(position);
        h.txtName.setText(item.displayName != null ? item.displayName : "");
        h.btnEye.setOnClickListener(v -> listener.onViewProfile(item));
        h.btnAccept.setVisibility(View.GONE);
        h.btnDecline.setVisibility(View.GONE);
    }

    @Override
    /**
     * Returns visible row count.
     *
     * @return number of filtered rows
     * @author Karina Zhang
     */
    public int getItemCount() {
        return visibleItems.size();
    }

    /**
     * Holds row views for the cancelled entrant list.
     *
     * Addresses: US 02.07.03 - Organizer: Notify All Cancelled
     *
     * @author Karina Zhang
     * @version 1.0
     */
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final ImageButton btnEye;
        final ImageButton btnAccept;
        final ImageButton btnDecline;

        /**
         * Maps layout ids to holder fields.
         *
         * @param itemView row root view
         * @author Karina Zhang
         */
        Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtEntrantName);
            btnEye = itemView.findViewById(R.id.btnViewEntrant);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    /**
     * Lightweight row model used by the cancelled entrants adapter.
     *
     * Addresses: US 02.07.03 - Organizer: Notify All Cancelled
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public static final class CancelledEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;

        /**
         * Builds one cancelled entrant row model.
         *
         * @param userId entrant device id used in waitlist docs
         * @param displayName display name shown in list
         * @param entryDocumentId waitlist entry Firestore doc id
         * @author Karina Zhang
         */
        public CancelledEntrantItem(String userId, String displayName, String entryDocumentId) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
        }
    }
}
