package com.example.waitwell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lottery.java
 * Core lottery sampling engine for WaitWell.
 * Randomly picks entrants from a waiting list for an event.
 * No Android or Firebase dependencies so its easy to unit test.
 * Used by US 02.05.02 and US 02.05.03.
 * Javadoc written with help from Claude (claude.ai)
 *
 * @author rehaan299
 */
public class Lottery {

    /**
     * Randomly picks up to sampleSize user IDs from the waiting list.
     * If sampleSize is bigger than the list, everyone gets selected.
     * The original list is never touched.
     *
     * Shuffling approach referenced from:
     * https://stackoverflow.com/questions/16000196/java-collections-shuffle
     *
     * @param waitingUserIds list of user IDs with waiting status
     * @param sampleSize     how many entrants the organizer wants to select
     * @return new list of randomly selected user IDs, empty if input is invalid
     */
    public static List<String> sample(List<String> waitingUserIds, int sampleSize) {
        if (waitingUserIds == null || waitingUserIds.isEmpty() || sampleSize <= 0) {
            return new ArrayList<>();
        }

        List<String> shuffled = new ArrayList<>(waitingUserIds);
        Collections.shuffle(shuffled);

        int count = Math.min(sampleSize, shuffled.size());
        return new ArrayList<>(shuffled.subList(0, count));
    }
}