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
        assertEquals(3, result.size());
    }

    @Test
    public void testSampleAllWhenSampleSizeLargerThanList() {
        List<String> waiting = Arrays.asList("user1", "user2");
        List<String> result = Lottery.sample(waiting, 10);
        assertEquals(2, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForNullInput() {
        List<String> result = Lottery.sample(null, 3);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForEmptyList() {
        List<String> result = Lottery.sample(Arrays.asList(), 3);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForZeroSampleSize() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        List<String> result = Lottery.sample(waiting, 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleReturnsEmptyForNegativeSampleSize() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        List<String> result = Lottery.sample(waiting, -1);
        assertEquals(0, result.size());
    }

    @Test
    public void testSampleOnlyContainsValidUserIds() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3", "user4", "user5");
        List<String> result = Lottery.sample(waiting, 3);
        for (String id : result) {
            assertTrue(waiting.contains(id));
        }
    }

    @Test
    public void testSampleDoesNotModifyOriginalList() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        int originalSize = waiting.size();
        Lottery.sample(waiting, 2);
        assertEquals(originalSize, waiting.size());
    }

    @Test
    public void testSampleWithExactSizeMatch() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3");
        List<String> result = Lottery.sample(waiting, 3);
        assertEquals(3, result.size());
    }

    @Test
    public void testSampleNoDuplicates() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3", "user4", "user5");
        List<String> result = Lottery.sample(waiting, 5);
        long distinctCount = result.stream().distinct().count();
        assertEquals(result.size(), distinctCount);
    }
    // US 02.05.03 - replacement draw should always pick exactly one person
    @Test
    public void testDrawReplacementSelectsExactlyOne() {
        List<String> waiting = Arrays.asList("user1", "user2", "user3", "user4");
        List<String> result = Lottery.sample(waiting, 1);
        assertEquals(1, result.size());
        assertTrue(waiting.contains(result.get(0)));
    }
}