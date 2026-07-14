package com.sgx.signature.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LatencyRecorder {
    private final List<Long> latencies = new ArrayList<>();

    public synchronized void record(long latencyMs) {
        latencies.add(latencyMs);
    }

    public synchronized long getAverage() {
        if (latencies.isEmpty()) return 0;
        long sum = 0;
        for (long l : latencies) sum += l;
        return sum / latencies.size();
    }

    public synchronized long getPercentile(double percentile) {
        if (latencies.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        if (index < 0) index = 0;
        return sorted.get(index);
    }
}
