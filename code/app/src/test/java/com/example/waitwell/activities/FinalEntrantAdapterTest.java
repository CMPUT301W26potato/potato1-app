package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, with JUnit 4 setup and assertions.
 *
 * I used Gemini to understand how to test adapter item count and name filtering
 * without needing a full UI render path.
 *
 * Sites I looked at:
 *
 * Testing RecyclerView adapters:
 * https://developer.android.com/guide/topics/ui/layout/recyclerview#test
 */

import static org.junit.Assert.*;

import org.junit.*;

import java.util.Arrays;

/**
 * Tests for {@link FinalEntrantAdapter} count and name filtering behavior.
 * Covers normal list usage and filtered subset behavior.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see FinalEntrantAdapter
 */
public class FinalEntrantAdapterTest {
    private FinalEntrantAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new FinalEntrantAdapter(item -> {});
    }

    @After
    public void tearDown() throws Exception {
        adapter = null;
    }

    /**
     * Checks that getItemCount reflects rows loaded into adapter.
     * This is the happy path.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGetItemCount_returnsCorrectCount() {
        adapter.setItems(Arrays.asList(
                new FinalEntrantAdapter.FinalEntrantItem("u1", "Amy", "d1"),
                new FinalEntrantAdapter.FinalEntrantItem("u2", "Ben", "d2")
        ));
        assertEquals(2, adapter.getItemCount());
    }

    /**
     * Checks that filter keeps rows with matching names only.
     * This is an alternative flow.
     *
     * @author Karina Zhang
     */
    @Test
    public void testSearchFilter_returnsMatchingSubset() {
        adapter.setItems(Arrays.asList(
                new FinalEntrantAdapter.FinalEntrantItem("u1", "Amy", "d1"),
                new FinalEntrantAdapter.FinalEntrantItem("u2", "Ben", "d2")
        ));
        adapter.setFilterQuery("am");
        assertEquals(1, adapter.getItemCount());
    }
}
