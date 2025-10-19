package dev.nullftc.profiler;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dev.nullftc.profiler.entry.ProfilerEntry;
import dev.nullftc.profiler.entry.ProfilerEntryFactory;
import dev.nullftc.profiler.exporter.ProfilerExporter;

public class Profiler {

    private final ProfilerEntryFactory factory;
    private final ProfilerExporter exporter;
    private final List<ProfilerEntry> entries = new ArrayList<>();
    private final Map<String, Long> activeTimers = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    private Profiler(ProfilerEntryFactory factory, ProfilerExporter exporter) {
        this.factory = factory;
        this.exporter = exporter;
    }

    public void start(String type) {
        lock.lock();
        try {
            activeTimers.put(type, System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }

    public void end(String type) {
        long end = System.currentTimeMillis();
        Long start;

        lock.lock();
        try {
            start = activeTimers.remove(type);
        } finally {
            lock.unlock();
        }

        if (start != null) {
            ProfilerEntry entry = factory.create(type, start, end);
            lock.lock();
            try {
                entries.add(entry);
            } finally {
                lock.unlock();
            }
        } else {
            System.err.println("Profiler warning: end() called for '" + type + "' with no start()");
        }
    }

    public void export() {
        List<ProfilerEntry> snapshot;
        lock.lock();
        try {
            snapshot = new ArrayList<>(entries);
        } finally {
            lock.unlock();
        }
        exporter.export(snapshot);
    }

    public void shutdown() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProfilerEntryFactory factory;
        private ProfilerExporter exporter;

        public Builder factory(ProfilerEntryFactory factory) {
            this.factory = factory;
            return this;
        }

        public Builder exporter(ProfilerExporter exporter) {
            this.exporter = exporter;
            return this;
        }

        public Profiler build() {
            if (factory == null)
                throw new IllegalStateException("ProfilerEntryFactory not set");
            if (exporter == null)
                throw new IllegalStateException("ProfilerExporter not set");
            return new Profiler(factory, exporter);
        }
    }
}
