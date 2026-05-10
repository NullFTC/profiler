package dev.nullftc.profiler.ftc;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar;

public final class FtcProfilerOpModeRegistrar {
    private static boolean registered;

    private FtcProfilerOpModeRegistrar() {
    }

    @OpModeRegistrar
    public static void register(OpModeManager manager) {
        if (registered || !(manager instanceof OpModeManagerNotifier)) {
            return;
        }
        registered = true;
        ((OpModeManagerNotifier) manager).registerListener(new ExportOnStop());
    }

    private static final class ExportOnStop implements OpModeManagerNotifier.Notifications {
        @Override
        public void onOpModePreInit(OpMode opMode) {
        }

        @Override
        public void onOpModePreStart(OpMode opMode) {
        }

        @Override
        public void onOpModePostStop(OpMode opMode) {
            FtcProfiler.exportActiveSessionsAsync();
        }
    }
}
