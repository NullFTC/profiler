package dev.nullftc.profiler.entry;

public interface ProfilerEntryFactory {
    ProfilerEntry create(String type, long start, long end);
}
