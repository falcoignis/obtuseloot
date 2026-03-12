package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeMutationEngine;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.lineage.LineageGenomeInheritance;
import obtuseloot.evolution.AdaptiveSupportAllocation;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.evolution.UtilityHistoryRollup;
import obtuseloot.artifacts.Artifact;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.lineage.EvolutionaryBiasGenome;
import obtuseloot.lineage.LineageBiasDimension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProceduralAbilityGenerator {

    private static final Map<AbilityTrigger, Double> TRIGGER_SATURATION_WEIGHTS = new EnumMap<>(AbilityTrigger.class);

    static {
        TRIGGER_SATURATION_WEIGHTS.put(AbilityTrigger.ON_WORLD_SCAN, 1.22D);
        TRIGGER_SATURATION_WEIGHTS.put(AbilityTrigger.ON_RITUAL_INTERACT, 1.15D);
        TRIGGER_SATURATION_WEIGHTS.put(AbilityTrigger.ON_BLOCK_INSPECT, 1.08D);
        TRIGGER_SATURATION_WEIGHTS.put(AbilityTrigger.ON_STRUCTURE_SENSE, 1.06D);
        TRIGGER_SATURATION_WEIGHTS.put(AbilityTrigger.ON_STRUCTURE_DISCOVERY, 1.04D);
        TRIGGER_SATURATION_WEIGHTS.put(AbilityTrigger.ON_CHUNK_ENTER, 1.04D);
    }
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
    private final RegulatoryGateResolver regulatoryGateResolver;
    private final RegulatoryEligibilityFilter regulatoryEligibilityFilter;
    private final LatentTraitActivationResolver latentTraitActivationResolver;

    public ProceduralAbilityGenerator(AbilityRegistry registry) {
        this(registry, null, null, null, null);
    }

    public TraitProjectionStats traitProjectionStats() {
        return traitInterferenceResolver.statsSnapshot();
    }

    public void setScoringMode(ScoringMode scoringMode) {
        traitInterferenceResolver.setScoringMode(scoringMode);
    }

    public ScoringMode scoringMode() {
        return traitInterferenceResolver.scoringMode();
    }

    public void resetProjectionStats() {
        traitInterferenceResolver.resetStats();
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
        this(registry, ecosystemEngine, lineageRegistry, lineageResolver, experienceEvolutionEngine, traitInteractionsEnabled, ScoringMode.PROJECTION_WITH_CACHE);
    }

    public ProceduralAbilityGenerator(AbilityRegistry registry,
                                      ArtifactEcosystemSelfBalancingEngine ecosystemEngine,
                                      LineageRegistry lineageRegistry,
                                      LineageInfluenceResolver lineageResolver,
                                      ExperienceEvolutionEngine experienceEvolutionEngine,
                                      boolean traitInteractionsEnabled,
                                      ScoringMode scoringMode) {
        this.registry = registry;
        this.ecosystemEngine = ecosystemEngine;
        this.lineageRegistry = lineageRegistry;
        this.lineageResolver = lineageResolver;
        this.genomeResolver = new GenomeResolver();
        this.mutationEngine = new GenomeMutationEngine();
        this.lineageGenomeInheritance = new LineageGenomeInheritance();
        this.traitInterferenceResolver = new TraitInterferenceResolver(registry.templates());
        this.traitInterferenceResolver.setScoringMode(scoringMode);
        this.experienceEvolutionEngine = experienceEvolutionEngine;
        this.traitInteractionsEnabled = traitInteractionsEnabled;
        this.regulatoryGateResolver = new RegulatoryGateResolver();
        this.regulatoryEligibilityFilter = new RegulatoryEligibilityFilter();
        this.latentTraitActivationResolver = new LatentTraitActivationResolver();
    }

    public AbilityProfile generate(Artifact artifact, int evolutionStage, ArtifactMemoryProfile memoryProfile) {
        UtilityHistoryRollup utilityHistory = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
        ArtifactLineage lineage = lineageRegistry == null ? null : lineageRegistry.assignLineage(artifact);
        double lineageMutationInfluence = lineageResolver == null ? 1.0D : lineageResolver.resolveMutationInfluence(lineage);
        String lineageId = lineage == null ? null : lineage.lineageId();
        AdaptiveSupportAllocation supportAllocation = experienceEvolutionEngine == null
                ? AdaptiveSupportAllocation.neutral()
                : experienceEvolutionEngine.adaptiveSupportFor(artifact.getArtifactSeed(), lineageId, lineageRegistry);
        double mutationEcologyPressure = experienceEvolutionEngine == null
                ? 1.0D
                : experienceEvolutionEngine.mutationEcologyPressureFor(artifact.getArtifactSeed(), lineageId, lineageRegistry);
        List<AbilityFamily> ranked = new ArrayList<>(List.of(AbilityFamily.values()));
        ranked.sort(Comparator.comparingDouble((AbilityFamily f) -> -scoreFamily(artifact, f, memoryProfile, lineage, utilityHistory)));

        ArtifactGenome baseGenome = mutationEngine.mutate(
                genomeResolver.resolve(artifact.getArtifactSeed()),
                evolutionStage,
                lineageMutationInfluence,
                mutationEcologyPressure,
                supportAllocation.mutationOpportunity(),
                lineage == null ? null : lineage.evolutionaryBiasGenome().tendencies());
        ArtifactGenome lineageGenome = (lineage == null)
                ? baseGenome
                : lineageGenomeInheritance.inherit(lineage, baseGenome, artifact.getArtifactSeed());
        ArtifactGenome evolvedGenome = experienceEvolutionEngine == null
                ? lineageGenome
                : experienceEvolutionEngine.applyExperienceFeedback(lineageGenome, artifact.getArtifactSeed(), 1, lineageMutationInfluence, lineageId, lineageRegistry);
        TraitInterferenceSnapshot preActivationInterference = traitInterferenceResolver.summarizeInterference(registry.templates(), evolvedGenome, traitInterferenceResolver.scoringMode());
        LatentActivationContext activationContext = LatentActivationContext.bounded(
                memoryProfile.survivalWeight() / 4.0D,
                preActivationInterference.latentActivationBias(),
                evolutionStage / 6.0D,
                Math.min(1.0D, artifact.getDriftLevel() / 8.0D),
                Math.min(1.0D, memoryProfile.pressure() / 12.0D));
        LatentActivationResult latentActivationResult = latentTraitActivationResolver.resolve(evolvedGenome, activationContext);
        ArtifactGenome genome = latentActivationResult.genome();
        int picks = evolutionStage >= 4 ? 3 : 2;
        AbilityRegulatoryProfile regulatoryProfile = regulatoryGateResolver.resolve(
                artifact,
                genome,
                memoryProfile,
                lineage,
                ecosystemEngine == null ? null : ecosystemEngine.pressureEngine());
        List<AbilityTemplate> allCandidates = registry.templates();
        List<AbilityTemplate> gatedCandidates = regulatoryEligibilityFilter.filter(allCandidates, regulatoryProfile);
        List<AbilityTemplate> scoringPool = gatedCandidates.isEmpty() ? allCandidates : gatedCandidates;
        TraitInterferenceSnapshot activeInterference = traitInterferenceResolver.summarizeInterference(scoringPool, genome, traitInterferenceResolver.scoringMode());

        long seed = artifact.getArtifactSeed() ^ artifact.getArchetypePath().hashCode() ^ artifact.getEvolutionPath().hashCode() ^ artifact.getDriftAlignment().hashCode()
                ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode() ^ memoryProfile.pressure();
        Random random = new Random(seed);

        List<AbilityTemplate> selected = traitInteractionsEnabled
                ? traitInterferenceResolver.selectTopWithInterferenceShuffle(scoringPool, genome, picks, seed ^ 0x5DEECE66DL, 0.94D, 3)
                : scoringPool.stream()
                .sorted(Comparator.comparingDouble((AbilityTemplate t) -> -scoreTemplate(t, artifact, memoryProfile, evolutionStage, utilityHistory, lineage, supportAllocation)))
                .limit(picks)
                .toList();
        List<AbilityDefinition> picked = new ArrayList<>();
        artifact.setLastRegulatoryProfile(regulatoryProfile.profileKey());
        artifact.setLastOpenRegulatoryGates(regulatoryProfile.openGatesCsv());
        artifact.setLastGateCandidatePool(allCandidates.size() + "->" + scoringPool.size());
        artifact.setLastInterferenceEffects(activeInterference.activeEffects().isEmpty() ? "none" : String.join(";", activeInterference.activeEffects().stream().limit(3).toList()));
        artifact.setLastLatentActivationRate(latentActivationResult.activationRate());
        artifact.setLastActivatedLatentTraits(latentActivationResult.activatedTraits().toString());
        for (AbilityTemplate template : selected) {
            picked.add(fromTemplate(template, template.family(), evolutionStage));
        }

        if (picked.size() < picks) {
            List<AbilityFamily> fallbackRanked = new ArrayList<>(ranked);
            for (AbilityFamily family : fallbackRanked) {
                List<AbilityTemplate> pool = scoringPool.stream()
                        .filter(t -> t.family() == family)
                        .filter(t -> picked.stream().noneMatch(p -> p.id().equals(t.id())))
                        .toList();
                if (pool.isEmpty()) {
                    continue;
                }
                AbilityTemplate t = pickTemplate(pool, artifact, memoryProfile, evolutionStage, random, utilityHistory, lineage, supportAllocation);
                picked.add(fromTemplate(t, family, evolutionStage));
                if (picked.size() >= picks) {
                    break;
                }
            }
        }

        if (lineageRegistry != null && lineage != null) {
            EvolutionaryBiasGenome observedBias = deriveObservedBias(selected, memoryProfile, utilityHistory);
            double ecologicalPressure = experienceEvolutionEngine == null ? 1.0D
                    : selected.stream()
                    .mapToDouble(t -> experienceEvolutionEngine.ecologyModifierFor(artifact.getArtifactSeed(), t.mechanic(), t.trigger(), lineage == null ? null : lineage.lineageId(), lineageRegistry))
                    .average()
                    .orElse(1.0D);
            lineageRegistry.recordDescendantBias(artifact, observedBias, ecologicalPressure, lineageMutationInfluence);
        }

        return new AbilityProfile("procedural-" + ranked.get(0).name().toLowerCase() + "-s" + evolutionStage, picked);
    }

    private AbilityDefinition fromTemplate(AbilityTemplate t, AbilityFamily family, int stage) { /* unchanged */
        return new AbilityDefinition(t.id(), t.name(), family, t.trigger(), t.mechanic(), t.effectPattern(), t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.fusionVariant(), t.memoryVariant(), t.supportModifiers(),
                List.of(new AbilityEffect(t.effectPattern(), AbilityEffectType.TRIGGERED_BEHAVIOR, 0.015D + (stage * 0.002D))), t.metadata(),
                "Template: " + t.id() + " | Core: " + t.effectPattern(), "Evolution: " + t.evolutionVariant(), "Drift: " + t.driftVariant(),
                "Awakening/Fusion: " + t.awakeningVariant() + " / " + t.fusionVariant(), "Memory: " + t.memoryVariant());
    }

    private AbilityTemplate pickTemplate(List<AbilityTemplate> pool, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, Random random, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation) {
        double total = 0.0D;
        double[] scores = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            scores[i] = Math.max(0.01D, scoreTemplate(pool.get(i), artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation) * rarityModifier(pool.get(i)));
            total += scores[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < pool.size(); i++) {
            roll -= scores[i];
            if (roll <= 0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    private double scoreTemplate(AbilityTemplate template, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation) {
        double score = 1.0D;
        double utilityYield = clamp(template.metadata().ecologicalYieldScore(), 0.42D, 1.28D);
        score *= utilityYield;
        if (template.trigger() == AbilityTrigger.ON_MEMORY_EVENT || template.trigger() == AbilityTrigger.ON_WITNESS_EVENT) score += memoryProfile.pressure() * 0.08D;
        if (template.trigger() == AbilityTrigger.ON_STRUCTURE_SENSE) score += memoryProfile.bossWeight() * 0.06D;
        if (template.mechanic() == AbilityMechanic.RITUAL_CHANNEL || template.mechanic() == AbilityMechanic.REVENANT_TRIGGER) score += memoryProfile.chaosWeight() * 0.07D;
        if (template.mechanic() == AbilityMechanic.NAVIGATION_ANCHOR || template.trigger() == AbilityTrigger.ON_WORLD_SCAN) score += memoryProfile.mobilityWeight() * 0.1D;
        if (template.mechanic() == AbilityMechanic.HARVEST_RELAY || template.trigger() == AbilityTrigger.ON_BLOCK_HARVEST) score += memoryProfile.survivalWeight() * 0.08D;
        if (template.metadata().affinities().contains("exploration")) score += memoryProfile.mobilityWeight() * 0.05D;
        if (template.metadata().affinities().contains("ritual")) score += memoryProfile.chaosWeight() * 0.05D;
        if (template.metadata().affinities().contains("gathering")) score += memoryProfile.survivalWeight() * 0.045D;
        if (template.metadata().affinities().contains("social")) score += memoryProfile.aggressionWeight() * 0.04D;
        if (template.metadata().hasAffinity("memory")) score += memoryProfile.disciplineWeight() * 0.05D;
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) && template.trigger() == AbilityTrigger.ON_AWAKENING) score += 0.5D;
        if (!"none".equalsIgnoreCase(artifact.getFusionPath()) && template.trigger() == AbilityTrigger.ON_FUSION) score += 0.5D;
        double utilityBias = utilityHistory.utilityScoreForTemplate(template.mechanic(), template.trigger());
        if (utilityHistory.hasUtilityHistory()) {
            double confidence = utilityHistory.confidence();
            score = (score * (1.0D - (0.45D * confidence))) + (utilityBias * (1.9D * confidence));
        }
        double ecology = experienceEvolutionEngine == null
                ? 1.0D
                : experienceEvolutionEngine.ecologyModifierFor(artifact.getArtifactSeed(), template.mechanic(), template.trigger(), lineage == null ? null : lineage.lineageId(), lineageRegistry);
        double triggerSaturation = triggerSaturationPenalty(template);
        double lineageTemplateInfluence = lineageResolver == null ? 1.0D : lineageResolver.resolveTemplateInfluence(lineage, template.metadata());
        double mutationInfluence = lineageResolver == null ? 1.0D : lineageResolver.resolveMutationInfluence(lineage);
        double mechanicInfluence = mechanicLineageWeight(template.mechanic(), lineage);
        double ecologicalCorrection = lineageResolver == null ? 1.0D : lineageResolver.resolveEcologicalCorrection(lineage, ecology);
        double opportunity = supportAllocation == null ? 1.0D : supportAllocation.reinforcementMultiplier();
        return score * rarityModifier(template) * ecology * triggerSaturation * lineageTemplateInfluence * mechanicInfluence * ecologicalCorrection * mutationInfluence * opportunity;
    }

    private double triggerSaturationPenalty(AbilityTemplate template) {
        long total = registry.templates().stream().filter(candidate -> candidate.trigger() == template.trigger()).count();
        double pressureWeight = TRIGGER_SATURATION_WEIGHTS.getOrDefault(template.trigger(), 1.0D);
        double pressure = Math.max(0.0D, total - 1.0D) * 0.045D * pressureWeight;
        return clamp(1.0D - pressure, 0.72D, 1.02D);
    }

    private EvolutionaryBiasGenome deriveObservedBias(List<AbilityTemplate> templates, ArtifactMemoryProfile memoryProfile, UtilityHistoryRollup utilityHistory) {
        EvolutionaryBiasGenome observed = new EvolutionaryBiasGenome();
        if (templates.isEmpty()) {
            return observed;
        }
        double activeRate = templates.stream().filter(template -> template.trigger() != AbilityTrigger.ON_AWAKENING && template.trigger() != AbilityTrigger.ON_FUSION).count() / (double) templates.size();
        observed.add(LineageBiasDimension.ACTIVE_BEHAVIOR, (activeRate - 0.5D) * 0.40D);
        long memoryTemplates = templates.stream().filter(template -> template.metadata().hasAffinity("memory")).count();
        observed.add(LineageBiasDimension.MEMORY_REACTIVITY, ((memoryTemplates / (double) templates.size()) * 0.45D) + (memoryProfile.pressure() * 0.01D));
        double nicheSpread = templates.stream().map(t -> t.mechanic().name()).distinct().count() / (double) templates.size();
        observed.add(LineageBiasDimension.SPECIALIZATION, (0.6D - nicheSpread) * 0.45D);
        observed.add(LineageBiasDimension.WEIRDNESS, templates.stream().filter(t -> t.family() == AbilityFamily.CHAOS).count() / (double) templates.size() * 0.40D);
        observed.add(LineageBiasDimension.RARITY_APPETITE, templates.stream().mapToDouble(this::rarityModifier).average().orElse(1.0D) < 1.0D ? -0.08D : 0.08D);
        observed.add(LineageBiasDimension.PATIENCE, templates.stream().filter(t -> t.trigger() == AbilityTrigger.ON_AWAKENING || t.trigger() == AbilityTrigger.ON_FUSION).count() / (double) templates.size() * 0.30D);
        observed.add(LineageBiasDimension.BUDGET_DISCIPLINE, Math.max(-0.25D, Math.min(0.25D, (templates.stream().mapToDouble(t -> t.metadata().triggerEfficiency()).average().orElse(1.0D) - 1.0D) * 0.4D)));
        observed.add(LineageBiasDimension.UTILITY_DENSITY_PREFERENCE, Math.max(-0.25D, Math.min(0.25D, (utilityHistory.utilityDensity() - 0.5D) * 0.40D)));
        observed.add(LineageBiasDimension.EXPLORATION_PREFERENCE, templates.stream().filter(t -> t.metadata().affinities().contains("exploration")).count() / (double) templates.size() * 0.40D);
        observed.add(LineageBiasDimension.SUPPORT_PREFERENCE, templates.stream().filter(t -> t.metadata().affinities().contains("support")).count() / (double) templates.size() * 0.34D);
        observed.add(LineageBiasDimension.RITUAL_PREFERENCE, templates.stream().filter(t -> t.metadata().affinities().contains("ritual")).count() / (double) templates.size() * 0.40D);
        observed.add(LineageBiasDimension.ENVIRONMENTAL_SENSITIVITY, Math.max(-0.20D, Math.min(0.20D, (memoryProfile.traumaWeight() * 0.02D) + (memoryProfile.bossWeight() * 0.015D))));
        observed.add(LineageBiasDimension.RELIABILITY, templates.stream().filter(t -> t.family() == AbilityFamily.CONSISTENCY || t.family() == AbilityFamily.PRECISION).count() / (double) templates.size() * 0.30D);
        observed.add(LineageBiasDimension.RISK_APPETITE, templates.stream().filter(t -> t.family() == AbilityFamily.BRUTALITY || t.family() == AbilityFamily.CHAOS).count() / (double) templates.size() * 0.30D);
        observed.add(LineageBiasDimension.COMPETITION_TOLERANCE, Math.max(-0.22D, Math.min(0.22D, memoryProfile.aggressionWeight() * 0.04D)));
        return observed;
    }

    private double rarityModifier(AbilityTemplate template) {
        if (ecosystemEngine == null || template == null) {
            return 1.0D;
        }
        double observedShare = ecosystemEngine.branchShare(template.id());
        double targetShare = 0.10D;
        double alpha = 0.5D;
        double modifier = 1.0D + alpha * (targetShare - observedShare);
        return Math.max(0.93D, Math.min(1.07D, modifier));
    }

    private double scoreFamily(Artifact artifact, AbilityFamily family, ArtifactMemoryProfile memoryProfile, ArtifactLineage lineage, UtilityHistoryRollup utilityHistory) {
        double base = switch (family) {
            case PRECISION -> artifact.getSeedPrecisionAffinity() + artifact.getDriftBias("precision") + memoryProfile.disciplineWeight() + (memoryProfile.bossWeight() * 0.2D);
            case BRUTALITY -> artifact.getSeedBrutalityAffinity()
                    + (artifact.getDriftBias("brutality") * 0.15D)
                    + (memoryProfile.aggressionWeight() * 0.10D)
                    + (memoryProfile.mobilityWeight() * 0.35D)
                    + (memoryProfile.disciplineWeight() * 0.25D);
            case SURVIVAL -> artifact.getSeedSurvivalAffinity() + artifact.getDriftBias("survival") + memoryProfile.survivalWeight() + (memoryProfile.traumaWeight() * 0.4D);
            case MOBILITY -> artifact.getSeedMobilityAffinity() + artifact.getDriftBias("mobility") + memoryProfile.mobilityWeight();
            case CHAOS -> artifact.getSeedChaosAffinity() + artifact.getDriftBias("chaos") + memoryProfile.chaosWeight() + (memoryProfile.traumaWeight() * 0.3D);
            case CONSISTENCY -> artifact.getSeedConsistencyAffinity() + artifact.getDriftBias("consistency") + memoryProfile.disciplineWeight() + (memoryProfile.bossWeight() * 0.15D);
        };
        String key = family.name().toLowerCase();
        double ecosystem = ecosystemEngine == null ? 1.0D : ecosystemEngine.weightForFamily(key);
        double lineageInfluence = (lineageResolver == null) ? 1.0D : lineageResolver.resolveFamilyInfluence(lineage, key);
        double mutationInfluence = (lineageResolver == null) ? 1.0D : lineageResolver.resolveMutationInfluence(lineage);
        double profileScore = base * ecosystem * lineageInfluence * mutationInfluence;
        if (experienceEvolutionEngine != null) {
            double familyEcology = registry.templates().stream()
                    .filter(template -> template.family() == family)
                    .mapToDouble(template -> experienceEvolutionEngine.ecologyModifierFor(artifact.getArtifactSeed(), template.mechanic(), template.trigger(), lineage == null ? null : lineage.lineageId(), lineageRegistry))
                    .average()
                    .orElse(1.0D);
            profileScore *= familyEcology * (lineageResolver == null ? 1.0D : lineageResolver.resolveEcologicalCorrection(lineage, familyEcology));
        }
        if (!utilityHistory.hasUtilityHistory()) {
            return profileScore;
        }
        double utilityFamilyBias = registry.templates().stream()
                .filter(template -> template.family() == family)
                .mapToDouble(template -> utilityHistory.utilityScoreForTemplate(template.mechanic(), template.trigger()))
                .max()
                .orElse(0.0D);
        double confidence = utilityHistory.confidence();
        return (profileScore * (1.0D - (0.55D * confidence))) + (utilityFamilyBias * (2.2D * confidence));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double mechanicLineageWeight(AbilityMechanic mechanic, ArtifactLineage lineage) {
        if (lineage == null) {
            return 1.0D;
        }
        EvolutionaryBiasGenome bias = lineage.evolutionaryBiasGenome();
        double risk = bias.tendency(LineageBiasDimension.RISK_APPETITE);
        double specialization = bias.tendency(LineageBiasDimension.SPECIALIZATION);
        double reliability = bias.tendency(LineageBiasDimension.RELIABILITY);
        double directional = switch (mechanic) {
            case UNSTABLE_DETONATION, CHAIN_ESCALATION, BURST_STATE -> (risk * 0.30D) - (reliability * 0.10D);
            case NAVIGATION_ANCHOR, HARVEST_RELAY, DEFENSIVE_THRESHOLD, GUARDIAN_PULSE -> (specialization * 0.18D) + (reliability * 0.14D);
            default -> (risk * 0.08D) + (specialization * 0.10D);
        };
        return Math.max(0.82D, Math.min(1.18D, 1.0D + directional));
    }
}
