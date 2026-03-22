package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7.3A — Disposition Coverage Integrity Check.
 *
 * Verifies that every convergence recipe and every awakening path produces a
 * non-zero shape signal (convergencePull / awakeningPull) in ArtifactDisposition.
 * A zero result means the identity path carries no directional personality weight,
 * which silences tone influence in lore and epithet generation.
 */
class DispositionCoverageTest {

    // ── Convergence recipes ──────────────────────────────────────────────────

    @Test
    void reaperVowConvergenceProducesNonZeroSignal() {
        Artifact artifact = convergenceArtifact("reaper-vow", "reaper-harrow-rite", "netherite_sword:harrow:melee_weapon");
        assertConvergencePullPositive(artifact, "reaper-vow");
    }

    @Test
    void horizonSyndicateConvergenceProducesNonZeroSignal() {
        Artifact artifact = convergenceArtifact("horizon-syndicate", "horizon-glide-wake", "elytra:glide:ranged_weapon");
        assertConvergencePullPositive(artifact, "horizon-syndicate");
    }

    @Test
    void skyBastionConvergenceProducesNonZeroSignal() {
        Artifact artifact = convergenceArtifact("sky-bastion", "aegis-wing-bulwark-rite", "elytra:bulwark:defensive_armor");
        assertConvergencePullPositive(artifact, "sky-bastion");
    }

    @Test
    void citadelHeartConvergenceProducesNonZeroSignal() {
        Artifact artifact = convergenceArtifact("citadel-heart", "citadel-bulwark-rite", "netherite_chestplate:bulwark:defensive_armor");
        assertConvergencePullPositive(artifact, "citadel-heart");
    }

    @Test
    void worldpiercerConvergenceProducesNonZeroSignal() {
        Artifact artifact = convergenceArtifact("worldpiercer", "worldpiercer-deadeye-surge", "trident:deadeye:spear");
        assertConvergencePullPositive(artifact, "worldpiercer");
    }

    // ── Awakening paths ──────────────────────────────────────────────────────

    @Test
    void executionersOathAwakeningProducesNonZeroSignal() {
        Artifact artifact = awakeningArtifact("Executioner's Oath", "reaper-edge-raw:aggression",
                "aggression>survival:balanced:exec");
        assertAwakeningPullPositive(artifact, "Executioner's Oath");
    }

    @Test
    void stormbladeAwakeningProducesNonZeroSignal() {
        Artifact artifact = awakeningArtifact("Stormblade", "tempest-sight-raw:discipline",
                "discipline>survival:balanced:storm");
        assertAwakeningPullPositive(artifact, "Stormblade");
    }

    @Test
    void bulwarkAscendantAwakeningProducesNonZeroSignal() {
        Artifact artifact = awakeningArtifact("Bulwark Ascendant", "bastion-core-raw:survival",
                "survival>discipline:balanced:bulwark");
        assertAwakeningPullPositive(artifact, "Bulwark Ascendant");
    }

    @Test
    void tempestStrideAwakeningProducesNonZeroSignal() {
        Artifact artifact = awakeningArtifact("Tempest Stride", "wind-channel-raw:mobility",
                "mobility>survival:balanced:tempest");
        assertAwakeningPullPositive(artifact, "Tempest Stride");
    }

    @Test
    void voidwakeCovenantAwakeningProducesNonZeroSignal() {
        Artifact artifact = awakeningArtifact("Voidwake Covenant", "void-mark-raw:chaos",
                "chaos>survival:balanced:void");
        assertAwakeningPullPositive(artifact, "Voidwake Covenant");
    }

    @Test
    void lastSurvivorAwakeningProducesNonZeroSignal() {
        Artifact artifact = awakeningArtifact("Last Survivor", "unyielding-guard-raw:survival",
                "survival>discipline:balanced:endure");
        assertAwakeningPullPositive(artifact, "Last Survivor");
    }

    // ── Tone dimension coverage ───────────────────────────────────────────────

