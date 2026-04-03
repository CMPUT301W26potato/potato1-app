// REHAAN'S ADDITION — US 02.02.02
/**
 * WaitlistMapActivity.java
 * Shows a Google Map with one marker per waitlist entrant,
 * plotted at the location where they joined the waitlist (US 02.02.02).
 * Only entrants whose waitlist_entries document has joinLatitude and
 * joinLongitude fields appear on the map.
 * Javadoc written with help from Claude (claude.ai)
 */
package com.example.waitwell.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WaitlistMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "WaitlistMapActivity";
    public static final String EXTRA_EVENT_ID = "event_id";

    private MapView mapView;
    private GoogleMap googleMap;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist_map);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        loadEntrantLocations();
    }

    private void loadEntrantLocations() {
        FirebaseHelper.getInstance().getEntriesWithLocationByEvent(eventId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, R.string.waitlist_map_no_locations, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<LatLng> points = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Double lat = doc.getDouble("joinLatitude");
                        Double lng = doc.getDouble("joinLongitude");
                        if (lat == null || lng == null) continue;

                        String userId = doc.getString("userId");
                        String label = userId != null ? userId : getString(R.string.unknown_user);

                        LatLng position = new LatLng(lat, lng);
                        googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(label));
                        points.add(position);
                    }

                    if (points.isEmpty()) {
                        Toast.makeText(this, R.string.waitlist_map_no_locations, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (points.size() == 1) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 12f));
                        return;
                    }

                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    for (LatLng p : points) {
                        boundsBuilder.include(p);
                    }
                    LatLngBounds bounds = boundsBuilder.build();
                    int padding = 120;
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load entrant locations", e);
                    Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show();
                });
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
// END REHAAN'S ADDITION