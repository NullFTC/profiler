package dev.nullftc.profiler.trace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TraceSpan {
    private final long id;
    private final long parentId;
    private final String name;
    private final long startNanos;
    private final long endNanos;
    private final String threadName;
    private final long threadId;
    private final Map<String, String> tags;

    public TraceSpan(long id, long parentId, String name, long startNanos, long endNanos, String threadName, long threadId, Map<String, String> tags) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.startNanos = startNanos;
        this.endNanos = endNanos;
        this.threadName = threadName;
        this.threadId = threadId;
        this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
    }

    public long getId() {
        return id;
    }

    public long getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getEndNanos() {
        return endNanos;
    }

    public long getDurationNanos() {
        return Math.max(0L, endNanos - startNanos);
    }

    public double getDurationMillis() {
        return getDurationNanos() / 1_000_000.0;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    public Map<String, String> getTags() {
        return tags;
    }
}
