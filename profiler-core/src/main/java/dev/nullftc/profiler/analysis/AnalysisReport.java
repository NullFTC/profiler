package dev.nullftc.profiler.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnalysisReport {
    private final List<AnalysisFinding> findings;

    public AnalysisReport(List<AnalysisFinding> findings) {
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
    }

    public List<AnalysisFinding> getFindings() {
        return findings;
    }
}
