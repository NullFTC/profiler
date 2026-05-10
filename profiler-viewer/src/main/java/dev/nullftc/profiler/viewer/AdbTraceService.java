package dev.nullftc.profiler.viewer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class AdbTraceService {
    public static final String DEFAULT_DEVICE_DIR = "/sdcard/FIRST/profiler";
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(10);

    private final String adbExecutable;
    private final String deviceDirectory;

    public AdbTraceService() {
        this(resolveAdbExecutable(), DEFAULT_DEVICE_DIR);
    }

    public AdbTraceService(String adbExecutable, String deviceDirectory) {
        this.adbExecutable = adbExecutable;
        this.deviceDirectory = deviceDirectory;
    }

    public List<AdbTraceFile> listTraceFiles() throws IOException, InterruptedException {
        String script = "for f in " + deviceDirectory + "/*.json " + deviceDirectory + "/*.csv; do [ -f \"$f\" ] && stat -c '%Y|%y|%s|%n' \"$f\"; done";
        CommandResult result = run(adbExecutable, "shell", "sh", "-c", script);
        if (result.exitCode != 0) {
            throw new IOException(result.stderr.isBlank() ? "adb list failed" : result.stderr.trim());
        }

        List<AdbTraceFile> files = new ArrayList<>();
        for (String line : result.stdout.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", 4);
            if (parts.length == 4) {
                files.add(new AdbTraceFile(parts[3], normalizeModified(parts[1]), parseLong(parts[2])));
            }
        }
        files.sort((left, right) -> right.getModified().compareTo(left.getModified()));
        return files;
    }

    private static String normalizeModified(String value) {
        int dot = value.indexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }

    public Path pull(AdbTraceFile traceFile) throws IOException, InterruptedException {
        Path cacheDir = Path.of(System.getProperty("user.home"), ".ftc-profiler", "traces");
        Files.createDirectories(cacheDir);
        Path target = cacheDir.resolve(traceFile.getName());
        CommandResult result = run(adbExecutable, "pull", traceFile.getPath(), target.toString());
        if (result.exitCode != 0) {
            throw new IOException(result.stderr.isBlank() ? "adb pull failed" : result.stderr.trim());
        }
        return target;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String resolveAdbExecutable() {
        String fromAndroidHome = adbFromSdk(System.getenv("ANDROID_HOME"));
        if (fromAndroidHome != null) {
            return fromAndroidHome;
        }

        String fromAndroidSdkRoot = adbFromSdk(System.getenv("ANDROID_SDK_ROOT"));
        if (fromAndroidSdkRoot != null) {
            return fromAndroidSdkRoot;
        }

        String fromLocalProperties = adbFromLocalProperties();
        if (fromLocalProperties != null) {
            return fromLocalProperties;
        }

        return "adb";
    }

    private static String adbFromSdk(String sdkPath) {
        if (sdkPath == null || sdkPath.isBlank()) {
            return null;
        }
        Path adb = Path.of(sdkPath, "platform-tools", isWindows() ? "adb.exe" : "adb");
        return Files.isRegularFile(adb) ? adb.toString() : null;
    }

    private static String adbFromLocalProperties() {
        Path localProperties = Path.of("local.properties");
        if (!Files.isRegularFile(localProperties)) {
            return null;
        }
        Properties properties = new Properties();
        try (var input = Files.newInputStream(localProperties)) {
            properties.load(input);
        } catch (IOException ignored) {
            return null;
        }
        return adbFromSdk(properties.getProperty("sdk.dir"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static CommandResult run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        boolean exited = process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!exited) {
            process.destroyForcibly();
            throw new IOException("adb command timed out");
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new CommandResult(process.exitValue(), stdout, stderr);
    }

    private static final class CommandResult {
        final int exitCode;
        final String stdout;
        final String stderr;

        CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
