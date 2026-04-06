package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.StorageReference;

/**
 * AdminImagesActivity allows admins to view and delete
 * all event images stored in Firebase Storage.
 * <p>
 * Images are displayed in a grid layout.
 * Admins can delete images with confirmation.
 *
 * @author Grace Shin
 */
public class AdminImagesActivity extends AppCompatActivity {

    RecyclerView recyclerImages;
    List<DocumentSnapshot> allImages;
    List<DocumentSnapshot> filteredImages;
    RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_images);

        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerImages.setLayoutManager(new GridLayoutManager(this, 2));

        loadImages();

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }

    /**
     * Loads all images from Firebase Storage.
     */
    private void loadImages() {

        List<String> imageUrls = new ArrayList<>();
        List<StorageReference> imageRefs = new ArrayList<>();

        recyclerImages.setAdapter(null);

        FirebaseStorage.getInstance()
                .getReference("event_images")
                .listAll()
                .addOnSuccessListener(listResult -> {

                    for (StorageReference fileRef : listResult.getItems()) {

                        fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {

                                    imageUrls.add(uri.toString());
                                    imageRefs.add(fileRef);

                                    // updates UI as images load
                                    setupAdapterFromUrls(imageUrls, imageRefs);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load images: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Sets up adapter using image URLs and references.
     */
    private void setupAdapterFromUrls(List<String> imageUrls, List<StorageReference> imageRefs) {

        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_admin_image, parent, false);
                return new RecyclerView.ViewHolder(view) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                String imageUrl = imageUrls.get(position);
                StorageReference fileRef = imageRefs.get(position);

                ImageView image = holder.itemView.findViewById(R.id.imgEvent);

                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .into(image);

                holder.itemView.findViewById(R.id.btnDelete)
                        .setOnClickListener(v -> showDeleteDialog(fileRef, imageUrl));
            }

            @Override
            public int getItemCount() {
                return imageUrls.size();
            }
        };

        recyclerImages.setAdapter(adapter);
    }

    /**
     * Shows confirmation dialog before deleting image.
     */
    private void showDeleteDialog(StorageReference fileRef, String imageUrl) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Yes", (d, w) -> deleteImage(fileRef, imageUrl))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes image from Firebase Storage.
     */
    private void deleteImage(StorageReference fileRef, String imageUrl) {

        fileRef.delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, AdminImageRemovedActivity.class);
                    intent.putExtra("image_url", imageUrl);
                    startActivity(intent);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // reload images when returning so it doesn't show old screen with deleted image still there
        loadImages();
    }


}
