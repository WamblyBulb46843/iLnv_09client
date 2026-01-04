package dev.iLnv_09.core.impl;

import java.util.ArrayDeque;
import java.util.Queue;

public class FPSManager {
    private final Queue<Long> records = new ArrayDeque<>();

    public void record() {
        records.offer(System.currentTimeMillis());
    }

    public int getFps() {
        final long now = System.currentTimeMillis();
        while (!records.isEmpty() && records.peek() < now - 1000L) {
            records.poll();
        }
        return records.size();
    }
}
