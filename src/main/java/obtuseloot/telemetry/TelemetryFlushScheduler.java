package obtuseloot.telemetry;

public class TelemetryFlushScheduler implements Runnable {
    private final EcosystemTelemetryEmitter emitter;

    public TelemetryFlushScheduler(EcosystemTelemetryEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void run() {
        emitter.scheduledTick(System.currentTimeMillis());
    }
}
