package obtuseloot.commands;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.convergence.ConvergenceEngine;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6.4 — Command & Permission Integrity Pass
 *
 * Verifies structural and behavioral invariants for the command layer without
 * requiring a live Bukkit server. Tests cover:
 *   - DebugTabCompleter TOP-level subcommand coverage
 *   - DebugTabCompleter subscriptions completion includes both actions and players
 *   - AwakeningEngine.forceAwakening() returns a complete ArtifactIdentityTransition
 *     that the command layer (debug awaken / simulate path awaken) can safely consume
 *   - Identity transition carries all fields required for replaceIdentity() to succeed
 *   - forceAwakening() returns null when artifact is already awakened (guard for no-op branch)
 *   - ConvergenceEngine.evaluate() returns a complete transition for the fuse command
 */
class CommandAuditIntegrityTest {

    // ─── DebugTabCompleter structural invariants ──────────────────────────────

    @Test
    void debugTabCompleterTopListContainsAllRequiredSubcommands() throws Exception {
        Field topField = DebugTabCompleter.class.getDeclaredField("TOP");
        topField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> top = (List<String>) topField.get(null);

        // Core inspection / mutation commands
        assertTrue(top.contains("inspect"), "TOP must include inspect");
        assertTrue(top.contains("rep"),     "TOP must include rep");
        assertTrue(top.contains("evolve"),  "TOP must include evolve");
        assertTrue(top.contains("drift"),   "TOP must include drift");
        assertTrue(top.contains("awaken"),  "TOP must include awaken");
        assertTrue(top.contains("fuse"),    "TOP must include fuse");
        assertTrue(top.contains("lore"),    "TOP must include lore");
        assertTrue(top.contains("reset"),   "TOP must include reset");
        assertTrue(top.contains("save"),    "TOP must include save");
        assertTrue(top.contains("reload"),  "TOP must include reload");
        assertTrue(top.contains("help"),    "TOP must include help");

        // Advanced / diagnostic subcommands
        assertTrue(top.contains("instability"),   "TOP must include instability");
        assertTrue(top.contains("archetype"),     "TOP must include archetype");
        assertTrue(top.contains("path"),          "TOP must include path");
        assertTrue(top.contains("simulate"),      "TOP must include simulate");
        assertTrue(top.contains("seed"),          "TOP must include seed");
        assertTrue(top.contains("ability"),       "TOP must include ability");
        assertTrue(top.contains("memory"),        "TOP must include memory");
        assertTrue(top.contains("persistence"),   "TOP must include persistence");
        assertTrue(top.contains("ecosystem"),     "TOP must include ecosystem");
        assertTrue(top.contains("lineage"),       "TOP must include lineage");
        assertTrue(top.contains("genome"),        "TOP must include genome");
        assertTrue(top.contains("projection"),    "TOP must include projection");
        assertTrue(top.contains("subscriptions"), "TOP must include subscriptions");
        assertTrue(top.contains("artifact"),      "TOP must include artifact");
    }

