package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.evolution.MechanicUtilitySignal;
import obtuseloot.evolution.UtilityHistoryRollup;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoreFragmentGeneratorTest {
    private final LoreFragmentGenerator generator = new LoreFragmentGenerator();
    private final LoreEngine loreEngine = new LoreEngine();

    @Test
    void loreFragmentRetellsRealTransitionsEventsAndIdentitySignals() {
        Artifact artifact = seeded(77L, EquipmentArchetype.DIAMOND_SWORD);
        artifact.setAwakeningPath("storm vow");
        artifact.setAwakeningIdentityShape("tempest duelist");
        artifact.setAwakeningExpressionTrace("lightning cadence");
        artifact.setAwakeningContinuityTrace("oathbound continuity");
        artifact.setConvergencePath("horizon syndicate");
        artifact.setConvergenceIdentityShape("raider saint");
        artifact.setConvergenceExpressionTrace("split mercy");
        artifact.setConvergenceContinuityTrace("borrowed reign");
        artifact.addNotableEvent("awakening.stormblade");
        artifact.addNotableEvent("memory.boss kill");

        String lore = generator.loreFragment(artifact).toLowerCase();

        assertTrue(lore.contains("awakening and convergence") || lore.contains("awakening pulled") || lore.contains("convergence pulled"));
        assertTrue(lore.contains("storm vow") || lore.contains("horizon syndicate") || lore.contains("oathbound continuity") || lore.contains("borrowed reign"));
        assertTrue(lore.contains("stormblade") || lore.contains("boss kill"));
        assertTrue(lore.contains("tempest duelist") || lore.contains("raider saint") || lore.contains("lightning cadence") || lore.contains("split mercy"));
    }

    @Test
    void loreFragmentDoesNotLeakTraceOrDebugStyleTokens() {
        Artifact artifact = seeded(12L, EquipmentArchetype.ELYTRA);
        artifact.setAwakeningPath("stormflight");
        artifact.setAwakeningLoreTrace("tempest-vector:alpha:memory-signature");
        artifact.setConvergenceLoreTrace("trace=debug-leak");
        artifact.setConvergenceContinuityTrace("path->override");
        artifact.addNotableEvent("artifact-convergence:horizon-syndicate:aa44bb");

        String lore = generator.loreFragment(artifact).toLowerCase();

        assertFalse(lore.contains("trace="));
        assertFalse(lore.contains("artifact-convergence"));
        assertFalse(lore.contains("->"));
        assertFalse(lore.contains("aa44bb"));
    }

    @Test
    void epithetFragmentStaysBriefGroundedAndDeterministic() {
        Artifact artifact = seeded(101L, EquipmentArchetype.TRIDENT);
        artifact.setAwakeningPath("undertow choir");
        artifact.setConvergencePath("reef covenant");
        artifact.setConvergenceIdentityShape("tidal jurist");
        artifact.addNotableEvent("memory.shipwreck vigil");

        String first = generator.epithetFragment(artifact);
        String second = generator.epithetFragment(artifact);

        assertEquals(first, second);
        String[] sentences = first.split("(?<=[.!?])\\s+");
        assertTrue(sentences.length >= 1 && sentences.length <= 2);
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            assertFalse(trimmed.isBlank());
            assertTrue(trimmed.split("\\s+").length <= 10, trimmed);
        }
        assertFalse(first.toLowerCase().contains("tier"));
        assertFalse(first.toLowerCase().contains("rarity"));
    }

    @Test
    void loreEnginePlacesSignificanceFirstThenEpithetThenLore() {
        Artifact artifact = seeded(202L, EquipmentArchetype.DIAMOND_CHESTPLATE);
        artifact.setAwakeningPath("bastion hymn");
        artifact.addNotableEvent("memory.last stand");
        ArtifactReputation reputation = new ArtifactReputation();

        List<String> lines = loreEngine.buildLoreLines(artifact, reputation);

        assertFalse(lines.isEmpty());
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("—"));
        assertNotEquals(lines.get(0), lines.get(1));
        assertNotEquals(lines.get(1), lines.get(2));
    }

    @Test
    void loreEnginePlayerFacingStackOmitsInternalStatAndTraceResidue() {
        Artifact artifact = seeded(303L, EquipmentArchetype.TRIDENT);
        artifact.setAwakeningPath("undertow choir");
        artifact.setAwakeningLineageTrace("lineage:undertow-choir:pressure=22");
        artifact.setConvergenceExpressionTrace("trace=debug-leak");
        artifact.addNotableEvent("artifact-convergence:horizon-syndicate:aa44bb");

        ArtifactReputation reputation = new ArtifactReputation();
        reputation.setPrecision(8);
        reputation.setMobility(9);

        String combined = String.join(" ", loreEngine.buildLoreLines(artifact, reputation)).toLowerCase();

        assertFalse(combined.contains(" p8 "));
        assertFalse(combined.contains("trace="));
        assertFalse(combined.contains("pressure"));
        assertFalse(combined.contains("aa44bb"));
    }


    @Test
    void lowSignalFragmentsStaySpecificWithoutOldSafeFallbacks() {
        Artifact artifact = seeded(404L, EquipmentArchetype.DIAMOND_BOOTS);
        artifact.setAwakeningPath("dormant");
        artifact.setConvergencePath("none");
        artifact.setLatentLineage("");
        artifact.setAwakeningLineageTrace("trace=debug");
        artifact.setConvergenceLineageTrace("artifact-cc44dd");
        artifact.setAwakeningExpressionTrace("trace=debug");
        artifact.setConvergenceExpressionTrace("none");
        artifact.setAwakeningIdentityShape("none");
        artifact.setConvergenceIdentityShape("none");
        artifact.setPersistenceOriginTimestamp(System.currentTimeMillis() - (3L * 24L * 60L * 60L * 1000L));
        artifact.setIdentityBirthTimestamp(artifact.getPersistenceOriginTimestamp());

        String significance = new obtuseloot.significance.ArtifactSignificanceResolver().resolve(artifact).format();
        String epithet = generator.epithetFragment(artifact);
        String lore = generator.loreFragment(artifact);

        assertFalse(significance.contains("Formed"));
        assertFalse(epithet.contains("It keeps its own measure"));
        assertFalse(epithet.contains("It keeps its own counsel"));
        assertTrue(epithet.toLowerCase().matches(".*(footing|movement|quick repositioning|steady use).*"), epithet);
        assertTrue(lore.toLowerCase().matches(".*(footing|called|time|use|pressure|power|path boots).*"), lore);
    }

    @Test
    void lowSignalEpithetVariesAcrossSimilarArtifactsByIdentitySeedAndRole() {
        Artifact boots = seeded(405L, EquipmentArchetype.DIAMOND_BOOTS);
        Artifact helm = seeded(406L, EquipmentArchetype.DIAMOND_HELMET);
        for (Artifact artifact : List.of(boots, helm)) {
            artifact.setAwakeningPath("dormant");
            artifact.setConvergencePath("none");
            artifact.setLatentLineage("");
            artifact.setAwakeningExpressionTrace("none");
            artifact.setConvergenceExpressionTrace("none");
            artifact.setAwakeningIdentityShape("none");
            artifact.setConvergenceIdentityShape("none");
        }

        String bootsEpithet = generator.epithetFragment(boots);
        String helmEpithet = generator.epithetFragment(helm);

        assertNotEquals(bootsEpithet, helmEpithet);
        assertTrue(bootsEpithet.toLowerCase().contains("footing") || bootsEpithet.toLowerCase().contains("movement"));
        assertTrue(helmEpithet.toLowerCase().contains("watchfulness")
                || helmEpithet.toLowerCase().contains("measured watch")
                || helmEpithet.toLowerCase().contains("steady use"));
    }

    @Test
    void lowSignalConsistencyArtifactsStayQuietWithoutFallingBackToSteadyUse() {
        Artifact boots = seeded(407L, EquipmentArchetype.DIAMOND_BOOTS);
        boots.setSeedPrecisionAffinity(0.18D);
        boots.setSeedBrutalityAffinity(0.12D);
        boots.setSeedSurvivalAffinity(0.24D);
        boots.setSeedMobilityAffinity(0.21D);
        boots.setSeedChaosAffinity(0.09D);
        boots.setSeedConsistencyAffinity(0.91D);
        boots.setPersistenceOriginTimestamp(System.currentTimeMillis() - (4L * 24L * 60L * 60L * 1000L));
        boots.setIdentityBirthTimestamp(boots.getPersistenceOriginTimestamp());

        String epithet = generator.epithetFragment(boots).toLowerCase();
        String combined = String.join(" ", loreEngine.buildLoreLines(boots, new ArtifactReputation())).toLowerCase();

        assertFalse(epithet.contains("steady use"), epithet);
        assertFalse(combined.contains("steady path boots"), combined);
        assertFalse(combined.contains("steady use"), combined);
        assertTrue(epithet.contains("reliable footing") || epithet.contains("footing"), epithet);
        assertTrue(combined.contains("sure-footed path boots")
                || combined.contains("reliable footing")
                || combined.contains("footing")
                || combined.contains("time"), combined);
    }

    @Test
    void highSignalEpithetVariationUsesMoreThanTinySharedPool() {
        java.util.Set<String> epithets = new java.util.LinkedHashSet<>();
        for (long seed = 408L; seed < 414L; seed++) {
            Artifact chestplate = seeded(seed, EquipmentArchetype.DIAMOND_CHESTPLATE);
            chestplate.setConvergencePath("bastion braid");
            chestplate.setConvergenceIdentityShape("shield cantor");
            chestplate.setConvergenceExpressionTrace("borrowed shelter");
            chestplate.setAwakeningPath("citadel hymn");
            chestplate.setAwakeningIdentityShape("ward oath");
            chestplate.setAwakeningExpressionTrace("held line");
            epithets.add(generator.epithetFragment(chestplate));
        }

        assertTrue(epithets.size() >= 4, epithets.toString());
        assertTrue(epithets.stream().anyMatch(line -> line.toLowerCase().contains("holding ground")
                || line.toLowerCase().contains("newer edge")
                || line.toLowerCase().contains("two claims")), epithets.toString());
    }

    @Test
    void lowSignalLoreUsesUtilityHistoryAsHonestAnchor() {
        Artifact boots = seeded(409L, EquipmentArchetype.DIAMOND_BOOTS);
        boots.setLastUtilityHistory(new UtilityHistoryRollup(
                0.64D,
                0.39D,
                0.58D,
                0.12D,
                0.52D,
                8L,
                Map.of(
                        "GLIDE_VECTOR@ON_JUMP", signal("GLIDE_VECTOR@ON_JUMP", 0.52D, 0.31D, 4L, 2L),
                        "DASH_BURST@ON_SPRINT", signal("DASH_BURST@ON_SPRINT", 0.49D, 0.28D, 4L, 2L)
                )).encode());

        String lore = generator.loreFragment(boots).toLowerCase();

        assertTrue(lore.contains("learned")
                || lore.contains("practice")
                || lore.contains("worth")
                || lore.contains("path boots"), lore);
        assertFalse(lore.contains("placeholder"));
    }

    private Artifact seeded(long seed, EquipmentArchetype archetype) {
        Artifact artifact = new Artifact(UUID.randomUUID(), archetype);
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.72D);
        artifact.setSeedSurvivalAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.DEFENSIVE_ARMOR) ? 0.92D : 0.34D);
        artifact.setSeedMobilityAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.MOBILITY) ? 0.87D : 0.28D);
        artifact.setSeedBrutalityAffinity(archetype.hasRole(obtuseloot.artifacts.EquipmentRole.WEAPON) ? 0.81D : 0.22D);
        artifact.setSeedChaosAffinity(0.31D);
        artifact.setSeedConsistencyAffinity(0.63D);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact;
    }

    private static MechanicUtilitySignal signal(String key, double validatedUtility, double utilityDensity, long attempts, long meaningful) {
        return new MechanicUtilitySignal(key, validatedUtility, utilityDensity, 0.5D, 0.08D, 0.04D, 0.02D, attempts, meaningful, 0.45D);
    }
}
