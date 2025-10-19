package dev.nullftc.profiler;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dev.nullftc.profiler.entry.ProfilerEntry;
import dev.nullftc.profiler.entry.ProfilerEntryFactory;
import dev.nullftc.profiler.exporter.ProfilerExporter;

public class Profiler {

    private final ProfilerEntryFactory factory;
    private final ProfilerExporter exporter;
    private final boolean async;
    private final List<ProfilerEntry> entries = new ArrayList<>();
    private final Map<String, Long> activeTimers = new HashMap<>();
    private final Lock lock = new ReentrantLock();
    private final ExecutorService executor;

    private Profiler(ProfilerEntryFactory factory, ProfilerExporter exporter, boolean async) {
        this.factory = factory;
        this.exporter = exporter;
        this.async = async;
        this.executor = async ? Executors.newSingleThreadExecutor() : null;
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
        Runnable task = () -> {
            List<ProfilerEntry> snapshot;
            lock.lock();
            try {
                snapshot = new ArrayList<>(entries);
            } finally {
                lock.unlock();
            }
            exporter.export(snapshot);
        };

        if (async && executor != null)
            executor.execute(task);
        else
            task.run();
    }

    public void shutdown() {
        if (executor != null)
            executor.shutdown();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProfilerEntryFactory factory;
        private ProfilerExporter exporter;
        private boolean async = true;

        public Builder factory(ProfilerEntryFactory factory) {
            this.factory = factory;
            return this;
        }

        public Builder exporter(ProfilerExporter exporter) {
            this.exporter = exporter;
            return this;
        }

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public Profiler build() {
            if (factory == null)
                throw new IllegalStateException("ProfilerEntryFactory not set");
            if (exporter == null)
                throw new IllegalStateException("ProfilerExporter not set");
            return new Profiler(factory, exporter, async);
        }
    }
}

