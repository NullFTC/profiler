package dev.nullftc.profiler.analysis;

import dev.nullftc.profiler.trace.TraceDiagnostic;
import dev.nullftc.profiler.trace.TraceSession;
import dev.nullftc.profiler.trace.TraceSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProfilerAnalyzer {
    private static final double FTC_LOOP_BUDGET_MILLIS = 20.0;
    private static final double SLOW_SPAN_MILLIS = 10.0;
    private static final double REPEATED_COST_MILLIS = 2.0;

    public AnalysisReport analyze(TraceSession session) {
        AnalysisContext context = new AnalysisContext(session);
        List<AnalysisFinding> findings = new ArrayList<>();
        addDiagnosticFindings(session, findings);
        addLoopFindings(context, findings);
        addSlowSpanFindings(context, findings);
        addAggregateFindings(context, findings);
        return new AnalysisReport(findings);
    }

    private static void addDiagnosticFindings(TraceSession session, List<AnalysisFinding> findings) {
        for (TraceDiagnostic diagnostic : session.getDiagnostics()) {
            findings.add(new AnalysisFinding(
                    AnalysisSeverity.WARNING,
                    "Profiler trace diagnostic: " + diagnostic.getCode(),
                    diagnostic.getMessage(),
                    "Fix instrumentation, then re-run the trace."
            ));
        }
    }

    private static void addLoopFindings(AnalysisContext context, List<AnalysisFinding> findings) {
        context.session.getSpans().stream()
                .filter(context::isLoopSpan)
                .filter(span -> span.getDurationMillis() >= FTC_LOOP_BUDGET_MILLIS)
                .sorted(Comparator.comparingLong(TraceSpan::getDurationNanos).reversed())
                .limit(3)
                .forEach(span -> {
                    TraceSpan child = context.slowestChild(span);
                    String evidence = String.format(
                            "%s took %.3f ms; slowest child is %s at %.3f ms",
                            span.getName(),
                            span.getDurationMillis(),
                            child == null ? "none" : child.getName(),
                            child == null ? 0.0 : child.getDurationMillis()
                    );
                    findings.add(new AnalysisFinding(
                            AnalysisSeverity.CRITICAL,
                            "Likely loop overrun: " + span.getName(),
                            evidence,
                            child == null
                                    ? "Add child spans around scheduler, drivetrain, localizer, telemetry, hardware I/O, and vision."
                                    : recommendationFor(child, context, true)
                    ));
                });
    }

    private static void addSlowSpanFindings(AnalysisContext context, List<AnalysisFinding> findings) {
        Set<Long> reported = new HashSet<>();
        context.session.getSpans().stream()
                .sorted(Comparator.comparingLong(TraceSpan::getDurationNanos).reversed())
                .limit(8)
                .filter(span -> span.getDurationMillis() >= SLOW_SPAN_MILLIS)
                .filter(span -> !context.isLoopSpan(span))
                .forEach(span -> {
                    if (!reported.add(span.getId())) {
                        return;
                    }
                    findings.add(new AnalysisFinding(
                            span.getDurationMillis() >= FTC_LOOP_BUDGET_MILLIS ? AnalysisSeverity.CRITICAL : AnalysisSeverity.WARNING,
                            "Likely " + categoryFor(span).toLowerCase(Locale.US) + " cost: " + span.getName(),
                            evidenceFor(span, context),
                            recommendationFor(span, context, false)
                    ));
                });
    }

    private static void addAggregateFindings(AnalysisContext context, List<AnalysisFinding> findings) {
        context.aggregates.entrySet().stream()
                .filter(entry -> entry.getValue().count >= 3)
                .sorted((left, right) -> Long.compare(right.getValue().totalNanos, left.getValue().totalNanos))
                .limit(5)
                .forEach(entry -> {
                    Aggregate aggregate = entry.getValue();
                    double averageMillis = aggregate.averageMillis();
                    if (averageMillis >= REPEATED_COST_MILLIS) {
                        TraceSpan representative = aggregate.slowestSpan;
                        findings.add(new AnalysisFinding(
                                averageMillis >= SLOW_SPAN_MILLIS ? AnalysisSeverity.WARNING : AnalysisSeverity.INFO,
                                "Recurring " + categoryFor(representative).toLowerCase(Locale.US) + " cost: " + entry.getKey(),
                                String.format("%s ran %d times with %.3f ms average and %.3f ms max", entry.getKey(), aggregate.count, averageMillis, aggregate.maxMillis()),
                                repeatedRecommendationFor(representative, aggregate, context)
                        ));
                    }
                });
    }

    private static String evidenceFor(TraceSpan span, AnalysisContext context) {
        String parent = context.parent(span) == null ? "root" : context.parent(span).getName();
        String tags = span.getTags().isEmpty() ? "none" : span.getTags().toString();
        return String.format("%s took %.3f ms on thread %s under %s; tags=%s", span.getName(), span.getDurationMillis(), span.getThreadName(), parent, tags);
    }

    private static String recommendationFor(TraceSpan span, AnalysisContext context, boolean causedLoopOverrun) {
        SpanDomain domain = SpanDomain.from(span);
        String prefix = causedLoopOverrun ? "Largest measured contributor. " : "";
        switch (domain) {
            case TELEMETRY:
                return prefix + "Check telemetry cadence and formatting. Lower the update rate or publish only changed fields.";
            case VISION:
                return prefix + "Check vision pipeline cost. Try async processing or lower frame size/rate. Split thresholding, contour detection, and pose estimation into spans.";
            case LOCALIZATION:
                return prefix + "Split encoder reads, IMU reads, and pose math. Separate SDK I/O from pose calculation.";
            case DRIVE:
                return prefix + "Check per-loop trajectory/path recomputation. Split localizer, controller math, and motor writes.";
            case SCHEDULER:
                return prefix + "Add spans around each command/subsystem execute path. Check blocking waits and hardware calls in execute().";
            case HARDWARE:
                return prefix + "Check duplicate reads and bulk reads. Group writes after control math.";
            case LOOP:
                TraceSpan child = context.slowestChild(span);
                return child == null
                        ? prefix + "Split the loop into scheduler, drivetrain, localizer, telemetry, hardware I/O, and vision spans."
                        : recommendationFor(child, context, causedLoopOverrun);
            default:
                return prefix + "Add a subsystem tag. Split I/O, allocation-heavy math, blocking calls, and library calls into spans.";
        }
    }

    private static String repeatedRecommendationFor(TraceSpan span, Aggregate aggregate, AnalysisContext context) {
        switch (SpanDomain.from(span)) {
            case LOOP:
                TraceSpan child = context.slowestChildAcross(aggregate.spans);
                return child == null
                        ? "Add child spans around scheduler, drivetrain, localizer, telemetry, hardware I/O, and vision."
                        : "Largest observed child: " + child.getName() + " at " + String.format("%.3f", child.getDurationMillis()) + " ms. " + recommendationFor(child, context, false);
            case TELEMETRY:
                return "Lower telemetry cadence or batch fields. Compare another trace.";
            case VISION:
                return "Try async vision processing or lower per-frame work. Keep latest-result consumption in the OpMode loop.";
            case LOCALIZATION:
                return "Verify bulk caching. Check repeated pose calculations in the same loop.";
            case DRIVE:
                return "Separate planning from control updates. Check trajectory or constraint regeneration.";
            case SCHEDULER:
                return "Add child spans around each scheduled command/subsystem.";
            case HARDWARE:
                return "Cache reads for the loop. Check duplicate get/set calls.";
            default:
                return recommendationFor(span, context, false);
        }
    }

    private static String categoryFor(TraceSpan span) {
        switch (SpanDomain.from(span)) {
            case TELEMETRY:
                return "Telemetry";
            case VISION:
                return "Vision";
            case LOCALIZATION:
                return "Localization";
            case DRIVE:
                return "Drivetrain";
            case SCHEDULER:
                return "Scheduler";
            case HARDWARE:
                return "Hardware I/O";
            case LOOP:
                return "Loop";
            default:
                return "Unclassified";
        }
    }

    private enum SpanDomain {
        TELEMETRY,
        VISION,
        LOCALIZATION,
        DRIVE,
        SCHEDULER,
        HARDWARE,
        LOOP,
        UNKNOWN;

        static SpanDomain from(TraceSpan span) {
            String haystack = (span.getName() + " " + span.getThreadName() + " " + span.getTags()).toLowerCase(Locale.US);
            if (containsAny(haystack, "telemetry", "dashboard")) {
                return TELEMETRY;
            }
            if (containsAny(haystack, "vision", "camera", "opencv", "pipeline", "april", "tfod")) {
                return VISION;
            }
            if (containsAny(haystack, "localiz", "pose", "imu", "odometr", "encoder")) {
                return LOCALIZATION;
            }
            if (containsAny(haystack, "drive", "mecanum", "swerve", "trajectory", "path", "follower")) {
                return DRIVE;
            }
            if (containsAny(haystack, "scheduler", "command")) {
                return SCHEDULER;
            }
            if (containsAny(haystack, "hardware", "motor", "servo", "sensor", "bulk", "read", "write", "i/o", "io")) {
                return HARDWARE;
            }
            if (containsAny(haystack, "opmode.loop", "loop") || "loop".equalsIgnoreCase(span.getTags().get("phase"))) {
                return LOOP;
            }
            return UNKNOWN;
        }

        private static boolean containsAny(String value, String... needles) {
            for (String needle : needles) {
                if (value.contains(needle)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class AnalysisContext {
        final TraceSession session;
        final Map<Long, TraceSpan> spansById = new HashMap<>();
        final Map<Long, List<TraceSpan>> childrenByParentId = new HashMap<>();
        final Map<String, Aggregate> aggregates = new HashMap<>();

        AnalysisContext(TraceSession session) {
            this.session = session;
            for (TraceSpan span : session.getSpans()) {
                spansById.put(span.getId(), span);
                childrenByParentId.computeIfAbsent(span.getParentId(), ignored -> new ArrayList<>()).add(span);
                aggregates.computeIfAbsent(span.getName(), ignored -> new Aggregate()).add(span);
            }
        }

        boolean isLoopSpan(TraceSpan span) {
            return SpanDomain.from(span) == SpanDomain.LOOP || "loop".equalsIgnoreCase(span.getTags().get("phase"));
        }

        TraceSpan parent(TraceSpan span) {
            return spansById.get(span.getParentId());
        }

        TraceSpan slowestChild(TraceSpan span) {
            return childrenByParentId.getOrDefault(span.getId(), Collections.emptyList()).stream()
                    .max(Comparator.comparingLong(TraceSpan::getDurationNanos))
                    .orElse(null);
        }

        TraceSpan slowestChildAcross(List<TraceSpan> parentSpans) {
            return parentSpans.stream()
                    .flatMap(span -> childrenByParentId.getOrDefault(span.getId(), Collections.emptyList()).stream())
                    .max(Comparator.comparingLong(TraceSpan::getDurationNanos))
                    .orElse(null);
        }
    }

    private static final class Aggregate {
        long count;
        long totalNanos;
        long maxNanos;
        TraceSpan slowestSpan;
        List<TraceSpan> spans = new ArrayList<>();

        void add(TraceSpan span) {
            spans.add(span);
            count++;
            totalNanos += span.getDurationNanos();
            if (span.getDurationNanos() >= maxNanos) {
                maxNanos = span.getDurationNanos();
                slowestSpan = span;
            }
        }

        double averageMillis() {
            return totalNanos / (double) count / 1_000_000.0;
        }

        double maxMillis() {
            return maxNanos / 1_000_000.0;
        }
    }
}
