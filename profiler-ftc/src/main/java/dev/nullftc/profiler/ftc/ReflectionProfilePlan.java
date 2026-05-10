package dev.nullftc.profiler.ftc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReflectionProfilePlan {
    private final List<ReflectionProfileTarget> targets;
    private final List<String> warnings;

    ReflectionProfilePlan(List<ReflectionProfileTarget> targets, List<String> warnings) {
        this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public List<ReflectionProfileTarget> getTargets() {
        return targets;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
