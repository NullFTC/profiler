package dev.nullftc.profiler.exporter;

import dev.nullftc.profiler.trace.TraceDiagnostic;
import dev.nullftc.profiler.trace.TraceSession;
import dev.nullftc.profiler.trace.TraceSpan;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class JsonTraceExporter implements TraceExporter {
    private final Path outputPath;

    public JsonTraceExporter(Path outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void export(TraceSession session) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("{\n");
            field(writer, "schemaVersion", String.valueOf(session.getSchemaVersion()), true);
            field(writer, "name", quote(session.getName()), true);
            field(writer, "createdWallTimeMillis", String.valueOf(session.getCreatedWallTimeMillis()), true);
            writer.write("  \"metadata\": ");
            writeMap(writer, session.getMetadata());
            writer.write(",\n");
            writer.write("  \"spans\": [\n");
            for (int i = 0; i < session.getSpans().size(); i++) {
                writeSpan(writer, session.getSpans().get(i));
                writer.write(i == session.getSpans().size() - 1 ? "\n" : ",\n");
            }
            writer.write("  ],\n");
            writer.write("  \"diagnostics\": [\n");
            for (int i = 0; i < session.getDiagnostics().size(); i++) {
                writeDiagnostic(writer, session.getDiagnostics().get(i));
                writer.write(i == session.getDiagnostics().size() - 1 ? "\n" : ",\n");
            }
            writer.write("  ]\n");
            writer.write("}\n");
        }
    }

    private static void field(BufferedWriter writer, String name, String value, boolean trailingComma) throws IOException {
        writer.write("  \"" + name + "\": " + value + (trailingComma ? "," : "") + "\n");
    }

    private static void writeSpan(BufferedWriter writer, TraceSpan span) throws IOException {
        writer.write("    {");
        writer.write("\"id\":" + span.getId());
        writer.write(",\"parentId\":" + span.getParentId());
        writer.write(",\"name\":" + quote(span.getName()));
        writer.write(",\"startNanos\":" + span.getStartNanos());
        writer.write(",\"endNanos\":" + span.getEndNanos());
        writer.write(",\"threadName\":" + quote(span.getThreadName()));
        writer.write(",\"threadId\":" + span.getThreadId());
        writer.write(",\"tags\":");
        writeMap(writer, span.getTags());
        writer.write("}");
    }

    private static void writeDiagnostic(BufferedWriter writer, TraceDiagnostic diagnostic) throws IOException {
        writer.write("    {");
        writer.write("\"code\":" + quote(diagnostic.getCode()));
        writer.write(",\"message\":" + quote(diagnostic.getMessage()));
        writer.write(",\"spanName\":" + quote(diagnostic.getSpanName()));
        writer.write("}");
    }

    private static void writeMap(BufferedWriter writer, Map<String, String> values) throws IOException {
        writer.write("{");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            writer.write(quote(entry.getKey()));
            writer.write(":");
            writer.write(quote(entry.getValue()));
            if (++index < values.size()) {
                writer.write(",");
            }
        }
        writer.write("}");
    }

    static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(c);
            }
        }
        builder.append('"');
        return builder.toString();
    }
}
