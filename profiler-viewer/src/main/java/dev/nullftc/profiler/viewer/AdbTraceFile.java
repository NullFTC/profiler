package dev.nullftc.profiler.viewer;

public final class AdbTraceFile {
    private final String path;
    private final String modified;
    private final long bytes;

    public AdbTraceFile(String path, String modified, long bytes) {
        this.path = path;
        this.modified = modified;
        this.bytes = bytes;
    }

    public String getPath() {
        return path;
    }

    public String getModified() {
        return modified;
    }

    public long getBytes() {
        return bytes;
    }

    public String getName() {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
