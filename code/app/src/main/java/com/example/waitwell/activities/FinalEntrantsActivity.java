package com.example.waitwell.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.CsvExportHelper;
import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.ProfilePreviewHelper;
import com.example.waitwell.Profile;
import com.example.waitwell.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Organizer screen that lists confirmed entrants and lets the organizer export the list as CSV.
 * This is the screen-side piece for enrolled view + export.
 *
 * Addresses: US 02.06.03 - Organizer: View Enrolled Entrants, US 02.06.05 - Organizer: Export Enrolled List CSV
 *
 * @author Karina Zhang
 * @version 1.0
 * @see com.example.waitwell.CsvExportHelper
 */
public class FinalEntrantsActivity extends OrganizerBaseActivity implements FinalEntrantAdapter.Listener {
    /*
     * I used Gemini to get my head around writing to a CSV file in Android
     * and how FileProvider works when sharing files through an Intent. It
     * explained why getExternalFilesDir is the safe place to write and how
     * the share sheet picks up the URI from there.
     * just used it to understand the approach before writing it myself.
     *
     * Sites I looked at:
     *
     * Android FileProvider - sharing files with other apps without a crash:
     * https://developer.android.com/reference/androidx/core/content/FileProvider
     *
     * Writing CSV in Java - BufferedWriter and how to format the rows:
     * https://www.baeldung.com/java-csv
     *
     * Android share intent - how ACTION_SEND works with a file URI:
     * https://developer.android.com/training/sharing/send
     */

    public static final String EXTRA_EVENT_ID = "event_id";
    private static final int REQ_EXPORT_STORAGE = 7021;

    private String eventId;
    private String eventTitle;
    private FinalEntrantAdapter adapter;
    private final FirebaseFirestore db = FirebaseHelper.getInstance().getDb();
    private String statusConfirmed;
    private List<FinalEntrantAdapter.FinalEntrantItem> loadedEntrantItems = new ArrayList<>();

