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
 * Adapter for the organizer final-entrants list with simple name filtering and profile view action.
 * It is used by the enrolled/final entrants story.
 *
 * Addresses: US 02.06.03 - Organizer: View Enrolled Entrants
 *
 * @author Karina Zhang
 * @version 1.0
 * @see FinalEntrantsActivity
 */
public class FinalEntrantAdapter extends RecyclerView.Adapter<FinalEntrantAdapter.Holder> {
    /*
     * I used Gemini to get my head around writing to a CSV file in Android
     * and how FileProvider works when sharing files through an Intent. It
     * explained why getExternalFilesDir is the safe place to write and how
     * the share sheet picks up the URI from there.
     * just used it to understand the approach before writing it myself.
     *
     * Sites I looked at:
     *
     * Android FileProvider - sharing files with other apps without a crash:
     * https://developer.android.com/reference/androidx/core/content/FileProvider
     *
     * Writing CSV in Java - BufferedWriter and how to format the rows:
     * https://www.baeldung.com/java-csv
     *
     * Android share intent - how ACTION_SEND works with a file URI:
     * https://developer.android.com/training/sharing/send
     */
    /**
     * Callback for row click actions.
     *
     * Addresses: US 02.06.03 - Organizer: View Enrolled Entrants
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public interface Listener {
        /**
         * Opens profile for selected entrant row.
         *
         * @param item selected row model
         * @author Karina Zhang
         */
        void onViewProfile(@NonNull FinalEntrantItem item);
    }

    private final Listener listener;
    private final List<FinalEntrantItem> allItems = new ArrayList<>();
    private final List<FinalEntrantItem> visibleItems = new ArrayList<>();
    private String filterQuery = "";

    /**
     * Creates adapter with callback target.
     *
     * @param listener parent callback
     * @author Karina Zhang
     */
    public FinalEntrantAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    /**
     * Replaces row list and reapplies active filter.
     *
     * @param items new row models
     * @author Karina Zhang
     */
    public void setItems(@NonNull List<FinalEntrantItem> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    /**
     * Updates filter text used on display names.
     *
     * @param query search text
     * @author Karina Zhang
     */
    public void setFilterQuery(@NonNull String query) {
        filterQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        applyFilter();
    }

    /**
     * Rebuilds visible rows from current filter.
     *
     * @author Karina Zhang
     */
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
    /**
     * Inflates one final-entrant row.
     *
     * @param parent recycler parent
     * @param viewType row type
     * @return holder for row
     * @author Karina Zhang
     */
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_final_entrant, parent, false);
        return new Holder(v);
    }

    @Override
    /**
     * Binds one row model into holder views.
     *
     * @param h holder to bind
     * @param position row index
     * @author Karina Zhang
     */
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
    /**
     * Returns count of currently visible rows.
     *
     * @return row count
     * @author Karina Zhang
     */
    public int getItemCount() {
        return visibleItems.size();
    }

    /**
     * Holds row views for final entrants.
     *
     * Addresses: US 02.06.03 - Organizer: View Enrolled Entrants
     *
     * @author Karina Zhang
     * @version 1.0
     */
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final ImageButton btnEye;

        /**
         * Maps row view ids into holder fields.
         *
         * @param itemView row root
         * @author Karina Zhang
         */
        Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtEntrantName);
            btnEye = itemView.findViewById(R.id.btnViewEntrant);
        }
    }

    /**
     * Row model for final entrants adapter.
     *
     * Addresses: US 02.06.03 - Organizer: View Enrolled Entrants
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public static final class FinalEntrantItem {
        public final String userId;
        public final String displayName;
        public final String entryDocumentId;

        /**
         * Creates one row model.
         *
         * @param userId entrant id
         * @param displayName entrant display name
         * @param entryDocumentId waitlist entry doc id
         * @author Karina Zhang
         */
        public FinalEntrantItem(String userId, String displayName, String entryDocumentId) {
            this.userId = userId;
            this.displayName = displayName;
            this.entryDocumentId = entryDocumentId;
        }
    }
}

