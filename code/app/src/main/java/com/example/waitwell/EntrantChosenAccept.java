package com.example.waitwell;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.activities.InvitationResponseActivity;

/**
 * Legacy entry point for the invitation response flow; forwards to {@link InvitationResponseActivity}.
 */
public class EntrantChosenAccept extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent in = getIntent();
        Intent out = new Intent(this, InvitationResponseActivity.class);
        if (in != null && in.getExtras() != null) {
            out.putExtras(in.getExtras());
        }
        startActivity(out);
        finish();
    }
}
