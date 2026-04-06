package com.example.waitwell;

/*
 * The general structure of these tests follows the same style as the
 * Lab 6 Espresso example from the course, but this file tests adapter logic in JUnit 4.
 *
 * I used Gemini to get my head around testing RecyclerView adapter behavior
 * through item-count checks without spinning up a full RecyclerView screen.
 *
 * Sites I looked at:
 *
 * Testing RecyclerView adapters:
 * https://developer.android.com/guide/topics/ui/layout/recyclerview#test
 */

import static org.junit.Assert.*;

import org.junit.*;

import java.util.ArrayList;

/**
 * Tests for {@link NotificationAdapter} with JVM-safe adapter surface checks.
 * Covers count behavior for empty and non-empty model lists.
 *
 * @author Karina Zhang
 * @version 1.0
 * @see NotificationAdapter
 */
public class NotificationAdapterTest {
    private NotificationAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new NotificationAdapter(new ArrayList<>());
    }

    @After
    public void tearDown() throws Exception {
        adapter = null;
    }

    /**
     * Checks that empty list gives zero item count.
     * This is a boundary case.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGetItemCount_emptyList_returnsZero() {
        assertEquals(0, adapter.getItemCount());
    }

    /**
     * Checks that non-empty list gives matching item count.
     * This is the happy path.
     *
     * @author Karina Zhang
     */
    @Test
    public void testGetItemCount_nonEmptyList_returnsSize() {
        ArrayList<NotificationModel> list = new ArrayList<>();
        list.add(new NotificationModel("e1", "Event", "msg", "View", NotificationModel.NotificationType.CHOSEN));
        NotificationAdapter nonEmpty = new NotificationAdapter(list);
        assertEquals(1, nonEmpty.getItemCount());
    }
}
