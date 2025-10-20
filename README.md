# 🕒 Profiler
*A lightweight, extensible profiling system for FTC codebases.*

---

## Overview

**Profiler** is a lightweight, thread-safe profiling framework designed for use in **FIRST Tech Challenge (FTC)** robotics applications. It lets you measure execution times for various parts of your code and export them to structured files like CSV for performance analysis.

This tool helps you identify which parts of your robot code are consuming the most time, which can be crucial for optimizing OpMode loops, path planners, and control subsystems.

---

## ⚡ Installation

You can grab the latest version of Profiler from [our Maven repository](https://maven.nullftc.dev/#/releases/dev/nullftc/Profiler). To use it in your TeamCode project, make sure to add the repository to the `repositories` block in your `build.gradle`:

```groovy
repositories {
    // ... other repositories ...
    maven {
        name "nullftcReleases"
        url "https://maven.nullftc.dev/releases"
    }
}
```

Then, add Profiler as a dependency in your dependencies block:
```
implementation "dev.nullftc:Profiler:<LATEST_VERSION>"
```

> 💡 Tip: Replace <LATEST_VERSION> with the version you want to use. Check the Maven page for the most up-to-date release.

---

## ⚙️ Example Usage

```java
import java.io.File;

@TeleOp(name = "Profiler Linear OpMode", group = "Test")
public class ProfilerLinearOpMode extends LinearOpMode {

    private Profiler profiler;

    @Override
    public void runOpMode() throws InterruptedException {
        File logsFolder = new File(AppUtil.FIRST_FOLDER, "logs");
        if (!logsFolder.exists()) logsFolder.mkdirs();

        long timestamp = System.currentTimeMillis();
        File file = new File(logsFolder, "profiler-" + timestamp + ".csv");

        profiler = Profiler.builder()
                .factory(new BasicProfilerEntryFactory())
                .exporter(new CSVProfilerExporter(file))
                .debugLog(true)
                .build();

        try {
            profiler.start("Init");
            // ... initialization logic ...
            profiler.end("Init");

            telemetry.addData("Status", "Waiting for start");
            telemetry.update();

            waitForStart();

            while (opModeIsActive() && !isStopRequested()) {
                profiler.start("Loop");
                // ... your main loop logic ...
                telemetry.update();
                profiler.end("Loop");
            }
        } finally {
            profiler.export();
            profiler.shutdown();
            telemetry.update();
        }
    }
}
```

---

## 🧩 Architecture Overview

Profiler is built around a **modular architecture** with clean separation of responsibilities:

| Component              | Responsibility                                                     |
|------------------------|--------------------------------------------------------------------|
| `ProfilerEntry`        | Abstract representation of a recorded event                        |
| `ProfilerEntryFactory` | Creates entries (allows different subclasses for specialized data) |
| `ProfilerExporter`     | Handles output logic                                               |
| `Profiler`             | Manages timing, synchronization, and lifecycle                     |


This structure allows you to add new entry types (e.g., subsystem-specific or multi-metric) or new exporters without touching the core code.

---

## 🧠 Extending the Profiler

### Custom Entry Type

```java
public class MotorProfilerEntry extends ProfilerEntry {
    private final double motorPower;

    public MotorProfilerEntry(String type, long start, long end, double motorPower) {
        super(type, start, end);
        this.motorPower = motorPower;
    }

    @Override
    public String[] toCSVRow() {
        return new String[] {
            getType(),
            String.valueOf(getStartTime()),
            String.valueOf(getEndTime()),
            String.valueOf(getDeltaTime()),
            String.valueOf(motorPower)
        };
    }
}
```

### Custom Exporter

```java
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.io.File;

public class JsonProfilerExporter implements ProfilerExporter {

    private final File outputFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonProfilerExporter(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void export(List<ProfilerEntry> entries) {
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(entries, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

---

## 📊 Using the Exported Data

Once Profiler has exported your data to a CSV, you can use our pre-built visualization to see exactly what’s taking up the most time in your loops!  

Check it out here: [Profiler Visualizer](https://insights.nullftc.dev/) – it makes spotting bottlenecks super easy. Hope you find it useful! 🙂
