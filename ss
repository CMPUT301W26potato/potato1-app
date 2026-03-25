[33mcommit a1758845cc2bacf0c53454774bf178314444a6e5[m[33m ([m[1;36mHEAD[m[33m -> [m[1;32mkarina-features[m[33m, [m[1;31morigin/karina-features[m[33m)[m
Author: Karina-Zhangg <xz30@ualberta.ca>
Date:   Wed Mar 25 16:37:17 2026 -0600

    I just fixed the following bugs from TA feedback:
    
    1. when you are an entrant and go to waitlist, able to rejoin the waitlist of the event you've already accepted/signed up for
    
    2. cannot make the start date as before the current date
    
    And then I added:
    
    1. Calendar view for events on entrant home page (i can view events by calendar, click on a date and see what events are happening that day, and clicking on that event leads to the join waitlist page, can filter by year and month)
    
    2. refined Organiser event creation (added event date and time on top of event registration date + event registration deadline)
    
    3. changed the GUI for entrant viewing event details to include viewing the event date and time, also viewing how many more days I can register until deadline. It's kinda chopped though im bad at colour theory

[1mdiff --git a/code/app/src/main/AndroidManifest.xml b/code/app/src/main/AndroidManifest.xml[m
[1mindex 559227a..273a5f8 100644[m
[1m--- a/code/app/src/main/AndroidManifest.xml[m
[1m+++ b/code/app/src/main/AndroidManifest.xml[m
[36m@@ -62,6 +62,9 @@[m
 [m
         <activity android:name=".activities.WaitListActivity"[m
             android:exported="false" />[m
[32m+[m[32m        <activity[m
[32m+[m[32m            android:name=".activities.EntrantCalendarActivity"[m
[32m+[m[32m            android:exported="false" />[m
 [m
 [m
         <!-- Organizer-only entry: no Entrant/Admin UI -->[m
[1mdiff --git a/code/app/src/main/java/com/example/waitwell/EventStatusUtils.java b/code/app/src/main/java/com/example/waitwell/EventStatusUtils.java[m
[1mnew file mode 100644[m
[1mindex 0000000..d8974c6[m
[1m--- /dev/null[m
[1m+++ b/code/app/src/main/java/com/example/waitwell/EventStatusUtils.java[m
[36m@@ -0,0 +1,103 @@[m
[32m+[m[32mpackage com.example.waitwell;[m
[32m+[m
[32m+[m[32mimport com.google.firebase.firestore.DocumentSnapshot;[m
[32m+[m
[32m+[m[32mimport java.util.Calendar;[m
[32m+[m[32mimport java.util.Date;[m
[32m+[m
[32m+[m[32m/**[m
[32m+[m[32m * Derives event lifecycle status from dates (not only the stored {@code status} field).[m
[32m+[m[32m * <ul>[m
[32m+[m[32m *   <li><b>Open</b> – registration deadline has not passed yet.</li>[m
[32m+[m[32m *   <li><b>Closed</b> – deadline has passed, but the event day has not passed yet.</li>[m
[32m+[m[32m *   <li><b>Completed</b> – calendar day is after the event date.</li>[m
[32m+[m[32m * </ul>[m
[32m+[m[32m */[m
[32m+[m[32mpublic final class EventStatusUtils {[m
[32m+[m
[32m+[m[32m    private EventStatusUtils() {[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public static String computeStatus(DocumentSnapshot doc) {[m
[32m+[m[32m        if (doc == null || !doc.exists()) {[m
[32m+[m[32m            return "closed";[m
[32m+[m[32m        }[m
[32m+[m[32m        return computeStatus([m
[32m+[m[32m                doc.getDate("eventDate"),[m
[32m+[m[32m                doc.getDate("registrationClose"),[m
[32m+[m[32m                new Date());[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /**[m
[32m+[m[32m     * @param eventDate        when the event happens (organizer "Date of event"); may be null for legacy docs[m
[32m+[m[32m     * @param registrationClose end of registration; may be null[m
[32m+[m[32m     * @param now              typically {@code new Date()}[m
[32m+[m[32m     */[m
[32m+[m[32m    public static String computeStatus(Date eventDate, Date registrationClose, Date now) {[m
[32m+[m[32m        if (eventDate != null && isCalendarDayAfter(now, eventDate)) {[m
[32m+[m[32m            return "completed";[m
[32m+[m[32m        }[m
[32m+[m[32m        if (registrationClose != null && registrationClose.before(now)) {[m
[32m+[m[32m            return "closed";[m
[32m+[m[32m        }[m
[32m+[m[32m        return "open";[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /**[m
[32m+[m[32m     * True if {@code now} falls on a calendar day strictly after {@code eventDate}'s day.[m
[32m+[m[32m     */[m
[32m+[m[32m    public static boolean isCalendarDayAfter(Date now, Date eventDate) {[m
[32m+[m[32m        Calendar eventCal = Calendar.getInstance();[m
[32m+[m[32m        eventCal.setTime(eventDate);[m
[32m+[m[32m        Calendar nowCal = Calendar.getInstance();[m
[32m+[m[32m        nowCal.setTime(now);[m
[32m+[m[32m        int ey = eventCal.get(Calendar.YEAR);[m
[32m+[m[32m        int em = eventCal.get(Calendar.MONTH);[m
[32m+[m[32m        int ed = eventCal.get(Calendar.DAY_OF_MONTH);[m
[32m+[m[32m        int ny = nowCal.get(Calendar.YEAR);[m
[32m+[m[32m        int nm = nowCal.get(Calendar.MONTH);[m
[32m+[m[32m        int nd = nowCal.get(Calendar.DAY_OF_MONTH);[m
[32m+[m[32m        if (ny != ey) {[m
[32m+[m[32m            return ny > ey;[m
[32m+[m[32m        }[m
[32m+[m[32m        if (nm != em) {[m
[32m+[m[32m            return nm > em;[m
[32m+[m[32m        }[m
[32m+[m[32m        return nd > ed;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /**[m
[32m+[m[32m     * True if {@code a}'s calendar day is strictly before {@code b}'s calendar day.[m
[32m+[m[32m     */[m
[32m+[m[32m    public static boolean isCalendarDayBefore(Date a, Date b) {[m
[32m+[m[32m        Calendar ca = Calendar.getInstance();[m
[32m+[m[32m        ca.setTime(a);[m
[32m+[m[32m        Calendar cb = Calendar.getInstance();[m
[32m+[m[32m        cb.setTime(b);[m
[32m+[m[32m        int ay = ca.get(Calendar.YEAR), am = ca.get(Calendar.MONTH), ad = ca.get(Calendar.DAY_OF_MONTH);[m
[32m+[m[32m        int by = cb.get(Calendar.YEAR), bm = cb.get(Calendar.MONTH), bd = cb.get(Calendar.DAY_OF_MONTH);[m
[32m+[m[32m        if (ay != by) {[m
[32m+[m[32m            return ay < by;[m
[32m+[m[32m        }[m
[32m+[m[32m        if (am != bm) {[m
[32m+[m[32m            return am < bm;[m
[32m+[m[32m        }[m
[32m+[m[32m        return ad < bd;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /** Local calendar start of today (00:00:00). */[m
[32m+[m[32m    public static Date startOfToday() {[m
[32m+[m[32m        return startOfDay(new Date());[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /** Local midnight on the same calendar day as {@code d}. */[m
[32m+[m[32m    public static Date startOfDay(Date d) {[m
[32m+[m[32m        Calendar c = Calendar.getInstance();[m
[32m+[m[32m        c.setTime(d);[m
[32m+[m[32m        c.set(Calendar.HOUR_OF_DAY, 0);[m
[32m+[m[32m        c.set(Calendar.MINUTE, 0);[m
[32m+[m[32m        c.set(Calendar.SECOND, 0);[m
[32m+[m[32m        c.set(Calendar.MILLISECOND, 0);[m
[32m+[m[32m        return c.getTime();[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
[1mdiff --git a/code/app/src/main/java/com/example/waitwell/activities/AllEventsActivity.java b/code/app/src/main/java/com/example/waitwell/activities/AllEventsActivity.java[m
[1mindex cd20ccc..5ae57d7 100644[m
[1m--- a/code/app/src/main/java/com/example/waitwell/activities/AllEventsActivity.java[m
[1m+++ b/code/app/src/main/java/com/example/waitwell/activities/AllEventsActivity.java[m
[36m@@ -16,6 +16,7 @@[m [mimport android.widget.Toast;[m
 import androidx.appcompat.app.AppCompatActivity;[m
 import androidx.appcompat.app.AlertDialog;[m
 [m
[32m+[m[32mimport com.example.waitwell.EventStatusUtils;[m
 import com.example.waitwell.FirebaseHelper;[m
 import com.example.waitwell.R;[m
 import com.google.android.material.bottomnavigation.BottomNavigationView;[m
[36m@@ -222,11 +223,9 @@[m [mpublic class AllEventsActivity extends AppCompatActivity {[m
         List<DocumentSnapshot> filtered = new ArrayList<>();[m
         for (DocumentSnapshot doc : allDocs) {[m
             String title    = doc.getString("title");[m
[31m-            String status   = doc.getString("status");[m
             String category = doc.getString("category");[m
 [m
             if (title == null) title = "";[m
[31m-            if (status == null) status = "";[m
             if (category == null) category = "";[m
 [m
             //text search on title[m
[36m@@ -235,7 +234,7 @@[m [mpublic class AllEventsActivity extends AppCompatActivity {[m
             }[m
             switch (filterMode) {[m
                 case "open":[m
[31m-                    if (!"open".equals(status)) continue;[m
[32m+[m[32m                    if (!"open".equals(EventStatusUtils.computeStatus(doc))) continue;[m
                     break;[m
                 case "category":[m
                     if (!category.equalsIgnoreCase(selectedCategory)) continue;[m
[36m@@ -274,16 +273,15 @@[m [mpublic class AllEventsActivity extends AppCompatActivity {[m
 [m
             String title= doc.getString("title");[m
             String location = doc.getString("location");[m
[31m-            String status = doc.getString("status");[m
             Double price = doc.getDouble("price");[m
             String eventId = doc.getId();[m
 [m
             if (title == null) title = "Untitled";[m
             if (location == null) location = "";[m
[31m-            if (status == null) status = "open";[m
             if (price == null) price = 0.0;[m
 [m
[31m-            boolean isOpen = "open".equals(status);[m
[32m+[m[32m            String lifecycle = EventStatusUtils.computeStatus(doc);[m
[32m+[m[32m            boolean isOpen = "open".equals(lifecycle);[m
 [m
             ((TextView) row.findViewById(R.id.txtEventTitle)).setText(title);[m
             ((TextView) row.findViewById(R.id.txtEventLocation)).setText(location);[m
[36m@@ -298,7 +296,11 @@[m [mpublic class AllEventsActivity extends AppCompatActivity {[m
 [m
             //status badge[m
             TextView badge = row.findViewById(R.id.txtEventStatus);[m
[31m-            if (isOpen) {[m
[32m+[m[32m            if ("completed".equals(lifecycle)) {[m
[32m+[m[32m                badge.setText(R.string.organizer_status_completed);[m
[32m+[m[32m                badge.setBackgroundResource(R.drawable.bg_status_completed);[m
[32m+[m[32m                badge.setTextColor(getColor(R.color.status_completed_text));[m
[32m+[m[32m            } else if (isOpen) {[m
                 badge.setText("Open");[m
                 badge.setBackgroundResource(R.drawable.bg_status_open);[m
                 badge.setTextColor(getColor(R.color.status_open_text));[m
[1mdiff --git a/code/app/src/main/java/com/example/waitwell/activities/EntrantCalendarActivity.java b/code/app/src/main/java/com/example/waitwell/activities/EntrantCalendarActivity.java[m
[1mnew file mode 100644[m
[1mindex 0000000..cdf92f7[m
[1m--- /dev/null[m
[1m+++ b/code/app/src/main/java/com/example/waitwell/activities/EntrantCalendarActivity.java[m
[36m@@ -0,0 +1,350 @@[m
[32m+[m[32mpackage com.example.waitwell.activities;[m
[32m+[m
[32m+[m[32mimport android.content.Intent;[m
[32m+[m[32mimport android.graphics.drawable.GradientDrawable;[m
[32m+[m[32mimport android.os.Bundle;[m
[32m+[m[32mimport android.view.LayoutInflater;[m
[32m+[m[32mimport android.view.View;[m
[32m+[m[32mimport android.widget.ImageButton;[m
[32m+[m[32mimport android.widget.LinearLayout;[m
[32m+[m[32mimport android.widget.NumberPicker;[m
[32m+[m[32mimport android.widget.TextView;[m
[32m+[m[32mimport android.widget.Toast;[m
[32m+[m
[32m+[m[32mimport androidx.appcompat.app.AlertDialog;[m
[32m+[m[32mimport androidx.appcompat.app.AppCompatActivity;[m
[32m+[m[32mimport androidx.core.content.ContextCompat;[m
[32m+[m
[32m+[m[32mimport com.example.waitwell.FirebaseHelper;[m
[32m+[m[32mimport com.example.waitwell.R;[m
[32m+[m[32mimport com.google.firebase.firestore.DocumentSnapshot;[m
[32m+[m[32mimport com.google.firebase.firestore.QuerySnapshot;[m
[32m+[m
[32m+[m[32mimport java.text.SimpleDateFormat;[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.Calendar;[m
[32m+[m[32mimport java.util.Date;[m
[32m+[m[32mimport java.util.HashMap;[m
[32m+[m[32mimport java.util.List;[m
[32m+[m[32mimport java.util.Locale;[m
[32m+[m[32mimport java.util.Map;[m
[32m+[m
[32m+[m[32m/**[m
[32m+[m[32m * Entrant calendar: custom month grid with event counts per day (green = has events, red = none).[m
[32m+[m[32m * Tapping a day lists events; each row opens {@link EventDetailActivity}.[m
[32m+[m[32m */[m
[32m+[m[32mpublic class EntrantCalendarActivity extends AppCompatActivity {[m
[32m+[m
[32m+[m[32m    private final Map<String, List<DocumentSnapshot>> eventsByDay = new HashMap<>();[m
[32m+[m
[32m+[m[32m    private LinearLayout calendarGridContainer;[m
[32m+[m[32m    private LinearLayout eventsForDateContainer;[m
[32m+[m[32m    private TextView txtMonthYear;[m
[32m+[m[32m    private ImageButton btnPrevMonth;[m
[32m+[m[32m    private ImageButton btnNextMonth;[m
[32m+[m
[32m+[m[32m    /** Month being shown (day-of-month ignored for display math). */[m
[32m+[m[32m    private final Calendar displayMonth = Calendar.getInstance();[m
[32m+[m[32m    private int selectedDayOfMonth = 1;[m
[32m+[m[32m    private String selectedDayKey = "";[m
[32m+[m
[32m+[m[32m    private final SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);[m
[32m+[m[32m    private final SimpleDateFormat badgeFormat = new SimpleDateFormat("MMM d", Locale.US);[m
[32m+[m[32m    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());[m
[32m+[m
[32m+[m[32m    private float density;[m
[32m+[m
[32m+[m[32m    @Override[m
[32m+[m[32m    protected void onCreate(Bundle savedInstanceState) {[m
[32m+[m[32m        super.onCreate(savedInstanceState);[m
[32m+[m[32m        setContentView(R.layout.activity_entrant_calendar);[m
[32m+[m
[32m+[m[32m        density = getResources().getDisplayMetrics().density;[m
[32m+[m
[32m+[m[32m        calendarGridContainer = findViewById(R.id.calendarGridContainer);[m
[32m+[m[32m        eventsForDateContainer = findViewById(R.id.eventsForDateContainer);[m
[32m+[m[32m        txtMonthYear = findViewById(R.id.txtMonthYear);[m
[32m+[m[32m        btnPrevMonth = findViewById(R.id.btnPrevMonth);[m
[32m+[m[32m        btnNextMonth = findViewById(R.id.btnNextMonth);[m
[32m+[m
[32m+[m[32m        findViewById(R.id.btnBack).setOnClickListener(v -> finish());[m
[32m+[m
[32m+[m[32m        btnPrevMonth.setOnClickListener(v -> {[m
[32m+[m[32m            displayMonth.add(Calendar.MONTH, -1);[m
[32m+[m[32m            pickDefaultSelectedDay();[m
[32m+[m[32m            rebuildCalendarGrid();[m
[32m+[m[32m            renderEventsForDate(selectedDayKey);[m
[32m+[m[32m        });[m
[32m+[m[32m        btnNextMonth.setOnClickListener(v -> {[m
[32m+[m[32m            displayMonth.add(Calendar.MONTH, 1);[m
[32m+[m[32m            pickDefaultSelectedDay();[m
[32m+[m[32m            rebuildCalendarGrid();[m
[32m+[m[32m            renderEventsForDate(selectedDayKey);[m
[32m+[m[32m        });[m
[32m+[m
[32m+[m[32m        txtMonthYear.setOnClickListener(v -> showMonthYearPickerDialog());[m
[32m+[m
[32m+[m[32m        pickDefaultSelectedDay();[m
[32m+[m[32m        updateSelectedKeyFromParts();[m
[32m+[m[32m        rebuildCalendarGrid();[m
[32m+[m[32m        renderEventsForDate(selectedDayKey);[m
[32m+[m[32m        loadEvents();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /** Opens a quick month + year picker (tap the month/year label in the header). */[m
[32m+[m[32m    private void showMonthYearPickerDialog() {[m
[32m+[m[32m        Calendar now = Calendar.getInstance();[m
[32m+[m[32m        int minYear = now.get(Calendar.YEAR) - 5;[m
[32m+[m[32m        int maxYear = now.get(Calendar.YEAR) + 8;[m
[32m+[m
[32m+[m[32m        LinearLayout container = new LinearLayout(this);[m
[32m+[m[32m        container.setOrientation(LinearLayout.HORIZONTAL);[m
[32m+[m[32m        int pad = (int) (16 * density);[m
[32m+[m[32m        container.setPadding(pad, pad, pad, pad);[m
[32m+[m
[32m+[m[32m        NumberPicker monthPicker = new NumberPicker(this);[m
[32m+[m[32m        monthPicker.setMinValue(0);[m
[32m+[m[32m        monthPicker.setMaxValue(11);[m
[32m+[m[32m        monthPicker.setDisplayedValues(buildMonthNames());[m
[32m+[m[32m        monthPicker.setValue(displayMonth.get(Calendar.MONTH));[m
[32m+[m[32m        monthPicker.setWrapSelectorWheel(false);[m
[32m+[m[32m        LinearLayout.LayoutParams monthLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);[m
[32m+[m[32m        monthPicker.setLayoutParams(monthLp);[m
[32m+[m
[32m+[m[32m        NumberPicker yearPicker = new NumberPicker(this);[m
[32m+[m[32m        yearPicker.setMinValue(minYear);[m
[32m+[m[32m        yearPicker.setMaxValue(maxYear);[m
[32m+[m[32m        int currentYear = displayMonth.get(Calendar.YEAR);[m
[32m+[m[32m        yearPicker.setValue(Math.min(maxYear, Math.max(minYear, currentYear)));[m
[32m+[m[32m        yearPicker.setWrapSelectorWheel(false);[m
[32m+[m[32m        LinearLayout.LayoutParams yearLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);[m
[32m+[m[32m        yearPicker.setLayoutParams(yearLp);[m
[32m+[m
[32m+[m[32m        container.addView(monthPicker);[m
[32m+[m[32m        container.addView(yearPicker);[m
[32m+[m
[32m+[m[32m        new AlertDialog.Builder(this)[m
[32m+[m[32m                .setTitle(R.string.calendar_pick_month_year_title)[m
[32m+[m[32m                .setView(container)[m
[32m+[m[32m                .setPositiveButton(android.R.string.ok, (dialog, which) -> {[m
[32m+[m[32m                    displayMonth.set(Calendar.MONTH, monthPicker.getValue());[m
[32m+[m[32m                    displayMonth.set(Calendar.YEAR, yearPicker.getValue());[m
[32m+[m[32m                    pickDefaultSelectedDay();[m
[32m+[m[32m                    updateSelectedKeyFromParts();[m
[32m+[m[32m                    rebuildCalendarGrid();[m
[32m+[m[32m                    renderEventsForDate(selectedDayKey);[m
[32m+[m[32m                })[m
[32m+[m[32m                .setNegativeButton(android.R.string.cancel, null)[m
[32m+[m[32m                .show();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    private static String[] buildMonthNames() {[m
[32m+[m[32m        String[] names = new String[12];[m
[32m+[m[32m        Calendar c = Calendar.getInstance();[m
[32m+[m[32m        for (int i = 0; i < 12; i++) {[m
[32m+[m[32m            c.set(Calendar.MONTH, i);[m
[32m+[m[32m            String name = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());[m
[32m+[m[32m            names[i] = name != null ? name : String.valueOf(i + 1);[m
[32m+[m[32m        }[m
[32m+[m[32m        return names;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    private void pickDefaultSelectedDay() {[m
[32m+[m[32m        Calendar today = Calendar.getInstance();[m
[32m+[m[32m        if (displayMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR)[m
[32m+[m[32m                && displayMonth.get(Calendar.MONTH) == today.get(Calendar.MONTH)) {[m
[32m+[m[32m            selectedDayOfMonth = today.get(Calendar.DAY_OF_MONTH);[m
[32m+[m[32m        } else {[m
[32m+[m[32m            selectedDayOfMonth = 1;[m
[32m+[m[32m        }[m
[32m+[m[32m        int max = displayMonth.getActualMaximum(Calendar.DAY_OF_MONTH);[m
[32m+[m[32m        if (selectedDayOfMonth > max) {[m
[32m+[m[32m            selectedDayOfMonth = max;[m
[32m+[m[32m        }[m
[32m+[m[32m        updateSelectedKeyFromParts();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    privat