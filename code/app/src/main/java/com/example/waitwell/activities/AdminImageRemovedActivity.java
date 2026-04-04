package com.example.waitwell.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.waitwell.R;

public class AdminImageRemovedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_image_deleted);

        String imageUrl = getIntent().getStringExtra("image_url");

        ImageView img = findViewById(R.id.imgDeleted);

        Glide.with(this)
                .load(imageUrl)
                .onlyRetrieveFromCache(true)
                .into(img);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }
}