    /**
     * Sets up final entrants list, export button, and organizer nav actions.
     *
     * @param savedInstanceState restore bundle, can be null
     * @author Karina Zhang
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_entrants);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.no_event_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        statusConfirmed = getString(R.string.firestore_waitlist_status_confirmed);

        setupOrganizerDrawer();

        RecyclerView recycler = findViewById(R.id.recyclerFinalEntrants);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FinalEntrantAdapter(this);
        recycler.setAdapter(adapter);

        EditText editSearch = findViewById(R.id.editSearch);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setFilterQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AppCompatButton btnRemoveSelected = findViewById(R.id.btnRemoveSelected);
        btnRemoveSelected.setVisibility(android.view.View.GONE);

        AppCompatButton btnExportCsv = findViewById(R.id.btnExportCsv);
        btnExportCsv.setOnClickListener(v -> onExportCsvClicked());

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

        loadFinalEntrants();
    }

    /**
     * Handles export click and requests legacy storage permission if needed.
     *
     * @author Karina Zhang
     */
    private void onExportCsvClicked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_EXPORT_STORAGE);
                return;
            }
        }
        performExport();
    }

    /**
     * Starts export flow once prerequisites are satisfied.
     *
     * @author Karina Zhang
     */
    private void performExport() {
        refreshEventTitleIfNeeded(this::fetchUsersAndWriteCsv);
    }

    /**
     * Loads event title once if missing, then runs the callback.
     *
     * @param then callback after title is ready
     * @author Karina Zhang
     */
    private void refreshEventTitleIfNeeded(@NonNull Runnable then) {
        if (!TextUtils.isEmpty(eventTitle)) {
            then.run();
            return;
        }
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String t = doc.getString("title");
                        eventTitle = t != null ? t : "";
                    } else {
                        eventTitle = "";
                    }
                    runOnUiThread(then::run);
                })
                .addOnFailureListener(e -> {
                    eventTitle = "";
                    runOnUiThread(then::run);
                });
    }

    /**
     * Fetches user docs for loaded entrants and writes/shares the CSV file.
     *
     * @author Karina Zhang
     */
    private void fetchUsersAndWriteCsv() {
        if (loadedEntrantItems.isEmpty()) {
            File f = CsvExportHelper.generateFinalEntrantsCsv(
                    this, eventTitle != null ? eventTitle : "", new ArrayList<>());
            shareOrToast(f);
            return;
        }
        int n = loadedEntrantItems.size();
        final DocumentSnapshot[] userDocs = new DocumentSnapshot[n];
        AtomicInteger remaining = new AtomicInteger(n);
        for (int i = 0; i < n; i++) {
            final int idx = i;
            FinalEntrantAdapter.FinalEntrantItem item = loadedEntrantItems.get(i);
            FirebaseHelper.getInstance().fetchUserDocumentForWaitlistUserId(item.userId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    userDocs[idx] = task.getResult();
                }
                if (remaining.decrementAndGet() == 0) {
                    List<CsvExportHelper.FinalEntrant> list = new ArrayList<>();
                    for (int j = 0; j < n; j++) {
                        FinalEntrantAdapter.FinalEntrantItem itemJ = loadedEntrantItems.get(j);
                        DocumentSnapshot doc = userDocs[j];
                        if (doc != null) {
                            list.add(buildFinalEntrantFromDoc(doc, itemJ.displayName));
                        } else {
                            list.add(new CsvExportHelper.FinalEntrant(itemJ.displayName, "", "", "", "", ""));
                        }
                    }
                    runOnUiThread(() -> {
                        File f = CsvExportHelper.generateFinalEntrantsCsv(
                                this, eventTitle != null ? eventTitle : "", list);
                        shareOrToast(f);
                    });
                }
            });
        }
    }

    /**
     * Maps one user Firestore doc into CSV row model with fallback name.
     *
     * @param doc user document snapshot
     * @param fallbackName name used when doc has no name field
     * @return csv row model
     * @author Karina Zhang
     */
    private CsvExportHelper.FinalEntrant buildFinalEntrantFromDoc(
            @NonNull DocumentSnapshot doc, String fallbackName) {
        String name = doc.getString("name");
        if (TextUtils.isEmpty(name)) {
            name = fallbackName != null ? fallbackName : "";
        }
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        String role = doc.getString("role");
        String deviceId = doc.getString("deviceId");
        String createdAt = "";
        if (doc.getTimestamp("createdAt") != null) {
            Date d = doc.getTimestamp("createdAt").toDate();
            createdAt =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(d);
        }
        return new CsvExportHelper.FinalEntrant(
                name != null ? name : "",
                email != null ? email : "",
                phone != null ? phone : "",
                role != null ? role : "",
                deviceId != null ? deviceId : "",
                createdAt);
    }

    /**
     * Shares generated CSV file, or shows a toast if file creation failed.
     *
     * @param f generated file object
     * @author Karina Zhang
     */
    private void shareOrToast(File f) {
        if (f == null || !f.exists()) {
            Toast.makeText(this, R.string.export_failed_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setClipData(ClipData.newUri(getContentResolver(), "", uri));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_final_entrants_chooser_title)));
    }

    /**
     * Receives storage permission result and continues export when granted.
     *
     * @param requestCode permission request id
     * @param permissions requested permission names
     * @param grantResults grant results array
     * @author Karina Zhang
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_EXPORT_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performExport();
            } else {
                Toast.makeText(this, R.string.export_storage_permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Loads confirmed entrants for this event and resolves user display names.
     *
     * @author Karina Zhang
     */
    private void loadFinalEntrants() {
        FirebaseHelper.getInstance()
                .getEntriesByEventAndStatus(eventId, statusConfirmed)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        loadedEntrantItems = new ArrayList<>();
                        adapter.setItems(Collections.emptyList());
                        return;
                    }
                    int total = snapshot.size();
                    AtomicInteger done = new AtomicInteger(0);
                    List<FinalEntrantAdapter.FinalEntrantItem> buf =
                            Collections.synchronizedList(new ArrayList<>());

                    for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
                        String userId = entryDoc.getString("userId");
                        if (userId == null) {
                            if (done.incrementAndGet() == total) {
                                finishLoad(buf);
                            }
                            continue;
                        }
                        String entryDocId = entryDoc.getId();
                        FirebaseHelper.getInstance().fetchUserDocumentForWaitlistUserId(userId, task -> {
                            String name = getString(R.string.unknown_user);
                            if (task.isSuccessful() && task.getResult() != null
                                    && task.getResult().exists()) {
                                String n = task.getResult().getString("name");
                                if (!TextUtils.isEmpty(n)) {
                                    name = n;
                                }
                            }
                            buf.add(new FinalEntrantAdapter.FinalEntrantItem(userId, name, entryDocId));
                            if (done.incrementAndGet() == total) {
                                finishLoad(buf);
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.could_not_load_entrants, Toast.LENGTH_SHORT).show());
    }

    /**
     * Sorts loaded rows by name and pushes them into adapter and cached export list.
     *
     * @param buf unsorted rows collected from Firestore callbacks
     * @author Karina Zhang
     */
    private void finishLoad(List<FinalEntrantAdapter.FinalEntrantItem> buf) {
        List<FinalEntrantAdapter.FinalEntrantItem> sorted = new ArrayList<>(buf);
        Collections.sort(sorted, Comparator.comparing(a -> a.displayName != null ? a.displayName : ""));
        runOnUiThread(() -> {
            loadedEntrantItems = new ArrayList<>(sorted);
            adapter.setItems(sorted);
            String q = ((EditText) findViewById(R.id.editSearch)).getText().toString();
            adapter.setFilterQuery(q);
        });
    }

    /**
     * Opens profile preview for selected final entrant.
     *
     * @param item selected row
     * @author Karina Zhang
     */
    @Override
    public void onViewProfile(@NonNull FinalEntrantAdapter.FinalEntrantItem item) {
        ProfilePreviewHelper.showProfileDialog(this, item.userId);
    }
}

