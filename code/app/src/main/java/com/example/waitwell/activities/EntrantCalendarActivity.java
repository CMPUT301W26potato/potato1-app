package com.example.waitwell.activities;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.waitwell.FirebaseHelper;
import com.example.waitwell.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Entrant calendar: custom month grid with event counts per day (green = has events, red = none).
 * Tapping a day lists events; each row opens {@link EventDetailActivity}.
 */
public class EntrantCalendarActivity extends AppCompatActivity {

    private final Map<String, List<DocumentSnapshot>> eventsByDay = new HashMap<>();

    private LinearLayout calendarGridContainer;
    private LinearLayout eventsForDateContainer;
    private TextView txtMonthYear;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;

    /** Month being shown (day-of-month ignored for display math). */
    private final Calendar displayMonth = Calendar.getInstance();
    private int selectedDayOfMonth = 1;
    private String selectedDayKey = "";

    private final SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat badgeFormat = new SimpleDateFormat("MMM d", Locale.US);
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_calendar);

        density = getResources().getDisplayMetrics().density;

        calendarGridContainer = findViewById(R.id.calendarGridContainer);
        eventsForDateContainer = findViewById(R.id.eventsForDateContainer);
        txtMonthYear = findViewById(R.id.txtMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPrevMonth.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, -1);
            pickDefaultSelectedDay();
            rebuildCalendarGrid();
            renderEventsForDate(selectedDayKey);
        });
        btnNextMonth.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, 1);
            pickDefaultSelectedDay();
            rebuildCalendarGrid();
            renderEventsForDate(selectedDayKey);
        });

        txtMonthYear.setOnClickListener(v -> showMonthYearPickerDialog());

        pickDefaultSelectedDay();
        updateSelectedKeyFromParts();
        rebuildCalendarGrid();
        renderEventsForDate(selectedDayKey);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /** Opens a quick month + year picker (tap the month/year label in the header). */
    private void showMonthYearPickerDialog() {
        Calendar now = Calendar.getInstance();
        int minYear = now.get(Calendar.YEAR) - 5;
        int maxYear = now.get(Calendar.YEAR) + 8;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        int pad = (int) (16 * density);
        container.setPadding(pad, pad, pad, pad);

        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(buildMonthNames());
        monthPicker.setValue(displayMonth.get(Calendar.MONTH));
        monthPicker.setWrapSelectorWheel(false);
        LinearLayout.LayoutParams monthLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        monthPicker.setLayoutParams(monthLp);

        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(minYear);
        yearPicker.setMaxValue(maxYear);
        int currentYear = displayMonth.get(Calendar.YEAR);
        yearPicker.setValue(Math.min(maxYear, Math.max(minYear, currentYear)));
        yearPicker.setWrapSelectorWheel(false);
        LinearLayout.LayoutParams yearLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        yearPicker.setLayoutParams(yearLp);

        container.addView(monthPicker);
        container.addView(yearPicker);

        new AlertDialog.Builder(this)
                .setTitle(R.string.calendar_pick_month_year_title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    displayMonth.set(Calendar.MONTH, monthPicker.getValue());
                    displayMonth.set(Calendar.YEAR, yearPicker.getValue());
                    pickDefaultSelectedDay();
                    updateSelectedKeyFromParts();
                    rebuildCalendarGrid();
                    renderEventsForDate(selectedDayKey);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static String[] buildMonthNames() {
        String[] names = new String[12];
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            c.set(Calendar.MONTH, i);
            String name = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            names[i] = name != null ? name : String.valueOf(i + 1);
        }
        return names;
    }

    private void pickDefaultSelectedDay() {
        Calendar today = Calendar.getInstance();
        if (displayMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && displayMonth.get(Calendar.MONTH) == today.get(Calendar.MONTH)) {
            selectedDayOfMonth = today.get(Calendar.DAY_OF_MONTH);
        } else {
            selectedDayOfMonth = 1;
        }
        int max = displayMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (selectedDayOfMonth > max) {
            selectedDayOfMonth = max;
        }
        updateSelectedKeyFromParts();
    }

    private void updateSelectedKeyFromParts() {
        Calendar c = (Calendar) displayMonth.clone();
        c.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        selectedDayKey = keyFormat.format(c.getTime());
    }

    private void loadEvents() {
        FirebaseHelper.getInstance().getAllEvents()
                .addOnSuccessListener(this::onEventsLoaded)
                .addOnFailureListener(e -> Toast.makeText(this, "Could not load events", Toast.LENGTH_SHORT).show());
    }

    private void onEventsLoaded(QuerySnapshot snapshot) {
        eventsByDay.clear();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Date dayDate = doc.getDate("eventDate");
            if (dayDate == null) {
                dayDate = doc.getDate("registrationClose");
            }
            if (dayDate == null) {
                dayDate = doc.getDate("registrationOpen");
            }
            if (dayDate == null) {
                continue;
            }
            String dayKey = keyFormat.format(dayDate);
            eventsByDay.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(doc);
        }
        rebuildCalendarGrid();
        renderEventsForDate(selectedDayKey);
    }

    private void rebuildCalendarGrid() {
        calendarGridContainer.removeAllViews();
        txtMonthYear.setText(monthYearFormat.format(displayMonth.getTime()));

        Calendar first = (Calendar) displayMonth.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = first.get(Calendar.DAY_OF_WEEK);
        int startOffset = (firstDayOfWeek - first.getFirstDayOfWeek() + 7) % 7;
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        int totalCells = startOffset + daysInMonth;
        int rowCount = (totalCells + 6) / 7;
        int dayCounter = 1;

        for (int r = 0; r < rowCount; r++) {
            LinearLayout row = newRow();
            for (int c = 0; c < 7; c++) {
                int idx = r * 7 + c;
                if (idx < startOffset || idx >= startOffset + daysInMonth) {
                    row.addView(emptySpacer());
                } else {
                    row.addView(buildDayCell(dayCounter));
                    dayCounter++;
                }
            }
            calendarGridContainer.addView(row);
        }
    }

    private LinearLayout newRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View emptySpacer() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (56 * density));
        lp.weight = 1f;
        v.setLayoutParams(lp);
        return v;
    }

    private View buildDayCell(int dayOfMonth) {
        View cell = LayoutInflater.from(this).inflate(R.layout.item_calendar_day_cell, calendarGridContainer, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.weight = 1f;
        cell.setLayoutParams(lp);

        TextView txtDay = cell.findViewById(R.id.txtDayNumber);
        TextView txtCount = cell.findViewById(R.id.txtEventCount);
        View root = cell.findViewById(R.id.calendarDayRoot);

        txtDay.setText(String.valueOf(dayOfMonth));

        Calendar dayCal = (Calendar) displayMonth.clone();
        dayCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        dayCal.set(Calendar.HOUR_OF_DAY, 0);
        dayCal.set(Calendar.MINUTE, 0);
        dayCal.set(Calendar.SECOND, 0);
        dayCal.set(Calendar.MILLISECOND, 0);
        String key = keyFormat.format(dayCal.getTime());

        List<DocumentSnapshot> list = eventsByDay.get(key);
        int count = list == null ? 0 : list.size();
        txtCount.setText(String.valueOf(count));

        boolean hasEvents = count > 0;
        boolean selected = dayOfMonth == selectedDayOfMonth;
        applyDayCellBackground(root, hasEvents, selected);

        root.setOnClickListener(v -> {
            selectedDayOfMonth = dayOfMonth;
            updateSelectedKeyFromParts();
            rebuildCalendarGrid();
            renderEventsForDate(selectedDayKey);
        });

        return cell;
    }

    private void applyDayCellBackground(View root, boolean hasEvents, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(8f * density);
        int fill = ContextCompat.getColor(this,
                hasEvents ? R.color.calendar_date_has_events_bg : R.color.calendar_date_no_events_bg);
        d.setColor(fill);
        if (selected) {
            d.setStroke(Math.max(1, (int) (2 * density)),
                    ContextCompat.getColor(this, R.color.primary));
        }
        root.setBackground(d);
    }

    private void renderEventsForDate(String dayKey) {
        eventsForDateContainer.removeAllViews();

        List<DocumentSnapshot> events = eventsByDay.get(dayKey);
        if (events == null || events.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.no_events_on_date));
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(14f);
            eventsForDateContainer.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocumentSnapshot doc : events) {
            View row = inflater.inflate(R.layout.item_event_row, eventsForDateContainer, false);
            TextView title = row.findViewById(R.id.txtRowName);
            TextView badge = row.findViewById(R.id.txtRowStatus);

            String eventTitle = doc.getString("title");
            if (eventTitle == null || eventTitle.trim().isEmpty()) {
                eventTitle = "Untitled Event";
            }
            title.setText(eventTitle);

            Date badgeDate = doc.getDate("eventDate");
            if (badgeDate == null) {
                badgeDate = doc.getDate("registrationClose");
            }
            if (badgeDate == null) {
                badgeDate = doc.getDate("registrationOpen");
            }
            if (badgeDate != null) {
                badge.setText(badgeFormat.format(badgeDate));
            } else {
                badge.setText("--");
            }
            badge.setBackgroundResource(R.drawable.bg_chip);
            badge.setTextColor(getColor(R.color.text_secondary));

            String eventId = doc.getId();
            row.setOnClickListener(v -> {
                Intent i = new Intent(EntrantCalendarActivity.this, EventDetailActivity.class);
                i.putExtra("event_id", eventId);
                startActivity(i);
            });

            eventsForDateContainer.addView(row);
        }
    }
}
