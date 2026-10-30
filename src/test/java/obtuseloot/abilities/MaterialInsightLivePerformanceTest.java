package obtuseloot.abilities;

import obtuseloot.config.RuntimeSettings;
import obtuseloot.persistence.ArtifactProfileCodec;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialInsightLivePerformanceTest {
    private static final String ID = "precision.material_insight";
    @Test void zeroRecoveryLeaseSurvivesSameItemReplacementAndReleasesAfterDelivery() throws Exception {
        try (var f = new InformationFixture()) {
            var config = new YamlConfiguration(); config.set("abilities.material-insight.recovery-ticks", 0); RuntimeSettings.load(config);
            var item = f.item(1, 1); var replacement = f.item(2, .5); replacement.setArtifactStorageKey(item.getArtifactStorageKey());
            var nested = new ArrayList<AbilityExecutionResult>();
            f.onMessage = () -> {
                obtuseloot.artifacts.ArtifactIdentityTransition.regenerate(item, replacement);
                nested.add(f.inspect(replacement, 100));
            };
            f.inspect(item, 100); assertEquals(1, f.messages.size());
            assertEquals(AbilityExecutionStatus.SUPPRESSED, nested.getFirst().status());
            f.onMessage = () -> { };
            assertEquals(AbilityExecutionStatus.SUCCESS, f.inspect(replacement, 101).status());
            assertEquals(2, f.messages.size());
        }
    }
    @Test void anotherAbilitysRecoveryOnTheSameItemBlocksInspection() throws Exception {
        try (var f = new InformationFixture()) {
            var item = f.item(1, 1); item.setAbilityReadyAt("mobility.double_jump", 150);
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.inspect(item, 149).status());
            assertEquals(0, item.abilityReadyAt(ID)); assertTrue(f.messages.isEmpty());
            assertEquals(AbilityExecutionStatus.SUCCESS, f.inspect(item, 150).status());
            assertEquals(240, item.abilityReadyAt(ID)); assertEquals(240, item.abilitiesReadyAt());
            var other = f.item(2, 1); assertEquals(AbilityExecutionStatus.SUCCESS, f.inspect(other, 150).status(), "Recovery is per item, not per holder");
        }
    }
    @Test void sameItemReplacementPreservesCommittedRecoveryButNewIdentityDoesNot() throws Exception {
        try (var f = new InformationFixture()) {
            var item = f.item(1, 1); f.inspect(item, 100);
            var replacement = f.item(2, .5); replacement.setArtifactStorageKey(item.getArtifactStorageKey());
            obtuseloot.artifacts.ArtifactIdentityTransition.regenerate(item, replacement);
            assertEquals(190, replacement.abilityReadyAt(ID));
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.inspect(replacement, 101).status());
            var newIdentity = f.item(3, .5);
            obtuseloot.artifacts.ArtifactIdentityTransition.regenerate(item, newIdentity);
            assertTrue(newIdentity.abilityRecoverySnapshot().isEmpty());
        }
    }
    @Test void deliveryKeepsProducingRevisionAndCommittedRecoveryAcrossEvolution() throws Exception {
        try (var f = new InformationFixture()) {
            var item = f.item(1, 1); var context = f.context(item, 100, f.event());
            var original = context.genomeRevision();
            f.onMessage = () -> {
                item.setEvolutionPath("strider-formed");
                assertTrue(new obtuseloot.evolution.GenomeEvolutionEngine().advance(item));
            };
            var result = f.executor.execute(f.definition, context, 1);
            assertEquals(5, result.observedAmount()); assertEquals(90, result.performance().recoveryTicks());
            assertEquals(0, original.revision()); assertEquals(1, item.getGenomeRevision());
            assertEquals(original, context.genomeRevision()); assertEquals(190, item.abilityReadyAt(ID));
            var shifted = context.withPosition(new obtuseloot.ecology.WorldPosition(InformationFixture.WORLD, 0, 0));
            assertEquals(100, shifted.gameTick()); assertEquals(original, shifted.genomeRevision());
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.executor.execute(f.definition, context, 1).status());
            f.onMessage = () -> { };
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.inspect(item, 189).status());
            assertEquals(AbilityExecutionStatus.SUCCESS, f.inspect(item, 190).status());
        }
    }
    @Test void deepGenesDeliverOnlyActualApplicableFactsAndPayLongerRecovery() throws Exception {
        try (var f = new InformationFixture()) {
            var low = f.inspect(f.item(1, 0), 100); var high = f.inspect(f.item(2, 1), 100);
            assertEquals(2, low.observedAmount()); assertEquals(30, low.performance().recoveryTicks());
            assertEquals(5, high.observedAmount()); assertEquals(90, high.performance().recoveryTicks());
            assertFalse(low.outputText().contains("growth")); assertTrue(high.outputText().contains("growth 4/7"));
            assertTrue(high.outputText().contains("soil farmland")); assertTrue(high.outputText().contains("moisture 7/7"));
            f.rich = false;
            var sparse = f.inspect(f.item(3, 1), 100);
            assertEquals(2, sparse.observedAmount()); assertEquals(90, sparse.performance().recoveryTicks());
            assertFalse(sparse.outputText().contains("growth")); assertEquals(3, f.messages.size());
        }
    }
    @Test void committedRecoverySurvivesTradeSerializationAndChangedConfiguration() throws Exception {
        try (var f = new InformationFixture()) {
            var item = f.item(1, 1); f.inspect(item, 100); assertEquals(190, item.abilityReadyAt(ID));
            var restored = ArtifactProfileCodec.decode(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item));
            f.holder = new UUID(0, 99); restored.setOwnerId(f.holder);
            var config = new YamlConfiguration(); config.set("abilities.material-insight.recovery-ticks", 0); RuntimeSettings.load(config);
            var early = f.inspect(restored, 189); assertEquals(AbilityExecutionStatus.SUPPRESSED, early.status()); assertNull(early.observedAmount());
            assertEquals(1, f.messages.size()); assertEquals(190, restored.abilityReadyAt(ID));
            assertEquals(AbilityExecutionStatus.SUCCESS, f.inspect(restored, 190).status()); assertEquals(2, f.messages.size());
        }
    }
    @Test void denialOfflineAndUnknownClockDoNotSendInformationOrConsumeRecovery() throws Exception {
        try (var f = new InformationFixture()) {
            var item = f.item(1, 1); var event = f.event(); event.setUseInteractedBlock(Event.Result.DENY);
            f.executor.execute(f.definition, f.context(item, 100, event), 1); assertEquals(Event.Result.DENY, event.useInteractedBlock());
            var base = f.context(item, 100, f.event());
            var offline = new AbilityEventContext(base.trigger(), item, base.reputation(), base.value(), base.source(),
                    base.runtimeContext(), base.liveEvent(), false, base.position(), base.activation(), base.genomeRevision(), base.gameTick());
            assertEquals(AbilityExecutionStatus.NO_OP, f.executor.execute(f.definition, offline, 1).status());
            var unavailable = f.executor.execute(f.definition, f.context(item, 100, f.event()).withGameTick(-1), 1);
            assertEquals(AbilityExecutionStatus.SUPPRESSED, unavailable.status()); assertEquals(0, item.abilityReadyAt(ID)); assertTrue(f.messages.isEmpty());
        }
    }
    @Test void failedDeliveryRollsBackReservationAndReentrantZeroRecoveryCannotDuplicateReports() throws Exception {
        try (var f = new InformationFixture()) {
            var item = f.item(1, 1); f.failDelivery = true;
            assertThrows(IllegalStateException.class, () -> f.inspect(item, 100)); assertEquals(0, item.abilityReadyAt(ID));
            f.failDelivery = false; var config = new YamlConfiguration(); config.set("abilities.material-insight.recovery-ticks", 0); RuntimeSettings.load(config);
            var nested = new ArrayList<AbilityExecutionResult>(); f.onMessage = () -> nested.add(f.inspect(item, 100));
            assertEquals(AbilityExecutionStatus.SUCCESS, f.inspect(item, 100).status());
            assertEquals(1, f.messages.size()); assertEquals(1, nested.size()); assertEquals(AbilityExecutionStatus.SUPPRESSED, nested.getFirst().status());
        }
    }
    @Test void recoveryCodecPreservesUnknownAbilityIdsAndRejectsMissingCorruptOrOldState() throws Exception {
        try (var f = new InformationFixture()) {
            var item = f.item(1, .5); item.setAbilityReadyAt("future.removed_ability", 700);
            String encoded = ArtifactProfileCodec.encode(item);
            assertEquals(700, ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded).abilityReadyAt("future.removed_ability"));
            for (String field : List.of("ability-recovery.count", "ability-recovery.entries.0.id", "ability-recovery.entries.0.ready-at")) {
                var yaml = new YamlConfiguration(); yaml.loadFromString(encoded); yaml.set(field, null);
                assertThrows(RuntimeException.class, () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), yaml.saveToString()));
            }
            assertThrows(RuntimeException.class, () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("profile-version: 5", "profile-version: 3")));
            assertThrows(RuntimeException.class, () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("ready-at: 700", "ready-at: -1")));
        }
    }
}
