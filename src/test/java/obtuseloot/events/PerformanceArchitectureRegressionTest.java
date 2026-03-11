package obtuseloot.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceArchitectureRegressionTest {

    @Test
    void clientChunkPacketEventIsNotUsedForServerStateMutation() throws IOException {
        String allMainSources = Files.readString(Path.of("src/main/java/obtuseloot/events/NonCombatAbilityListener.java"))
                + Files.readString(Path.of("src/main/java/obtuseloot/events/StructureSenseService.java"));
        assertFalse(allMainSources.contains("PlayerChunkLoadEvent"));
    }

    @Test
    void structureSenseChecksChunkSignalsBeforeWorldLocateCalls() throws IOException {
        String source = Files.readString(Path.of("src/main/java/obtuseloot/events/StructureSenseService.java"));
        int chunkSignal = source.indexOf("hasChunkStructureSignals(chunk) || locateUsingWorldApi(chunk, now)");
        assertTrue(chunkSignal >= 0, "Expected chunk-signal-first short-circuit ordering");
        assertTrue(source.contains("LOCATE_FAILURE_BACKOFF_MS"), "Expected locate failure backoff guard");
        assertTrue(source.contains(",\n                    false\n            );"), "Expected findUnexplored=false locate call");
    }


    @Test
    void runtimePathDoesNotUseBroadNearbyEntityScans() throws IOException {
        String source = Files.readString(Path.of("src/main/java/obtuseloot/events/NonCombatAbilityListener.java"));
        assertFalse(source.contains("getNearbyEntities("));
        assertFalse(source.contains("getEntities("));
    }

    @Test
    void runtimePathHasNoTeleportFlowsNeedingAsyncChunkLoads() throws IOException {
        String source = Files.readString(Path.of("src/main/java/obtuseloot/events/NonCombatAbilityListener.java"));
        assertFalse(source.contains("teleportAsync("));
        assertFalse(source.contains(".teleport("));
    }

    @Test
    void nonCombatListenerCleansUpCoalescedWorkOnQuit() throws IOException {
        String source = Files.readString(Path.of("src/main/java/obtuseloot/events/NonCombatAbilityListener.java"));
        assertTrue(source.contains("onQuit(PlayerQuitEvent event)"));
        assertTrue(source.contains("coalescer.cancelByPrefix(\"sense:\" + playerId + \":\")"));
    }
}
