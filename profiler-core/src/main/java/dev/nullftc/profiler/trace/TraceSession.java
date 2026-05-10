package dev.nullftc.profiler.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TraceSession {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private final int schemaVersion;
    private final String name;
    private final long createdWallTimeMillis;
    private final Map<String, String> metadata;
    private final List<TraceSpan> spans;
    private final List<TraceDiagnostic> diagnostics;

    public TraceSession(int schemaVersion, String name, long createdWallTimeMillis, Map<String, String> metadata, List<TraceSpan> spans, List<TraceDiagnostic> diagnostics) {
        this.schemaVersion = schemaVersion;
        this.name = name;
        this.createdWallTimeMillis = createdWallTimeMillis;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        this.spans = Collections.unmodifiableList(new ArrayList<>(spans));
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getName() {
        return name;
    }

    public long getCreatedWallTimeMillis() {
        return createdWallTimeMillis;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public List<TraceSpan> getSpans() {
        return spans;
    }

    public List<TraceDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
