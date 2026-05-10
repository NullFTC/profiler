package dev.nullftc.profiler.ftc;

import dev.nullftc.profiler.ProfileGroup;
import dev.nullftc.profiler.ProfileIgnore;
import dev.nullftc.profiler.Profiled;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReflectionProfileScanner {
    private ReflectionProfileScanner() {
    }

    public static ReflectionProfilePlan scan(Object owner, boolean deepScan) {
        List<ReflectionProfileTarget> targets = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (owner == null) {
            warnings.add("No owner object was provided for reflection profiling.");
            return new ReflectionProfilePlan(targets, warnings);
        }

        Class<?> type = owner.getClass();
        if (type.isAnnotationPresent(ProfileIgnore.class)) {
            warnings.add(type.getName() + " is annotated with @ProfileIgnore.");
            return new ReflectionProfilePlan(targets, warnings);
        }

        String group = groupName(type);
        for (Method method : type.getDeclaredMethods()) {
            if (shouldSkip(method)) {
                continue;
            }
            Profiled profiled = method.getAnnotation(Profiled.class);
            if (profiled != null) {
                targets.add(new ReflectionProfileTarget(method, spanName(group, method, profiled), "annotated"));
            } else if (isConventionTarget(method)) {
                targets.add(new ReflectionProfileTarget(method, group + "." + method.getName(), "ftc-convention"));
            } else if (deepScan && looksLikeLoopWork(method)) {
                targets.add(new ReflectionProfileTarget(method, group + "." + method.getName(), "deep-scan-candidate"));
            }
        }

        if (targets.isEmpty()) {
            warnings.add("No @Profiled methods or FTC convention methods were discovered on " + type.getName() + ".");
        }
        warnings.add("Measure reflected targets with FtcProfilerSession.time(...) or wrapper code.");
        return new ReflectionProfilePlan(targets, warnings);
    }

    private static boolean shouldSkip(Method method) {
        int modifiers = method.getModifiers();
        return method.isSynthetic()
                || method.isBridge()
                || Modifier.isStatic(modifiers)
                || method.isAnnotationPresent(ProfileIgnore.class);
    }

    private static boolean isConventionTarget(Method method) {
        String name = method.getName();
        return method.getParameterTypes().length == 0
                && (name.equals("init")
                || name.equals("init_loop")
                || name.equals("start")
                || name.equals("loop")
                || name.equals("stop")
                || name.equals("update")
                || name.equals("periodic")
                || name.equals("read")
                || name.equals("write"));
    }

    private static boolean looksLikeLoopWork(Method method) {
        if (method.getParameterTypes().length > 1) {
            return false;
        }
        String lower = method.getName().toLowerCase(Locale.US);
        return lower.contains("update")
                || lower.contains("loop")
                || lower.contains("drive")
                || lower.contains("localiz")
                || lower.contains("telemetry")
                || lower.contains("scheduler")
                || lower.contains("command");
    }

    private static String groupName(Class<?> type) {
        ProfileGroup profileGroup = type.getAnnotation(ProfileGroup.class);
        if (profileGroup != null && !profileGroup.value().isEmpty()) {
            return profileGroup.value();
        }
        Profiled profiled = type.getAnnotation(Profiled.class);
        if (profiled != null && !profiled.group().isEmpty()) {
            return profiled.group();
        }
        return type.getSimpleName();
    }

    private static String spanName(String group, Method method, Profiled profiled) {
        if (!profiled.value().isEmpty()) {
            return profiled.value();
        }
        if (!profiled.group().isEmpty()) {
            return profiled.group() + "." + method.getName();
        }
        return group + "." + method.getName();
    }
}
