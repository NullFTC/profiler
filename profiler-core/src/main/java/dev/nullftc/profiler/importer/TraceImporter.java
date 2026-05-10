package dev.nullftc.profiler.importer;

import dev.nullftc.profiler.trace.TraceSession;

import java.io.IOException;
import java.nio.file.Path;

public interface TraceImporter {
    TraceSession importTrace(Path path) throws IOException;
}
