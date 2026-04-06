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
 * Adapter for organizer waitlist request rows, with accept/decline/profile actions.
 * Used in the view-requests screen and notification flow.
 *
 * Addresses: US 02.02.01 - Organizer: View Waitlist Entrants, US 02.05.01 - Organizer: Notify Chosen Entrants
 *
 * @author Karina Zhang
 * @version 1.0
 * @see ViewRequestsActivity
 */
public class WaitlistEntrantAdapter extends RecyclerView.Adapter<WaitlistEntrantAdapter.Holder> {
    /*
     * Asked Gemini how to structure notification documents in Firestore so
     * the entrant side can read them and figure out what type they are. It
     * helped me think through what fields to include and how to trigger the
     * write at the right point in the flow.
     * getting the concept down.
     *
     * Sites I looked at:
     *
     * Firestore - writing documents to a collection:
     * https://firebase.google.com/docs/firestore/manage-data/add-data
     *
     * Firestore real-time listeners - snapshot listeners for live updates:
     * https://firebase.google.com/docs/firestore/query-data/listen
     */

    /**
     * Row action callbacks consumed by the parent activity.
     *
     * Addresses: US 02.02.01 - Organizer: View Waitlist Entrants, US 02.05.01 - Organizer: Notify Chosen Entrants
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public interface Listener {
        /**
         * Opens profile preview for one waitlist row.
         *
         * @param item selected row
         * @author Karina Zhang
         */
        void onViewProfile(@NonNull WaitlistEntrantItem item);

        /**
         * Accepts one waiting entrant.
         *
         * @param item selected row
         * @author Karina Zhang
         */
        void onAccept(@NonNull WaitlistEntrantItem item);

        /**
         * Declines one waiting entrant.
         *
         * @param item selected row
         * @author Karina Zhang
         */
        void onDecline(@NonNull WaitlistEntrantItem item);
    }

    private final Listener listener;
    private final List<WaitlistEntrantItem> allItems = new ArrayList<>();
    private final List<WaitlistEntrantItem> visibleItems = new ArrayList<>();
    private String filterQuery = "";

    /**
     * Creates adapter with activity callback target.
     *
     * @param listener callback receiver for row actions
     * @author Karina Zhang
     */
    public WaitlistEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    /**
     * Replaces all row items and reapplies current filter text.
     *
     * @param items rows to show
     * @author Karina Zhang
     */
    public void setItems(@NonNull List<WaitlistEntrantItem> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    /**
     * Updates filter text used against display names.
     *
     * @param query filter text from search box
     * @author Karina Zhang
     */
    public void setFilterQuery(@NonNull String query) {
        filterQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        applyFilter();
    }

    /**
     * Applies name filter and refreshes visible rows.
     *
     * @author Karina Zhang
     */
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

    /**
     * Returns a copy of currently visible rows.
     *
     * @return visible row snapshot
     * @author Karina Zhang
     */
    @NonNull
    public List<WaitlistEntrantItem> getVisibleItemsSnapshot() {
        return new ArrayList<>(visibleItems);
    }

    /**
     * Inflates one waitlist row.
     *
     * @param parent recycler parent
     * @param viewType row type
     * @return row holder
     * @author Karina Zhang
     */
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waitlist_entrant_request, parent, false);
        return new Holder(v);
    }

    /**
     * Binds row values and action buttons.
     *
     * @param h holder to bind
     * @param position row position
     * @author Karina Zhang
     */
    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        WaitlistEntrantItem item = visibleItems.get(position);
        h.txtName.setText(item.displayName != null ? item.displayName : "");
        h.btnEye.setOnClickListener(v -> listener.onViewProfile(item));
        h.btnAccept.setOnClickListener(v -> listener.onAccept(item));
        h.btnDecline.setOnClickListener(v -> listener.onDecline(item));
    }

    /**
     * Returns count of filtered rows.
     *
     * @return visible row count
     * @author Karina Zhang
     */
    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    /**
     * Holds views for one waitlist row.
     *
     * Addresses: US 02.02.01 - Organizer: View Waitlist Entrants
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
         * Maps row layout ids into fields.
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
     * Row model with user id, display name, and waitlist entry doc id.
     *
     * Addresses: US 02.02.01 - Organizer: View Waitlist Entrants
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public static final class WaitlistEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;

        /**
         * Creates one waitlist row model.
         *
         * @param userId entrant id
         * @param displayName entrant display name
         * @param entryDocumentId waitlist entry doc id
         * @author Karina Zhang
         */
        public WaitlistEntrantItem(String userId, String displayName, String entryDocumentId) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
        }
    }
}

