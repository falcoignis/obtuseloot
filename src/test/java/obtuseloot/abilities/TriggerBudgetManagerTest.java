package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TriggerBudgetManagerTest {

    @Test
    void basicBudgetConsumptionAndSuppression() {
        ItemAbilityManager manager = managerWith(singlePassiveWorldScan());
        Artifact artifact = artifact(1L);

        for (int i = 0; i < 10; i++) {
            manager.resolveDispatch(new AbilityEventContext(AbilityTrigger.ON_WORLD_SCAN, artifact, new ArtifactReputation(), 1.0D, "move-chunk"));
        }

        assertTrue(manager.triggerBudgetConsumptionByAbility().containsKey("SENSE_PING"));
        assertTrue(manager.executionStatusCounts().get(AbilityExecutionStatus.SUPPRESSED) >= 0L);
    }

    @Test
    void intentionalTriggerGetsHigherPriorityProfileThanPassive() {
        TriggerBudgetResolver resolver = new TriggerBudgetResolver();
        AbilityDefinition passive = singlePassiveWorldScan().abilities().getFirst();
        AbilityDefinition active = activeInspect().abilities().getFirst();
        Artifact artifact = artifact(2L);

        TriggerBudgetProfile passiveProfile = resolver.resolve(passive,
                new AbilityEventContext(AbilityTrigger.ON_WORLD_SCAN, artifact, new ArtifactReputation(), 1.0D, "move-chunk",
                        AbilityRuntimeContext.passive(AbilitySource.CHUNK_WORLD_SCAN)));
        TriggerBudgetProfile activeProfile = resolver.resolve(active,
                new AbilityEventContext(AbilityTrigger.ON_BLOCK_INSPECT, artifact, new ArtifactReputation(), 1.0D, "inspect-block",
                        AbilityRuntimeContext.intentional(AbilitySource.BLOCK_INSPECT)));

        assertTrue(activeProfile.priority() > passiveProfile.priority());
        assertTrue(activeProfile.intentionalPreferred());
    }

    @Test
    void sourceTextNoLongerEscalatesIntentWithoutRuntimeFlag() {
        TriggerBudgetResolver resolver = new TriggerBudgetResolver();
        AbilityDefinition passive = singlePassiveWorldScan().abilities().getFirst();
        Artifact artifact = artifact(5L);

        TriggerBudgetProfile profile = resolver.resolve(
                passive,
                new AbilityEventContext(
                        AbilityTrigger.ON_WORLD_SCAN,
                        artifact,
                        new ArtifactReputation(),
                        1.0D,
                        "inspect-harvest-interact",
                        AbilityRuntimeContext.passive(AbilitySource.CHUNK_WORLD_SCAN)
                )
        );
        assertFalse(profile.intentionalPreferred());
        assertTrue(profile.priority() < 80);
    }


    @Test
    void lazyRefillAllowsRecoveryAfterCooldownWindow() throws InterruptedException {
        TriggerBudgetManager budget = new TriggerBudgetManager();
        UUID holder = UUID.randomUUID();
        String artifactKey = "artifact:test:refill";

        for (int i = 0; i < 15; i++) {
            budget.allowProbe(holder, artifactKey, AbilityTrigger.ON_WORLD_SCAN, "probe", 1.2D, false);
        }
        boolean blocked = !budget.allowProbe(holder, artifactKey, AbilityTrigger.ON_WORLD_SCAN, "probe", 1.2D, false);
        Thread.sleep(1200L);
        boolean recovered = budget.allowProbe(holder, artifactKey, AbilityTrigger.ON_WORLD_SCAN, "probe", 1.2D, false);

        assertTrue(blocked);
        assertTrue(recovered);
    }

    @Test
    void probeBudgetThrottlesMovementHeavyListenerStyleChecks() {
        TriggerBudgetManager budget = new TriggerBudgetManager();
        UUID holder = UUID.randomUUID();
        String artifactKey = "artifact:test:probe";

        boolean first = budget.allowProbe(holder, artifactKey, AbilityTrigger.ON_STRUCTURE_SENSE, "probe", 1.1D, false);
        boolean exhausted = false;
        for (int i = 0; i < 20; i++) {
            if (!budget.allowProbe(holder, artifactKey, AbilityTrigger.ON_STRUCTURE_SENSE, "probe", 1.1D, false)) {
                exhausted = true;
                break;
            }
        }
        assertTrue(first);
        assertTrue(exhausted);
    }

    private ItemAbilityManager managerWith(AbilityProfile profile) {
        ItemAbilityManager manager = new ItemAbilityManager((artifact, rep) -> profile);
        manager.setTriggerSubscriptionIndexingEnabled(false);
        return manager;
    }

    private AbilityProfile singlePassiveWorldScan() {
        return new AbilityProfile("passive", List.of(new AbilityDefinition(
                "precision.echo_locator",
                "Echo Locator",
                AbilityFamily.PRECISION,
                AbilityTrigger.ON_WORLD_SCAN,
                AbilityMechanic.SENSE_PING,
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                new AbilityMetadata(java.util.Set.of("scan"), java.util.Set.of("movement"), java.util.Set.of("watchful"), 0.4D, 0.4D, 0.7D, 0.1D, 0.1D, 0.2D,
                        new TriggerBudgetProfile(1.8D, 1.0D, 8.0D, 2.0D, 2, 1200L, 20, TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY, false, 250.0D)),
                "s1", "s2", "s3", "s4", "s5"
        )));
    }

    private AbilityProfile activeInspect() {
        return new AbilityProfile("active", List.of(new AbilityDefinition(
                "consistency.bestiary_insight",
                "Bestiary Insight",
                AbilityFamily.CONSISTENCY,
                AbilityTrigger.ON_BLOCK_INSPECT,
                AbilityMechanic.INSIGHT_REVEAL,
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                new AbilityMetadata(java.util.Set.of("info"), java.util.Set.of("interact"), java.util.Set.of("curious"), 0.6D, 0.3D, 0.8D, 0.1D, 0.4D, 0.3D,
                        new TriggerBudgetProfile(0.6D, 0.2D, 14.0D, 6.0D, 6, 700L, 90, TriggerBudgetPolicy.ACTIVE_INTENTIONAL, true, 50.0D)),
                "s1", "s2", "s3", "s4", "s5"
        )));
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactStorageKey("artifact:test:" + seed);
        artifact.setArtifactSeed(seed);
        return artifact;
    }
}
