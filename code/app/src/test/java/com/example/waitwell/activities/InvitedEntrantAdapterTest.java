package com.example.waitwell.activities;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, while keeping adapter checks in JUnit 4.
 *
 * I used Gemini to understand how to test status-filter combinations and selection state
 * in a RecyclerView adapter without a live RecyclerView screen.
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
 * Tests for {@link InvitedEntrantAdapter} filter and selection behavior.
 * Covers enrolled/cancelled/pending filtering plus checked-rows output.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see InvitedEntrantAdapter
 */
public class InvitedEntrantAdapterTest {
    private InvitedEntrantAdapter adapter;
    private int selectionChangedCalls;

    @Before
    public void setUp() throws Exception {
        selectionChangedCalls = 0;
        adapter = new InvitedEntrantAdapter(new InvitedEntrantAdapter.Listener() {
            @Override public void onViewProfile(InvitedEntrantAdapter.InvitedEntrantItem item) {}
            @Override public void onSelectionChanged() { selectionChangedCalls++; }
        });
        adapter.setStatusConstants("selected", "confirmed", "cancelled");
    }

    @After
    public void tearDown() throws Exception {
        adapter = null;
    }

    /**
     * Checks that enrolled filter keeps only confirmed-status rows.
     * This is an alternative flow.
     *
     * @author Karina Zhang
     */
    @Test
    public void testFilterByEnrolled_returnsOnlyConfirmed() {
        adapter.setItems(Arrays.asList(
                new InvitedEntrantAdapter.InvitedEntrantItem("u1", "A", "d1", "confirmed"),
                new InvitedEntrantAdapter.InvitedEntrantItem("u2", "B", "d2", "cancelled"),
                new InvitedEntrantAdapter.InvitedEntrantItem("u3", "C", "d3", "selected")
        ));
        adapter.setStatusFilters(true, false, false);
        assertEquals(1, adapter.getItemCount());
    }

    /**
     * Checks that pending filter keeps selected-status rows.
     * This is an alternative flow.
     *
     * @author Karina Zhang
     */
    @Test
    public void testFilterByPending_returnsOnlySelected() {
        adapter.setItems(Arrays.asList(
                new InvitedEntrantAdapter.InvitedEntrantItem("u1", "A", "d1", "confirmed"),
                new InvitedEntrantAdapter.InvitedEntrantItem("u3", "C", "d3", "selected")
        ));
        adapter.setStatusFilters(false, false, true);
        assertEquals(1, adapter.getItemCount());
    }

    /**
     * Checks that getSelectedEntrants is empty before any checkbox interaction.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGetSelectedEntrants_noCheckedItems_returnsEmptyList() {
        adapter.setItems(List.of(new InvitedEntrantAdapter.InvitedEntrantItem("u1", "A", "d1", "selected")));
        assertTrue(adapter.getSelectedEntrants().isEmpty());
        assertTrue(selectionChangedCalls > 0);
    }
}
