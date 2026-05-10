package dev.nullftc.profiler.ftc;

import java.lang.reflect.Method;

public final class ReflectionProfileTarget {
    private final Method method;
    private final String spanName;
    private final String reason;

    ReflectionProfileTarget(Method method, String spanName, String reason) {
        this.method = method;
        this.spanName = spanName;
        this.reason = reason;
    }

    public Method getMethod() {
        return method;
    }

    public String getSpanName() {
        return spanName;
    }

    public String getReason() {
        return reason;
    }

    public String describe() {
        return spanName + " -> " + method.getDeclaringClass().getName() + "#" + method.getName() + " (" + reason + ")";
    }
}
