package dev.nullftc.profiler.exporter;

import dev.nullftc.profiler.Profiler;
import dev.nullftc.profiler.entry.ProfilerEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Deprecated
public class CSVProfilerExporter implements ProfilerExporter {
    private final File file;

    public CSVProfilerExporter(File file) {
        this.file = file;
    }

    @Override
    public void export(List<ProfilerEntry> entries) {
        if (file == null) {
            return;
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                Profiler.LOGGER.warn("Could not create profiler export directory: {}", parent);
            }

            try (FileWriter writer = new FileWriter(file, false)) {
                writer.append("Type,Start Time,End Time,Delta Time (ms)\n");
                for (ProfilerEntry entry : entries) {
                    writer.append(String.join(",", entry.toCSVRow())).append("\n");
                }
            }
        } catch (IOException e) {
            Profiler.LOGGER.error("Error exporting CSV: {}", String.valueOf(e));
        }
    }
}
