package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, but this file focuses on adapter unit tests.
 *
 * I used Gemini to get my head around testing RecyclerView adapter filtering logic
 * without needing a real Android context or rendered list.
 *
 * Sites I looked at:
 *
 * Testing RecyclerView adapters:
 * https://developer.android.com/guide/topics/ui/layout/recyclerview#test
 */

import static org.junit.Assert.*;

import org.junit.*;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link WaitlistEntrantAdapter} around count and filtering behavior.
 * Covers happy path and basic boundary cases with empty filters.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see WaitlistEntrantAdapter
 */
public class WaitlistEntrantAdapterTest {
    private WaitlistEntrantAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new WaitlistEntrantAdapter(new WaitlistEntrantAdapter.Listener() {
            @Override public void onViewProfile(WaitlistEntrantAdapter.WaitlistEntrantItem item) {}
            @Override public void onAccept(WaitlistEntrantAdapter.WaitlistEntrantItem item) {}
            @Override public void onDecline(WaitlistEntrantAdapter.WaitlistEntrantItem item) {}
        });
    }

    @After
    public void tearDown() throws Exception {
        adapter = null;
    }

    /**
     * Checks that item count matches number of rows after setItems.
     * This is the happy path.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGetItemCount_returnsCorrectCount() {
        adapter.setItems(Arrays.asList(
                new WaitlistEntrantAdapter.WaitlistEntrantItem("u1", "Amy", "d1"),
                new WaitlistEntrantAdapter.WaitlistEntrantItem("u2", "Bob", "d2")
        ));
        assertEquals(2, adapter.getItemCount());
    }

    /**
     * Checks that search filter keeps only names containing the query.
     * This is an alternative flow.
     *
     * @author Karina Zhang
     */
    @Test
    public void testSearchFilter_returnsMatchingNameOnly() {
        adapter.setItems(Arrays.asList(
                new WaitlistEntrantAdapter.WaitlistEntrantItem("u1", "Alice", "d1"),
                new WaitlistEntrantAdapter.WaitlistEntrantItem("u2", "Bob", "d2")
        ));
        adapter.setFilterQuery("ali");
        List<WaitlistEntrantAdapter.WaitlistEntrantItem> visible = adapter.getVisibleItemsSnapshot();
        assertEquals(1, visible.size());
        assertEquals("Alice", visible.get(0).displayName);
    }
}
