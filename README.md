![Profiler header](docs/assets/header.png)

# Profiler

Profiler is a WIP rewrite of the old FTC profiler. The goal is pretty simple: record what your robot code is spending time on, pull the trace off the robot, and make it easier to see what is making the loop slow.

V2 is not stable yet. The API, trace format, and viewer are all still being worked on.

## Modules

- `profiler-core`: trace model, span API, import/export, aggregation, and analyzer rules. No FTC SDK dependency.
- `profiler-ftc`: FTC/Android integration, default robot export path, reflection target discovery, and the legacy API adapter.
- `profiler-viewer`: strict WIP JavaFX viewer for opening local traces or pulling traces from a robot over ADB.

## V2 Runtime Usage

This is the new direction, but treat it as WIP.

```java
import dev.nullftc.profiler.Profiled;
import dev.nullftc.profiler.ftc.FtcProfiler;
import dev.nullftc.profiler.ftc.FtcProfiler.FtcProfilerSession;

public class RobotContainer {
    @Profiled
    public void update() {
        // subsystem and scheduler work
    }
}

// In an OpMode or helper:
try (FtcProfilerSession profiler = FtcProfiler.auto(this).deepScan(true).start()) {
    profiler.time("loop.robotUpdate", robot::update);
}
```

Reflection only finds targets. It does not magically time every method call. Timings come from `time(...)`, spans, or wrapper code.

By default, FTC traces write to:

```text
/sdcard/FIRST/profiler
```

When the OpMode stops, active V2 profiler sessions export on a background thread through an FTC `OpModeRegistrar` hook. You can still call `close()` yourself if you want to export earlier.

## Legacy API

The old API still exists in `profiler-ftc` for now.

```java
Profiler profiler = Profiler.builder()
        .factory(new BasicProfilerEntryFactory())
        .exporter(new CSVProfilerExporter(file))
        .build();

profiler.start("Loop");
profiler.end("Loop");
profiler.export();
```

New code should use `ProfilerSession` or `FtcProfiler`, but the legacy path is still there while V2 settles.

## Offline Analysis

Strict WIP. The viewer exists, but it is not polished yet.

Run it:

```bash
./gradlew :profiler-viewer:run
```

Local trace:

1. Click `Open Trace`.
2. Pick a JSON or CSV trace.

Robot trace over ADB:

1. Connect to the Control Hub over ADB Wi-Fi or USB.
2. Click `Refresh Device`.
3. Pick a file from `/sdcard/FIRST/profiler`.
4. Click `Analyze Selected`.

The viewer pulls the file into `~/.ftc-profiler/traces`, then shows aggregate timings and analyzer findings.

## Viewer Release Candidate

Build the viewer zip:

```bash
./gradlew :profiler-viewer:viewerCi
```

Output:

```text
profiler-viewer/build/release-candidates/ftc-profiler-viewer-0.2.0-SNAPSHOT-rc.zip
```
