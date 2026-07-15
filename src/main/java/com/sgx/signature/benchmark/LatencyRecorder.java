package com.sgx.signature.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LatencyRecorder {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private final List<Long> latenciesMicros = new ArrayList<>();

    public synchronized void recordMicros(long latencyMicros) {
        latenciesMicros.add(latencyMicros);
    }

    public synchronized double getAverageMillis() {
        if (latenciesMicros.isEmpty()) return 0;
        long sum = 0;
        for (long latency : latenciesMicros) sum += latency;
        return (sum / (double) latenciesMicros.size()) / 1_000.0;
    }

    public synchronized double getPercentileMillis(double percentile) {
        if (latenciesMicros.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(latenciesMicros);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        if (index < 0) index = 0;
        return sorted.get(index) / 1_000.0;
    }
}
