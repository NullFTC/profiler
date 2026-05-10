package dev.nullftc.profiler.viewer;

import dev.nullftc.profiler.analysis.AnalysisFinding;
import dev.nullftc.profiler.analysis.ProfilerAnalyzer;
import dev.nullftc.profiler.analysis.SpanAggregates;
import dev.nullftc.profiler.importer.TraceImporters;
import dev.nullftc.profiler.trace.TraceSession;

import java.nio.file.Path;

public final class TraceValidationMain {
    private TraceValidationMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: TraceValidationMain <trace.json|trace.csv>");
        }

        TraceSession session = TraceImporters.load(Path.of(args[0]));
        int aggregateCount = SpanAggregates.byName(session).size();
        int findingCount = new ProfilerAnalyzer().analyze(session).getFindings().size();
        System.out.println("Loaded trace: " + session.getName());
        System.out.println("Spans: " + session.getSpans().size());
        System.out.println("Aggregates: " + aggregateCount);
        System.out.println("Findings: " + findingCount);

        for (AnalysisFinding finding : new ProfilerAnalyzer().analyze(session).getFindings()) {
            System.out.println(finding.getSeverity() + ": " + finding.getTitle() + " - " + finding.getEvidence());
            System.out.println("  Recommendation: " + finding.getRecommendation());
        }

        if (session.getSpans().isEmpty()) {
            throw new IllegalStateException("Trace contained no spans.");
        }
    }
}
