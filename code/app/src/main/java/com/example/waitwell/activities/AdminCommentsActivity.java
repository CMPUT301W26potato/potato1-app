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

public class AdminCommentsActivity extends AppCompatActivity {

    private String eventId;
    private LinearLayout commentsContainer;
    private TextView txtEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_comments);

        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null) {
            finish();
            return;
        }

        commentsContainer = findViewById(R.id.commentsContainer);
        txtEmpty = findViewById(R.id.txtNoComments);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadComments();
    }

    private void loadComments() {
        commentsContainer.removeAllViews();

        FirebaseHelper.getInstance().getDb()
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

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String text = doc.getString("text");
                        String username = doc.getString("username");
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
                        txt.setTypeface(getResources().getFont(R.font.poppins));
                        txt.setText((username != null ? username : "User") + ": " + text);
                        box.addView(txt);

                        TextView deleteBtn = new TextView(this);
                        deleteBtn.setTypeface(getResources().getFont(R.font.poppinsmedium));
                        deleteBtn.setText("Delete");
                        deleteBtn.setTextColor(
                                ContextCompat.getColor(this, android.R.color.holo_red_dark)
                        );

                        deleteBtn.setOnClickListener(v -> deleteComment(commentId));

                        box.addView(deleteBtn);

                        commentsContainer.addView(box);
                    }
                });
    }

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
