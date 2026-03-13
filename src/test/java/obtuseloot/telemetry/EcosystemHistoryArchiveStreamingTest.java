package obtuseloot.telemetry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemHistoryArchiveStreamingTest {

    @TempDir
    Path tempDir;

    @Test
    void readRecentUsesBoundedWindowAndCopyToPreservesArchive() throws Exception {
        Path archivePath = tempDir.resolve("telemetry.log");
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);

        List<EcosystemTelemetryEvent> events = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            events.add(new EcosystemTelemetryEvent(
                    1_000L + i,
                    EcosystemTelemetryEventType.ABILITY_EXECUTION,
                    i,
                    "lin-" + (i % 7),
                    "niche-" + (i % 5),
                    TelemetryFieldContract.normalize(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                            Map.of("niche", "niche-" + (i % 5), "lineage_id", "lin-" + (i % 7),
                                    "ability_id", "ability", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"))));
        }
        archive.append(events);

        List<EcosystemTelemetryEvent> recent = archive.readRecent(10);
        assertEquals(10, recent.size());
        assertEquals(1_110L, recent.get(0).timestampMs());
        assertEquals(1_119L, recent.get(9).timestampMs());

        Path copied = tempDir.resolve("copy.log");
        archive.copyTo(copied);
        assertTrue(Files.exists(copied));
        assertEquals(Files.size(archivePath), Files.size(copied));
    }
}
