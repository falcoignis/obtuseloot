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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class ProceduralAbilityGenerator {
    private static final double NOVELTY_FLOOR = 0.15D;
    private static final double NOVELTY_FLOOR_PENALTY = 0.65D;
    private static final double HIGH_SIMILARITY_THRESHOLD = 0.80D;
    private static final double MODERATE_SIMILARITY_THRESHOLD = 0.62D;
    private static final double SAME_NICHE_NOVELTY_WEIGHT = 0.68D;
    private static final double GLOBAL_NOVELTY_WEIGHT = 0.32D;
    private static final double NICHE_WEIGHT_EXPONENT = 1.85D;

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
    private final AbilityDiversityIndex diversityIndex;
    private final EcosystemRoleClassifier roleClassifier;

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
        this.diversityIndex = AbilityDiversityIndex.instance();
        this.roleClassifier = new EcosystemRoleClassifier();
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
        double total = 0.0D;
        double[] scores = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            if (excludedIds.contains(pool.get(i).id())) {
                scores[i] = 0.0D;
                continue;
            }
            scores[i] = Math.max(0.01D, compositeTemplateScore(pool.get(i), artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, java.util.List.of()) * rarityModifier(pool.get(i)));
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
            AbilityTemplate best = remaining.stream()
                    .max(Comparator.comparingDouble(t -> compositeTemplateScore(t, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected)))
                    .orElse(null);
            if (best == null) {
                break;
            }
            selected.add(best);
            remaining.remove(best);
            if (traitInteractionsEnabled && remaining.size() > 1) {
                remaining.sort(Comparator.comparingDouble((AbilityTemplate t) -> compositeTemplateScore(t, artifact, memoryProfile, stage, utilityHistory, lineage, supportAllocation, nicheProfile, variantProfile, activePool, motifAnchor, selected) + (random.nextDouble() * 0.01D)).reversed());
            }
        }
        return selected;
    }

    private double compositeTemplateScore(AbilityTemplate template, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, UtilityHistoryRollup utilityHistory, ArtifactLineage lineage, AdaptiveSupportAllocation supportAllocation, ArtifactNicheProfile nicheProfile, NicheVariantProfile variantProfile, List<AbilityDiversityIndex.AbilitySignature> activePool, AbilityDiversityIndex.AbilitySignature motifAnchor, List<AbilityTemplate> selected) {
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
        double nicheConsistencyPenalty = nicheConsistencyPenalty(template, nicheProfile, memoryProfile);
        double lineageBias = lineageCombinationBias(template, lineage);
        double motifBias = motifAnchor == null ? 1.0D : motifAnchorBias(candidate, motifAnchor, variantProfile);
        return base * nicheBias * nicheConsistencyPenalty * lineageBias * motifBias * noveltyBonus * noveltyFloorPenalty * similarityPenalty;
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
        boolean matches = nicheTaxonomy.nichesFor(template.mechanic(), template.trigger()).contains(dominant);
        double familyBias = switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> template.family() == AbilityFamily.MOBILITY ? 1.16D : 0.92D;
            case FARMING_WORLDKEEPING, PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION -> template.family() == AbilityFamily.SURVIVAL ? 1.18D : 0.90D;
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> template.family() == AbilityFamily.CHAOS ? 1.18D : 0.90D;
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> template.family() == AbilityFamily.CONSISTENCY ? 1.14D : 0.92D;
            default -> 1.0D;
        };
        double memoryTuning = template.metadata().hasAffinity("exploration") ? 1.0D + (memoryProfile.mobilityWeight() * 0.04D) : 1.0D;
        return clamp((matches ? 1.24D : 0.84D) * familyBias * memoryTuning, 0.74D, 1.34D);
    }


    private double nicheConsistencyPenalty(AbilityTemplate template, ArtifactNicheProfile nicheProfile, ArtifactMemoryProfile memoryProfile) {
        MechanicNicheTag dominant = nicheProfile == null ? MechanicNicheTag.GENERALIST : nicheProfile.dominantNiche();
        Set<MechanicNicheTag> templateNiches = nicheTaxonomy.nichesFor(template.mechanic(), template.trigger());
        double mechanicMismatch = templateNiches.contains(dominant) ? 0.0D : 0.55D;
        double triggerMismatch = switch (dominant) {
            case NAVIGATION, ENVIRONMENTAL_SENSING -> navigationTriggerMismatch(template.trigger());
            case FARMING_WORLDKEEPING, PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION -> survivalTriggerMismatch(template.trigger());
            case RITUAL_STRANGE_UTILITY, MEMORY_HISTORY -> ritualTriggerMismatch(template.trigger());
            case SUPPORT_COHESION, SOCIAL_WORLD_INTERACTION -> supportTriggerMismatch(template.trigger());
            default -> 0.10D;
        };
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
        return clamp(value, 0.88D, 1.18D);
    }

    private double motifAnchorBias(AbilityDiversityIndex.AbilitySignature candidate, AbilityDiversityIndex.AbilitySignature anchor, NicheVariantProfile variantProfile) {
        double similarity = AbilityDiversityIndex.similarity(candidate, anchor);
        if (variantProfile != null && variantProfile.isAlphaVariant()) {
            return clamp(1.0D + ((0.34D - similarity) * 0.12D), 0.97D, 1.05D);
        }
        return clamp(1.0D + ((0.42D - Math.abs(similarity - 0.42D)) * 0.09D), 0.98D, 1.04D);
    }

    private NicheVariantProfile resolveVariantProfile(Artifact artifact) {
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin == null || plugin.getArtifactUsageTracker() == null) {
            return null;
        }
        return plugin.getArtifactUsageTracker().nichePopulationTracker().variantFor(artifact.getArtifactSeed());
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
