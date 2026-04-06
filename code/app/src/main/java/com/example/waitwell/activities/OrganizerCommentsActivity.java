package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.waitwell.DeviceUtils;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * OrganizerCommentsActivity allows an organizer to view, post, and delete
 * comments for a specific event. Comments are stored in Firestore with
 * the organizer's userId and role. Each comment is displayed in a box
 * with a delete button for easy moderation.
 *
 * author: Sarang Kim
 */
public class OrganizerCommentsActivity extends OrganizerBaseActivity {

    private String eventId;
    private LinearLayout commentsContainer;
    private EditText editComment;
    private View btnPostComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_comments);

        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null) {
            finish();
            return;
        }
        editComment = findViewById(R.id.editComment);
        btnPostComment = findViewById(R.id.btnPostComment);

        btnPostComment.setOnClickListener(v -> postComment());

        commentsContainer = findViewById(R.id.commentsContainer);

        setupOrganizerDrawer();

        BottomNavigationView nav = findViewById(R.id.organizerBottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_organizer_bottom_back) {
                finish();
                return true;
            }
            if (id == R.id.nav_organizer_bottom_home) {
                Intent intent = new Intent(this, OrganizerEntryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
            return false;
        });

        loadComments();
    }

    /**
     * Loads comments for the current event from Firestore, orders
     * them by timestamp descending, and dynamically creates a view
     * for each comment. Organizer comments are labeled as such.
     */
    private void loadComments() {
        commentsContainer.removeAllViews();

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

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

                        TextView txt = new TextView(this);
                        txt.setTypeface(ResourcesCompat.getFont(this, R.font.poppins));
                        txt.setText((username != null ? username : "User") + ": " + text);

                        // shows organizer label
                        if ("organizer".equals(role)) {
                            txt.setText("[Organizer] " + username + ": " + text);
                        } else {
                            txt.setText((username != null ? username : "User") + ": " + text);
                        }

                        box.addView(txt);

                        // DELETE BUTTON
                        TextView deleteBtn = new TextView(this);
                        deleteBtn.setTypeface(ResourcesCompat.getFont(this, R.font.poppinsmedium));

                        deleteBtn.setText("Delete");
                        deleteBtn.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));

                        deleteBtn.setOnClickListener(v -> deleteComment(commentId));

                        box.addView(deleteBtn);

                        commentsContainer.addView(box);
                    }
                });
    }

    /**
     * Deletes a comment with the given ID from Firestore and refreshes
     * the comment list.
     *
     * @param commentId the Firestore document ID of the comment to delete
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
    private void postComment() {
        String text = editComment.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // get logged-in userId (same as Profile screen)
        String tempUserId = getSharedPreferences("WaitWellPrefs", MODE_PRIVATE)
                .getString("userId", null);

        if (tempUserId == null) {
            tempUserId = DeviceUtils.getDeviceId(this);
        }

        final String userId = tempUserId;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // get name from users collection
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String username = "Organizer";
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            username = name;
                        }
                    }

                    String commentId = db.collection("events")
                            .document(eventId)
                            .collection("comments")
                            .document()
                            .getId();

                    java.util.Map<String, Object> comment = new java.util.HashMap<>();
                    comment.put("text", text);
                    comment.put("userId", userId);
                    comment.put("username", username);
                    comment.put("timestamp", new java.util.Date());
                    comment.put("role", "organizer");

                    db.collection("events")
                            .document(eventId)
                            .collection("comments")
                            .document(commentId)
                            .set(comment)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show();
                                editComment.setText("");
                                loadComments();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show());
                });
    }
}