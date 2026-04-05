package com.example.waitwell.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.waitwell.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.waitwell.R;

/**
 * AdminCommentsActivity allows an admin to view and manage comments
 * for a specific event. Comments are loaded from Firestore and displayed
 * in a vertical list. Admins can delete individual comments.
 *
 * author: Sarang Kim
 */
public class AdminCommentsActivity extends AppCompatActivity {

    // The ID of the event for which comments are displayed
    private String eventId;

    // Container for dynamically added comment views
    private LinearLayout commentsContainer;

    // TextView shown when there are no comments
    private TextView txtEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_comments);

        // get the event ID from the intent extras
        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null) {
            finish(); // close activity if no event ID provided
            return;
        }

        // initialize views
        commentsContainer = findViewById(R.id.commentsContainer);
        txtEmpty = findViewById(R.id.txtNoComments);

        // setup back button listener
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // load comments for the event
        loadComments();
    }

    /**
     * Loads comments for the current event from Firestore and dynamically
     * creates a view for each comment. If no comments exist, shows a message.
     */
    private void loadComments() {
        commentsContainer.removeAllViews(); // clear existing views

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        return;
                    } else {
                        txtEmpty.setVisibility(View.GONE);
                    }

                    // create a view for each comment
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String text = doc.getString("text");
                        String username = doc.getString("username");
                        String role = doc.getString("role");
                        String commentId = doc.getId();

                        if (text == null) continue;

                        LinearLayout box = new LinearLayout(this);
                        box.setOrientation(LinearLayout.VERTICAL);
                        box.setPadding(16,16,16,16);
                        box.setBackgroundResource(R.drawable.bg_comment_box);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(0,8,0,8);
                        box.setLayoutParams(params);

                        // comment text
                        TextView txt = new TextView(this);
                        txt.setTypeface(getResources().getFont(R.font.poppins));
                        txt.setText((username != null ? username : "User") + ": " + text);
                        box.addView(txt);

                        // delete button
                        TextView deleteBtn = new TextView(this);
                        deleteBtn.setTypeface(getResources().getFont(R.font.poppinsmedium));
                        deleteBtn.setText("Delete");
                        deleteBtn.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                        deleteBtn.setOnClickListener(v -> deleteComment(commentId));
                        box.addView(deleteBtn);

                        commentsContainer.addView(box);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load comments: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Deletes a comment with the given ID from Firestore and reloads
     * the comment list.
     *
     * @param commentId the ID of the comment to delete
     */
    private void deleteComment(String commentId) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadComments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
    }
}