    @Test
    void allConvergencePathsProduceAtLeastOneToneDimension() {
        String[][] paths = {
                {"reaper-vow",        "reaper-harrow-rite",     "netherite_sword:harrow:melee_weapon"},
                {"horizon-syndicate", "horizon-glide-wake",     "elytra:glide:ranged_weapon"},
                {"sky-bastion",       "aegis-wing-bulwark-rite", "elytra:bulwark:defensive_armor"},
                {"citadel-heart",     "citadel-bulwark-rite",   "netherite_chestplate:bulwark:defensive_armor"},
                {"worldpiercer",      "worldpiercer-deadeye-surge", "trident:deadeye:spear"},
        };
        for (String[] path : paths) {
            Artifact artifact = convergenceArtifact(path[0], path[1], path[2]);
            ArtifactReputation rep = new ArtifactReputation();
            ArtifactDisposition d = ArtifactDisposition.resolve(artifact, rep);
            assertFalse(d.drive().isBlank(),
                    "Convergence '" + path[0] + "' produced blank drive");
            assertNotNull(d.temperament(),
                    "Convergence '" + path[0] + "' produced null temperament");
            assertNotNull(d.direction(),
                    "Convergence '" + path[0] + "' produced null direction");
        }
    }

    @Test
    void allAwakeningPathsProduceAtLeastOneToneDimension() {
        String[][] paths = {
                {"Executioner's Oath", "reaper-edge-raw:aggression",   "aggression>survival:balanced:exec"},
                {"Stormblade",         "tempest-sight-raw:discipline", "discipline>survival:balanced:storm"},
                {"Bulwark Ascendant",  "bastion-core-raw:survival",    "survival>discipline:balanced:bulwark"},
                {"Tempest Stride",     "wind-channel-raw:mobility",    "mobility>survival:balanced:tempest"},
                {"Voidwake Covenant",  "void-mark-raw:chaos",          "chaos>survival:balanced:void"},
                {"Last Survivor",      "unyielding-guard-raw:survival","survival>discipline:balanced:endure"},
        };
        for (String[] path : paths) {
            Artifact artifact = awakeningArtifact(path[0], path[1], path[2]);
            ArtifactReputation rep = new ArtifactReputation();
            ArtifactDisposition d = ArtifactDisposition.resolve(artifact, rep);
            assertFalse(d.drive().isBlank(),
                    "Awakening '" + path[0] + "' produced blank drive");
            assertNotNull(d.temperament(),
                    "Awakening '" + path[0] + "' produced null temperament");
            assertNotNull(d.direction(),
                    "Awakening '" + path[0] + "' produced null direction");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertConvergencePullPositive(Artifact artifact, String recipeId) {
        ArtifactReputation rep = new ArtifactReputation();
        ArtifactDisposition disposition = ArtifactDisposition.resolve(artifact, rep);
        assertTrue(disposition.convergencePull() > 0.0D,
                "Convergence recipe '" + recipeId + "' produced zero convergencePull — dead personality path");
    }

    private void assertAwakeningPullPositive(Artifact artifact, String profileName) {
        ArtifactReputation rep = new ArtifactReputation();
        ArtifactDisposition disposition = ArtifactDisposition.resolve(artifact, rep);
        assertTrue(disposition.awakeningPull() > 0.0D,
                "Awakening path '" + profileName + "' produced zero awakeningPull — dead personality path");
    }

    private Artifact convergenceArtifact(String convergencePath, String identityShape, String expressionTrace) {
        Artifact artifact = base(1000L + convergencePath.hashCode(), EquipmentArchetype.NETHERITE_SWORD);
        artifact.setConvergencePath(convergencePath);
        artifact.setConvergenceIdentityShape(identityShape);
        artifact.setConvergenceExpressionTrace(expressionTrace);
        artifact.setAwakeningPath("dormant");
        return artifact;
    }

    private Artifact awakeningArtifact(String awakeningPath, String identityShape, String expressionTrace) {
        Artifact artifact = base(2000L + awakeningPath.hashCode(), EquipmentArchetype.NETHERITE_SWORD);
        artifact.setConvergencePath("none");
        artifact.setAwakeningPath(awakeningPath);
        artifact.setAwakeningIdentityShape(identityShape);
        artifact.setAwakeningExpressionTrace(expressionTrace);
        return artifact;
    }

    private Artifact base(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.65D);
        artifact.setSeedBrutalityAffinity(0.55D);
        artifact.setSeedSurvivalAffinity(0.60D);
        artifact.setSeedMobilityAffinity(0.45D);
        artifact.setSeedChaosAffinity(0.30D);
        artifact.setSeedConsistencyAffinity(0.58D);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact;
    }
}
