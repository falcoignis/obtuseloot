package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageBiasDimension;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.evolution.MechanicNicheTag;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColdTemplateBootstrappingProbeTest {

    @Test
    void coldBootstrappingRestoresReachabilityWithoutBreakingDistributionOrNovelty() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);
        LineageRegistry lineageRegistry = new LineageRegistry();
        ProceduralAbilityGenerator lineageGenerator = new ProceduralAbilityGenerator(registry, null, lineageRegistry, new LineageInfluenceResolver());

        CategoryProbe stealth = probeCategory(generator, registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION, stealthProfile(), 120, 1_000_000L, null, 0.0D, 0.0D, 0.0D);
        CategoryProbe defense = probeCategory(generator, registry, AbilityCategory.DEFENSE_WARDING, defenseProfile(), 120, 2_000_000L, null, 0.0D, 0.0D, 0.0D);
        CategoryProbe ritual = probeCategory(lineageGenerator, registry, AbilityCategory.RITUAL_STRANGE_UTILITY, ritualProfile(), 150, 3_000_000L, "cold-bootstrap-ritual", 0.04D, 0.30D, 0.24D);
        NoveltySnapshot novelty = noveltyProbe(new ProceduralAbilityGenerator(registry), 180, 4_000_000L);

        System.out.println("COLD_BOOTSTRAP_STEALTH hits=" + stealth.totalHits + " reachable=" + stealth.distribution.size() + "/" + templateCount(registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION) + " dist=" + summarize(stealth.distribution));
        System.out.println("COLD_BOOTSTRAP_DEFENSE hits=" + defense.totalHits + " reachable=" + defense.distribution.size() + "/" + templateCount(registry, AbilityCategory.DEFENSE_WARDING) + " dist=" + summarize(defense.distribution));
        System.out.println("COLD_BOOTSTRAP_RITUAL hits=" + ritual.totalHits + " reachable=" + ritual.distribution.size() + "/" + templateCount(registry, AbilityCategory.RITUAL_STRANGE_UTILITY) + " dist=" + summarize(ritual.distribution));
        System.out.printf("COLD_BOOTSTRAP_NOVELTY avg_novelty=%.4f avg_similarity=%.4f intra_novelty=%.4f global_novelty=%.4f%n",
                novelty.averageNovelty(), novelty.averageSimilarity(), novelty.averageIntraNicheNovelty(), novelty.averageGlobalNovelty());

        assertEquals(9, templateCount(registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION));
        assertEquals(templateCount(registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION), stealth.distribution.size(), "Stealth should reach every template.");
        assertTrue(stealth.distribution.values().stream().noneMatch(v -> v <= 0), "Stealth should have no zero-hit templates.");
        assertTrue(stealth.topShare() < 0.50D, "Stealth should not collapse into a single dominant template.");

        assertEquals(templateCount(registry, AbilityCategory.DEFENSE_WARDING), defense.distribution.size(), "Defense should reach every template.");
        assertTrue(defense.distribution.containsKey("warding.false_threshold"), "Defense should now surface threshold deception.");
        assertTrue(defense.topShare() < 0.50D, "Defense should stay bounded.");

        assertEquals(templateCount(registry, AbilityCategory.RITUAL_STRANGE_UTILITY), ritual.distribution.size(), "Ritual should reach every template.");
        assertTrue(ritual.distribution.containsKey("ritual.moon_debt"), "Ritual should surface moon_debt.");
        assertTrue(ritual.distribution.containsKey("ritual.oath_circuit"), "Ritual should surface oath_circuit.");
        assertTrue(ritual.topShare() < 0.50D, "Ritual should stay bounded.");

        assertTrue(novelty.averageNovelty() >= 0.17D, "Novelty ordering should remain in the healthy band.");
        assertTrue(novelty.averageSimilarity() < 0.83D, "Similarity should remain below regression thresholds.");
        assertTrue(novelty.averageIntraNicheNovelty() > novelty.averageGlobalNovelty(), "Intra-niche novelty should remain stronger than global novelty.");
    }

    private CategoryProbe probeCategory(ProceduralAbilityGenerator generator,
                                        AbilityRegistry registry,
                                        AbilityCategory category,
                                        ArtifactMemoryProfile memoryProfile,
                                        int artifacts,
                                        long seedBase,
                                        String lineageId,
                                        double explorationBias,
                                        double ritualBias,
                                        double weirdnessBias) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        int hits = 0;
        LineageRegistry lineageRegistry = lineageId == null ? null : new LineageRegistry();
        ProceduralAbilityGenerator actualGenerator = lineageRegistry == null
                ? generator
                : new ProceduralAbilityGenerator(registry, null, lineageRegistry, new LineageInfluenceResolver());
        for (int i = 0; i < artifacts; i++) {
            Artifact artifact = artifact(seedBase + i, lineageId);
            if (lineageRegistry != null) {
                ArtifactLineage lineage = lineageRegistry.assignLineage(artifact);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.EXPLORATION_PREFERENCE, explorationBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.RITUAL_PREFERENCE, ritualBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.WEIRDNESS, weirdnessBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.SUPPORT_PREFERENCE, 0.10D);
            }
            AbilityProfile profile = actualGenerator.generate(artifact, 4, memoryProfile);
            for (AbilityDefinition ability : profile.abilities()) {
                if (categoryFor(registry, ability.id()) != category) {
                    continue;
                }
                counts.merge(ability.id(), 1, Integer::sum);
                hits++;
            }
        }
        return new CategoryProbe(counts, hits);
    }

    private NoveltySnapshot noveltyProbe(ProceduralAbilityGenerator generator, int artifacts, long seedBase) {
        java.util.List<Double> noveltyScores = new java.util.ArrayList<>();
        java.util.List<Double> similarityScores = new java.util.ArrayList<>();
        java.util.List<Double> intraNoveltyScores = new java.util.ArrayList<>();
        java.util.List<Double> globalNoveltyScores = new java.util.ArrayList<>();
        java.util.List<AbilityDiversityIndex.AbilitySignature> observed = new java.util.ArrayList<>();
        AbilityDiversityIndex diversityIndex = AbilityDiversityIndex.instance();
        for (int i = 0; i < artifacts; i++) {
            Artifact artifact = artifact(seedBase + i, null);
            ArtifactMemoryProfile profile = switch (i % 3) {
                case 0 -> stealthProfile();
                case 1 -> defenseProfile();
                default -> ritualProfile();
            };
            AbilityProfile generated = generator.generate(artifact, 4, profile);
            MechanicNicheTag niche = switch (i % 3) {
                case 0 -> MechanicNicheTag.SOCIAL_WORLD_INTERACTION;
                case 1 -> MechanicNicheTag.PROTECTION_WARDING;
                default -> MechanicNicheTag.RITUAL_STRANGE_UTILITY;
            };
            for (AbilityDefinition ability : generated.abilities()) {
                AbilityTemplate template = registryTemplate(ability.id());
                AbilityDiversityIndex.AbilitySignature candidate = diversityIndex.fromTemplate(artifact.getArtifactSeed(), artifact.getLatentLineage(), niche, null, template);
                double nearestGlobal = observed.stream()
                        .mapToDouble(existing -> AbilityDiversityIndex.similarity(candidate, existing))
                        .max()
                        .orElse(0.0D);
                double nearestSameNiche = observed.stream()
                        .filter(existing -> existing.niche() == niche)
                        .mapToDouble(existing -> AbilityDiversityIndex.similarity(candidate, existing))
                        .max()
                        .orElse(0.0D);
                noveltyScores.add(1.0D - nearestGlobal);
                similarityScores.add(nearestGlobal);
                intraNoveltyScores.add(1.0D - nearestSameNiche);
                globalNoveltyScores.add(1.0D - nearestGlobal);
                observed.add(candidate);
            }
        }
        return new NoveltySnapshot(
                noveltyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                similarityScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                intraNoveltyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                globalNoveltyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D));
    }

    private AbilityTemplate registryTemplate(String id) {
        return new AbilityRegistry().templates().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
    }

    private int templateCount(AbilityRegistry registry, AbilityCategory category) {
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

    private Artifact artifact(long seed, String lineageId) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey("artifact:" + seed);
        artifact.setLatentLineage(lineageId == null ? "cold-bootstrap" : lineageId);
        artifact.setSeedPrecisionAffinity(0.52D);
        artifact.setSeedBrutalityAffinity(0.26D);
        artifact.setSeedSurvivalAffinity(0.60D);
        artifact.setSeedMobilityAffinity(0.60D);
        artifact.setSeedChaosAffinity(0.58D);
        artifact.setSeedConsistencyAffinity(0.58D);
        return artifact;
    }

    private ArtifactMemoryProfile stealthProfile() { return new ArtifactMemoryProfile(7, 1.0D, 1.1D, 1.0D, 0.9D, 1.7D, 0.9D, 0.5D); }
    private ArtifactMemoryProfile defenseProfile() { return new ArtifactMemoryProfile(8, 1.2D, 1.4D, 1.0D, 1.8D, 0.9D, 0.7D, 0.4D); }
    private ArtifactMemoryProfile ritualProfile() { return new ArtifactMemoryProfile(8, 1.2D, 1.1D, 0.8D, 0.9D, 0.9D, 1.6D, 0.5D); }

    private record CategoryProbe(Map<String, Integer> distribution, int totalHits) {
        private double topShare() {
            return distribution.values().stream().mapToInt(Integer::intValue).max().orElse(0) / (double) Math.max(1, totalHits);
        }
    }

    private record NoveltySnapshot(double averageNovelty,
                                   double averageSimilarity,
                                   double averageIntraNicheNovelty,
                                   double averageGlobalNovelty) {}
}
