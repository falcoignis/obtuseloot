package obtuseloot.bootstrap;

import obtuseloot.telemetry.EcosystemHistoryArchive;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.RollupStateHydrator;
import obtuseloot.telemetry.ScheduledEcosystemRollups;
import obtuseloot.telemetry.TelemetryAggregationAnalytics;
import obtuseloot.telemetry.TelemetryAggregationBuffer;
import obtuseloot.telemetry.TelemetryAggregationService;
import obtuseloot.telemetry.TelemetryFlushScheduler;
import obtuseloot.telemetry.TelemetryRollupSnapshotStore;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public final class TelemetryBootstrap {
    private TelemetryBootstrap() {
    }

    public static Result initialize(Logger logger,
                                    obtuseloot.evolution.params.EcosystemTuningProfile tuningProfile,
                                    PluginPathLayout paths) {
        TelemetryAggregationBuffer telemetryBuffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive telemetryArchive = new EcosystemHistoryArchive(paths.telemetryArchivePath());
        ScheduledEcosystemRollups scheduledRollups = new ScheduledEcosystemRollups(telemetryBuffer, tuningProfile.telemetryRollupIntervalMs());
        TelemetryRollupSnapshotStore snapshotStore = new TelemetryRollupSnapshotStore(paths.telemetrySnapshotPath());
        RollupStateHydrator hydrator = new RollupStateHydrator(snapshotStore, telemetryArchive, tuningProfile.telemetryRehydrateReplayWindowEvents());

        TelemetryAggregationService aggregationService = new TelemetryAggregationService(
                telemetryBuffer,
                telemetryArchive,
                scheduledRollups,
                tuningProfile.telemetryArchiveBatchSize(),
                snapshotStore,
                hydrator);
        aggregationService.initializeFromHistory();
        logger.info("[Telemetry] initialization mode=" + aggregationService.initialization().mode()
                + ", replayedEvents=" + aggregationService.initialization().replayedEvents()
                + ", durationMs=" + aggregationService.initialization().durationMs());

        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(aggregationService);
        TelemetryAggregationAnalytics analytics = new TelemetryAggregationAnalytics(scheduledRollups);
        return new Result(emitter, analytics);
    }

    public static BukkitTask scheduleFlushTask(JavaPlugin plugin,
                                               EcosystemTelemetryEmitter ecosystemTelemetryEmitter,
                                               long flushIntervalTicks) {
        return plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                new TelemetryFlushScheduler(ecosystemTelemetryEmitter),
                flushIntervalTicks,
                flushIntervalTicks);
    }

    public record Result(EcosystemTelemetryEmitter emitter, TelemetryAggregationAnalytics analytics) {
    }
}
