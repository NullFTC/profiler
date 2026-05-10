package dev.nullftc.profiler.exporter;

import dev.nullftc.profiler.trace.TraceSession;
import dev.nullftc.profiler.trace.TraceSpan;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvTraceExporter implements TraceExporter {
    private final Path outputPath;

    public CsvTraceExporter(Path outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void export(TraceSession session) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("Id,Parent Id,Name,Start Nanos,End Nanos,Duration Nanos,Thread,Tags\n");
            for (TraceSpan span : session.getSpans()) {
                writer.write(span.getId() + ",");
                writer.write(span.getParentId() + ",");
                writer.write(escape(span.getName()) + ",");
                writer.write(span.getStartNanos() + ",");
                writer.write(span.getEndNanos() + ",");
                writer.write(span.getDurationNanos() + ",");
                writer.write(escape(span.getThreadName()) + ",");
                writer.write(escape(span.getTags().toString()));
                writer.write("\n");
            }
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
