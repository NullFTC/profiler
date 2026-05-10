package dev.nullftc.profiler.importer;

import dev.nullftc.profiler.trace.TraceDiagnostic;
import dev.nullftc.profiler.trace.TraceSession;
import dev.nullftc.profiler.trace.TraceSpan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CsvTraceImporter implements TraceImporter {
    @Override
    public TraceSession importTrace(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<TraceSpan> spans = new ArrayList<>();
        List<TraceDiagnostic> diagnostics = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            try {
                if (parts.length >= 7 && "Id".equalsIgnoreCase(lines.get(0).split(",", -1)[0])) {
                    spans.add(new TraceSpan(
                            Long.parseLong(parts[0]),
                            Long.parseLong(parts[1]),
                            parts[2],
                            Long.parseLong(parts[3]),
                            Long.parseLong(parts[4]),
                            parts[6],
                            0L,
                            parseTags(parts.length >= 8 ? parts[7] : "")
                    ));
                } else if (parts.length >= 4) {
                    long startMillis = Long.parseLong(parts[1]);
                    long endMillis = Long.parseLong(parts[2]);
                    spans.add(new TraceSpan(
                            i,
                            0L,
                            parts[0],
                            startMillis * 1_000_000L,
                            endMillis * 1_000_000L,
                            "legacy",
                            0L,
                            singletonMap("source", "legacy-csv")
                    ));
                }
            } catch (RuntimeException exception) {
                diagnostics.add(new TraceDiagnostic("csv_parse_error", "could not parse row " + (i + 1), null));
            }
        }
        return new TraceSession(TraceSession.CURRENT_SCHEMA_VERSION, path.getFileName().toString(), Files.getLastModifiedTime(path).toMillis(), singletonMap("source", "csv"), spans, diagnostics);
    }

    private static Map<String, String> parseTags(String value) {
        if (value == null || value.length() < 2) {
            return Collections.emptyMap();
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return Collections.emptyMap();
        }

        Map<String, String> tags = new LinkedHashMap<>();
        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return tags;
        }

        for (String pair : content.split(",")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                tags.put(parts[0].trim(), parts[1].trim());
            }
        }
        return tags;
    }

    private static Map<String, String> singletonMap(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
