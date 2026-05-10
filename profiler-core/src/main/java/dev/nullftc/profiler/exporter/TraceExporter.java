package dev.nullftc.profiler.exporter;

import dev.nullftc.profiler.trace.TraceSession;

import java.io.IOException;

public interface TraceExporter {
    void export(TraceSession session) throws IOException;
}
