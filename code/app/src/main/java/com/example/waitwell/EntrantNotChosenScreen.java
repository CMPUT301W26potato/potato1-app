package com.example.waitwell;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class EntrantNotChosenScreen extends AppCompatActivity {
    //button goes back to the entrant notifications screen

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.not_chosen_notification);

        Button acceptButton = findViewById(R.id.back_button);

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EntrantNotChosenScreen.this, EntrantNotificationScreen.class);
                startActivity(intent);
            }
        });


    }
}
