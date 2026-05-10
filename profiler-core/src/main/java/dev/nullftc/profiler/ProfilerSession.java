package dev.nullftc.profiler;

import dev.nullftc.profiler.analysis.ProfilerAnalyzer;
import dev.nullftc.profiler.trace.TraceDiagnostic;
import dev.nullftc.profiler.trace.TraceSession;
import dev.nullftc.profiler.trace.TraceSpan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public final class ProfilerSession implements AutoCloseable {
    private final String name;
    private final long createdWallTimeMillis;
    private final long zeroNanos;
    private final AtomicLong nextSpanId = new AtomicLong(1L);
    private final ThreadLocal<Deque<OpenSpan>> openSpans = ThreadLocal.withInitial(ArrayDeque::new);
    private final List<TraceSpan> spans = new ArrayList<>();
    private final List<TraceDiagnostic> diagnostics = new ArrayList<>();
    private final Map<String, String> metadata = new LinkedHashMap<>();
    private volatile boolean closed;

    private ProfilerSession(String name, Map<String, String> metadata) {
        this.name = name;
        this.createdWallTimeMillis = System.currentTimeMillis();
        this.zeroNanos = System.nanoTime();
        this.metadata.putAll(metadata);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public SpanScope span(String name) {
        return span(name, Collections.emptyMap());
    }

    public SpanScope span(String name, Map<String, String> tags) {
        if (closed) {
            diagnostic("session_closed", "span opened after session close: " + name, null);
        }

        Deque<OpenSpan> stack = openSpans.get();
        OpenSpan parent = stack.peek();
        OpenSpan openSpan = new OpenSpan(
                nextSpanId.getAndIncrement(),
                parent == null ? 0L : parent.id,
                name,
                System.nanoTime() - zeroNanos,
                Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                tags
        );
        stack.push(openSpan);
        return new SpanScope(this, openSpan);
    }

    public void time(String name, Runnable runnable) {
        try (SpanScope ignored = span(name)) {
            runnable.run();
        }
    }

    public <T> T time(String name, Callable<T> callable) throws Exception {
        try (SpanScope ignored = span(name)) {
            return callable.call();
        }
    }

    void closeSpan(OpenSpan openSpan) {
        Deque<OpenSpan> stack = openSpans.get();
        OpenSpan current = stack.poll();
        if (current == null) {
            diagnostic("unmatched_close", "span closed with no active span: " + openSpan.name, openSpan.name);
            return;
        }
        if (current.id != openSpan.id) {
            diagnostic("out_of_order_close", "span closed out of order: " + openSpan.name, openSpan.name);
            stack.removeIf(span -> span.id == openSpan.id);
        }

        long endNanos = System.nanoTime() - zeroNanos;
        synchronized (spans) {
            spans.add(new TraceSpan(
                    openSpan.id,
                    openSpan.parentId,
                    openSpan.name,
                    openSpan.startNanos,
                    endNanos,
                    openSpan.threadName,
                    openSpan.threadId,
                    openSpan.tags
            ));
        }
    }

    public void diagnostic(String code, String message, String spanName) {
        synchronized (diagnostics) {
            diagnostics.add(new TraceDiagnostic(code, message, spanName));
        }
    }

    public TraceSession snapshot() {
        synchronized (spans) {
            synchronized (diagnostics) {
                return new TraceSession(
                        TraceSession.CURRENT_SCHEMA_VERSION,
                        name,
                        createdWallTimeMillis,
                        metadata,
                        new ArrayList<>(spans),
                        new ArrayList<>(diagnostics)
                );
            }
        }
    }

    public ProfilerAnalyzer analyzer() {
        return new ProfilerAnalyzer();
    }

    @Override
    public void close() {
        closed = true;
        Deque<OpenSpan> stack = openSpans.get();
        while (!stack.isEmpty()) {
            OpenSpan span = stack.pop();
            diagnostic("unclosed_span", "span was still open at session close: " + span.name, span.name);
        }
    }

    static final class OpenSpan {
        final long id;
        final long parentId;
        final String name;
        final long startNanos;
        final String threadName;
        final long threadId;
        final Map<String, String> tags;

        OpenSpan(long id, long parentId, String name, long startNanos, String threadName, long threadId, Map<String, String> tags) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.startNanos = startNanos;
            this.threadName = threadName;
            this.threadId = threadId;
            this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        }
    }

    public static final class Builder {
        private final String name;
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder metadata(String key, String value) {
            metadata.put(key, value);
            return this;
        }

        public ProfilerSession start() {
            return new ProfilerSession(name, metadata);
        }
    }
}
