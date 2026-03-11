package com.example.waitwell;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class EntrantChosenAccept extends AppCompatActivity {
    //set up the two buttons to accept or decline the chosen invitation

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chosen_notification);

        //take the event name and message from the notification
        String eventName = getIntent().getStringExtra("eventName");
        String message = getIntent().getStringExtra("message");

        TextView eventTitle = findViewById(R.id.eventTitle);
        eventTitle.setText(eventName);

        TextView messageText = findViewById(R.id.message);
        messageText.setText(message);


        Button acceptButton = findViewById(R.id.accept);

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do something when clicked

            }
        });

        Button declineButton = findViewById(R.id.decline);

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do something when clicked

            }
        });
    }

}
