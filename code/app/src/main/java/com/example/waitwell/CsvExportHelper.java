package com.example.waitwell;

import android.content.Context;
import android.os.Environment;

import com.example.waitwell.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helper for building/exporting the final entrants CSV file.
 * Keeps CSV formatting logic out of activity classes.
 *
 * Addresses: US 02.06.05 - Organizer: Export Enrolled List CSV
 *
 * @author Karina Zhang
 * @version 1.0
 * @see com.example.waitwell.activities.FinalEntrantsActivity
 */
public final class CsvExportHelper {
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

    private static final Pattern NON_FILE_CHARS = Pattern.compile("[^a-zA-Z0-9]+");

    /**
     * Row model used while building CSV export rows.
     *
     * Addresses: US 02.06.05 - Organizer: Export Enrolled List CSV
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public static final class FinalEntrant {
        public final String name;
        public final String email;
        public final String phone;
        public final String role;
        public final String deviceId;
        public final String createdAt;

        public FinalEntrant(String name, String email, String phone, String role, String deviceId, String createdAt) {
            this.name = name != null ? name : "";
            this.email = email != null ? email : "";
            this.phone = phone != null ? phone : "";
            this.role = role != null ? role : "";
            this.deviceId = deviceId != null ? deviceId : "";
            this.createdAt = createdAt != null ? createdAt : "";
        }
    }

    public static File generateFinalEntrantsCsv(Context context, String eventName, List<FinalEntrant> entrants) {
        try {
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) {
                return null;
            }
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            String safeName = sanitizeEventNameForFile(eventName);
            String datePart = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            String filename = safeName + "_FinalEntrants_" + datePart + ".csv";
            File outFile = new File(dir, filename);

            String exportedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String lineEvent = context.getString(R.string.csv_export_line_event, eventName != null ? eventName : "");
            String lineExported = context.getString(R.string.csv_export_line_exported, exportedAt);

            String hName = context.getString(R.string.csv_header_name);
            String hEmail = context.getString(R.string.csv_header_email);
            String hPhone = context.getString(R.string.csv_header_phone_number);
            String hRole = context.getString(R.string.csv_header_role);
            String hDevice = context.getString(R.string.csv_header_device_id);
            String hCreated = context.getString(R.string.csv_header_account_created);

            try (FileOutputStream fos = new FileOutputStream(outFile);
                 OutputStreamWriter w = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                w.write(lineEvent);
                w.write("\r\n");
                w.write(lineExported);
                w.write("\r\n");
                w.write(escapeCsvField(hName));
                w.write(",");
                w.write(escapeCsvField(hEmail));
                w.write(",");
                w.write(escapeCsvField(hPhone));
                w.write(",");
                w.write(escapeCsvField(hRole));
                w.write(",");
                w.write(escapeCsvField(hDevice));
                w.write(",");
                w.write(escapeCsvField(hCreated));
                w.write("\r\n");
                for (FinalEntrant e : entrants) {
                    w.write(escapeCsvField(e.name));
                    w.write(",");
                    w.write(escapeCsvField(e.email));
                    w.write(",");
                    w.write(escapeCsvField(e.phone));
                    w.write(",");
                    w.write(escapeCsvField(e.role));
                    w.write(",");
                    w.write(escapeCsvField(e.deviceId));
                    w.write(",");
                    w.write(escapeCsvField(e.createdAt));
                    w.write("\r\n");
                }
            }
            return outFile;
        } catch (Throwable t) {
            return null;
        }
    }

    static String sanitizeEventNameForFile(String eventName) {
        if (eventName == null || eventName.trim().isEmpty()) {
            return "Event";
        }
        String s = NON_FILE_CHARS.matcher(eventName.trim()).replaceAll("_");
        while (s.startsWith("_")) {
            s = s.substring(1);
        }
        while (s.endsWith("_")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.isEmpty() ? "Event" : s;
    }

    static String escapeCsvField(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private CsvExportHelper() {}
}

