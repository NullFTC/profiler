# 🕒 Profiler
*A lightweight, extensible profiling system for FTC codebases.*

---

## Overview

**Profiler** is a lightweight, thread-safe profiling framework designed for use in **FIRST Tech Challenge (FTC)** robotics applications. It lets you measure execution times for various parts of your code and export them to structured files like CSV for performance analysis.

This tool helps you identify which parts of your robot code are consuming the most time, which can be crucial for optimizing OpMode loops, path planners, and control subsystems.

---

## ⚙️ Example Usage

```java
Profiler profiler = Profiler.builder()
    .factory(new BasicProfilerEntryFactory())
    .exporter(new CsvProfilerExporter(new File(AppUtils.APP_FOLDER + "/profiler.csv")))
    .debugLog(false) // Logs *everything*
    .build();

profiler.start("init");
// ... initialization code
profiler.end("init");

profiler.start("read");
// ... your main control loop
profiler.end("read");

profiler.export();   // Writes asynchronously if async=true
profiler.shutdown(); // Cleanly stops background threads
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
