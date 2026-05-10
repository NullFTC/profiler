package dev.nullftc.profiler.exporter;

import dev.nullftc.profiler.entry.ProfilerEntry;

import java.util.List;

@Deprecated
public interface ProfilerExporter {
    void export(List<ProfilerEntry> entries);
}
