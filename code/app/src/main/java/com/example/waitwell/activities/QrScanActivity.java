package com.example.waitwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.FirebaseHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * QR code scanner activity (US 01.06.01).
 *
 * Launches the ZXing camera scanner immediately on creation. When the entrant
 * scans a promotional QR code, the encoded event ID is looked up in Firestore
 * and EventDetailActivity is opened so they can view details and join the waitlist.
 */
public class QrScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan event QR code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String scannedText = result.getContents();
            if (scannedText == null) {
                finish();
                return;
            }
            openEventDetail(scannedText.trim());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void openEventDetail(String eventId) {
        FirebaseHelper.getInstance().getEvent(eventId)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Intent intent = new Intent(this, EventDetailActivity.class);
                        intent.putExtra("event_id", eventId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not verify event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
