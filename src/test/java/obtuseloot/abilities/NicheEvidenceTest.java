package obtuseloot.abilities;

import obtuseloot.evolution.*;
import obtuseloot.lineage.*;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.EnumMap;
import java.util.TreeMap;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;

class NicheEvidenceTest {
    private final AbilityRegistry registry = new AbilityRegistry().implementedOnly();
    private final ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);
    private final ArtifactMemoryProfile emptyMemory = new ArtifactMemoryProfile(0, 0, 0, 0, 0, 0, 0, 0);

    @Test void unobservedItemsRemainUnspecialized() throws Exception {
        var role = role(UtilityHistoryRollup.empty(), emptyMemory, null);
        assertEquals(MechanicNicheTag.GENERALIST, role.dominantNiche());
        assertEquals(java.util.Set.of(MechanicNicheTag.GENERALIST), role.niches());
    }

    @Test void MemoryAndLineageAreNotFabricatedOutcomeEvidence() throws Exception {
        var lineage = new ArtifactLineage("exploratory-ancestry");
        lineage.evolutionaryBiasGenome().add(LineageBiasDimension.EXPLORATION_PREFERENCE, .5);
        var memories = new ArtifactMemoryProfile(4, 1, 2, 3, 4, 5, 6, 7);
        assertEquals(MechanicNicheTag.GENERALIST, role(UtilityHistoryRollup.empty(), memories, lineage).dominantNiche());
    }

    @Test void missingOutcomesCannotInventAMobilityNicheMultiplier() throws Exception {
        var role = role(UtilityHistoryRollup.empty(), emptyMemory, null);
        Method weight = ProceduralAbilityGenerator.class.getDeclaredMethod("nicheWeight", AbilityTemplate.class,
                ArtifactNicheProfile.class, ArtifactMemoryProfile.class);
        weight.setAccessible(true);
        double mobility = (double) weight.invoke(generator, registry.templateById("mobility.fleetfoot"), role, emptyMemory);
        double engineering = (double) weight.invoke(generator, registry.templateById("engineering.redstone_sympathy"), role, emptyMemory);
        System.out.println("UNOBSERVED_NICHE_BIAS role=" + role.dominantNiche() + " mobility=" + mobility + " engineering=" + engineering);
        assertEquals(engineering, mobility, 1e-12, "No observed niche justifies a preferred family");
    }

    @Test void measuredRolesRemainExactlyThoseClassifiedFromOutcomes() throws Exception {
        var profile = new ArtifactUsageProfile();
        profile.recordUtilityOutcome(new UtilityOutcomeRecord("precision.material_insight", AbilityMechanic.INSIGHT_REVEAL,
                AbilityTrigger.ON_BLOCK_INSPECT, AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.INFORMATION,
                true, true, 1, 1, "observed-block", 1, "block", new AbilityPerformanceMeasurement(4, 60)));
        var history = UtilityHistoryRollup.fromProfile(profile);
        var expected = new EcosystemRoleClassifier().classify(history.signalByMechanicTrigger());
        assertNotEquals(MechanicNicheTag.GENERALIST, expected.dominantNiche());
        assertEquals(expected, role(history, emptyMemory, null));
    }

    @Test void actualMemoryStillInfluencesItsExistingCategoryScore() throws Exception {
        Method score = ProceduralAbilityGenerator.class.getDeclaredMethod("categoryWeight", AbilityTemplate.class,
                ArtifactNicheProfile.class, ArtifactMemoryProfile.class, ArtifactLineage.class, NicheVariantProfile.class);
        score.setAccessible(true);
        var generalist = new EcosystemRoleClassifier().classify(Map.of());
        var template = registry.templateById("mobility.fleetfoot");
        double empty = (double) score.invoke(generator, template, generalist, emptyMemory, null, null);
        double traveled = (double) score.invoke(generator, template, generalist,
                new ArtifactMemoryProfile(1, 0, 0, 0, 0, 1, 0, 0), null, null);
        assertTrue(traveled > empty, "Removing invented role evidence must retain the independent memory score");
    }

    @Test void dominantTagCanHideDistinctSecondaryAbilities() {
        var taxonomy = new NicheTaxonomy();
        var dominantCounts = new TreeMap<String, Integer>();
        var profiles = java.util.List.of(java.util.List.of("mobility.fleetfoot", "precision.material_insight"),
                java.util.List.of("mobility.fleetfoot", "survival.gentle_harvest"));
        var completeCounts = new TreeMap<String, Integer>();
        for (var profile : profiles) {
            var votes = new EnumMap<MechanicNicheTag, Integer>(MechanicNicheTag.class);
            for (var id : profile) {
                var template = registry.templateById(id);
                taxonomy.nichesFor(template.mechanic(), template.trigger()).forEach(tag -> votes.merge(tag, 1, Integer::sum));
            }
            var dominant = votes.entrySet().stream().max(Comparator.<Map.Entry<MechanicNicheTag, Integer>>comparingInt(Map.Entry::getValue)
                    .thenComparing(e -> -e.getKey().ordinal())).orElseThrow().getKey();
            dominantCounts.merge(dominant.name(), 1, Integer::sum);
            completeCounts.merge(profile.stream().sorted().reduce((a,b) -> a + "+" + b).orElseThrow(), 1, Integer::sum);
        }
        assertEquals(0, obtuseloot.analytics.NicheDiversityMetrics.diversity(dominantCounts).simpsonDiversity());
        assertEquals(.5, obtuseloot.analytics.NicheDiversityMetrics.diversity(completeCounts).simpsonDiversity());
    }

    private ArtifactNicheProfile role(UtilityHistoryRollup history, ArtifactMemoryProfile memory, ArtifactLineage lineage) throws Exception {
        Method method = ProceduralAbilityGenerator.class.getDeclaredMethod("resolveScoringNicheProfile",
                UtilityHistoryRollup.class, ArtifactMemoryProfile.class, ArtifactLineage.class);
        method.setAccessible(true);
        return (ArtifactNicheProfile) method.invoke(generator, history, memory, lineage);
    }
}
