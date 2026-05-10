package dev.nullftc.profiler.importer;

import dev.nullftc.profiler.trace.TraceSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public final class TraceImporters {
    private TraceImporters() {
    }

    public static TraceSession load(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.US);
        if (name.endsWith(".csv")) {
            return new CsvTraceImporter().importTrace(path);
        }
        return new JsonTraceImporter().importTrace(path);
    }
}
