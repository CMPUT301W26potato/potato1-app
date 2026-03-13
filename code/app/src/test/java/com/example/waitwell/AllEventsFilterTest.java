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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests the filtering logic from AllEventsActivity using Mockito.
 *
 * Instead of launching the real activity, we extract the same filtering
 * logic that applyFilters() uses and test it against mock DocumentSnapshots.
 * Written with help from Claude (claude.ai)
 */
@RunWith(MockitoJUnitRunner.class)
public class AllEventsFilterTest {

    private List<DocumentSnapshot> allDocs;

    @Before
    public void setUp() {
        // Create mock documents matching our real Firestore data
        DocumentSnapshot yoga = createMockEvent("yoga_001", "Yoga Hatha",
                "open", 10.99, "Mindfulness", "Edmonton, Alberta");

        DocumentSnapshot swim = createMockEvent("swim_001", "Swimming Lessons",
                "open", 13.0, "Swimming", "Edmonton Recreation Center");

        DocumentSnapshot boxing = createMockEvent("boxing_001", "Boxing",
                "open", 10.99, "Boxing", "Edmonton, Alberta");

        DocumentSnapshot closedEvent = createMockEvent("closed_001", "Old Dance Class",
                "closed", 5.0, "Dance", "Edmonton, Alberta");

        DocumentSnapshot freeEvent = createMockEvent("free_001", "Free Yoga in the Park",
                "open", 0.0, "Yoga", "Hawrelak Park");

        allDocs = Arrays.asList(yoga, swim, boxing, closedEvent, freeEvent);
    }

    private DocumentSnapshot createMockEvent(String id, String title, String status,
                                              double price, String category, String location) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getString("title")).thenReturn(title);
        when(doc.getString("status")).thenReturn(status);
        when(doc.getString("category")).thenReturn(category);
        return doc;
    }

    /**
     * Replicates the filtering logic from AllEventsActivity.applyFilters().
     * Extracted here so we can unit test it without launching an Activity.
     */
    private List<DocumentSnapshot> filterEvents(String searchQuery,
                                                 String filterMode,
                                                 String selectedCategory) {
        String query = searchQuery.trim().toLowerCase();

        return allDocs.stream().filter(doc -> {
            String title = doc.getString("title");
            String status = doc.getString("status");
            String category = doc.getString("category");

            if (title == null) title = "";
            if (status == null) status = "";
            if (category == null) category = "";

            // Text search
            if (!query.isEmpty() && !title.toLowerCase().contains(query)) {
                return false;
            }
            // Filter mode
            switch (filterMode) {
                case "open":
                    return "open".equals(status);
                case "category":
                    return category.equalsIgnoreCase(selectedCategory);
                default: // "all"
                    return true;
            }
        }).collect(Collectors.toList());
    }

    //"All" filter
    @Test
    public void testFilterAll_noSearch_returnsEverything() {
        List<DocumentSnapshot> result = filterEvents("", "all", null);
        assertEquals(5, result.size());
    }

    // "Open" filter

    @Test
    public void testFilterOpen_excludesClosed() {
        List<DocumentSnapshot> result = filterEvents("", "open", null);
        assertEquals(4, result.size());
        for (DocumentSnapshot doc : result) {
            assertEquals("open", doc.getString("status"));
        }
    }

    // Category filter

    @Test
    public void testFilterCategory_swimming() {
        List<DocumentSnapshot> result = filterEvents("", "category", "Swimming");
        assertEquals(1, result.size());
        assertEquals("Swimming Lessons", result.get(0).getString("title"));
    }

    @Test
    public void testFilterCategory_boxing() {
        List<DocumentSnapshot> result = filterEvents("", "category", "Boxing");
        assertEquals(1, result.size());
        assertEquals("Boxing", result.get(0).getString("title"));
    }

    @Test
    public void testFilterCategory_noMatch() {
        List<DocumentSnapshot> result = filterEvents("", "category", "Piano");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterCategory_caseInsensitive() {
        List<DocumentSnapshot> result = filterEvents("", "category", "swimming");
        assertEquals(1, result.size());
    }

    // Search

    @Test
    public void testSearch_matchesPartialTitle() {
        List<DocumentSnapshot> result = filterEvents("yoga", "all", null);
        assertEquals(2, result.size()); // "Yoga Hatha" and "Free Yoga in the Park"
    }

    @Test
    public void testSearch_caseInsensitive() {
        List<DocumentSnapshot> result = filterEvents("BOXING", "all", null);
        assertEquals(1, result.size());
        assertEquals("Boxing", result.get(0).getString("title"));
    }

    @Test
    public void testSearch_noMatch() {
        List<DocumentSnapshot> result = filterEvents("piano", "all", null);
        assertTrue(result.isEmpty());
    }

    //Combined search + filter

    @Test
    public void testSearch_plusOpenFilter() {
        // Search "yoga" + filter "open" → should return 2 (both yoga events are open)
        List<DocumentSnapshot> result = filterEvents("yoga", "open", null);
        assertEquals(2, result.size());
    }

    @Test
    public void testSearch_plusCategoryFilter() {
        // Search "free" + category "Yoga" → matches "Free Yoga in the Park"
        List<DocumentSnapshot> result = filterEvents("free", "category", "Yoga");
        assertEquals(1, result.size());
        assertEquals("Free Yoga in the Park", result.get(0).getString("title"));
    }

    @Test
    public void testSearch_plusCategoryFilter_noOverlap() {
        // Search "boxing" + category "Swimming" → no overlap
        List<DocumentSnapshot> result = filterEvents("boxing", "category", "Swimming");
        assertTrue(result.isEmpty());
    }

    // Edge cases

    @Test
    public void testEmptySearch_emptyFilter() {
        List<DocumentSnapshot> result = filterEvents("", "all", null);
        assertEquals(5, result.size());
    }

    @Test
    public void testWhitespaceSearch_ignored() {
        List<DocumentSnapshot> result = filterEvents("   ", "all", null);
        assertEquals(5, result.size());
    }
}