    @Test
    void debugTabCompleterSubscriptionActionsIncludesStats() throws Exception {
        Field field = DebugTabCompleter.class.getDeclaredField("SUBSCRIPTION_ACTIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) field.get(null);

        assertTrue(actions.contains("stats"),
                "SUBSCRIPTION_ACTIONS must include 'stats' so tab completion offers it at arg[2]");
    }

    @Test
    void debugTabCompleterSimulatePathsIncludesAwaken() throws Exception {
        Field field = DebugTabCompleter.class.getDeclaredField("SIM_PATHS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) field.get(null);

        assertTrue(paths.contains("awaken"), "SIM_PATHS must include 'awaken' so simulate path awaken is tab-completable");
        assertTrue(paths.contains("drift"),  "SIM_PATHS must include 'drift'");
        assertTrue(paths.contains("boss"),   "SIM_PATHS must include 'boss'");
    }

    // ─── AwakeningEngine: forceAwakening returns complete transition ──────────

    @Test
    void forceAwakeningReturnsNonNullTransitionForEligibleArtifact() {
        AwakeningEngine engine = new AwakeningEngine();

        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArtifactSeed(77L);
        artifact.setArchetypePath("ravager");
        artifact.setLatentLineage("common");
        for (int i = 0; i < 6; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifact.addLoreHistory("campaign-alpha");
        artifact.addLoreHistory("campaign-beta");
        artifact.addLoreHistory("campaign-gamma");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(15);
        rep.setKills(6);

        // This is the method called by both `debug awaken` and `simulate path awaken`.
        // The fix ensures the caller no longer discards this result.
        ArtifactIdentityTransition transition = engine.forceAwakening(null, artifact, rep);

        assertNotNull(transition,
                "forceAwakening must return a non-null transition for an eligible ravager artifact");
    }

    @Test
    void forceAwakeningTransitionCarriesFieldsRequiredForReplaceIdentity() {
        AwakeningEngine engine = new AwakeningEngine();

        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArtifactSeed(88L);
        artifact.setArchetypePath("ravager");
        artifact.setLatentLineage("ancient-rune");
        for (int i = 0; i < 6; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifact.addLoreHistory("war-campaign");
        artifact.addLoreHistory("siege-campaign");
        artifact.addLoreHistory("final-stand");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(15);
        rep.setKills(6);

        ArtifactIdentityTransition transition = engine.forceAwakening(null, artifact, rep);
        assertNotNull(transition);

        Artifact replacement = transition.replacement();

        // replaceIdentity() requires these fields to be populated on the replacement:
        assertNotNull(replacement.getArtifactStorageKey(),
                "replacement must preserve the artifact storage key for replaceIdentity()");
        assertEquals(artifact.getArtifactStorageKey(), replacement.getArtifactStorageKey(),
                "storage key must be identical to the original so the manager can locate the record");
        assertEquals(artifact.getOwnerId(), replacement.getOwnerId(),
                "owner ID must be preserved across identity replacement");
        assertNotEquals(artifact.getArtifactSeed(), replacement.getArtifactSeed(),
                "awakening must produce a new distinct seed");
        assertNotNull(transition.reason(),
                "transition reason must be set for lineage recording");
        assertFalse(transition.reason().isBlank(),
                "transition reason must not be blank");
        assertNotEquals("none", replacement.getAwakeningPath(),
                "replacement must carry a named awakening path");
    }

    @Test
    void forceAwakeningReturnsNullWhenAlreadyAwakened() {
        AwakeningEngine engine = new AwakeningEngine();

        // An artifact that has already undergone awakening must not be re-awakened.
        // The `debug awaken` command checks for null and shows "already active" message.
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setAwakeningPath("Executioner's Oath");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(20);
        rep.setKills(10);

        assertNull(engine.forceAwakening(null, artifact, rep),
                "forceAwakening must return null when awakening is already active, so the command shows the correct no-op message");
    }

    @Test
    void forceAwakeningReturnsNullForIneligibleArchetype() {
        // Artifacts with archetype "unformed" have no awakening profile in the engine's switch —
        // resolve() returns null, so forceAwakening must return null.
        // The `simulate path awaken` null-check must handle this cleanly.
        AwakeningEngine engine = new AwakeningEngine();

        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArtifactSeed(33L);
        artifact.setArchetypePath("unformed");
        for (int i = 0; i < 10; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setMobility(20);
        rep.setKills(10);
        rep.setBossKills(3);

        assertNull(engine.forceAwakening(null, artifact, rep),
                "forceAwakening must return null for archetypes with no awakening profile (e.g. unformed)");
    }

    // ─── ConvergenceEngine: evaluate returns complete transition for fuse ─────

    // ─── Post-transition inspect correctness ─────────────────────────────────

    /**
     * Verifies that the replacement returned by forceAwakening reflects the post-transition
     * artifact state that `debug inspect` must show. No stale pre-awakening values must leak.
     */
    @Test
    void inspectReflectsReplacementAfterAwakening() {
        AwakeningEngine engine = new AwakeningEngine();

        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifact.setArtifactSeed(100L);
        artifact.setArchetypePath("ravager");
        artifact.setLatentLineage("common");
        for (int i = 0; i < 6; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifact.addLoreHistory("battle-one");
        artifact.addLoreHistory("battle-two");
        artifact.addLoreHistory("battle-three");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(15);
        rep.setKills(6);

        long originalSeed = artifact.getArtifactSeed();
        String originalAwakeningPath = artifact.getAwakeningPath();

        // Command equivalent of `debug awaken`
        ArtifactIdentityTransition transition = engine.forceAwakening(null, artifact, rep);
        assertNotNull(transition, "forceAwakening must return a transition for an eligible artifact");

        // `debug inspect` must resolve the replacement, not the stale original
        Artifact replacement = transition.replacement();

        // namingSeed has changed
        assertNotEquals(originalSeed, replacement.getArtifactSeed(),
                "inspect must show the new seed, not the pre-awakening seed");

        // awakeningPath is set (not dormant)
        assertEquals("dormant", originalAwakeningPath,
                "original artifact must have been dormant before transition");
        assertNotEquals("dormant", replacement.getAwakeningPath(),
                "inspect must show the replacement's named awakening path, not the original dormant state");

        // identity fields reflect replacement, not stale pre-awakening values
        assertNotEquals(artifact.getArtifactSeed(), replacement.getArtifactSeed(),
                "replacement seed must differ from original");

        // storageKey and ownerId stable (required for inspect to resolve the correct record)
        assertEquals(artifact.getArtifactStorageKey(), replacement.getArtifactStorageKey(),
                "storage key must be stable across awakening so inspect resolves correctly");
        assertEquals(artifact.getOwnerId(), replacement.getOwnerId(),
                "owner ID must be stable across awakening");

        // no stale convergence field carried from pre-awakening state
        assertEquals("none", replacement.getConvergencePath(),
                "convergence path must remain none after awakening (no stale pre-transition value)");
    }

    /**
     * Verifies the awakening → convergence identity chain is correct and observable.
     * The valid game sequence is: awaken first (A → B), then converge (B → C).
     * Convergence requires the artifact to already be awakened (awakeningPath != "dormant"),
     * so awakening is always the prerequisite step.
     *
     * Asserts:
     *   - B has awakening metadata (awakeningPath set, seed changed)
     *   - C has convergence metadata (convergencePath set, seed changed again)
     *   - C retains B's awakening metadata (awakening fields carried through convergence)
     *   - namingSeed differs at each stage A → B → C
     *   - storageKey and ownerId remain stable across all transitions
     *   - no stale fields from A (dormant awakening, none convergence) leak into C
     */
    @Test
    void convergenceThenAwakeningProducesCleanReplacementChain() {
        AwakeningEngine awakeningEngine = new AwakeningEngine();
        ConvergenceEngine convergenceEngine = new ConvergenceEngine();

        // Step 1: artifact A eligible for awakening (netherite_sword / ravager)
        Artifact artifactA = new Artifact(UUID.randomUUID(), "netherite_sword");
        artifactA.setArtifactSeed(200L);
        artifactA.setArchetypePath("ravager");
        artifactA.setLatentLineage("ancient-blade");
        for (int i = 0; i < 6; i++) artifactA.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifactA.getMemory().record(ArtifactMemoryEvent.FIRST_KILL);
        artifactA.addLoreHistory("campaign-alpha");
        artifactA.addLoreHistory("campaign-beta");
        artifactA.addLoreHistory("campaign-gamma");

        ArtifactReputation repA = new ArtifactReputation();
        repA.setBrutality(20);
        repA.setKills(6);

        // Step 2: `debug awaken` on A — A → B
        ArtifactIdentityTransition transitionAB = awakeningEngine.forceAwakening(null, artifactA, repA);
        assertNotNull(transitionAB, "forceAwakening must produce a transition for an eligible artifact A");

        Artifact artifactB = transitionAB.replacement();

        // B has awakening metadata; convergence path is still clean
        assertNotEquals("dormant", artifactB.getAwakeningPath(),
                "B must carry a named awakening path");
        assertEquals("none", artifactB.getConvergencePath(),
                "B must not yet have convergence metadata");
        assertNotEquals(artifactA.getArtifactSeed(), artifactB.getArtifactSeed(),
                "B seed must differ from A seed");
        assertEquals(artifactA.getArtifactStorageKey(), artifactB.getArtifactStorageKey(),
                "storage key must be stable A→B");
        assertEquals(artifactA.getOwnerId(), artifactB.getOwnerId(),
                "owner ID must be stable A→B");

        // Step 3: `debug fuse` on B — B → C
        // reaper-vow supports MELEE_WEAPON (netherite_sword), requires totalScore >= 96,
        // bossKills >= 1, memoryPressure >= 5 (B inherits A's 7 memory events → pressure=7 ✓)
        ArtifactReputation repB = new ArtifactReputation();
        repB.setBrutality(25);
        repB.setPrecision(25);
        repB.setSurvival(15);
        repB.setKills(12);
        repB.setBossKills(2);
        // totalScore = 25+25+15+(12*2)+(2*5) = 99 >= 96 ✓

        ArtifactIdentityTransition transitionBC = convergenceEngine.evaluateSimulation(artifactB, repB);
        assertNotNull(transitionBC, "convergence must produce a transition for an awakened artifact B");

        Artifact artifactC = transitionBC.replacement();

        // C has convergence metadata
        assertNotEquals("none", artifactC.getConvergencePath(),
                "C must carry a named convergence path");

        // C retains B's awakening metadata — convergence createReplacement must not drop awakening fields
        assertEquals(artifactB.getAwakeningPath(), artifactC.getAwakeningPath(),
                "C must retain B's awakening path");

        // namingSeed differs at each stage
        assertNotEquals(artifactA.getArtifactSeed(), artifactB.getArtifactSeed(),
                "A and B seeds must differ");
        assertNotEquals(artifactB.getArtifactSeed(), artifactC.getArtifactSeed(),
                "B and C seeds must differ");
        assertNotEquals(artifactA.getArtifactSeed(), artifactC.getArtifactSeed(),
                "A and C seeds must differ");

        // storageKey and ownerId stable across entire chain
        assertEquals(artifactA.getArtifactStorageKey(), artifactC.getArtifactStorageKey(),
                "storage key must be stable A→B→C");
        assertEquals(artifactA.getOwnerId(), artifactC.getOwnerId(),
                "owner ID must be stable A→B→C");

        // no stale A fields in C: A was dormant and unconverged, C must carry both
        assertEquals("dormant", artifactA.getAwakeningPath(),
                "original A was dormant before any transition");
        assertNotEquals("dormant", artifactC.getAwakeningPath(),
                "C must not revert to A's dormant awakening state");
        assertNotEquals("none", artifactC.getConvergencePath(),
                "C must not revert to A's none convergence path");
    }

    @Test
    void convergenceEvaluateReturnsTransitionWithReasonForLineageRecording() {
        ConvergenceEngine engine = new ConvergenceEngine();

        Artifact artifact = new Artifact(UUID.randomUUID(), "bow");
        artifact.setArtifactSeed(41L);
        artifact.setArchetypePath("deadeye");
        artifact.setEvolutionPath("advanced-deadeye");
        artifact.setLatentLineage("lineage-omega");
        // Convergence requires awakeningPath != "dormant" (artifact must have been awakened first)
        artifact.setAwakeningPath("Stormblade");
        artifact.getMemory().record(ArtifactMemoryEvent.AWAKENING);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        // historyScore = loreHistory(0) + notableEvents(1) + memory pressure(6) = 7
        // horizon-syndicate minHistoryScore = max(6, 92/12) = 7
        artifact.addNotableEvent("awakening.stormblade");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(28);
        rep.setMobility(18);
        rep.setBrutality(12);
        rep.setKills(14);
        rep.setBossKills(2);

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);

        // The `debug fuse` command calls engine.evaluate() and then:
        //   replaceIdentity(), transitionIdentity(), recordIdentityTransition(), trackConvergenceParticipation()
        // All require a non-null transition with a valid reason.
        assertNotNull(transition,
                "convergence evaluate must return a transition for an eligible artifact");
        assertNotNull(transition.reason(),
                "transition reason must be set for lineage recording in the fuse command");
        assertFalse(transition.reason().isBlank(),
                "transition reason must not be blank");
        assertEquals(artifact.getArtifactStorageKey(), transition.replacement().getArtifactStorageKey(),
                "storage key must be preserved for replaceIdentity() to work");
        assertEquals(artifact.getOwnerId(), transition.replacement().getOwnerId(),
                "owner must be preserved");
        assertNotEquals("none", transition.replacement().getConvergencePath(),
                "replacement must carry a named convergence path");
    }
}
