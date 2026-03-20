package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeMutationEngine;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.lineage.LineageGenomeInheritance;
import obtuseloot.ObtuseLoot;
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
import obtuseloot.evolution.MechanicNicheTag;
import obtuseloot.evolution.NicheTaxonomy;
import obtuseloot.evolution.NicheVariantProfile;
import obtuseloot.evolution.ArtifactNicheProfile;
import obtuseloot.evolution.EcosystemRoleClassifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class ProceduralAbilityGenerator {
    private static final double NOVELTY_FLOOR = 0.15D;
    private static final double NOVELTY_FLOOR_PENALTY = 0.65D;
    private static final double HIGH_SIMILARITY_THRESHOLD = 0.80D;
    private static final double MODERATE_SIMILARITY_THRESHOLD = 0.62D;
    private static final double SAME_NICHE_NOVELTY_WEIGHT = 0.68D;
    private static final double GLOBAL_NOVELTY_WEIGHT = 0.32D;
    private static final double NICHE_WEIGHT_EXPONENT = 1.85D;
    private static final int RECENT_TEMPLATE_WINDOW_LIMIT = 96;
    private static final int RECENT_CATEGORY_WINDOW_LIMIT = 144;
    private static final double TEMPLATE_RECENCY_MAX_SWING = 0.28D;
    private static final double CATEGORY_EXPOSURE_MAX_SWING = 0.08D;
    private static final double TAIL_PRESERVATION_MAX_BOOST = 0.10D;
    private static final double LOW_VOLUME_CATEGORY_MAX_BOOST = 0.14D;
    private static final double MAX_COMBINED_SELECTION_PENALTY = 0.25D;
    private static final double TRIGGER_SMOOTHING_MAX_RELIEF = 0.10D;
    private static final double NARROW_TRIGGER_SMOOTHING_MAX_RELIEF = 0.15D;
    private static final int SMALL_CATEGORY_TEMPLATE_LIMIT = 6;
    private static final double GLOBAL_CATEGORY_COMPRESSION_GAMMA = 1.75D;
    private static final double GLOBAL_CATEGORY_MEAN_ANCHOR_BLEND = 0.24D;
    private static final double GLOBAL_CATEGORY_TOP_CAP_RATIO = 1.50D;
    private static final double CATEGORY_TOP_THREE_SHARE_THRESHOLD = 0.70D;
    private static final double CATEGORY_TOP_THREE_SHARE_TARGET = 0.62D;
    private static final double SMALL_CATEGORY_SAMPLING_WEIGHT_FLOOR = 0.97D;
    private static final double SMALL_CATEGORY_SAMPLING_WEIGHT_CEILING = 1.03D;
    private static final double SMALL_CATEGORY_UNIFORM_BLEND = 0.45D;
    private static final double SMALL_CATEGORY_RECENCY_STRENGTH = 1.35D;
    private static final double SMALL_CATEGORY_DIVERSITY_STRENGTH = 0.20D;
    private static final int COLD_LIFETIME_WARM_THRESHOLD = 2;
    private static final double COLD_TEMPLATE_MAX_BOOST = 0.22D;
    private static final double CATEGORY_LOCAL_COLD_BALANCE_MAX_BOOST = 0.12D;
    private static final double UNDER_SAMPLED_APPLICABILITY_MAX_BOOST = 0.16D;
    private static final double UNDER_SAMPLED_TRIGGER_RELIEF_MAX = 0.08D;
    private static final double HIGH_COMPLEXITY_TEMPLATE_FLOOR = 0.67D;
    private static final double UNSEEN_TEMPLATE_OVERRIDE_PROBABILITY = 0.12D;
    private static final double HIGH_FREQUENCY_CONTEXT_MISALIGN_DAMPENING = 0.88D;

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
    private final NicheTaxonomy nicheTaxonomy;
    private final Map<MechanicNicheTag, Double> nicheTemplatePressure;
    private final Map<AbilityCategory, Double> categoryTemplatePressure;
    private final AbilityDiversityIndex diversityIndex;
    private final EcosystemRoleClassifier roleClassifier;
    private final Deque<String> recentTemplateSelections;
    private final Deque<AbilityCategory> recentCategorySelections;
    private final Map<String, AbilityTemplate> templatesById;
    private final Map<AbilityCategory, Integer> recentCategoryUsage;
    private final Map<String, Integer> recentTemplateUsage;
    private final Map<String, Integer> lifetimeTemplateUsage;

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
        this.nicheTaxonomy = new NicheTaxonomy();
        this.nicheTemplatePressure = computeNicheTemplatePressure();
        this.categoryTemplatePressure = computeCategoryTemplatePressure();
        this.diversityIndex = AbilityDiversityIndex.instance();
        this.roleClassifier = new EcosystemRoleClassifier();
        this.recentTemplateSelections = new ArrayDeque<>();
        this.recentCategorySelections = new ArrayDeque<>();
        this.templatesById = registry.templates().stream().collect(java.util.stream.Collectors.toMap(AbilityTemplate::id, template -> template));
        this.recentCategoryUsage = new EnumMap<>(AbilityCategory.class);
        this.recentTemplateUsage = new java.util.HashMap<>();
        this.lifetimeTemplateUsage = new java.util.HashMap<>();
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
        ArtifactNicheProfile nicheProfile = resolveScoringNicheProfile(utilityHistory, memoryProfile);
        NicheVariantProfile variantProfile = resolveVariantProfile(artifact);
        List<AbilityDiversityIndex.AbilitySignature> activePool = diversityIndex.activePool(artifact.getArtifactSeed());
        AbilityDiversityIndex.AbilitySignature motifAnchor = diversityIndex.motifAnchor(activePool, nicheProfile.dominantNiche(), lineageId, variantProfile);

        long seed = artifact.getArtifactSeed() ^ artifact.getArchetypePath().hashCode() ^ artifact.getEvolutionPath().hashCode() ^ artifact.getDriftAlignment().hashCode()
                ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode() ^ memoryProfile.pressure();
        Random random = new Random(seed);

        List<AbilityTemplate> selected = selectTemplates(scoringPool, artifact, memoryProfile, evolutionStage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, picks, random);
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
                AbilityTemplate t = pickTemplate(pool, artifact, memoryProfile, evolutionStage, random, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, picked.stream().map(AbilityDefinition::id).collect(java.util.stream.Collectors.toSet()));
                picked.add(fromTemplate(t, family, evolutionStage));
                if (picked.size() >= picks) {
                    break;
                }
            }
        }

        recordRecentSelections(selected);
        diversityIndex.record(artifact.getArtifactSeed(), lineageId, nicheProfile.dominantNiche(), variantProfile, selected);
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


    private ArtifactNicheProfile resolveScoringNicheProfile(UtilityHistoryRollup utilityHistory, ArtifactMemoryProfile memoryProfile) {
        ArtifactNicheProfile classified = roleClassifier.classify(utilityHistory.signalByMechanicTrigger());
        if (utilityHistory.hasUtilityHistory() || classified.dominantNiche() != MechanicNicheTag.GENERALIST) {
            return classified;
        }

        MechanicNicheTag inferredDominant = inferDominantNiche(memoryProfile);
        Map<MechanicNicheTag, Double> scores = new EnumMap<>(MechanicNicheTag.class);
        scores.put(inferredDominant, 1.0D + dominantNicheWeight(memoryProfile, inferredDominant));
        scores.put(MechanicNicheTag.GENERALIST, 0.35D);
        return new ArtifactNicheProfile(
                inferredDominant,
                EnumSet.of(inferredDominant, MechanicNicheTag.GENERALIST),
                Map.copyOf(scores),
                classified.specialization());
    }

    private MechanicNicheTag inferDominantNiche(ArtifactMemoryProfile memoryProfile) {
        double navigationScore = memoryProfile.mobilityWeight() + (memoryProfile.disciplineWeight() * 0.12D);
        double ritualScore = memoryProfile.chaosWeight() + (memoryProfile.disciplineWeight() * 0.18D) + (memoryProfile.pressure() * 0.015D);
        double protectionScore = memoryProfile.survivalWeight() + (memoryProfile.traumaWeight() * 0.18D) + (memoryProfile.bossWeight() * 0.12D);
        double supportScore = memoryProfile.aggressionWeight() + (memoryProfile.disciplineWeight() * 0.10D);

        MechanicNicheTag dominant = MechanicNicheTag.NAVIGATION;
        double max = navigationScore;
        if (ritualScore > max) {
            dominant = memoryProfile.disciplineWeight() >= memoryProfile.chaosWeight() ? MechanicNicheTag.MEMORY_HISTORY : MechanicNicheTag.RITUAL_STRANGE_UTILITY;
            max = ritualScore;
        }
        if (protectionScore > max) {
            dominant = memoryProfile.survivalWeight() >= (memoryProfile.traumaWeight() + memoryProfile.bossWeight())
                    ? MechanicNicheTag.ENVIRONMENTAL_ADAPTATION
                    : MechanicNicheTag.PROTECTION_WARDING;
            max = protectionScore;
        }
        if (supportScore > max) {
            dominant = memoryProfile.aggressionWeight() > memoryProfile.disciplineWeight()
                    ? MechanicNicheTag.SOCIAL_WORLD_INTERACTION
                    : MechanicNicheTag.SUPPORT_COHESION;
        }
        return dominant;
    }

    private double dominantNicheWeight(ArtifactMemoryProfile memoryProfile, MechanicNicheTag dominant) {
        return switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> memoryProfile.mobilityWeight() * 0.20D;
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> Math.max(memoryProfile.chaosWeight(), memoryProfile.disciplineWeight()) * 0.18D;
            case ENVIRONMENTAL_ADAPTATION, PROTECTION_WARDING, FARMING_WORLDKEEPING -> memoryProfile.survivalWeight() * 0.20D;
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> Math.max(memoryProfile.aggressionWeight(), memoryProfile.disciplineWeight()) * 0.15D;
            default -> 0.10D;
        };
    }

    private AbilityDefinition fromTemplate(AbilityTemplate t, AbilityFamily family, int stage) { /* unchanged */
        return new AbilityDefinition(t.id(), t.name(), family, t.trigger(), t.mechanic(), t.effectPattern(), t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.fusionVariant(), t.memoryVariant(), t.supportModifiers(),
                List.of(new AbilityEffect(t.effectPattern(), AbilityEffectType.TRIGGERED_BEHAVIOR, 0.015D + (stage * 0.002D))), t.metadata(),
                "Template: " + t.id() + " | Core: " + t.effectPattern(), "Evolution: " + t.evolutionVariant(), "Drift: " + t.driftVariant(),
                "Awakening/Fusion: " + t.awakeningVariant() + " / " + t.fusionVariant(), "Memory: " + t.memoryVariant());
    }

    private AbilityTemplate pickTemplate(List<AbilityTemplate> pool, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, Random random, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation, ArtifactNicheProfile nicheProfile, NicheVariantProfile variantProfile, List<AbilityDiversityIndex.AbilitySignature> activePool, AbilityDiversityIndex.AbilitySignature motifAnchor, Set<String> excludedIds) {
        List<AbilityTemplate> filtered = pool.stream()
                .filter(template -> !excludedIds.contains(template.id()))
                .toList();
        if (filtered.isEmpty()) {
            return pool.get(pool.size() - 1);
        }
        return selectNextTemplate(filtered, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, java.util.List.of(), random);
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
        double nicheSaturation = nicheTemplatePenalty(template);
        double lineageTemplateInfluence = lineageResolver == null ? 1.0D : lineageResolver.resolveTemplateInfluence(lineage, template.metadata());
        double mutationInfluence = lineageResolver == null ? 1.0D : lineageResolver.resolveMutationInfluence(lineage);
        double mechanicInfluence = mechanicLineageWeight(template.mechanic(), lineage);
        double ecologicalCorrection = lineageResolver == null ? 1.0D : lineageResolver.resolveEcologicalCorrection(lineage, ecology);
        double opportunity = supportAllocation == null ? 1.0D : supportAllocation.reinforcementMultiplier();
        return score * rarityModifier(template) * ecology * triggerSaturation * nicheSaturation * lineageTemplateInfluence * mechanicInfluence * ecologicalCorrection * mutationInfluence * opportunity;
    }

    private double triggerSaturationPenalty(AbilityTemplate template) {
        long total = registry.templates().stream().filter(candidate -> candidate.trigger() == template.trigger()).count();
        double pressureWeight = TRIGGER_SATURATION_WEIGHTS.getOrDefault(template.trigger(), 1.0D);
        double pressure = Math.max(0.0D, total - 1.0D) * 0.045D * pressureWeight;
        return clamp(1.0D - pressure, 0.72D, 1.02D);
    }

    private double nicheTemplatePenalty(AbilityTemplate template) {
        double pressure = nicheTaxonomy.nichesFor(template.mechanic(), template.trigger()).stream()
                .mapToDouble(tag -> nicheTemplatePressure.getOrDefault(tag, 0.0D))
                .average()
                .orElse(0.0D);
        pressure += categoryTemplatePressure.getOrDefault(template.category(), 0.0D) * 0.65D;
        double lowYieldPenalty = clamp((0.62D - template.metadata().ecologicalYieldScore()) * 0.12D, 0.0D, 0.07D);
        return clamp(1.0D - pressure - lowYieldPenalty, 0.74D, 1.06D);
    }

    private Map<MechanicNicheTag, Double> computeNicheTemplatePressure() {
        Map<MechanicNicheTag, Integer> counts = new EnumMap<>(MechanicNicheTag.class);
        for (AbilityTemplate template : registry.templates()) {
            for (MechanicNicheTag niche : nicheTaxonomy.nichesFor(template.mechanic(), template.trigger())) {
                counts.merge(niche, 1, Integer::sum);
            }
        }
        double average = counts.values().stream().mapToInt(Integer::intValue).average().orElse(1.0D);
        Map<MechanicNicheTag, Double> pressure = new EnumMap<>(MechanicNicheTag.class);
        counts.forEach((niche, count) -> {
            double crowded = clamp((count - average) / Math.max(1.0D, average), 0.0D, 1.0D);
            pressure.put(niche, crowded * 0.08D);
        });
        return Map.copyOf(pressure);
    }

    private Map<AbilityCategory, Double> computeCategoryTemplatePressure() {
        Map<AbilityCategory, Integer> counts = new EnumMap<>(AbilityCategory.class);
        for (AbilityTemplate template : registry.templates()) {
            counts.merge(template.category(), 1, Integer::sum);
        }
        double average = counts.values().stream().mapToInt(Integer::intValue).average().orElse(1.0D);
        Map<AbilityCategory, Double> pressure = new EnumMap<>(AbilityCategory.class);
        counts.forEach((category, count) -> {
            double crowded = clamp((count - average) / Math.max(1.0D, average), 0.0D, 1.0D);
            pressure.put(category, crowded * 0.07D);
        });
        return Map.copyOf(pressure);
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


    private List<AbilityTemplate> selectTemplates(List<AbilityTemplate> pool, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation, ArtifactNicheProfile nicheProfile, NicheVariantProfile variantProfile, List<AbilityDiversityIndex.AbilitySignature> activePool, AbilityDiversityIndex.AbilitySignature motifAnchor, int picks, Random random) {
        List<AbilityTemplate> remaining = new ArrayList<>(pool);
        List<AbilityTemplate> selected = new ArrayList<>();
        while (!remaining.isEmpty() && selected.size() < picks) {
            AbilityTemplate best = selectNextTemplate(remaining, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected, random);
            if (best == null) {
                break;
            }
            selected.add(best);
            remaining.remove(best);
            if (traitInteractionsEnabled && remaining.size() > 1) {
                Map<AbilityTemplate, Double> rescored = compositeTemplateScores(remaining, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected);
                remaining.sort(Comparator.comparingDouble((AbilityTemplate t) -> rescored.getOrDefault(t, 0.0001D)).reversed());
            }
        }
        return selected;
    }

    private double compositeTemplateScore(AbilityTemplate template, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation, ArtifactNicheProfile nicheProfile, NicheVariantProfile variantProfile, List<AbilityDiversityIndex.AbilitySignature> activePool, AbilityDiversityIndex.AbilitySignature motifAnchor, List<AbilityTemplate> selected) {
        return compositeTemplateScore(template, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected, null);
    }

    private double compositeTemplateScore(AbilityTemplate template, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation, ArtifactNicheProfile nicheProfile, NicheVariantProfile variantProfile, List<AbilityDiversityIndex.AbilitySignature> activePool, AbilityDiversityIndex.AbilitySignature motifAnchor, List<AbilityTemplate> selected, CategoryApplicabilityProfile applicabilityProfile) {
        double baseComposite = baseCompositeTemplateScore(template, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected);
        double applicabilityBias = underSampledApplicabilityBoost(template, nicheProfile, memoryProfile, applicabilityProfile);
        return baseComposite * applicabilityBias;
    }

    private double baseCompositeTemplateScore(AbilityTemplate template, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation, ArtifactNicheProfile nicheProfile, NicheVariantProfile variantProfile, List<AbilityDiversityIndex.AbilitySignature> activePool, AbilityDiversityIndex.AbilitySignature motifAnchor, List<AbilityTemplate> selected) {
        double base = scoreTemplate(template, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation);
        MechanicNicheTag dominantNiche = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        AbilityDiversityIndex.AbilitySignature candidate = diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominantNiche, variantProfile, template);
        AbilitySimilarityProfile similarityProfile = abilitySimilarityProfile(candidate, activePool, selected, artifact, lineage, dominantNiche, variantProfile);
        double intraNovelty = 1.0D - similarityProfile.sameNicheSimilarity();
        double globalNovelty = 1.0D - similarityProfile.globalSimilarity();
        double noveltyBonus = noveltyBonus(intraNovelty, globalNovelty, variantProfile);
        double noveltyFloorPenalty = noveltyFloorPenalty(intraNovelty);
        double similarityPenalty = activePoolSimilarityPenalty(similarityProfile, variantProfile);
        double nicheBias = Math.pow(nicheWeight(template, nicheProfile, memoryProfile), NICHE_WEIGHT_EXPONENT);
        double categoryBias = categoryWeight(template, nicheProfile, memoryProfile, lineage, variantProfile);
        double nicheConsistencyPenalty = nicheConsistencyPenalty(template, nicheProfile, memoryProfile);
        double lineageBias = lineageCombinationBias(template, lineage);
        double motifBias = motifAnchor == null ? 1.0D : motifAnchorBias(candidate, motifAnchor, variantProfile);
        double recencyBias = recentTemplateRecencyBias(template);
        double diversityBias = intraCategoryDiversityBias(template, artifact, lineage, dominantNiche, variantProfile, selected);
        double tailBias = tailPreservationBias(template, intraNovelty, nicheBias * categoryBias, nicheProfile, similarityProfile);
        double coldBias = coldTemplateBoost(template);
        double boundedPenalty = boundedPenaltyMultiplier(recencyBias, nicheConsistencyPenalty);
        double contextAlignDampening = isHighFrequencyContextMisaligned(template) ? HIGH_FREQUENCY_CONTEXT_MISALIGN_DAMPENING : 1.0D;
        return base * nicheBias * categoryBias * boundedPenalty * lineageBias * motifBias * noveltyBonus * noveltyFloorPenalty * similarityPenalty * diversityBias * tailBias * coldBias * contextAlignDampening;
    }

    private Map<AbilityTemplate, Double> compositeTemplateScores(List<AbilityTemplate> templates, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation, ArtifactNicheProfile nicheProfile, NicheVariantProfile variantProfile, List<AbilityDiversityIndex.AbilitySignature> activePool, AbilityDiversityIndex.AbilitySignature motifAnchor, List<AbilityTemplate> selected) {
        Map<AbilityTemplate, Double> scores = new HashMap<>();
        Map<AbilityCategory, List<AbilityTemplate>> byCategory = templates.stream().collect(Collectors.groupingBy(AbilityTemplate::category, () -> new EnumMap<>(AbilityCategory.class), Collectors.toList()));
        for (List<AbilityTemplate> categoryTemplates : byCategory.values()) {
            Map<AbilityTemplate, Double> baseScores = new HashMap<>();
            for (AbilityTemplate template : categoryTemplates) {
                baseScores.put(template, baseCompositeTemplateScore(template, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected));
            }
            Map<AbilityTemplate, CategoryApplicabilityProfile> applicabilityProfiles = categoryApplicabilityProfiles(categoryTemplates, baseScores);
            for (AbilityTemplate template : categoryTemplates) {
                double applicabilityBias = underSampledApplicabilityBoost(template, nicheProfile, memoryProfile, applicabilityProfiles.get(template));
                scores.put(template, baseScores.getOrDefault(template, 0.0001D) * applicabilityBias);
            }
        }
        return scores;
    }

    private Map<AbilityTemplate, CategoryApplicabilityProfile> categoryApplicabilityProfiles(List<AbilityTemplate> templates, Map<AbilityTemplate, Double> baseScores) {
        if (templates.isEmpty()) {
            return Map.of();
        }
        List<AbilityTemplate> sorted = new ArrayList<>(templates);
        sorted.sort(Comparator.comparingDouble((AbilityTemplate template) -> baseScores.getOrDefault(template, 0.0D)).reversed());
        double mean = templates.stream().mapToDouble(template -> baseScores.getOrDefault(template, 0.0D)).average().orElse(0.0D);
        Map<AbilityTemplate, CategoryApplicabilityProfile> profiles = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            AbilityTemplate template = sorted.get(i);
            double score = baseScores.getOrDefault(template, 0.0D);
            double relativeToMean = mean <= 1.0E-9D ? 1.0D : score / mean;
            profiles.put(template, new CategoryApplicabilityProfile(i, relativeToMean));
        }
        return profiles;
    }

    private AbilityTemplate selectNextTemplate(List<AbilityTemplate> remaining,
                                               Artifact artifact,
                                               ArtifactMemoryProfile memoryProfile,
                                               int stage,
                                               UtilityHistoryRollup utilityHistory,
                                               ArtifactLineage lineage,
                                               AdaptiveSupportAllocation supportAllocation,
                                               ArtifactNicheProfile nicheProfile,
                                               NicheVariantProfile variantProfile,
                                               List<AbilityDiversityIndex.AbilitySignature> activePool,
                                               AbilityDiversityIndex.AbilitySignature motifAnchor,
                                               List<AbilityTemplate> selected,
                                               Random random) {
        MechanicNicheTag dominantNiche = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        Map<AbilityCategory, List<AbilityTemplate>> byCategory = new EnumMap<>(AbilityCategory.class);
        for (AbilityTemplate template : remaining) {
            byCategory.computeIfAbsent(template.category(), ignored -> new ArrayList<>()).add(template);
        }
        Map<AbilityCategory, CategorySelectionProfile> categoryProfiles = new EnumMap<>(AbilityCategory.class);
        for (Map.Entry<AbilityCategory, List<AbilityTemplate>> entry : byCategory.entrySet()) {
            Map<AbilityTemplate, Double> categoryTemplateScores = compositeTemplateScores(entry.getValue(), artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected);
            AbilityTemplate bestTemplate = entry.getValue().stream()
                    .max(Comparator.comparingDouble(t -> categoryTemplateScores.getOrDefault(t, 0.0001D)))
                    .orElse(null);
            if (bestTemplate == null) {
                continue;
            }
            double templateScore = categoryTemplateScores.getOrDefault(bestTemplate, 0.0001D);
            double categoryScore = categorySelectionScore(entry.getKey(), bestTemplate, templateScore, dominantNiche, nicheProfile, memoryProfile, activePool, artifact, lineage, variantProfile, selected);
            categoryProfiles.put(entry.getKey(), new CategorySelectionProfile(bestTemplate, templateScore, categoryScore));
        }
        if (categoryProfiles.isEmpty()) {
            return null;
        }
        Map<AbilityCategory, Double> normalizedCategoryScores = normalizeCategoryScores(categoryProfiles);
        double total = normalizedCategoryScores.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll = random.nextDouble() * Math.max(total, 0.0001D);
        AbilityCategory chosenCategory = null;
        for (Map.Entry<AbilityCategory, Double> entry : normalizedCategoryScores.entrySet()) {
            roll -= entry.getValue();
            if (roll <= 0.0D) {
                chosenCategory = entry.getKey();
                break;
            }
        }
        if (chosenCategory == null) {
            chosenCategory = normalizedCategoryScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }
        List<AbilityTemplate> chosenTemplates = byCategory.get(chosenCategory);
        CategorySelectionProfile chosenProfile = categoryProfiles.get(chosenCategory);
        if (chosenTemplates == null || chosenTemplates.isEmpty()) {
            return chosenProfile == null ? null : chosenProfile.bestTemplate();
        }
        if (chosenTemplates.size() == 1) {
            return chosenProfile == null ? null : chosenProfile.bestTemplate();
        }
        return weightedTemplateSelection(chosenTemplates, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected, random);
    }

    private AbilityTemplate weightedTemplateSelection(List<AbilityTemplate> templates,
                                                      Artifact artifact,
                                                      ArtifactMemoryProfile memoryProfile,
                                                      int stage,
                                                      UtilityHistoryRollup utilityHistory,
                                                      ArtifactLineage lineage,
                                                      AdaptiveSupportAllocation supportAllocation,
                                                      ArtifactNicheProfile nicheProfile,
                                                      NicheVariantProfile variantProfile,
                                                      List<AbilityDiversityIndex.AbilitySignature> activePool,
                                                      AbilityDiversityIndex.AbilitySignature motifAnchor,
                                                      List<AbilityTemplate> selected,
                                                      Random random) {
        Map<AbilityTemplate, Double> compositeScores = compositeTemplateScores(templates, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected);
        double[] scores = new double[templates.size()];
        for (int i = 0; i < templates.size(); i++) {
            scores[i] = Math.max(0.0001D, compositeScores.getOrDefault(templates.get(i), 0.0001D));
        }
        double[] samplingScores = normalizeCategoryTemplateScores(templates, scores);
        applyCategoryLocalColdBalancing(templates, samplingScores);
        double total = 0.0D;
        double[] weights = new double[templates.size()];
        for (int i = 0; i < templates.size(); i++) {
            double score = samplingScores[i];
            double weight = Math.sqrt(score);
            if (isSmallCategory(templates.get(i).category())) {
                weight = Math.pow(Math.max(score, 0.0001D), 0.20D);
                weight *= smallCategorySamplingWeight(samplingScores, i);
                weight = lerp(weight, 1.0D, SMALL_CATEGORY_UNIFORM_BLEND);
            }
            weights[i] = weight;
            total += weight;
        }
        total = applyConditionalTopThreeCompression(weights, total);
        double roll = random.nextDouble() * Math.max(total, 0.0001D);
        for (int i = 0; i < templates.size(); i++) {
            roll -= weights[i];
            if (roll <= 0.0D) {
                AbilityTemplate selectedTemplate = templates.get(i);
                return applyUnseenTemplateOverride(selectedTemplate, templates, samplingScores, artifact, memoryProfile, lineage, variantProfile, activePool, selected, random);
            }
        }
        AbilityTemplate selectedTemplate = templates.get(templates.size() - 1);
        return applyUnseenTemplateOverride(selectedTemplate, templates, samplingScores, artifact, memoryProfile, lineage, variantProfile, activePool, selected, random);
    }

    private AbilityTemplate applyUnseenTemplateOverride(AbilityTemplate selectedTemplate,
                                                        List<AbilityTemplate> templates,
                                                        double[] samplingScores,
                                                        Artifact artifact,
                                                        ArtifactMemoryProfile memoryProfile,
                                                        ArtifactLineage lineage,
                                                        NicheVariantProfile variantProfile,
                                                        List<AbilityDiversityIndex.AbilitySignature> activePool,
                                                        List<AbilityTemplate> selected,
                                                        Random random) {
        if (selectedTemplate == null || isUnseenTemplate(selectedTemplate) || random.nextDouble() >= UNSEEN_TEMPLATE_OVERRIDE_PROBABILITY) {
            return selectedTemplate;
        }
        List<Integer> unseenEligibleIndexes = new ArrayList<>();
        for (int i = 0; i < templates.size(); i++) {
            AbilityTemplate candidate = templates.get(i);
            if (!isUnseenTemplate(candidate) || !qualifiesForUnseenOverride(candidate, artifact, memoryProfile, lineage, variantProfile, activePool, selected)) {
                continue;
            }
            unseenEligibleIndexes.add(i);
        }
        if (unseenEligibleIndexes.isEmpty()) {
            return selectedTemplate;
        }
        double total = 0.0D;
        for (int index : unseenEligibleIndexes) {
            total += Math.max(0.0001D, samplingScores[index]);
        }
        double roll = random.nextDouble() * Math.max(total, 0.0001D);
        for (int index : unseenEligibleIndexes) {
            roll -= Math.max(0.0001D, samplingScores[index]);
            if (roll <= 0.0D) {
                return templates.get(index);
            }
        }
        return templates.get(unseenEligibleIndexes.get(unseenEligibleIndexes.size() - 1));
    }

    private double smallCategorySamplingWeight(double[] normalizedScores, int index) {
        if (normalizedScores.length <= 1) {
            return 1.0D;
        }
        double min = java.util.Arrays.stream(normalizedScores).min().orElse(normalizedScores[index]);
        double max = java.util.Arrays.stream(normalizedScores).max().orElse(normalizedScores[index]);
        double span = Math.max(max - min, 1.0E-9D);
        double relative = (normalizedScores[index] - min) / span;
        return SMALL_CATEGORY_SAMPLING_WEIGHT_FLOOR
                + (relative * (SMALL_CATEGORY_SAMPLING_WEIGHT_CEILING - SMALL_CATEGORY_SAMPLING_WEIGHT_FLOOR));
    }

    private double[] normalizeCategoryTemplateScores(List<AbilityTemplate> templates, double[] scores) {
        if (templates.isEmpty() || templates.size() <= 1) {
            return scores;
        }
        double minScore = Double.POSITIVE_INFINITY;
        double maxScore = Double.NEGATIVE_INFINITY;
        double sum = 0.0D;
        for (double score : scores) {
            minScore = Math.min(minScore, score);
            maxScore = Math.max(maxScore, score);
            sum += score;
        }
        if (!Double.isFinite(minScore) || !Double.isFinite(maxScore) || maxScore <= minScore) {
            return scores;
        }
        double meanScore = sum / scores.length;
        double span = Math.max(maxScore - minScore, 1.0E-9D);
        double[] normalizedScores = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            double normalized = clamp((scores[i] - minScore) / span, 0.0D, 1.0D);
            double curved = Math.pow(normalized, 1.0D / GLOBAL_CATEGORY_COMPRESSION_GAMMA);
            double reExpanded = minScore + (curved * span);
            normalizedScores[i] = lerp(reExpanded, meanScore, GLOBAL_CATEGORY_MEAN_ANCHOR_BLEND);
        }
        return applyMeanRelativeSoftTopCap(normalizedScores, meanScore);
    }

    private double[] applyMeanRelativeSoftTopCap(double[] scores, double meanScore) {
        if (scores.length <= 1 || meanScore <= 0.0D || !Double.isFinite(meanScore)) {
            return scores;
        }
        double capStart = meanScore;
        double capRange = Math.max(meanScore * (GLOBAL_CATEGORY_TOP_CAP_RATIO - 1.0D), 1.0E-9D);
        double[] capped = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            double score = Math.max(0.0001D, scores[i]);
            if (score <= capStart) {
                capped[i] = score;
                continue;
            }
            double excess = score - capStart;
            capped[i] = capStart + (capRange * Math.tanh(excess / capRange));
        }
        return capped;
    }

    private double applyConditionalTopThreeCompression(double[] weights, double totalWeight) {
        if (weights.length <= 3 || totalWeight <= 0.0D) {
            return totalWeight;
        }
        if (estimateTopThreeShare(weights, totalWeight) <= CATEGORY_TOP_THREE_SHARE_THRESHOLD) {
            return totalWeight;
        }
        double meanWeight = totalWeight / weights.length;
        double low = 0.0D;
        double high = 1.0D;
        for (int i = 0; i < 24; i++) {
            double mid = (low + high) / 2.0D;
            double[] candidate = new double[weights.length];
            double candidateTotal = 0.0D;
            for (int j = 0; j < weights.length; j++) {
                candidate[j] = lerp(weights[j], meanWeight, mid);
                candidateTotal += candidate[j];
            }
            if (estimateTopThreeShare(candidate, candidateTotal) > CATEGORY_TOP_THREE_SHARE_TARGET) {
                low = mid;
            } else {
                high = mid;
            }
        }
        double adjustedTotal = 0.0D;
        for (int i = 0; i < weights.length; i++) {
            weights[i] = lerp(weights[i], meanWeight, high);
            adjustedTotal += weights[i];
        }
        return adjustedTotal;
    }

    private double estimateTopThreeShare(double[] weights, double totalWeight) {
        if (weights.length == 0 || totalWeight <= 0.0D) {
            return 0.0D;
        }
        return java.util.Arrays.stream(weights)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .limit(3)
                .mapToDouble(weight -> weight / totalWeight)
                .sum();
    }


    private double lerp(double start, double end, double alpha) {
        double t = clamp(alpha, 0.0D, 1.0D);
        return start + ((end - start) * t);
    }

    private double categorySelectionScore(AbilityCategory category,
                                          AbilityTemplate bestTemplate,
                                          double bestTemplateScore,
                                          MechanicNicheTag dominantNiche,
                                          ArtifactNicheProfile nicheProfile,
                                          ArtifactMemoryProfile memoryProfile,
                                          List<AbilityDiversityIndex.AbilitySignature> activePool,
                                          Artifact artifact,
                                          ArtifactLineage lineage,
                                          NicheVariantProfile variantProfile,
                                          List<AbilityTemplate> selected) {
        double categoryExposureBias = categoryExposureBias(category, dominantNiche, activePool);
        double lowVolumeBoost = lowVolumeCategoryBoost(bestTemplate, nicheProfile, activePool, artifact, lineage, variantProfile, selected);
        double categoryStageBias = Math.sqrt(categoryWeight(bestTemplate, nicheProfile, memoryProfile, lineage, variantProfile));
        double nicheStageBias = Math.sqrt(nicheWeight(bestTemplate, nicheProfile, memoryProfile));
        return Math.max(0.0001D, bestTemplateScore * categoryStageBias * nicheStageBias * boundedPenaltyMultiplier(categoryExposureBias, 1.0D) * lowVolumeBoost);
    }

    private Map<AbilityCategory, Double> normalizeCategoryScores(Map<AbilityCategory, CategorySelectionProfile> categoryProfiles) {
        Map<AbilityCategory, Double> normalized = new EnumMap<>(AbilityCategory.class);
        double total = categoryProfiles.values().stream().mapToDouble(CategorySelectionProfile::categoryScore).sum();
        double targetTotal = Math.max(1.0D, categoryProfiles.size());
        double scale = total <= 0.0D ? 1.0D : targetTotal / total;
        categoryProfiles.forEach((category, profile) -> normalized.put(category, Math.max(0.0001D, profile.categoryScore() * scale)));
        return normalized;
    }

    private double boundedPenaltyMultiplier(double... multipliers) {
        double product = 1.0D;
        double totalPenalty = 0.0D;
        for (double multiplier : multipliers) {
            double safe = clamp(multiplier, 0.0D, 2.0D);
            product *= safe;
            if (safe < 1.0D) {
                totalPenalty += 1.0D - safe;
            }
        }
        if (totalPenalty <= MAX_COMBINED_SELECTION_PENALTY) {
            return product;
        }
        double allowedProduct = 1.0D - MAX_COMBINED_SELECTION_PENALTY;
        double lift = allowedProduct / Math.max(product, 0.0001D);
        return clamp(product * Math.max(1.0D, lift), allowedProduct, 2.0D);
    }


    private void recordRecentSelections(List<AbilityTemplate> selected) {
        for (AbilityTemplate template : selected) {
            pushRecentTemplate(template.id());
            pushLifetimeTemplate(template.id());
            pushRecentCategory(template.category());
        }
    }

    private void pushRecentTemplate(String templateId) {
        recentTemplateSelections.addLast(templateId);
        recentTemplateUsage.merge(templateId, 1, Integer::sum);
        while (recentTemplateSelections.size() > RECENT_TEMPLATE_WINDOW_LIMIT) {
            String expired = recentTemplateSelections.pollFirst();
            if (expired != null) {
                decrementCount(recentTemplateUsage, expired);
            }
        }
    }

    private void pushRecentCategory(AbilityCategory category) {
        recentCategorySelections.addLast(category);
        recentCategoryUsage.merge(category, 1, Integer::sum);
        while (recentCategorySelections.size() > RECENT_CATEGORY_WINDOW_LIMIT) {
            AbilityCategory expired = recentCategorySelections.pollFirst();
            if (expired != null) {
                decrementCount(recentCategoryUsage, expired);
            }
        }
    }

    private <T> void decrementCount(Map<T, Integer> counts, T key) {
        counts.computeIfPresent(key, (ignored, current) -> current <= 1 ? null : current - 1);
    }


    private void pushLifetimeTemplate(String templateId) {
        lifetimeTemplateUsage.merge(templateId, 1, (current, increment) -> Math.min(COLD_LIFETIME_WARM_THRESHOLD + 1, current + increment));
    }

    private boolean isUnseenTemplate(AbilityTemplate template) {
        return lifetimeTemplateUsage.getOrDefault(template.id(), 0) <= 0;
    }

    private double coldTemplateBoost(AbilityTemplate template) {
        if (!isColdTemplate(template)) {
            return 1.0D;
        }
        double recentScarcity = 1.0D - clamp(recentTemplateFrequency(template), 0.0D, 1.0D);
        double lifetimeScarcity = 1.0D - clamp(lifetimeTemplateUsage.getOrDefault(template.id(), 0) / (double) COLD_LIFETIME_WARM_THRESHOLD, 0.0D, 1.0D);
        double boost = Math.max(recentScarcity * 0.70D, lifetimeScarcity);
        return clamp(1.0D + (boost * COLD_TEMPLATE_MAX_BOOST), 1.0D, 1.0D + COLD_TEMPLATE_MAX_BOOST);
    }

    private boolean isColdTemplate(AbilityTemplate template) {
        int recentCount = recentTemplateUsage.getOrDefault(template.id(), 0);
        int lifetimeCount = lifetimeTemplateUsage.getOrDefault(template.id(), 0);
        if (recentCount == 0) {
            return lifetimeCount <= COLD_LIFETIME_WARM_THRESHOLD;
        }
        int sameCategoryTotal = recentCategoryTemplateTotal(template.category());
        if (sameCategoryTotal <= 0) {
            return lifetimeCount <= COLD_LIFETIME_WARM_THRESHOLD;
        }
        long categoryTemplates = registry.templates().stream()
                .filter(candidate -> candidate.category() == template.category())
                .count();
        double expectedShare = 1.0D / Math.max(1.0D, categoryTemplates);
        double observedShare = recentCount / (double) Math.max(1, sameCategoryTotal);
        return lifetimeCount <= 1 && observedShare <= (expectedShare * 0.25D);
    }

    private void applyCategoryLocalColdBalancing(List<AbilityTemplate> templates, double[] samplingScores) {
        if (templates.size() <= 1) {
            return;
        }
        long coldTemplates = templates.stream().filter(this::isColdTemplate).count();
        if (coldTemplates == 0 || coldTemplates == templates.size()) {
            return;
        }
        int sameCategoryTotal = recentCategoryTemplateTotal(templates.get(0).category());
        long categoryTemplateCount = templates.size();
        double expectedShare = 1.0D / Math.max(1.0D, categoryTemplateCount);
        for (int i = 0; i < templates.size(); i++) {
            AbilityTemplate template = templates.get(i);
            if (!isColdTemplate(template)) {
                continue;
            }
            double observedShare = sameCategoryTotal <= 0 ? 0.0D : recentTemplateUsage.getOrDefault(template.id(), 0) / (double) sameCategoryTotal;
            double scarcity = clamp((expectedShare - observedShare) / Math.max(expectedShare, 0.0001D), 0.0D, 1.0D);
            samplingScores[i] *= 1.0D + (scarcity * CATEGORY_LOCAL_COLD_BALANCE_MAX_BOOST);
        }
    }

    private double recentTemplateRecencyBias(AbilityTemplate template) {
        int sameCategoryTotal = recentCategoryTemplateTotal(template.category());
        if (sameCategoryTotal <= 0) {
            return 1.03D;
        }
        int templateCount = recentTemplateUsage.getOrDefault(template.id(), 0);
        long uniqueCategoryTemplates = registry.templates().stream()
                .filter(candidate -> candidate.category() == template.category())
                .map(AbilityTemplate::id)
                .distinct()
                .count();
        double expectedShare = 1.0D / Math.max(1.0D, uniqueCategoryTemplates);
        double observedShare = templateCount / (double) sameCategoryTotal;
        double pressure = clamp((expectedShare - observedShare) / Math.max(expectedShare, 0.0001D), -1.0D, 1.0D);
        double negativeSwing = TEMPLATE_RECENCY_MAX_SWING + 0.17D;
        if (isSmallCategory(template.category())) {
            negativeSwing = Math.min(TEMPLATE_RECENCY_MAX_SWING + 0.17D, negativeSwing * SMALL_CATEGORY_RECENCY_STRENGTH);
        }
        if (pressure >= 0.0D) {
            double positiveSwing = TEMPLATE_RECENCY_MAX_SWING;
            if (isSmallCategory(template.category())) {
                positiveSwing = Math.min(TEMPLATE_RECENCY_MAX_SWING + 0.08D, positiveSwing * 1.18D);
            }
            return clamp(1.0D + (pressure * positiveSwing), 1.0D, 1.0D + positiveSwing);
        }
        double bias = 1.0D + (pressure * negativeSwing);
        if (isSmallCategory(template.category())) {
            double excessShare = Math.max(0.0D, observedShare - expectedShare);
            double extraPenalty = clamp(excessShare / Math.max(expectedShare, 0.0001D), 0.0D, 1.0D) * 0.12D;
            bias -= extraPenalty;
        }
        return clamp(bias, 1.0D - negativeSwing, 1.0D);
    }


    private double intraCategoryDiversityBias(AbilityTemplate template,
                                              Artifact artifact,
                                              ArtifactLineage lineage,
                                              MechanicNicheTag dominantNiche,
                                              NicheVariantProfile variantProfile,
                                              List<AbilityTemplate> selected) {
        if (!isSmallCategory(template.category())) {
            return 1.0D;
        }
        double recentSimilarity = selected.stream()
                .filter(existing -> existing.category() == template.category())
                .map(existing -> diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominantNiche, variantProfile, existing))
                .mapToDouble(existing -> AbilityDiversityIndex.similarity(
                        diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominantNiche, variantProfile, template),
                        existing))
                .max()
                .orElseGet(() -> recentTemplateSelections.stream()
                        .map(this::templateById)
                        .filter(Objects::nonNull)
                        .filter(existing -> existing.category() == template.category())
                        .limit(SMALL_CATEGORY_TEMPLATE_LIMIT)
                        .map(existing -> diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominantNiche, variantProfile, existing))
                        .mapToDouble(existing -> AbilityDiversityIndex.similarity(
                                diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominantNiche, variantProfile, template),
                                existing))
                        .max()
                        .orElse(0.0D));
        return clamp(1.0D - (recentSimilarity * SMALL_CATEGORY_DIVERSITY_STRENGTH), 0.78D, 1.0D);
    }


    private double underSampledApplicabilityBoost(AbilityTemplate template, ArtifactNicheProfile nicheProfile, ArtifactMemoryProfile memoryProfile) {
        return underSampledApplicabilityBoost(template, nicheProfile, memoryProfile, null);
    }

    private double underSampledApplicabilityBoost(AbilityTemplate template, ArtifactNicheProfile nicheProfile, ArtifactMemoryProfile memoryProfile, CategoryApplicabilityProfile applicabilityProfile) {
        if (!isUnderSampledHighComplexityTemplate(template)) {
            return 1.0D;
        }
        double scarcity = underSampledScarcity(template);
        double triggerFit = secondaryTriggerApplicability(template, nicheProfile);
        double adjacentActionFit = adjacentActionClassApplicability(template, memoryProfile);
        double boost = scarcity * ((triggerFit * 0.60D) + (adjacentActionFit * 0.40D)) * UNDER_SAMPLED_APPLICABILITY_MAX_BOOST;
        double applicabilityBoost = clamp(1.0D + boost, 1.0D, 1.0D + UNDER_SAMPLED_APPLICABILITY_MAX_BOOST);
        if (applicabilityProfile == null) {
            return applicabilityBoost;
        }
        if (applicabilityProfile.rank() == 0) {
            return 1.0D;
        }
        if (applicabilityProfile.isDominant()) {
            return 1.0D + ((applicabilityBoost - 1.0D) * 0.25D);
        }
        return applicabilityBoost;
    }

    private record CategoryApplicabilityProfile(int rank, double relativeToMean) {
        private boolean isDominant() {
            return rank <= 2 || relativeToMean >= 1.25D;
        }
    }

    private double underSampledTriggerRelief(AbilityTemplate template, MechanicNicheTag dominant) {
        if (!isUnderSampledHighComplexityTemplate(template)) {
            return 0.0D;
        }
        Set<AbilityTrigger> nearTriggers = adjacentTriggerFamily(template.trigger());
        if (nearTriggers.isEmpty()) {
            return 0.0D;
        }
        double closeness = triggerContextSimilarity(template.trigger(), familyCompatibleTriggers(dominant, nearTriggers));
        return closeness * underSampledScarcity(template) * UNDER_SAMPLED_TRIGGER_RELIEF_MAX;
    }

    private Set<AbilityTrigger> familyCompatibleTriggers(MechanicNicheTag dominant, Set<AbilityTrigger> nearTriggers) {
        Set<AbilityTrigger> compatible = new java.util.HashSet<>(nearTriggers);
        switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> compatible.addAll(Set.of(AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityTrigger.ON_CHUNK_ENTER, AbilityTrigger.ON_BLOCK_INSPECT, AbilityTrigger.ON_ELEVATION_CHANGE));
            case FARMING_WORLDKEEPING, PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION -> compatible.addAll(Set.of(AbilityTrigger.ON_BLOCK_HARVEST, AbilityTrigger.ON_LOW_HEALTH, AbilityTrigger.ON_HIT, AbilityTrigger.ON_CHUNK_ENTER, AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_WEATHER_CHANGE, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityTrigger.ON_TIME_OF_DAY_TRANSITION, AbilityTrigger.ON_ITEM_PICKUP, AbilityTrigger.ON_ENTITY_INSPECT));
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> compatible.addAll(Set.of(AbilityTrigger.ON_RITUAL_INTERACT, AbilityTrigger.ON_MEMORY_EVENT, AbilityTrigger.ON_WITNESS_EVENT, AbilityTrigger.ON_AWAKENING, AbilityTrigger.ON_FUSION, AbilityTrigger.ON_RITUAL_COMPLETION));
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> compatible.addAll(Set.of(AbilityTrigger.ON_WITNESS_EVENT, AbilityTrigger.ON_MEMORY_EVENT, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_SOCIAL_INTERACT, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityTrigger.ON_PLAYER_TRADE));
            default -> {}
        }
        return compatible;
    }

    private boolean isUnderSampledHighComplexityTemplate(AbilityTemplate template) {
        return templateComplexityScore(template) >= HIGH_COMPLEXITY_TEMPLATE_FLOOR && underSampledScarcity(template) >= 0.35D;
    }

    private double underSampledScarcity(AbilityTemplate template) {
        double recentScarcity = 1.0D - clamp(recentTemplateFrequency(template), 0.0D, 1.0D);
        double lifetimeScarcity = 1.0D - clamp(lifetimeTemplateUsage.getOrDefault(template.id(), 0) / (double) COLD_LIFETIME_WARM_THRESHOLD, 0.0D, 1.0D);
        return clamp(Math.max(recentScarcity * 0.75D, lifetimeScarcity), 0.0D, 1.0D);
    }

    private double templateComplexityScore(AbilityTemplate template) {
        AbilityMetadata metadata = template.metadata();
        double metadataBreadth = clamp((metadata.utilityDomains().size() + metadata.triggerClasses().size() + metadata.affinities().size()) / 9.0D, 0.0D, 1.0D);
        double utilityDepth = clamp(metadata.utilityPotential(), 0.0D, 1.0D);
        return clamp((metadataBreadth * 0.45D) + (utilityDepth * 0.55D), 0.0D, 1.0D);
    }

    private double secondaryTriggerApplicability(AbilityTemplate template, ArtifactNicheProfile nicheProfile) {
        MechanicNicheTag dominant = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        Set<AbilityTrigger> nearTriggers = adjacentTriggerFamily(template.trigger());
        if (nearTriggers.isEmpty()) {
            return 0.0D;
        }
        return triggerContextSimilarity(template.trigger(), familyCompatibleTriggers(dominant, nearTriggers));
    }

    private double adjacentActionClassApplicability(AbilityTemplate template, ArtifactMemoryProfile memoryProfile) {
        return switch (template.trigger()) {
            case ON_BLOCK_INSPECT -> clamp(Math.max(memoryProfile.mobilityWeight(), memoryProfile.disciplineWeight()) / 1.8D, 0.0D, 1.0D);
            case ON_STRUCTURE_PROXIMITY -> clamp(Math.max(memoryProfile.mobilityWeight(), memoryProfile.survivalWeight()) / 1.9D, 0.0D, 1.0D);
            case ON_PLAYER_TRADE, ON_SOCIAL_INTERACT -> clamp(Math.max(memoryProfile.aggressionWeight(), memoryProfile.disciplineWeight()) / 1.8D, 0.0D, 1.0D);
            case ON_MOVEMENT, ON_REPOSITION -> clamp(Math.max(memoryProfile.mobilityWeight(), memoryProfile.pressure() / 10.0D), 0.0D, 1.0D);
            default -> 0.45D;
        };
    }

    private Set<AbilityTrigger> adjacentTriggerFamily(AbilityTrigger trigger) {
        return switch (trigger) {
            case ON_BLOCK_INSPECT -> Set.of(AbilityTrigger.ON_ENTITY_INSPECT, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityTrigger.ON_WITNESS_EVENT);
            case ON_STRUCTURE_PROXIMITY -> Set.of(AbilityTrigger.ON_STRUCTURE_SENSE, AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityTrigger.ON_MOVEMENT, AbilityTrigger.ON_WEATHER_CHANGE);
            case ON_STRUCTURE_SENSE -> Set.of(AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityTrigger.ON_MEMORY_EVENT);
            case ON_WORLD_SCAN -> Set.of(AbilityTrigger.ON_STRUCTURE_PROXIMITY);
            case ON_MOVEMENT -> Set.of(AbilityTrigger.ON_CHUNK_ENTER, AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_REPOSITION);
            case ON_REPOSITION -> Set.of(AbilityTrigger.ON_MOVEMENT, AbilityTrigger.ON_CHUNK_ENTER);
            case ON_PLAYER_TRADE -> Set.of(AbilityTrigger.ON_SOCIAL_INTERACT, AbilityTrigger.ON_PLAYER_GROUP_ACTION);
            case ON_SOCIAL_INTERACT -> Set.of(AbilityTrigger.ON_PLAYER_TRADE, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityTrigger.ON_WITNESS_EVENT);
            case ON_TIME_OF_DAY_TRANSITION -> Set.of(AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_WEATHER_CHANGE);
            default -> Set.of();
        };
    }

    private boolean isHighFrequencyContextMisaligned(AbilityTemplate template) {
        AbilityTrigger trigger = template.trigger();
        if (trigger != AbilityTrigger.ON_LOW_HEALTH && trigger != AbilityTrigger.ON_TIME_OF_DAY_TRANSITION) {
            return false;
        }
        Set<MechanicNicheTag> templateNiches = nicheTaxonomy.nichesFor(template.mechanic(), trigger);
        Set<MechanicNicheTag> categoryNiches = template.category().niches();
        return templateNiches.stream().noneMatch(categoryNiches::contains);
    }

    private boolean isSmallCategory(AbilityCategory category) {
        return categoryTemplateCount(category) <= SMALL_CATEGORY_TEMPLATE_LIMIT;
    }

    private int categoryTemplateCount(AbilityCategory category) {
        return (int) registry.templates().stream()
                .filter(template -> template.category() == category)
                .count();
    }

    private double categoryExposureBias(AbilityCategory category, MechanicNicheTag dominantNiche, List<AbilityDiversityIndex.AbilitySignature> activePool) {
        Map<AbilityCategory, Double> exposure = new EnumMap<>(AbilityCategory.class);
        for (AbilityCategory value : AbilityCategory.values()) {
            exposure.put(value, 0.0D);
        }
        double totalWeight = 0.0D;
        for (AbilityDiversityIndex.AbilitySignature signature : activePool) {
            if (signature.category() == null) {
                continue;
            }
            exposure.merge(signature.category(), 1.0D, Double::sum);
            totalWeight += 1.0D;
        }
        for (Map.Entry<AbilityCategory, Integer> entry : recentCategoryUsage.entrySet()) {
            exposure.merge(entry.getKey(), entry.getValue() * 0.6D, Double::sum);
            totalWeight += entry.getValue() * 0.6D;
        }
        if (totalWeight <= 0.0D) {
            return 1.02D;
        }
        long compatibleCategories = java.util.Arrays.stream(AbilityCategory.values())
                .filter(value -> dominantNiche == null || value.niches().contains(dominantNiche))
                .count();
        double expectedShare = 1.0D / Math.max(1.0D, compatibleCategories);
        double compatibleWeight = exposure.entrySet().stream()
                .filter(entry -> dominantNiche == null || entry.getKey().niches().contains(dominantNiche))
                .mapToDouble(Map.Entry::getValue)
                .sum();
        double observedShare = category.niches().contains(dominantNiche)
                ? exposure.getOrDefault(category, 0.0D) / Math.max(1.0D, compatibleWeight)
                : exposure.getOrDefault(category, 0.0D) / totalWeight;
        double pressure = clamp((expectedShare - observedShare) / Math.max(expectedShare, 0.0001D), -1.0D, 1.0D);
        double positiveSwing = category.niches().contains(dominantNiche) ? CATEGORY_EXPOSURE_MAX_SWING : 0.03D;
        double negativeSwing = CATEGORY_EXPOSURE_MAX_SWING;
        if (pressure >= 0.0D) {
            return clamp(1.0D + (pressure * positiveSwing), 1.0D, 1.0D + positiveSwing);
        }
        return clamp(1.0D + (pressure * negativeSwing), 1.0D - negativeSwing, 1.0D);
    }

    private double tailPreservationBias(AbilityTemplate template, double intraNovelty, double nicheAlignment, ArtifactNicheProfile nicheProfile, AbilitySimilarityProfile similarityProfile) {
        double noveltyQualified = intraNovelty >= NOVELTY_FLOOR ? 1.0D : 0.0D;
        double nicheQualified = clamp((nicheAlignment - 0.82D) / 0.32D, 0.0D, 1.0D);
        double similarityQualified = similarityProfile.sameNicheSimilarity() < HIGH_SIMILARITY_THRESHOLD ? 1.0D : 0.0D;
        double scarcity = 1.0D - clamp(recentTemplateFrequency(template), 0.0D, 1.0D);
        double boost = scarcity * noveltyQualified * nicheQualified * similarityQualified * TAIL_PRESERVATION_MAX_BOOST;
        return clamp(1.0D + boost, 1.0D, 1.0D + TAIL_PRESERVATION_MAX_BOOST);
    }

    private double recentTemplateFrequency(AbilityTemplate template) {
        int sameCategoryTotal = recentCategoryTemplateTotal(template.category());
        if (sameCategoryTotal <= 0) {
            return 0.0D;
        }
        int templateCount = recentTemplateUsage.getOrDefault(template.id(), 0);
        return templateCount / (double) sameCategoryTotal;
    }

    private AbilityTemplate templateById(String templateId) {
        return templatesById.get(templateId);
    }

    private int recentCategoryTemplateTotal(AbilityCategory category) {
        return recentTemplateUsage.entrySet().stream()
                .map(entry -> Map.entry(templateById(entry.getKey()), entry.getValue()))
                .filter(entry -> entry.getKey() != null && entry.getKey().category() == category)
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    private AbilitySimilarityProfile abilitySimilarityProfile(AbilityDiversityIndex.AbilitySignature candidate,
                                                            List<AbilityDiversityIndex.AbilitySignature> activePool,
                                                            List<AbilityTemplate> selected,
                                                            Artifact artifact,
                                                            ArtifactLineage lineage,
                                                            MechanicNicheTag dominantNiche,
                                                            NicheVariantProfile variantProfile) {
        double sameNicheActive = activePool.stream()
                .filter(existing -> sameNiche(candidate, existing, dominantNiche))
                .mapToDouble(existing -> AbilityDiversityIndex.similarity(candidate, existing))
                .max()
                .orElse(0.0D);
        double crossNicheActive = activePool.stream()
                .filter(existing -> !sameNiche(candidate, existing, dominantNiche))
                .mapToDouble(existing -> AbilityDiversityIndex.similarity(candidate, existing))
                .max()
                .orElse(0.0D);
        double selectionSimilarity = selected.stream()
                .map(sel -> diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominantNiche, variantProfile, sel))
                .mapToDouble(existing -> AbilityDiversityIndex.similarity(candidate, existing))
                .max()
                .orElse(0.0D);
        double sameNicheSimilarity = Math.max(sameNicheActive, selectionSimilarity);
        double globalSimilarity = Math.max(Math.max(sameNicheActive, crossNicheActive), selectionSimilarity);
        return new AbilitySimilarityProfile(clamp(sameNicheSimilarity, 0.0D, 1.0D), clamp(globalSimilarity, 0.0D, 1.0D), clamp(crossNicheActive, 0.0D, 1.0D));
    }

    private boolean sameNiche(AbilityDiversityIndex.AbilitySignature candidate, AbilityDiversityIndex.AbilitySignature existing, MechanicNicheTag dominantNiche) {
        MechanicNicheTag candidateNiche = candidate.niche() == null ? dominantNiche : candidate.niche();
        MechanicNicheTag existingNiche = existing.niche() == null ? MechanicNicheTag.GENERALIST : existing.niche();
        return Objects.equals(candidateNiche, existingNiche);
    }

    private double noveltyBonus(double intraNovelty, double globalNovelty, NicheVariantProfile variantProfile) {
        double pressure = noveltyPressure(variantProfile);
        double weightedNovelty = (Math.pow(intraNovelty, 0.78D) * SAME_NICHE_NOVELTY_WEIGHT)
                + (Math.pow(globalNovelty, 0.78D) * GLOBAL_NOVELTY_WEIGHT * crossNicheNoveltyPressure(variantProfile));
        return 1.0D + (weightedNovelty * pressure);
    }

    private double crossNicheNoveltyPressure(NicheVariantProfile variantProfile) {
        if (variantProfile == null) {
            return 0.50D;
        }
        return variantProfile.isAlphaVariant()
                ? clamp(0.60D + (variantProfile.mutationBias() * 0.12D), 0.52D, 0.74D)
                : clamp(0.24D + (variantProfile.retentionBias() * 0.08D), 0.20D, 0.35D);
    }

    private double noveltyPressure(NicheVariantProfile variantProfile) {
        if (variantProfile == null) return 0.66D;
        return variantProfile.isAlphaVariant()
                ? 0.80D * variantProfile.mutationBias()
                : 0.22D * variantProfile.retentionBias();
    }

    private double noveltyFloorPenalty(double novelty) {
        if (novelty >= NOVELTY_FLOOR) {
            return 1.0D;
        }
        double ratio = clamp(novelty / NOVELTY_FLOOR, 0.0D, 1.0D);
        return clamp(NOVELTY_FLOOR_PENALTY + (ratio * ratio * (1.0D - NOVELTY_FLOOR_PENALTY)), NOVELTY_FLOOR_PENALTY, 1.0D);
    }

    private double activePoolSimilarityPenalty(AbilitySimilarityProfile similarityProfile, NicheVariantProfile variantProfile) {
        double sameNiche = similarityProfile.sameNicheSimilarity();
        double crossNiche = similarityProfile.crossNicheSimilarity();
        double highSimilarityPenalty = variantProfile != null && variantProfile.isAlphaVariant() ? 0.77D : 0.83D;
        if (sameNiche >= HIGH_SIMILARITY_THRESHOLD) {
            double ratio = clamp((sameNiche - HIGH_SIMILARITY_THRESHOLD) / (1.0D - HIGH_SIMILARITY_THRESHOLD), 0.0D, 1.0D);
            return clamp(1.0D - (ratio * highSimilarityPenalty), 1.0D - highSimilarityPenalty, 1.0D);
        }
        if (sameNiche >= MODERATE_SIMILARITY_THRESHOLD) {
            double ratio = clamp((sameNiche - MODERATE_SIMILARITY_THRESHOLD) / (HIGH_SIMILARITY_THRESHOLD - MODERATE_SIMILARITY_THRESHOLD), 0.0D, 1.0D);
            double crossRelief = (1.0D - ratio) * (variantProfile != null && variantProfile.isAlphaVariant() ? 0.05D : 0.02D);
            return clamp(0.98D - (ratio * 0.17D) + crossRelief, 0.84D, 1.00D);
        }
        double sameNicheLift = clamp((MODERATE_SIMILARITY_THRESHOLD - sameNiche) / MODERATE_SIMILARITY_THRESHOLD, 0.0D, 1.0D);
        double crossNichePenalty = clamp((crossNiche - MODERATE_SIMILARITY_THRESHOLD) / (1.0D - MODERATE_SIMILARITY_THRESHOLD), 0.0D, 1.0D);
        double variantLift = variantProfile != null && variantProfile.isAlphaVariant() ? 0.10D : 0.04D;
        return clamp(1.0D + (sameNicheLift * variantLift) - (crossNichePenalty * 0.03D), 0.97D, 1.10D);
    }

    private double nicheWeight(AbilityTemplate template, ArtifactNicheProfile nicheProfile, ArtifactMemoryProfile memoryProfile) {
        MechanicNicheTag dominant = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        Set<MechanicNicheTag> templateNiches = nicheTaxonomy.nichesFor(template.mechanic(), template.trigger());
        boolean matches = templateNiches.contains(dominant);
        boolean adjacent = !matches && nicheAdjacent(templateNiches, dominant);
        double familyBias = switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> template.family() == AbilityFamily.MOBILITY ? 1.16D : 0.92D;
            case FARMING_WORLDKEEPING, PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION -> template.family() == AbilityFamily.SURVIVAL ? 1.18D : 0.90D;
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> template.family() == AbilityFamily.CHAOS ? 1.18D : 0.90D;
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> template.family() == AbilityFamily.CONSISTENCY ? 1.14D : 0.92D;
            default -> 1.0D;
        };
        double memoryTuning = template.metadata().hasAffinity("exploration") ? 1.0D + (memoryProfile.mobilityWeight() * 0.04D) : 1.0D;
        double nicheMatchWeight = matches ? 1.24D : (adjacent ? 1.05D : 0.84D);
        return clamp(nicheMatchWeight * familyBias * memoryTuning, 0.74D, 1.34D);
    }

    private double categoryWeight(AbilityTemplate template,
                                  ArtifactNicheProfile nicheProfile,
                                  ArtifactMemoryProfile memoryProfile,
                                  ArtifactLineage lineage,
                                  NicheVariantProfile variantProfile) {
        MechanicNicheTag dominant = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        AbilityCategory category = template.category();
        double nicheMatch = category.niches().contains(dominant) ? 1.10D : (nicheAdjacent(category.niches(), dominant) ? 1.02D : 0.94D);
        double memoryBias = switch (category) {
            case TRAVERSAL_MOBILITY -> 1.0D + (memoryProfile.mobilityWeight() * 0.05D);
            case SENSING_INFORMATION -> 1.0D + (memoryProfile.disciplineWeight() * 0.06D);
            case SURVIVAL_ADAPTATION, DEFENSE_WARDING -> 1.0D + (memoryProfile.survivalWeight() * 0.05D);
            case COMBAT_TACTICAL_CONTROL -> 1.0D + (memoryProfile.aggressionWeight() * 0.04D);
            case RESOURCE_FARMING_LOGISTICS, CRAFTING_ENGINEERING_AUTOMATION -> 1.0D + (memoryProfile.disciplineWeight() * 0.06D) + (memoryProfile.survivalWeight() * 0.04D);
            case SOCIAL_SUPPORT_COORDINATION -> 1.0D + (memoryProfile.disciplineWeight() * 0.02D) + (memoryProfile.aggressionWeight() * 0.03D);
            case RITUAL_STRANGE_UTILITY -> 1.0D + (memoryProfile.chaosWeight() * 0.05D);
            case STEALTH_TRICKERY_DISRUPTION -> 1.0D + (memoryProfile.mobilityWeight() * 0.03D) + (memoryProfile.chaosWeight() * 0.02D);
        };
        double lineageBias = lineage == null ? 1.0D : switch (category) {
            case TRAVERSAL_MOBILITY, SENSING_INFORMATION -> 1.0D + (lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.EXPLORATION_PREFERENCE) * 0.08D);
            case SOCIAL_SUPPORT_COORDINATION, DEFENSE_WARDING, RESOURCE_FARMING_LOGISTICS, CRAFTING_ENGINEERING_AUTOMATION -> 1.0D + (lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.SUPPORT_PREFERENCE) * 0.07D);
            case RITUAL_STRANGE_UTILITY, STEALTH_TRICKERY_DISRUPTION, COMBAT_TACTICAL_CONTROL -> 1.0D + (lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS) * 0.06D);
            case SURVIVAL_ADAPTATION -> 1.0D + (lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RELIABILITY) * 0.05D);
        };
        return clamp(nicheMatch * memoryBias * lineageBias * variantCategoryBias(template, variantProfile), 0.84D, 1.28D);
    }


    private double nicheConsistencyPenalty(AbilityTemplate template, ArtifactNicheProfile nicheProfile, ArtifactMemoryProfile memoryProfile) {
        MechanicNicheTag dominant = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        Set<MechanicNicheTag> templateNiches = nicheTaxonomy.nichesFor(template.mechanic(), template.trigger());
        double mechanicMismatch = templateNiches.contains(dominant) ? 0.0D : (nicheAdjacent(templateNiches, dominant) ? 0.24D : 0.55D);
        double triggerMismatch = switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> navigationTriggerMismatch(template.trigger());
            case FARMING_WORLDKEEPING, PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION -> survivalTriggerMismatch(template.trigger());
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> ritualTriggerMismatch(template.trigger());
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> supportTriggerMismatch(template.trigger());
            default -> 0.10D;
        };
        triggerMismatch = smoothedTriggerMismatch(template, dominant, triggerMismatch);
        double affinityMismatch = affinityMismatch(template, dominant, memoryProfile);
        double penalty = Math.min(0.18D, (mechanicMismatch * 0.09D) + (triggerMismatch * 0.05D) + (affinityMismatch * 0.03D));
        return clamp(1.0D - penalty, 0.82D, 1.0D);
    }

    private double navigationTriggerMismatch(AbilityTrigger trigger) {
        return switch (trigger) {
            case ON_WORLD_SCAN, ON_STRUCTURE_DISCOVERY, ON_CHUNK_ENTER, ON_STRUCTURE_SENSE, ON_BLOCK_INSPECT -> 0.0D;
            case ON_MEMORY_EVENT, ON_WITNESS_EVENT -> 0.18D;
            default -> 0.28D;
        };
    }

    private double survivalTriggerMismatch(AbilityTrigger trigger) {
        return switch (trigger) {
            case ON_BLOCK_HARVEST, ON_LOW_HEALTH, ON_HIT, ON_CHUNK_ENTER, ON_WORLD_SCAN -> 0.0D;
            case ON_MEMORY_EVENT, ON_WITNESS_EVENT -> 0.14D;
            default -> 0.24D;
        };
    }

    private double ritualTriggerMismatch(AbilityTrigger trigger) {
        return switch (trigger) {
            case ON_RITUAL_INTERACT, ON_MEMORY_EVENT, ON_WITNESS_EVENT, ON_AWAKENING, ON_FUSION -> 0.0D;
            case ON_WORLD_SCAN, ON_STRUCTURE_SENSE -> 0.10D;
            default -> 0.22D;
        };
    }

    private double supportTriggerMismatch(AbilityTrigger trigger) {
        return switch (trigger) {
            case ON_WITNESS_EVENT, ON_MEMORY_EVENT, ON_STRUCTURE_SENSE, ON_WORLD_SCAN -> 0.0D;
            case ON_CHUNK_ENTER, ON_BLOCK_INSPECT -> 0.10D;
            default -> 0.20D;
        };
    }


    private double smoothedTriggerMismatch(AbilityTemplate template, MechanicNicheTag dominant, double triggerMismatch) {
        if (triggerMismatch <= 0.0D) {
            return 0.0D;
        }
        if (!template.category().niches().contains(dominant) && !nicheAdjacent(template.category().niches(), dominant)) {
            return triggerMismatch;
        }
        Set<AbilityTrigger> compatibleTriggers = switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> Set.of(AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityTrigger.ON_CHUNK_ENTER, AbilityTrigger.ON_BLOCK_INSPECT, AbilityTrigger.ON_ELEVATION_CHANGE);
            case FARMING_WORLDKEEPING, PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION -> Set.of(AbilityTrigger.ON_BLOCK_HARVEST, AbilityTrigger.ON_LOW_HEALTH, AbilityTrigger.ON_HIT, AbilityTrigger.ON_CHUNK_ENTER, AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_WEATHER_CHANGE, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityTrigger.ON_TIME_OF_DAY_TRANSITION, AbilityTrigger.ON_ITEM_PICKUP, AbilityTrigger.ON_ENTITY_INSPECT);
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> Set.of(AbilityTrigger.ON_RITUAL_INTERACT, AbilityTrigger.ON_MEMORY_EVENT, AbilityTrigger.ON_WITNESS_EVENT, AbilityTrigger.ON_AWAKENING, AbilityTrigger.ON_FUSION, AbilityTrigger.ON_RITUAL_COMPLETION);
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> Set.of(AbilityTrigger.ON_WITNESS_EVENT, AbilityTrigger.ON_MEMORY_EVENT, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityTrigger.ON_WORLD_SCAN, AbilityTrigger.ON_SOCIAL_INTERACT, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityTrigger.ON_PLAYER_TRADE);
            default -> Set.of();
        };
        double classOverlap = triggerContextSimilarity(template.trigger(), compatibleTriggers);
        double smoothingMax = categoryTriggerSpan(template.category()) <= 4 ? NARROW_TRIGGER_SMOOTHING_MAX_RELIEF : TRIGGER_SMOOTHING_MAX_RELIEF;
        double categoryRelief = template.category() == AbilityCategory.STEALTH_TRICKERY_DISRUPTION ? 1.0D : 0.72D;
        double underSampledRelief = underSampledTriggerRelief(template, dominant);
        double relief = classOverlap * categoryRelief * smoothingMax;
        relief += underSampledRelief;
        return clamp(triggerMismatch - relief, 0.0D, triggerMismatch);
    }

    private double triggerContextSimilarity(AbilityTrigger trigger, Set<AbilityTrigger> closeTriggers) {
        if (closeTriggers.contains(trigger)) {
            return 1.0D;
        }
        return switch (trigger) {
            case ON_PLAYER_WITNESS -> closeTriggers.contains(AbilityTrigger.ON_WITNESS_EVENT) ? 0.85D : 0.0D;
            case ON_STRUCTURE_PROXIMITY -> closeTriggers.contains(AbilityTrigger.ON_STRUCTURE_SENSE) || closeTriggers.contains(AbilityTrigger.ON_STRUCTURE_DISCOVERY) ? 0.88D : 0.0D;
            case ON_PLAYER_TRADE -> closeTriggers.contains(AbilityTrigger.ON_SOCIAL_INTERACT) ? 0.74D : 0.0D;
            case ON_MOVEMENT -> closeTriggers.contains(AbilityTrigger.ON_CHUNK_ENTER) || closeTriggers.contains(AbilityTrigger.ON_WORLD_SCAN) ? 0.72D : 0.0D;
            case ON_BLOCK_INSPECT -> closeTriggers.contains(AbilityTrigger.ON_ENTITY_INSPECT) ? 0.62D : 0.0D;
            case ON_TIME_OF_DAY_TRANSITION -> closeTriggers.contains(AbilityTrigger.ON_WORLD_SCAN) || closeTriggers.contains(AbilityTrigger.ON_WEATHER_CHANGE) ? 0.66D : 0.0D;
            default -> 0.0D;
        };
    }



    private int categoryTriggerSpan(AbilityCategory category) {
        return (int) registry.templates().stream()
                .filter(template -> template.category() == category)
                .map(AbilityTemplate::trigger)
                .distinct()
                .count();
    }

    private double lowVolumeCategoryBoost(AbilityTemplate template,
                                          ArtifactNicheProfile nicheProfile,
                                          List<AbilityDiversityIndex.AbilitySignature> activePool,
                                          Artifact artifact,
                                          ArtifactLineage lineage,
                                          NicheVariantProfile variantProfile,
                                          List<AbilityTemplate> selected) {
        MechanicNicheTag dominant = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        AbilityDiversityIndex.AbilitySignature candidate = diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominant, variantProfile, template);
        AbilitySimilarityProfile similarityProfile = abilitySimilarityProfile(candidate, activePool, selected, artifact, lineage, dominant, variantProfile);
        double intraNovelty = 1.0D - similarityProfile.sameNicheSimilarity();
        boolean noveltyQualified = intraNovelty >= NOVELTY_FLOOR;
        boolean nicheQualified = template.category().niches().contains(dominant) || nicheAdjacent(template.category().niches(), dominant);
        boolean similarityQualified = similarityProfile.sameNicheSimilarity() < HIGH_SIMILARITY_THRESHOLD;
        if (!noveltyQualified || !nicheQualified || !similarityQualified) {
            return 1.0D;
        }
        double categoryFrequency = recentCategoryFrequency(template.category(), activePool);
        double scarcity = clamp((0.24D - categoryFrequency) / 0.24D, 0.0D, 1.0D);
        return clamp(1.0D + (scarcity * LOW_VOLUME_CATEGORY_MAX_BOOST), 1.0D, 1.0D + LOW_VOLUME_CATEGORY_MAX_BOOST);
    }

    private boolean qualifiesForUnseenOverride(AbilityTemplate template,
                                               Artifact artifact,
                                               ArtifactMemoryProfile memoryProfile,
                                               ArtifactLineage lineage,
                                               NicheVariantProfile variantProfile,
                                               List<AbilityDiversityIndex.AbilitySignature> activePool,
                                               List<AbilityTemplate> selected) {
        MechanicNicheTag dominant = memoryProfile == null ? MechanicNicheTag.GENERALIST : inferDominantNiche(memoryProfile);
        AbilityDiversityIndex.AbilitySignature candidate = diversityIndex.fromTemplate(artifact.getArtifactSeed(), lineage == null ? null : lineage.lineageId(), dominant, variantProfile, template);
        AbilitySimilarityProfile similarityProfile = abilitySimilarityProfile(candidate, activePool, selected, artifact, lineage, dominant, variantProfile);
        double intraNovelty = 1.0D - similarityProfile.sameNicheSimilarity();
        boolean noveltyQualified = intraNovelty >= NOVELTY_FLOOR;
        boolean similarityQualified = similarityProfile.sameNicheSimilarity() < HIGH_SIMILARITY_THRESHOLD;
        boolean nicheQualified = template.category().niches().contains(dominant) || nicheAdjacent(template.category().niches(), dominant);
        return noveltyQualified && similarityQualified && nicheQualified;
    }

    private double recentCategoryFrequency(AbilityCategory category, List<AbilityDiversityIndex.AbilitySignature> activePool) {
        double observed = activePool.stream().filter(signature -> signature.category() == category).count();
        double recent = recentCategoryUsage.getOrDefault(category, 0);
        double total = Math.max(1.0D, activePool.size() + recentCategoryUsage.values().stream().mapToInt(Integer::intValue).sum());
        return (observed + recent) / total;
    }

    private boolean nicheAdjacent(Set<MechanicNicheTag> niches, MechanicNicheTag dominant) {
        return niches.stream().anyMatch(niche -> nicheAdjacent(niche, dominant));
    }

    private boolean nicheAdjacent(MechanicNicheTag left, MechanicNicheTag right) {
        if (left == null || right == null || left == right) {
            return false;
        }
        return switch (left) {
            case NAVIGATION -> right == MechanicNicheTag.MOBILITY_UTILITY || right == MechanicNicheTag.STRUCTURE_SENSING;
            case MOBILITY_UTILITY -> right == MechanicNicheTag.NAVIGATION || right == MechanicNicheTag.SOCIAL_WORLD_INTERACTION;
            case STRUCTURE_SENSING -> right == MechanicNicheTag.ENVIRONMENTAL_SENSING || right == MechanicNicheTag.INSPECT_INFORMATION || right == MechanicNicheTag.NAVIGATION;
            case ENVIRONMENTAL_SENSING -> right == MechanicNicheTag.STRUCTURE_SENSING || right == MechanicNicheTag.INSPECT_INFORMATION;
            case INSPECT_INFORMATION -> right == MechanicNicheTag.ENVIRONMENTAL_SENSING || right == MechanicNicheTag.STRUCTURE_SENSING || right == MechanicNicheTag.SOCIAL_WORLD_INTERACTION;
            case SOCIAL_WORLD_INTERACTION -> right == MechanicNicheTag.MOBILITY_UTILITY || right == MechanicNicheTag.INSPECT_INFORMATION || right == MechanicNicheTag.SUPPORT_COHESION;
            case SUPPORT_COHESION -> right == MechanicNicheTag.SOCIAL_WORLD_INTERACTION || right == MechanicNicheTag.PROTECTION_WARDING;
            case PROTECTION_WARDING -> right == MechanicNicheTag.ENVIRONMENTAL_ADAPTATION || right == MechanicNicheTag.SUPPORT_COHESION;
            case ENVIRONMENTAL_ADAPTATION -> right == MechanicNicheTag.PROTECTION_WARDING || right == MechanicNicheTag.FARMING_WORLDKEEPING;
            case FARMING_WORLDKEEPING -> right == MechanicNicheTag.ENVIRONMENTAL_ADAPTATION || right == MechanicNicheTag.SUPPORT_COHESION;
            case MEMORY_HISTORY -> right == MechanicNicheTag.RITUAL_STRANGE_UTILITY || right == MechanicNicheTag.HIGH_COST_UTILITY;
            case RITUAL_STRANGE_UTILITY -> right == MechanicNicheTag.MEMORY_HISTORY || right == MechanicNicheTag.HIGH_COST_UTILITY;
            case HIGH_COST_UTILITY -> right == MechanicNicheTag.RITUAL_STRANGE_UTILITY || right == MechanicNicheTag.MEMORY_HISTORY;
            case GENERALIST -> false;
        };
    }

    private double affinityMismatch(AbilityTemplate template, MechanicNicheTag dominant, ArtifactMemoryProfile memoryProfile) {
        return switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> template.metadata().hasAffinity("exploration") ? 0.0D : 0.35D - Math.min(0.15D, memoryProfile.mobilityWeight() * 0.05D);
            case FARMING_WORLDKEEPING, PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION -> template.metadata().hasAffinity("gathering") || template.metadata().hasAffinity("support") ? 0.0D : 0.28D;
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> template.metadata().hasAffinity("ritual") || template.metadata().hasAffinity("memory") ? 0.0D : 0.32D;
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> template.metadata().hasAffinity("social") || template.metadata().hasAffinity("support") ? 0.0D : 0.30D;
            default -> 0.0D;
        };
    }
    private double lineageCombinationBias(AbilityTemplate template, ArtifactLineage lineage) {
        if (lineage == null) return 1.0D;
        EvolutionaryBiasGenome bias = lineage.evolutionaryBiasGenome();
        double exploration = bias.tendency(LineageBiasDimension.EXPLORATION_PREFERENCE);
        double ritual = bias.tendency(LineageBiasDimension.RITUAL_PREFERENCE);
        double support = bias.tendency(LineageBiasDimension.SUPPORT_PREFERENCE);
        double weirdness = bias.tendency(LineageBiasDimension.WEIRDNESS);
        double value = 1.0D;
        if (template.metadata().affinities().contains("exploration")) value += exploration * 0.09D;
        if (template.metadata().affinities().contains("ritual")) value += ritual * 0.09D;
        if (template.metadata().affinities().contains("support")) value += support * 0.08D;
        if (template.family() == AbilityFamily.CHAOS || template.metadata().utilityDomains().contains("ritual-utility")) value += weirdness * 0.10D;
        if (template.category() == AbilityCategory.STEALTH_TRICKERY_DISRUPTION || template.category() == AbilityCategory.COMBAT_TACTICAL_CONTROL) value += weirdness * 0.05D;
        if (template.category() == AbilityCategory.RESOURCE_FARMING_LOGISTICS || template.category() == AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION) value += support * 0.04D;
        return clamp(value, 0.88D, 1.18D);
    }

    private double motifAnchorBias(AbilityDiversityIndex.AbilitySignature candidate, AbilityDiversityIndex.AbilitySignature anchor, NicheVariantProfile variantProfile) {
        double similarity = AbilityDiversityIndex.similarity(candidate, anchor);
        if (variantProfile != null && variantProfile.isAlphaVariant()) {
            return clamp(1.0D + ((0.34D - similarity) * 0.12D), 0.97D, 1.05D);
        }
        return clamp(1.0D + ((0.42D - Math.abs(similarity - 0.42D)) * 0.09D), 0.98D, 1.04D);
    }

    private record CategorySelectionProfile(AbilityTemplate bestTemplate, double templateScore, double categoryScore) {}

    private NicheVariantProfile resolveVariantProfile(Artifact artifact) {
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin == null || plugin.getArtifactUsageTracker() == null) {
            return null;
        }
        return plugin.getArtifactUsageTracker().nichePopulationTracker().variantFor(artifact.getArtifactSeed());
    }

    private double variantCategoryBias(AbilityTemplate template, NicheVariantProfile variantProfile) {
        if (variantProfile == null) {
            return 1.0D;
        }
        AbilityCategory category = template.category();
        if (variantProfile.isAlphaVariant()) {
            return switch (category) {
                case TRAVERSAL_MOBILITY, SENSING_INFORMATION, COMBAT_TACTICAL_CONTROL, RITUAL_STRANGE_UTILITY, STEALTH_TRICKERY_DISRUPTION ->
                        clamp(1.03D * variantProfile.mutationBias(), 0.96D, 1.12D);
                default -> clamp(0.98D + ((variantProfile.mutationBias() - 1.0D) * 0.04D), 0.94D, 1.04D);
            };
        }
        return switch (category) {
            case DEFENSE_WARDING, RESOURCE_FARMING_LOGISTICS, CRAFTING_ENGINEERING_AUTOMATION, SOCIAL_SUPPORT_COORDINATION, SURVIVAL_ADAPTATION ->
                    clamp(1.02D * variantProfile.retentionBias(), 0.97D, 1.10D);
            default -> clamp(0.98D + ((variantProfile.retentionBias() - 1.0D) * 0.03D), 0.95D, 1.03D);
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record AbilitySimilarityProfile(double sameNicheSimilarity,
                                            double globalSimilarity,
                                            double crossNicheSimilarity) {
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
