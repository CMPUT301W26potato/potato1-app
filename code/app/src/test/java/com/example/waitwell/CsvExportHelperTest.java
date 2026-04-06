package com.example.waitwell;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 setup/cleanup flow.
 *
 * I used Gemini to get my head around mocking Android Context for file output
 * and then checking CSV content from a temp file in a JVM unit test.
 *
 * Sites I looked at:
 *
 * Mockito docs:
 * https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
 *
 * Testing file I/O in Android:
 * https://developer.android.com/training/data-storage/files/external-scoped
 */

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.os.Environment;

import org.junit.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link CsvExportHelper} covering happy path, empty input, and null input.
 * Mocks Context string/file calls so CSV logic runs without Android storage services.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see CsvExportHelper
 */
public class CsvExportHelperTest {
    private Context context;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        tempDir = Files.createTempDirectory("csv-test").toFile();
        when(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).thenReturn(tempDir);
        when(context.getString(eq(R.string.csv_export_line_event), any())).thenReturn("Event,Test");
        when(context.getString(eq(R.string.csv_export_line_exported), any())).thenReturn("Exported,Now");
        when(context.getString(R.string.csv_header_name)).thenReturn("Name");
        when(context.getString(R.string.csv_header_email)).thenReturn("Email");
        when(context.getString(R.string.csv_header_phone_number)).thenReturn("Phone");
        when(context.getString(R.string.csv_header_role)).thenReturn("Role");
        when(context.getString(R.string.csv_header_device_id)).thenReturn("Device");
        when(context.getString(R.string.csv_header_account_created)).thenReturn("Created");
    }

    @After
    public void tearDown() throws Exception {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            tempDir.delete();
        }
    }

    /**
     * Checks that valid entrants generate a non-null file with header and data rows.
     * This is the happy path.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGenerateFinalEntrantsCsv_validList_returnsFileWithRows() throws Exception {
        List<CsvExportHelper.FinalEntrant> entrants = new ArrayList<>();
        entrants.add(new CsvExportHelper.FinalEntrant("A", "a@x.com", "1", "entrant", "d1", "today"));
        entrants.add(new CsvExportHelper.FinalEntrant("B", "b@x.com", "2", "entrant", "d2", "today"));
        File file = CsvExportHelper.generateFinalEntrantsCsv(context, "My Event", entrants);
        assertNotNull(file);
        assertTrue(file.exists());
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("Name,Email,Phone,Role,Device,Created"));
        assertTrue(text.contains("A,a@x.com,1,entrant,d1,today"));
        assertTrue(text.contains("B,b@x.com,2,entrant,d2,today"));
    }

    /**
     * Checks that empty entrant list still returns a file with headers.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGenerateFinalEntrantsCsv_emptyList_returnsHeaderOnlyFile() throws Exception {
        File file = CsvExportHelper.generateFinalEntrantsCsv(context, "Event", new ArrayList<>());
        assertNotNull(file);
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("Name,Email,Phone,Role,Device,Created"));
    }

    /**
     * Checks that null entrant list is handled and returns null instead of crashing.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGenerateFinalEntrantsCsv_nullList_returnsNull() {
        File file = CsvExportHelper.generateFinalEntrantsCsv(context, "Event", null);
        assertNull(file);
    }
}
