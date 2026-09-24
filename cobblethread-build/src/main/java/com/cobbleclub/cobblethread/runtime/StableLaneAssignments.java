package com.cobbleclub.cobblethread.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Assigns each key to one stable serial lane for the lifetime of the scheduler. */
public final class StableLaneAssignments<K> {
    private final int laneCount;
    private final AtomicInteger nextLane = new AtomicInteger();
    private final Map<K, Integer> assignments = new ConcurrentHashMap<>();

    public StableLaneAssignments(int laneCount) {
        if (laneCount < 1) throw new IllegalArgumentException("laneCount must be positive");
        this.laneCount = laneCount;
    }

    public int laneFor(K key) {
        if (key == null) throw new NullPointerException("key");
        return assignments.computeIfAbsent(key,
            ignored -> Math.floorMod(nextLane.getAndIncrement(), laneCount));
    }

    public int size() {
        return assignments.size();
    }

    public void clear() {
        assignments.clear();
        nextLane.set(0);
    }
}
