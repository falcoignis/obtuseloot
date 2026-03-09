package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeMutationEngine;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.lineage.LineageGenomeInheritance;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.artifacts.Artifact;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ProceduralAbilityGenerator {
    private final AbilityRegistry registry;
    private final ArtifactEcosystemSelfBalancingEngine ecosystemEngine;
    private final LineageRegistry lineageRegistry;
    private final LineageInfluenceResolver lineageResolver;
    private final GenomeResolver genomeResolver;
    private final GenomeMutationEngine mutationEngine;
    private final LineageGenomeInheritance lineageGenomeInheritance;
    private final TraitInterferenceResolver traitInterferenceResolver;
    private final ExperienceEvolutionEngine experienceEvolutionEngine;
    private final boolean traitInteractionsEnabled;

    public ProceduralAbilityGenerator(AbilityRegistry registry) {
        this(registry, null, null, null, null);
    }

    public ProceduralAbilityGenerator(AbilityRegistry registry,
                                      ArtifactEcosystemSelfBalancingEngine ecosystemEngine,
                                      LineageRegistry lineageRegistry,
                                      LineageInfluenceResolver lineageResolver) {
        this(registry, ecosystemEngine, lineageRegistry, lineageResolver, null);
    }

    public ProceduralAbilityGenerator(AbilityRegistry registry,
                                      ArtifactEcosystemSelfBalancingEngine ecosystemEngine,
                                      LineageRegistry lineageRegistry,
                                      LineageInfluenceResolver lineageResolver,
                                      ExperienceEvolutionEngine experienceEvolutionEngine) {
        this(registry, ecosystemEngine, lineageRegistry, lineageResolver, experienceEvolutionEngine, true);
    }

    public ProceduralAbilityGenerator(AbilityRegistry registry,
                                      ArtifactEcosystemSelfBalancingEngine ecosystemEngine,
                                      LineageRegistry lineageRegistry,
                                      LineageInfluenceResolver lineageResolver,
                                      ExperienceEvolutionEngine experienceEvolutionEngine,
                                      boolean traitInteractionsEnabled) {
        this.registry = registry;
        this.ecosystemEngine = ecosystemEngine;
        this.lineageRegistry = lineageRegistry;
        this.lineageResolver = lineageResolver;
        this.genomeResolver = new GenomeResolver();
        this.mutationEngine = new GenomeMutationEngine();
        this.lineageGenomeInheritance = new LineageGenomeInheritance();
        this.traitInterferenceResolver = new TraitInterferenceResolver(registry.templates());
        this.experienceEvolutionEngine = experienceEvolutionEngine;
        this.traitInteractionsEnabled = traitInteractionsEnabled;
    }

    public AbilityProfile generate(Artifact artifact, int evolutionStage, ArtifactMemoryProfile memoryProfile) {
        ArtifactLineage lineage = lineageRegistry == null ? null : lineageRegistry.assignLineage(artifact);
        List<AbilityFamily> ranked = new ArrayList<>(List.of(AbilityFamily.values()));
        ranked.sort(Comparator.comparingDouble((AbilityFamily f) -> -scoreFamily(artifact, f, memoryProfile, lineage)));

        ArtifactGenome baseGenome = mutationEngine.mutate(genomeResolver.resolve(artifact.getArtifactSeed()), evolutionStage);
        ArtifactGenome lineageGenome = (lineage == null)
                ? baseGenome
                : lineageGenomeInheritance.inherit(lineage, baseGenome, artifact.getArtifactSeed());
        ArtifactGenome genome = experienceEvolutionEngine == null
                ? lineageGenome
                : experienceEvolutionEngine.applyExperienceFeedback(lineageGenome, artifact.getArtifactSeed());
        int picks = evolutionStage >= 4 ? 3 : 2;
        List<AbilityTemplate> selected = traitInteractionsEnabled
                ? traitInterferenceResolver.selectTop(registry.templates(), genome, picks)
                : registry.templates().stream()
                .sorted(Comparator.comparingDouble((AbilityTemplate t) -> -scoreTemplate(t, artifact, memoryProfile, evolutionStage)))
                .limit(picks)
                .toList();

        long seed = artifact.getArtifactSeed() ^ artifact.getArchetypePath().hashCode() ^ artifact.getEvolutionPath().hashCode() ^ artifact.getDriftAlignment().hashCode()
                ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode() ^ memoryProfile.pressure();
        Random random = new Random(seed);
        List<AbilityDefinition> picked = new ArrayList<>();
        for (AbilityTemplate template : selected) {
            picked.add(fromTemplate(template, template.family(), evolutionStage));
        }

        if (picked.size() < picks) {
            List<AbilityFamily> fallbackRanked = new ArrayList<>(ranked);
            for (AbilityFamily family : fallbackRanked) {
                List<AbilityTemplate> pool = registry.byFamily(family).stream()
                        .filter(t -> picked.stream().noneMatch(p -> p.id().equals(t.id())))
                        .toList();
                if (pool.isEmpty()) {
                    continue;
                }
                AbilityTemplate t = pickTemplate(pool, artifact, memoryProfile, evolutionStage, random);
                picked.add(fromTemplate(t, family, evolutionStage));
                if (picked.size() >= picks) {
                    break;
                }
            }
        }

        if (evolutionStage >= 5 && ("paradox".equalsIgnoreCase(artifact.getDriftAlignment()) || memoryProfile.chaosWeight() > 2.5D)) {
            AbilityTemplate paradox = registry.templates().stream().filter(t -> t.id().contains("chaos.paradox")).findFirst().orElse(null);
            if (paradox != null && picked.stream().noneMatch(a -> a.id().equals(paradox.id()))) {
                picked.add(fromTemplate(paradox, AbilityFamily.CHAOS, evolutionStage));
            }
        }

        return new AbilityProfile("procedural-" + ranked.get(0).name().toLowerCase() + "-s" + evolutionStage, picked);
    }

    private AbilityDefinition fromTemplate(AbilityTemplate t, AbilityFamily family, int stage) { /* unchanged */
        return new AbilityDefinition(t.id(), t.name(), family, t.trigger(), t.mechanic(), t.effectPattern(), t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.fusionVariant(), t.memoryVariant(), t.supportModifiers(),
                List.of(new AbilityEffect(t.effectPattern(), AbilityEffectType.TRIGGERED_BEHAVIOR, 0.015D + (stage * 0.002D))),
                "Template: " + t.id() + " | Core: " + t.effectPattern(), "Evolution: " + t.evolutionVariant(), "Drift: " + t.driftVariant(),
                "Awakening/Fusion: " + t.awakeningVariant() + " / " + t.fusionVariant(), "Memory: " + t.memoryVariant());
    }

    private AbilityTemplate pickTemplate(List<AbilityTemplate> pool, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, Random random) { /* unchanged */
        double total = 0.0D;
        double[] scores = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            scores[i] = Math.max(0.01D, scoreTemplate(pool.get(i), artifact, memoryProfile, stage));
            total += scores[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < pool.size(); i++) {
            roll -= scores[i];
            if (roll <= 0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    private double scoreTemplate(AbilityTemplate template, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage) { /* unchanged */
        double score = 1.0D;
        if (template.trigger() == AbilityTrigger.ON_MEMORY_EVENT) score += memoryProfile.pressure() * 0.08D;
        if (template.trigger() == AbilityTrigger.ON_BOSS_KILL) score += memoryProfile.bossWeight() * 0.15D;
        if (template.mechanic() == AbilityMechanic.UNSTABLE_DETONATION) score += memoryProfile.chaosWeight() * 0.09D;
        if (template.mechanic() == AbilityMechanic.MOVEMENT_ECHO) score += memoryProfile.mobilityWeight() * 0.1D;
        if (template.mechanic() == AbilityMechanic.DEFENSIVE_THRESHOLD || template.mechanic() == AbilityMechanic.RECOVERY_WINDOW) score += memoryProfile.survivalWeight() * 0.1D;
        if (template.id().contains("paradox")) score += ("paradox".equalsIgnoreCase(artifact.getDriftAlignment()) ? 1.2D : 0.0D) + (stage >= 5 ? 0.6D : 0.0D);
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) && template.trigger() == AbilityTrigger.ON_AWAKENING) score += 1.0D;
        if (!"none".equalsIgnoreCase(artifact.getFusionPath()) && template.trigger() == AbilityTrigger.ON_FUSION) score += 0.8D;
        return score;
    }

    private double scoreFamily(Artifact artifact, AbilityFamily family, ArtifactMemoryProfile memoryProfile, ArtifactLineage lineage) {
        double base = switch (family) {
            case PRECISION -> artifact.getSeedPrecisionAffinity() + artifact.getDriftBias("precision") + memoryProfile.disciplineWeight() + (memoryProfile.bossWeight() * 0.2D);
            case BRUTALITY -> artifact.getSeedBrutalityAffinity() + artifact.getDriftBias("brutality") + memoryProfile.aggressionWeight();
            case SURVIVAL -> artifact.getSeedSurvivalAffinity() + artifact.getDriftBias("survival") + memoryProfile.survivalWeight() + (memoryProfile.traumaWeight() * 0.4D);
            case MOBILITY -> artifact.getSeedMobilityAffinity() + artifact.getDriftBias("mobility") + memoryProfile.mobilityWeight();
            case CHAOS -> artifact.getSeedChaosAffinity() + artifact.getDriftBias("chaos") + memoryProfile.chaosWeight() + (memoryProfile.traumaWeight() * 0.3D);
            case CONSISTENCY -> artifact.getSeedConsistencyAffinity() + artifact.getDriftBias("consistency") + memoryProfile.disciplineWeight() + (memoryProfile.bossWeight() * 0.15D);
        };
        String key = family.name().toLowerCase();
        double ecosystem = ecosystemEngine == null ? 1.0D : ecosystemEngine.weightForFamily(key);
        double lineageInfluence = (lineageResolver == null) ? 1.0D : lineageResolver.resolveFamilyInfluence(lineage, key);
        return base * ecosystem * lineageInfluence;
    }
}
