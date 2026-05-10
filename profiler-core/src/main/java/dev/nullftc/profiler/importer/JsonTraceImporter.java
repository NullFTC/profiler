package dev.nullftc.profiler.importer;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonTraceImporter implements TraceImporter {
    private static final Pattern SPAN_PATTERN = Pattern.compile("\\{\\\"id\\\":(\\d+),\\\"parentId\\\":(\\d+),\\\"name\\\":\\\"(.*?)\\\",\\\"startNanos\\\":(\\d+),\\\"endNanos\\\":(\\d+),\\\"threadName\\\":\\\"(.*?)\\\",\\\"threadId\\\":(\\d+),\\\"tags\\\":\\{.*?}}");
    private static final Pattern NAME_PATTERN = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"(.*?)\\\"");
    private static final Pattern CREATED_PATTERN = Pattern.compile("\\\"createdWallTimeMillis\\\"\\s*:\\s*(\\d+)");

    @Override
    public TraceSession importTrace(Path path) throws IOException {
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        List<TraceSpan> spans = new ArrayList<>();
        Matcher matcher = SPAN_PATTERN.matcher(json);
        while (matcher.find()) {
            spans.add(new TraceSpan(
                    Long.parseLong(matcher.group(1)),
                    Long.parseLong(matcher.group(2)),
                    unescape(matcher.group(3)),
                    Long.parseLong(matcher.group(4)),
                    Long.parseLong(matcher.group(5)),
                    unescape(matcher.group(6)),
                    Long.parseLong(matcher.group(7)),
                    Collections.emptyMap()
            ));
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "json");
        return new TraceSession(TraceSession.CURRENT_SCHEMA_VERSION, match(NAME_PATTERN, json, path.getFileName().toString()), Long.parseLong(match(CREATED_PATTERN, json, "0")), metadata, spans, Collections.emptyList());
    }

    private static String match(Pattern pattern, String value, String fallback) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? unescape(matcher.group(1)) : fallback;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }
}
