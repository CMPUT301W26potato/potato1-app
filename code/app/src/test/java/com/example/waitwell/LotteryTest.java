package com.example.waitwell;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

/**
 * Rehaan's addition for US 02.05.02:
 * Unit tests for the Lottery sampling engine.

 */
public class LotteryTest {

    @Test
    public void testSampleReturnsCorrectCount() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3", "user4", "user5");
        List<String> result = Lottery.sample(waiting, 3);
        // Happy path: asking for 3 results should give exactly 3.
        assertEquals(3, result.size());
    }

    @Test
    public void testSampleAllWhenSampleSizeLargerThanList() {
        List<String> waiting = Arrays.asList("user1", "user2");
        List<String> result = Lottery.sample(waiting, 10);
        // If we ask for more than exist, we just get the whole list back.
        assertEquals(2, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForNullInput() {
        List<String> result = Lottery.sample(null, 3);
        // Null waiting list should behave like "no entrants" instead of crashing.
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForEmptyList() {
        List<String> result = Lottery.sample(Arrays.asList(), 3);
        // Empty list in should give an empty list out.
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForZeroSampleSize() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        List<String> result = Lottery.sample(waiting, 0);
        // Asking for zero winners should give zero results.
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForNegativeSampleSize() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        List<String> result = Lottery.sample(waiting, -1);
        // Negative sample sizes are treated like "no sampling".
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleOnlyContainsValidUserIds() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3", "user4", "user5");
        List<String> result = Lottery.sample(waiting, 3);
        for (String id : result) {
            // Every picked id must have come from the original waiting list.
            assertTrue(waiting.contains(id));
        }
    }

    @Test
    public void testSampleDoesNotModifyOriginalList() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        int originalSize = waiting.size();
        Lottery.sample(waiting, 2);
        // The helper should never mutate the source list.
        assertEquals(originalSize, waiting.size());
    }

    @Test
    public void testSampleWithExactSizeMatch() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        List<String> result = Lottery.sample(waiting, 3);
        // If we ask for exactly the list size, we should see all of them.
        assertEquals(3, result.size());
    }

    @Test
    public void testSampleNoDuplicates() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3", "user4", "user5");
        List<String> result = Lottery.sample(waiting, 5);
        long distinctCount = result.stream().distinct().count();
        // Even when we ask for a lot of winners we should not see duplicates.
        assertEquals(result.size(), distinctCount);
    }
}