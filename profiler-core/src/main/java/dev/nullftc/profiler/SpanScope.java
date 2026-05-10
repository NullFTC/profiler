package dev.nullftc.profiler;

public final class SpanScope implements AutoCloseable {
    private final ProfilerSession session;
    private final ProfilerSession.OpenSpan openSpan;
    private boolean closed;

    SpanScope(ProfilerSession session, ProfilerSession.OpenSpan openSpan) {
        this.session = session;
        this.openSpan = openSpan;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        session.closeSpan(openSpan);
    }
}
