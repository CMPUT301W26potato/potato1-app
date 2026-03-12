package com.example.waitwell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rehaan's addition for 02.05.02
 * Core lottery sampling engine.
 * Handles random selection of entrants from a waiting list.
 * Used by US 02.05.02 (Execute Lottery Sampling) and
 * US 02.05.03 (Draw Replacement Applicants).
 */
public class Lottery {

    /**
     * Randomly samples user IDs from the provided waiting list.
     * If sampleSize ? size selected, all entrants are selected.
     * The original list is never modified.
     *
     * @param waitingUserIds  which is the list of user IDs currently with "waiting" status
     * @param sampleSize   which is the number of entrants the organizer wants to select
     * @return a new list of randomly selected user IDs
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