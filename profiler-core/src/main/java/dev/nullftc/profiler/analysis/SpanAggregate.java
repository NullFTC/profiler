package dev.nullftc.profiler.analysis;

import dev.nullftc.profiler.trace.TraceSpan;

public final class SpanAggregate {
    private final String name;
    private int count;
    private long totalNanos;
    private long maxNanos;

    public SpanAggregate(String name) {
        this.name = name;
    }

    public void add(TraceSpan span) {
        count++;
        totalNanos += span.getDurationNanos();
        maxNanos = Math.max(maxNanos, span.getDurationNanos());
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public double getTotalMillis() {
        return totalNanos / 1_000_000.0;
    }

    public double getAverageMillis() {
        return count == 0 ? 0.0 : getTotalMillis() / count;
    }

    public double getMaxMillis() {
        return maxNanos / 1_000_000.0;
    }
}
