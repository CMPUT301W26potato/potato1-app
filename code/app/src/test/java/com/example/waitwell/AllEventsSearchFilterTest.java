package com.example.waitwell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests the filtering logic from AllEventsActivity.applyFilters() using Mockito.
 *
 * The filterEvents helper replicates the exact logic from applyFilters() (lines 387-454)
 * so we can unit-test all filter modes without launching the Activity.
 * * Written with help from Claude (claude.ai)
 */
@RunWith(MockitoJUnitRunner.class)
public class AllEventsSearchFilterTest {

    private List<DocumentSnapshot> allDocs;

    private static Date dateOf(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private DocumentSnapshot createMockEvent(String title, List<String> categories,
                                              String description, String location,
                                              boolean isPrivate, Date eventDate,
                                              Date registrationClose, Long waitlistLimit) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getBoolean("isPrivate")).thenReturn(isPrivate);
        when(doc.getString("title")).thenReturn(title);
        when(doc.get("categories")).thenReturn(categories);
        when(doc.getString("description")).thenReturn(description);
        when(doc.getString("location")).thenReturn(location);
        when(doc.getDate("eventDate")).thenReturn(eventDate);
        when(doc.getDate("registrationClose")).thenReturn(registrationClose);
        when(doc.getLong("waitlistLimit")).thenReturn(waitlistLimit);
        return doc;
    }

    @Before
    public void setUp() {
        DocumentSnapshot yoga = createMockEvent("Yoga Hatha",
                Arrays.asList("Mindfulness", "Fitness"),
                "Relaxing yoga session", "Edmonton, Alberta",
                false, dateOf(2099, 6, 1), dateOf(2099, 5, 25), 15L);

        DocumentSnapshot swim = createMockEvent("Swimming Lessons",
                Arrays.asList("Swimming"),
                "Learn to swim", "Edmonton Recreation Center",
                false, dateOf(2099, 7, 1), dateOf(2099, 6, 25), 30L);

        DocumentSnapshot boxing = createMockEvent("Boxing",
                Arrays.asList("Boxing", "Fitness"),
                "Boxing intro", "Edmonton, Alberta",
                false, dateOf(2099, 8, 1), dateOf(2099, 7, 25), 60L);

        DocumentSnapshot closed = createMockEvent("Old Dance Class",
                Arrays.asList("Dance"),
                "Dance workshop", "Edmonton, Alberta",
                false, dateOf(2099, 9, 1), dateOf(2020, 1, 1), 10L);

        DocumentSnapshot completed = createMockEvent("Past Marathon",
                Arrays.asList("Running"),
                "Marathon event", "River Valley",
                false, dateOf(2020, 1, 1), dateOf(2019, 12, 1), 100L);

        DocumentSnapshot free = createMockEvent("Free Yoga in the Park",
                Arrays.asList("Yoga", "Mindfulness"),
                "Outdoor yoga", "Hawrelak Park",
                false, dateOf(2099, 10, 1), dateOf(2099, 9, 25), 0L);

        DocumentSnapshot privateEvent = createMockEvent("Private Gala",
                Arrays.asList("Social"),
                "Invitation only", "Downtown Edmonton",
                true, dateOf(2099, 11, 1), dateOf(2099, 10, 25), 50L);

        DocumentSnapshot nullFields = createMockEvent("Bare Event",
                null, null, null,
                false, dateOf(2099, 12, 1), dateOf(2099, 11, 25), null);

        allDocs = Arrays.asList(yoga, swim, boxing, closed, completed, free, privateEvent, nullFields);
    }


    private List<DocumentSnapshot> filterEvents(String searchQuery, String filterMode,
                                                 Set<String> selectedCategories,
                                                 Long startDateMillis, Long endDateMillis,
                                                 String selectedCapacityRange) {
        String query = searchQuery.trim().toLowerCase();
        List<DocumentSnapshot> filtered = new ArrayList<>();

        for (DocumentSnapshot doc : allDocs) {
            if (Boolean.TRUE.equals(doc.getBoolean("isPrivate"))) {
                continue;
            }

            String title = doc.getString("title");
            @SuppressWarnings("unchecked")
            List<String> categories = (List<String>) doc.get("categories");
            String description = doc.getString("description");
            String location = doc.getString("location");

            if (title == null) title = "";
            if (categories == null) categories = new ArrayList<>();
            if (description == null) description = "";
            if (location == null) location = "";

            // Text search across multiple fields
            if (!query.isEmpty()) {
                boolean matchFound = title.toLowerCase().contains(query) ||
                        description.toLowerCase().contains(query) ||
                        (categories.toString().toLowerCase().contains(query)) ||
                        location.toLowerCase().contains(query);
                if (!matchFound) {
                    continue;
                }
            }

            // Filter mode
            switch (filterMode) {
                case "open":
                    if (!"open".equals(EventStatusUtils.computeStatus(doc))) continue;
                    break;
                case "category":
                    if (categories == null) categories = new ArrayList<>();
                    boolean match = false;
                    for (String cat : categories) {
                        if (selectedCategories.contains(cat)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) continue;
                    break;
                case "date_range":
                    Date eventDate = doc.getDate("eventDate");
                    if (eventDate == null) continue;
                    if (startDateMillis != null && eventDate.getTime() < startDateMillis) continue;
                    if (endDateMillis != null && eventDate.getTime() > endDateMillis + 86400000L) continue;
                    break;
                case "event_capacity":
                    Long capVal = doc.getLong("waitlistLimit");
                    int c = (capVal != null) ? capVal.intValue() : 0;
                    if ("Small (1-20)".equals(selectedCapacityRange) && (c < 1 || c > 20)) continue;
                    if ("Medium (21-50)".equals(selectedCapacityRange) && (c < 21 || c > 50)) continue;
                    if ("Large (51+)".equals(selectedCapacityRange) && c < 51) continue;
                    break;
                // "all" — no filtering
            }

            filtered.add(doc);
        }
        return filtered;
    }

    /** Convenience: for "all" and "open" filters with no extra params. */
    private List<DocumentSnapshot> filterEvents(String searchQuery, String filterMode) {
        return filterEvents(searchQuery, filterMode, null, null, null, null);
    }

    /** Convenience: for "category" filter. */
    private List<DocumentSnapshot> filterByCategory(String searchQuery, Set<String> selectedCategories) {
        return filterEvents(searchQuery, "category", selectedCategories, null, null, null);
    }

    /** Convenience: for "date_range" filter. */
    private List<DocumentSnapshot> filterByDateRange(String searchQuery, Long startMillis, Long endMillis) {
        return filterEvents(searchQuery, "date_range", null, startMillis, endMillis, null);
    }

    /** Convenience: for "event_capacity" filter. */
    private List<DocumentSnapshot> filterByCapacity(String searchQuery, String capacityRange) {
        return filterEvents(searchQuery, "event_capacity", null, null, null, capacityRange);
    }

    private Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private List<String> getTitles(List<DocumentSnapshot> docs) {
        return docs.stream().map(d -> d.getString("title")).collect(Collectors.toList());
    }

    // ========== Private event exclusion ==========

    @Test
    public void testPrivateEventsNeverAppear_allFilter() {
        List<DocumentSnapshot> result = filterEvents("", "all");
        assertEquals(7, result.size());
        List<String> titles = getTitles(result);
        assertTrue(!titles.contains("Private Gala"));
    }

    @Test
    public void testPrivateEventsNeverAppear_withMatchingSearch() {
        List<DocumentSnapshot> result = filterEvents("gala", "all");
        assertTrue(result.isEmpty());
    }

    // ========== Text search ==========

    @Test
    public void testSearch_matchesTitlePartial() {
        List<DocumentSnapshot> result = filterEvents("yoga", "all");
        assertEquals(2, result.size());
        List<String> titles = getTitles(result);
        assertTrue(titles.contains("Yoga Hatha"));
        assertTrue(titles.contains("Free Yoga in the Park"));
    }

    @Test
    public void testSearch_matchesDescription() {
        List<DocumentSnapshot> result = filterEvents("learn to swim", "all");
        assertEquals(1, result.size());
        assertEquals("Swimming Lessons", result.get(0).getString("title"));
    }

    @Test
    public void testSearch_matchesCategoriesList() {
        List<DocumentSnapshot> result = filterEvents("mindfulness", "all");
        assertEquals(2, result.size());
        List<String> titles = getTitles(result);
        assertTrue(titles.contains("Yoga Hatha"));
        assertTrue(titles.contains("Free Yoga in the Park"));
    }

    @Test
    public void testSearch_matchesLocation() {
        List<DocumentSnapshot> result = filterEvents("hawrelak", "all");
        assertEquals(1, result.size());
        assertEquals("Free Yoga in the Park", result.get(0).getString("title"));
    }

    @Test
    public void testSearch_caseInsensitive() {
        List<DocumentSnapshot> result = filterEvents("BOXING", "all");
        assertEquals(1, result.size());
        assertEquals("Boxing", result.get(0).getString("title"));
    }

    @Test
    public void testSearch_partialCategoryMatch() {
        // "fit" matches "Fitness" in the categories list toString()
        List<DocumentSnapshot> result = filterEvents("fit", "all");
        assertEquals(2, result.size());
        List<String> titles = getTitles(result);
        assertTrue(titles.contains("Yoga Hatha"));
        assertTrue(titles.contains("Boxing"));
    }

    @Test
    public void testSearch_noMatch() {
        List<DocumentSnapshot> result = filterEvents("piano", "all");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearch_matchesNullFieldsEvent() {
        List<DocumentSnapshot> result = filterEvents("bare", "all");
        assertEquals(1, result.size());
        assertEquals("Bare Event", result.get(0).getString("title"));
    }

    // ========== "all" filter ==========

    @Test
    public void testFilterAll_noSearch_returnsAllNonPrivate() {
        List<DocumentSnapshot> result = filterEvents("", "all");
        assertEquals(7, result.size());
    }

    @Test
    public void testFilterAll_withSearch_filtersOnlyByText() {
        List<DocumentSnapshot> result = filterEvents("edmonton", "all");
        List<String> titles = getTitles(result);
        // "Edmonton" appears in location of yoga, swim, boxing, closed
        assertTrue(titles.contains("Yoga Hatha"));
        assertTrue(titles.contains("Swimming Lessons"));
        assertTrue(titles.contains("Boxing"));
        assertTrue(titles.contains("Old Dance Class"));
        assertEquals(4, result.size());
    }




    // ========== "date_range" filter ==========

    @Test
    public void testFilterDateRange_withinRange() {
        // Range that encompasses yoga's eventDate (2099-06-01)
        // swim (2099-07-01) also passes because end+1day = 2099-07-01 and the check is >
        long start = dateOf(2099, 5, 1).getTime();
        long end = dateOf(2099, 6, 30).getTime();
        List<DocumentSnapshot> result = filterByDateRange("", start, end);
        List<String> titles = getTitles(result);
        assertTrue(titles.contains("Yoga Hatha"));
        assertEquals(2, result.size());
    }

    @Test
    public void testFilterDateRange_beforeRange() {
        // Range entirely in the past — no 2099 events should match
        // (completed event at 2020-01-01 will match though)
        long start = dateOf(2019, 1, 1).getTime();
        long end = dateOf(2019, 6, 1).getTime();
        List<DocumentSnapshot> result = filterByDateRange("", start, end);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterDateRange_endDateInclusiveWithMargin() {
        // End date exactly on yoga's eventDate — should still include due to +86400000 margin
        long start = dateOf(2099, 5, 1).getTime();
        long end = dateOf(2099, 6, 1).getTime();
        List<DocumentSnapshot> result = filterByDateRange("", start, end);
        assertEquals(1, result.size());
        assertEquals("Yoga Hatha", result.get(0).getString("title"));
    }

    @Test
    public void testFilterDateRange_nullEventDate_excluded() {
        // Create a special doc list with a null-eventDate event
        DocumentSnapshot nullDate = createMockEvent("No Date Event", Arrays.asList("Test"),
                "Test", "Test", false, null, dateOf(2099, 1, 1), 10L);
        List<DocumentSnapshot> savedDocs = allDocs;
        allDocs = Arrays.asList(nullDate);
        List<DocumentSnapshot> result = filterByDateRange("", dateOf(2099, 1, 1).getTime(), dateOf(2099, 12, 31).getTime());
        assertTrue(result.isEmpty());
        allDocs = savedDocs;
    }

    @Test
    public void testFilterDateRange_onlyStartDate() {
        // Only start set, end null — events after start are included
        long start = dateOf(2099, 9, 15).getTime();
        List<DocumentSnapshot> result = filterByDateRange("", start, null);
        List<String> titles = getTitles(result);
        // Events with eventDate >= 2099-09-15: free (10-01), private excluded, nullFields (12-01)
        // closed has eventDate 2099-09-01 which is before start
        assertTrue(titles.contains("Free Yoga in the Park"));
        assertTrue(titles.contains("Bare Event"));
        assertEquals(2, result.size());
    }

    @Test
    public void testFilterDateRange_onlyEndDate() {
        // Only end set, start null — events before end+1day are included
        long end = dateOf(2020, 1, 1).getTime();
        List<DocumentSnapshot> result = filterByDateRange("", null, end);
        // completed event has eventDate 2020-01-01, end+1day = 2020-01-02, so included
        assertEquals(1, result.size());
        assertEquals("Past Marathon", result.get(0).getString("title"));
    }

    // ========== "event_capacity" filter ==========

    @Test
    public void testFilterCapacity_small() {
        List<DocumentSnapshot> result = filterByCapacity("", "Small (1-20)");
        List<String> titles = getTitles(result);
        // yoga=15, closed=10
        assertTrue(titles.contains("Yoga Hatha"));
        assertTrue(titles.contains("Old Dance Class"));
        assertEquals(2, result.size());
    }

    @Test
    public void testFilterCapacity_medium() {
        List<DocumentSnapshot> result = filterByCapacity("", "Medium (21-50)");
        List<String> titles = getTitles(result);
        // swim=30
        assertTrue(titles.contains("Swimming Lessons"));
        assertEquals(1, result.size());
    }

    @Test
    public void testFilterCapacity_large() {
        List<DocumentSnapshot> result = filterByCapacity("", "Large (51+)");
        List<String> titles = getTitles(result);
        // boxing=60, completed=100
        assertTrue(titles.contains("Boxing"));
        assertTrue(titles.contains("Past Marathon"));
        assertEquals(2, result.size());
    }

    @Test
    public void testFilterCapacity_nullWaitlistTreatedAsZero() {
        // nullFields event has null waitlistLimit, treated as 0
        // 0 does not match Small (1-20), Medium (21-50), or Large (51+)
        List<DocumentSnapshot> result = filterByCapacity("bare", "Small (1-20)");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterCapacity_zeroWaitlistLimit() {
        // free event has waitlistLimit=0, does not match any range
        List<DocumentSnapshot> result = filterByCapacity("free yoga", "Small (1-20)");
        assertTrue(result.isEmpty());
    }

    // ========== Combined search + filter ==========

    @Test
    public void testSearch_plusOpenFilter() {
        List<DocumentSnapshot> result = filterEvents("yoga", "open");
        assertEquals(2, result.size());
        List<String> titles = getTitles(result);
        assertTrue(titles.contains("Yoga Hatha"));
        assertTrue(titles.contains("Free Yoga in the Park"));
    }

    @Test
    public void testSearch_plusCategoryFilter() {
        List<DocumentSnapshot> result = filterByCategory("free", setOf("Yoga"));
        assertEquals(1, result.size());
        assertEquals("Free Yoga in the Park", result.get(0).getString("title"));
    }

    @Test
    public void testSearch_plusCategoryFilter_noOverlap() {
        List<DocumentSnapshot> result = filterByCategory("boxing", setOf("Swimming"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearch_plusDateRangeFilter() {
        // Search "yoga" + date range that only covers yoga's eventDate
        long start = dateOf(2099, 5, 1).getTime();
        long end = dateOf(2099, 6, 30).getTime();
        List<DocumentSnapshot> result = filterEvents("yoga", "date_range", null, start, end, null);
        assertEquals(1, result.size());
        assertEquals("Yoga Hatha", result.get(0).getString("title"));
    }

    // ========== Edge cases ==========

    @Test
    public void testEmptySearch_allFilter() {
        List<DocumentSnapshot> result = filterEvents("", "all");
        assertEquals(7, result.size());
    }

    @Test
    public void testWhitespaceSearch_treatedAsEmpty() {
        List<DocumentSnapshot> result = filterEvents("   ", "all");
        assertEquals(7, result.size());
    }

    @Test
    public void testNullFieldsEvent_survivesAllFilter() {
        List<DocumentSnapshot> result = filterEvents("", "all");
        List<String> titles = getTitles(result);
        assertTrue(titles.contains("Bare Event"));
    }

    @Test
    public void testEmptyDocList_returnsEmpty() {
        List<DocumentSnapshot> savedDocs = allDocs;
        allDocs = new ArrayList<>();
        List<DocumentSnapshot> result = filterEvents("", "all");
        assertTrue(result.isEmpty());
        allDocs = savedDocs;
    }
}
