package dev.nullftc.profiler.ftc;

import dev.nullftc.profiler.ProfilerSession;
import dev.nullftc.profiler.SpanScope;
import dev.nullftc.profiler.exporter.JsonTraceExporter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class FtcProfiler {
    private final Object owner;
    private final File outputFile;
    private boolean deepScan;

    private FtcProfiler(Object owner, File outputFile) {
        this.owner = owner;
        this.outputFile = outputFile;
    }

    public static FtcProfiler auto(Object owner) {
        return new FtcProfiler(owner, defaultOutputFile());
    }

    public FtcProfiler outputFile(File outputFile) {
        return new FtcProfiler(owner, outputFile);
    }

    public FtcProfiler deepScan(boolean deepScan) {
        this.deepScan = deepScan;
        return this;
    }

    public FtcProfilerSession start() {
        ReflectionProfilePlan plan = ReflectionProfileScanner.scan(owner, deepScan);
        ProfilerSession session = ProfilerSession.builder(owner.getClass().getSimpleName())
                .metadata("platform", "ftc")
                .metadata("profiledClass", owner.getClass().getName())
                .metadata("reflectionTargets", String.valueOf(plan.getTargets().size()))
                .start();

        for (ReflectionProfileTarget target : plan.getTargets()) {
            session.diagnostic("reflection_target", target.describe(), target.getSpanName());
        }
        for (String warning : plan.getWarnings()) {
            session.diagnostic("reflection_warning", warning, null);
        }

        return new FtcProfilerSession(session, outputFile);
    }

    private static File defaultOutputFile() {
        File root = new File("/sdcard/FIRST/profiler");
        return new File(root, "trace-" + System.currentTimeMillis() + ".json");
    }

    public static final class FtcProfilerSession implements AutoCloseable {
        private final ProfilerSession session;
        private final File outputFile;

        private FtcProfilerSession(ProfilerSession session, File outputFile) {
            this.session = session;
            this.outputFile = outputFile;
        }

        public void time(String name, Runnable runnable) {
            session.time(name, runnable);
        }

        public void time(String name, Map<String, String> tags, Runnable runnable) {
            try (SpanScope ignored = session.span(name, tags)) {
                runnable.run();
            }
        }

        public ProfilerSession rawSession() {
            return session;
        }

        public void export() throws IOException {
            new JsonTraceExporter(outputFile.toPath()).export(session.snapshot());
        }

        @Override
        public void close() throws IOException {
            session.close();
            export();
        }
    }
}
