package com.example.waitwell.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.waitwell.R;

/**
 * AdminImageRemovedActivity shows a confirmation screen
 * after an image has been deleted.
 *
 * It displays the deleted image (if cached locally)
 * and allows the admin to go back.
 *
 * @author Grace Shin
 */
public class AdminImageRemovedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_image_deleted);
        // get image URL passed from previous screen to show in confirmation page
        String imageUrl = getIntent().getStringExtra("image_url");

        ImageView img = findViewById(R.id.imgDeleted);

        Glide.with(this)
                .load(imageUrl)
                .onlyRetrieveFromCache(true)
                .into(img);
        // back button
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }
}
