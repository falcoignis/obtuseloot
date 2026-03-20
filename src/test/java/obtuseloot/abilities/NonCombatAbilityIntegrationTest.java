package obtuseloot.abilities;

import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NonCombatAbilityIntegrationTest {

    @Test
    void registryContainsCuratedNonCombatPool() {
        AbilityRegistry registry = new AbilityRegistry();
        assertTrue(registry.templates().size() >= 50);
        assertTrue(registry.templates().stream().noneMatch(t -> t.trigger() == AbilityTrigger.ON_HIT || t.trigger() == AbilityTrigger.ON_KILL));
    }


    @Test
    void registryContainsEcologicalAbilityBatch() {
        AbilityRegistry registry = new AbilityRegistry();
        List<String> ids = registry.templates().stream().map(AbilityTemplate::id).toList();
        assertTrue(ids.contains("exploration.trail_sense"));
        assertTrue(ids.contains("gathering.forager_memory"));
        assertTrue(ids.contains("ritual.pattern_resonance"));
        assertTrue(ids.contains("social.witness_imprint"));
        assertTrue(ids.contains("environment.terrain_affinity"));
    }

    @Test
    void metadataIsPresentForAllAbilities() {
        AbilityRegistry registry = new AbilityRegistry();
        assertTrue(registry.templates().stream().allMatch(t -> t.metadata() != null && !t.metadata().utilityDomains().isEmpty()));
        assertTrue(registry.templates().stream().allMatch(t -> t.metadata().triggerBudgetProfile() != null));
    }

    @Test
    void movementAndStructureTriggersAreRepresented() {
        AbilityRegistry registry = new AbilityRegistry();
        List<AbilityTrigger> triggers = registry.templates().stream().map(AbilityTemplate::trigger).toList();
        assertTrue(triggers.contains(AbilityTrigger.ON_WORLD_SCAN));
        assertTrue(triggers.contains(AbilityTrigger.ON_STRUCTURE_SENSE));
        assertTrue(triggers.contains(AbilityTrigger.ON_BLOCK_HARVEST));
    }

    @Test
    void memoryWatchfulRitualProfilesSelectDifferentAbilities() {
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(new AbilityRegistry());

        Artifact memoryArtifact = artifact(11L);
        Artifact watchfulArtifact = artifact(12L);
        Artifact ritualArtifact = artifact(13L);

        ArtifactMemoryProfile memoryProfile = new ArtifactMemoryProfile(9, 1.1D, 1.6D, 1.0D, 1.0D, 1.0D, 0.8D, 0.6D);
        ArtifactMemoryProfile watchfulProfile = new ArtifactMemoryProfile(3, 0.7D, 0.8D, 1.6D, 1.3D, 1.4D, 0.2D, 0.1D);
        ArtifactMemoryProfile ritualProfile = new ArtifactMemoryProfile(7, 1.2D, 1.0D, 1.1D, 0.9D, 0.8D, 1.3D, 0.6D);

        List<String> memoryIds = ids(generator.generate(memoryArtifact, 4, memoryProfile));
        List<String> watchfulIds = ids(generator.generate(watchfulArtifact, 4, watchfulProfile));
        List<String> ritualIds = ids(generator.generate(ritualArtifact, 4, ritualProfile));

        assertNotEquals(memoryIds, watchfulIds);
        assertNotEquals(ritualIds, watchfulIds);
    }


    @Test
    void registryPoolAvoidsDirectDamageCombatTriggers() {
        AbilityRegistry registry = new AbilityRegistry();
        assertTrue(registry.templates().stream().noneMatch(t -> switch (t.trigger()) {
            case ON_HIT, ON_KILL, ON_BOSS_KILL, ON_CHAIN_COMBAT, ON_MULTI_KILL -> true;
            default -> false;
        }));
    }

    @Test
    void expandedRegistryCoversAllNewCategories() {
        AbilityRegistry registry = new AbilityRegistry();
        for (AbilityCategory category : AbilityCategory.values()) {
            assertTrue(registry.templates().stream().anyMatch(t -> t.category() == category), "missing category " + category);
            assertTrue(registry.byCategory(category).size() >= 2, "category should have multiple templates " + category);
        }
    }

    @Test
    void regulatoryFilterCanAdmitStructureAndMemoryAbilities() {
        AbilityRegistry registry = new AbilityRegistry();
        RegulatoryEligibilityFilter filter = new RegulatoryEligibilityFilter();
        AbilityRegulatoryProfile profile = new RegulatoryGateResolver().resolve(
                artifact(77L),
                new GenomeResolver().resolve(77L),
                new ArtifactMemoryProfile(8, 1.0D, 1.5D, 1.0D, 1.2D, 1.0D, 0.7D, 0.3D),
                null,
                null
        );

        List<AbilityTemplate> filtered = filter.filter(registry.templates(), profile);
        assertFalse(filtered.isEmpty());
        assertTrue(filtered.stream().map(AbilityTemplate::trigger).distinct().count() >= 2);
        assertTrue(filtered.stream().anyMatch(t -> t.trigger() == AbilityTrigger.ON_WORLD_SCAN || t.trigger() == AbilityTrigger.ON_BLOCK_INSPECT || t.trigger() == AbilityTrigger.ON_BLOCK_HARVEST));
    }

    private List<String> ids(AbilityProfile profile) {
        return profile.abilities().stream().map(AbilityDefinition::id).sorted().collect(Collectors.toList());
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "elytra");
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.5D);
        artifact.setSeedBrutalityAffinity(0.1D);
        artifact.setSeedSurvivalAffinity(0.5D);
        artifact.setSeedMobilityAffinity(0.5D);
        artifact.setSeedChaosAffinity(0.5D);
        artifact.setSeedConsistencyAffinity(0.5D);
        return artifact;
    }
}
