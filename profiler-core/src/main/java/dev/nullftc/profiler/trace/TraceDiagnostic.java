package dev.nullftc.profiler.trace;

public final class TraceDiagnostic {
    private final String code;
    private final String message;
    private final String spanName;

    public TraceDiagnostic(String code, String message, String spanName) {
        this.code = code;
        this.message = message;
        this.spanName = spanName;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getSpanName() {
        return spanName;
    }
}
