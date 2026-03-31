package com.example.waitwell.activities;



import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * AdminEventsActivity allows administrators to view
 * and manage all events stored in Firestore.
 *
 * Events are displayed in a RecyclerView grid layout.
 * Each card shows the event title and organizer name.
 *
 * Administrators can remove events from the system.
 * When an event is removed, it is deleted from Firestore
 * and a confirmation screen is displayed.
 *
 * Firestore data retrieval is handled through FirebaseHelper.
 *
 * AI Usage:
 *
 * @author Grace Shin
 */
public class AdminEventsActivity extends AppCompatActivity {

    RecyclerView recyclerEvents;
    /**
     * Initializes the activity and loads event data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_events);



        recyclerEvents = findViewById(R.id.recyclerEvents);
        // Display events in a 2 column grid
        recyclerEvents.setLayoutManager(new GridLayoutManager(this, 2));

        loadEvents();
        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
        });


    }

    /**
     * Retrieves all events from Firestore and populates the RecyclerView with event cards.
     */
    private void loadEvents() {

        FirebaseHelper.getInstance().getAllEvents()
                .addOnSuccessListener(snapshot -> {

                    List<DocumentSnapshot> events = snapshot.getDocuments();

                    // Adapter dynamically creates event cards
                    RecyclerView.Adapter adapter = new RecyclerView.Adapter() {

                        @Override
                        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                            View view = LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.item_admin_event, parent, false);

                            return new RecyclerView.ViewHolder(view) {};
                        }

                        @Override
                        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                            DocumentSnapshot doc = events.get(position);

                            String title = doc.getString("title");
                            String organizer = doc.getString("organizerName");

                            // Populate card UI elements
                            ((TextView) holder.itemView.findViewById(R.id.txtEventName)).setText(title);
                            ((TextView) holder.itemView.findViewById(R.id.txtOrganizer)).setText(organizer);

                            holder.itemView.findViewById(R.id.btnRemoveEvent)
                                    .setOnClickListener(v -> removeEvent(doc.getId()));

                            holder.itemView.findViewById(R.id.btnRemoveComments)
                                    .setOnClickListener(v -> {
                                        Intent intent = new Intent(v.getContext(), AdminCommentsActivity.class);
                                        v.getContext().startActivity(intent);
                                        intent.putExtra("event_id", doc.getId());
                                        startActivity(intent);
                                    });
                        }

                        @Override
                        public int getItemCount() {
                            return events.size();
                        }
                    };

                    recyclerEvents.setAdapter(adapter);

                });
    }

    /**
     * Deletes an event from Firestore.
     */
    private void removeEvent(String eventId) {

        FirebaseHelper.getInstance()
                .removeEvent(eventId)
                .addOnSuccessListener(unused -> {
                    // Navigate to confirmation screen
                    Intent intent = new Intent(
                            this,
                            AdminEventRemovedActivity.class
                    );

                    startActivity(intent);
                });
    }

}
