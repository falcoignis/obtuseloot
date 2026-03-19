package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntraCategoryNormalizationProbeTest {

    @Test
    void smallCategoryNormalizationKeepsStealthReachableWithoutRunawayLeader() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);

        DistributionProbe stealth = probeCategory(generator, registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION, stealthProfile(), 40, 910_000L);
        DistributionProbe survival = probeCategory(new ProceduralAbilityGenerator(registry), registry, AbilityCategory.SURVIVAL_ADAPTATION, survivalProfile(), 40, 920_000L);
        DistributionProbe sensing = probeCategory(new ProceduralAbilityGenerator(registry), registry, AbilityCategory.SENSING_INFORMATION, sensingProfile(), 40, 930_000L);

        System.out.println("STEALTH_NORMALIZATION_PROBE hits=" + stealth.totalHits + " dist=" + summarize(stealth.distribution));
        System.out.println("SURVIVAL_NORMALIZATION_PROBE hits=" + survival.totalHits + " dist=" + summarize(survival.distribution));
        System.out.println("SENSING_NORMALIZATION_PROBE hits=" + sensing.totalHits + " dist=" + summarize(sensing.distribution));

        assertTrue(stealth.totalHits >= 15, "Stealth probe should generate enough observations for reachability auditing.");
        assertEquals(categoryTemplateCount(registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION), stealth.distribution.size(),
                "All stealth templates must remain reachable.");
        assertTrue(stealth.topShare() < 0.70D, "Stealth normalization should reduce but not eliminate concentration in the probe.");
        assertTrue(stealth.topThreeShare() < 0.90D, "Stealth tail templates should still remain in the mix.");

        assertTrue(survival.totalHits >= 25, "Survival probe should remain active.");
        assertTrue(survival.topThreeShare() < 0.65D, "Survival top-3 should remain below 65%.");

        assertTrue(sensing.totalHits >= 8, "Sensing probe should remain active.");
        assertTrue(sensing.topShare() < 0.34D, "Sensing should remain balanced without a single runaway leader.");
        assertTrue(sensing.topThreeShare() < 0.72D, "Sensing should retain balanced spread.");
    }

    private DistributionProbe probeCategory(ProceduralAbilityGenerator generator,
                                            AbilityRegistry registry,
                                            AbilityCategory category,
                                            ArtifactMemoryProfile memoryProfile,
                                            int artifacts,
                                            long seedBase) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        int hits = 0;
        for (int i = 0; i < artifacts; i++) {
            AbilityProfile profile = generator.generate(artifact(seedBase + i), 4, memoryProfile);
            for (AbilityDefinition ability : profile.abilities()) {
                AbilityCategory actualCategory = categoryFor(registry, ability.id());
                if (actualCategory != category) {
                    continue;
                }
                counts.merge(ability.id(), 1, Integer::sum);
                hits++;
            }
        }
        return new DistributionProbe(counts, hits);
    }

    private int categoryTemplateCount(AbilityRegistry registry, AbilityCategory category) {
        return (int) registry.templates().stream().filter(template -> template.category() == category).count();
    }

    private AbilityCategory categoryFor(AbilityRegistry registry, String id) {
        return registry.templates().stream()
                .filter(template -> template.id().equals(id))
                .map(AbilityTemplate::category)
                .findFirst()
                .orElseThrow();
    }

    private String summarize(Map<String, Integer> distribution) {
        int total = Math.max(1, distribution.values().stream().mapToInt(Integer::intValue).sum());
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> entry.getKey() + "=" + String.format("%.2f", entry.getValue() / (double) total))
                .collect(Collectors.joining(", "));
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey("artifact:" + seed);
        artifact.setLatentLineage("normalization-probe");
        artifact.setSeedPrecisionAffinity(0.50D);
        artifact.setSeedBrutalityAffinity(0.24D);
        artifact.setSeedSurvivalAffinity(0.58D);
        artifact.setSeedMobilityAffinity(0.58D);
        artifact.setSeedChaosAffinity(0.54D);
        artifact.setSeedConsistencyAffinity(0.56D);
        return artifact;
    }

    private ArtifactMemoryProfile stealthProfile() {
        return new ArtifactMemoryProfile(7, 1.0D, 1.1D, 1.1D, 0.9D, 1.7D, 0.9D, 0.5D);
    }

    private ArtifactMemoryProfile survivalProfile() {
        return new ArtifactMemoryProfile(8, 1.3D, 0.9D, 0.9D, 1.8D, 0.8D, 0.5D, 0.4D);
    }

    private ArtifactMemoryProfile sensingProfile() {
        return new ArtifactMemoryProfile(8, 1.4D, 1.2D, 0.8D, 1.0D, 1.6D, 0.6D, 0.3D);
    }

    private record DistributionProbe(Map<String, Integer> distribution, int totalHits) {
        private double topShare() {
            return distribution.values().stream().mapToInt(Integer::intValue).max().orElse(0) / (double) Math.max(1, totalHits);
        }

        private double topThreeShare() {
            return distribution.values().stream()
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .mapToInt(Integer::intValue)
                    .sum() / (double) Math.max(1, totalHits);
        }
    }
}
