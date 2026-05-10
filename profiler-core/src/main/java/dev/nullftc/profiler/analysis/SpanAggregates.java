package dev.nullftc.profiler.analysis;

import dev.nullftc.profiler.trace.TraceSession;
import dev.nullftc.profiler.trace.TraceSpan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpanAggregates {
    private SpanAggregates() {
    }

    public static List<SpanAggregate> byName(TraceSession session) {
        Map<String, SpanAggregate> aggregates = new LinkedHashMap<>();
        for (TraceSpan span : session.getSpans()) {
            aggregates.computeIfAbsent(span.getName(), SpanAggregate::new).add(span);
        }
        List<SpanAggregate> result = new ArrayList<>(aggregates.values());
        result.sort(Comparator.comparingDouble(SpanAggregate::getTotalMillis).reversed());
        return result;
    }
}
