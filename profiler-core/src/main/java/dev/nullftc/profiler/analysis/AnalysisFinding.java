package dev.nullftc.profiler.analysis;

public final class AnalysisFinding {
    private final AnalysisSeverity severity;
    private final String title;
    private final String evidence;
    private final String recommendation;

    public AnalysisFinding(AnalysisSeverity severity, String title, String evidence, String recommendation) {
        this.severity = severity;
        this.title = title;
        this.evidence = evidence;
        this.recommendation = recommendation;
    }

    public AnalysisSeverity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
