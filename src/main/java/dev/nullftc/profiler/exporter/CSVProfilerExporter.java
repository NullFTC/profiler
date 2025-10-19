package dev.nullftc.profiler.exporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import dev.nullftc.profiler.entry.ProfilerEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVProfilerExporter implements ProfilerExporter {
    private final File file;

    public CSVProfilerExporter(File file) {
        this.file = file;
    }

    @Override
    public void export(List<ProfilerEntry> entries) {
        if (file == null) return;

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            FileWriter writer = new FileWriter(file);
            try {
                writer.append("Type,Start Time,End Time,Delta Time (ms)\n");
                for (ProfilerEntry entry : entries) {
                    String[] row = entry.toCSVRow();
                    writer.append(String.join(",", row)).append("\n");
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


