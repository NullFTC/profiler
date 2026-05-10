package dev.nullftc.profiler.entry;

@Deprecated
public interface ProfilerEntryFactory {
    ProfilerEntry create(String type, long start, long end);
}
