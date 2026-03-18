package obtuseloot.abilities.mutation;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.*;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.EvolutionaryBiasGenome;
import obtuseloot.lineage.LineageBiasDimension;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbilityMutationEngine {
    public AbilityMutationResult mutate(Artifact artifact, List<AbilityDefinition> definitions, ArtifactMemoryProfile memoryProfile, boolean driftMutation) {
        return mutate(artifact, definitions, memoryProfile, driftMutation, UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()));
    }

    public AbilityMutationResult mutate(Artifact artifact,
                                        List<AbilityDefinition> definitions,
                                        ArtifactMemoryProfile memoryProfile,
                                        boolean driftMutation,
                                        UtilityHistoryRollup utilityHistory) {
        return mutate(artifact, definitions, memoryProfile, driftMutation, utilityHistory, null, null);
    }

    public AbilityMutationResult mutate(Artifact artifact,
                                        List<AbilityDefinition> definitions,
                                        ArtifactMemoryProfile memoryProfile,
                                        boolean driftMutation,
                                        UtilityHistoryRollup utilityHistory,
                                        ArtifactLineage lineage,
                                        NicheVariantProfile variantProfile) {
        List<AbilityMutation> out = new ArrayList<>();
        boolean instabilityExceeded = artifact.hasInstability() && artifact.getDriftLevel() > 2;
        boolean chaosGrowth = "volatile".equalsIgnoreCase(artifact.getDriftAlignment()) || "paradox".equalsIgnoreCase(artifact.getDriftAlignment());
        boolean awakeningDriftInteraction = !"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) && chaosGrowth;
        boolean memoryPressure = memoryProfile.pressure() >= 4;
        if (!(driftMutation || instabilityExceeded || chaosGrowth || awakeningDriftInteraction || memoryPressure)) {
            return new AbilityMutationResult(definitions, out);
        }

        Random r = new Random(artifact.getArtifactSeed() ^ artifact.getDriftLevel() ^ artifact.getDriftAlignment().hashCode() ^ artifact.getAwakeningPath().hashCode()
                ^ artifact.getFusionPath().hashCode() ^ memoryProfile.pressure() ^ definitions.size());

        EcosystemRoleClassifier roleClassifier = new EcosystemRoleClassifier();
        ArtifactNicheProfile nicheProfile = roleClassifier.classify(utilityHistory.signalByMechanicTrigger());
        double specializationBias = nicheProfile.specialization().specializationScore();
        List<AbilityMechanic> underrepresentedUsefulMechanics = utilityHistory.signalByMechanicTrigger().entrySet().stream()
                .filter(entry -> entry.getValue().utilityDensity() > 0.30D && entry.getValue().attempts() >= 2)
                .sorted(java.util.Comparator.comparingDouble((java.util.Map.Entry<String, MechanicUtilitySignal> e) -> e.getValue().utilityDensity()).reversed())
                .map(entry -> entry.getKey().split("@")[0])
                .map(name -> {
                    try {
                        return AbilityMechanic.valueOf(name);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        List<AbilityDefinition> mutated = new ArrayList<>();
        int mutationCount = 0;
        for (AbilityDefinition definition : definitions) {
            AbilityTrigger beforeTrigger = definition.trigger();
            AbilityMechanic beforeMechanic = definition.mechanic();
            String beforePattern = definition.effectPattern();

            AbilityTrigger trigger = mutateTrigger(beforeTrigger, definition.mechanic(), chaosGrowth, memoryProfile, r, utilityHistory, lineage, variantProfile);
            if (trigger != beforeTrigger) {
                out.add(new AbilityMutation("trigger mutation", beforeTrigger.name(), trigger.name(), "drift/memory alignment shifted trigger cadence"));
                mutationCount++;
            }

            AbilityMechanic mechanic = mutateMechanic(beforeMechanic, definition.family(), chaosGrowth, memoryProfile, r, utilityHistory, specializationBias, underrepresentedUsefulMechanics, lineage, variantProfile);
            if (mechanic != beforeMechanic) {
                out.add(new AbilityMutation("mechanic mutation", beforeMechanic.name(), mechanic.name(), "mechanic remapped to keep mutation behavior active"));
                mutationCount++;
            }

            String effectPattern = beforePattern;
            boolean exploratoryVariant = variantProfile != null && variantProfile.isAlphaVariant();
            if (memoryPressure || chaosGrowth || awakeningDriftInteraction || exploratoryVariant) {
                String mutationSuffix = exploratoryVariant ? "hybrid" : (chaosGrowth ? "entropy" : "memory");
                effectPattern = beforePattern + " [mutated:" + mutationSuffix + "]";
                out.add(new AbilityMutation("pattern mutation", beforePattern, effectPattern, "mutation rewrote active pattern text"));
                mutationCount++;
            }

            List<AbilityModifier> support = new ArrayList<>(definition.supportModifiers());
            if (memoryProfile.bossWeight() > 0.5D) {
                support.add(new AbilityModifier("support.boss-memory", "boss memory pressure tuning", 0.03D, false));
            }

            mutated.add(new AbilityDefinition(
                    definition.id(),
                    definition.name(),
                    definition.family(),
                    trigger,
                    mechanic,
                    effectPattern,
                    definition.evolutionVariant(),
                    definition.driftVariant(),
                    definition.awakeningVariant(),
                    definition.fusionVariant(),
                    definition.memoryVariant() + (memoryPressure ? " + memory echo" : ""),
                    support,
                    List.of(new AbilityEffect(effectPattern, AbilityEffectType.TRIGGERED_BEHAVIOR, 0.02D + (mutationCount * 0.001D))),
                    definition.metadata(),
                    definition.stage1(),
                    definition.stage2() + " [mutation=" + mutationCount + "]",
                    definition.stage3() + " [flavor=" + (chaosGrowth ? "volatile" : "disciplined") + "]",
                    definition.stage4(),
                    definition.stage5()));
        }
        return new AbilityMutationResult(mutated, out);
    }

    private AbilityTrigger mutateTrigger(AbilityTrigger current,
                                         AbilityMechanic mechanic,
                                         boolean chaosGrowth,
                                         ArtifactMemoryProfile memoryProfile,
                                         Random random,
                                         UtilityHistoryRollup utilityHistory,
                                         ArtifactLineage lineage,
                                         NicheVariantProfile variantProfile) {
        if (chaosGrowth && mechanic == AbilityMechanic.REVENANT_TRIGGER) {
            return AbilityTrigger.ON_WITNESS_EVENT;
        }
        if (memoryProfile.disciplineWeight() > memoryProfile.chaosWeight() && mechanic == AbilityMechanic.INSIGHT_REVEAL) {
            return AbilityTrigger.ON_BLOCK_INSPECT;
        }
        if (memoryProfile.survivalWeight() > 1.2D && mechanic == AbilityMechanic.HARVEST_RELAY) {
            return AbilityTrigger.ON_BLOCK_HARVEST;
        }
        if (memoryProfile.mobilityWeight() > 0.8D && mechanic == AbilityMechanic.NAVIGATION_ANCHOR) {
            return AbilityTrigger.ON_WORLD_SCAN;
        }
        if (variantProfile != null && variantProfile.isAlphaVariant() && random.nextDouble() < 0.36D) {
            return switch (mechanic) {
                case RITUAL_CHANNEL, REVENANT_TRIGGER -> AbilityTrigger.ON_WITNESS_EVENT;
                case NAVIGATION_ANCHOR, ECOLOGICAL_PATHING -> AbilityTrigger.ON_STRUCTURE_DISCOVERY;
                case HARVEST_RELAY, GUARDIAN_PULSE -> AbilityTrigger.ON_PLAYER_GROUP_ACTION;
                default -> current;
            };
        }
        if (lineage != null && utilityHistory.hasUtilityHistory() && random.nextDouble() < 0.65D) {
            return utilityHistory.preferredTrigger(current);
        }
        if (variantProfile != null && !variantProfile.isAlphaVariant() && random.nextDouble() < 0.45D) {
            return current;
        }
        return random.nextDouble() < 0.2D ? AbilityTrigger.ON_MEMORY_EVENT : current;
    }

    private AbilityMechanic mutateMechanic(AbilityMechanic current,
                                           AbilityFamily family,
                                           boolean chaosGrowth,
                                           ArtifactMemoryProfile memoryProfile,
                                           Random random,
                                           UtilityHistoryRollup utilityHistory,
                                           double specializationBias,
                                           List<AbilityMechanic> underrepresentedUsefulMechanics,
                                           ArtifactLineage lineage,
                                           NicheVariantProfile variantProfile) {
        if (chaosGrowth && family == AbilityFamily.CHAOS) {
            return AbilityMechanic.UNSTABLE_DETONATION;
        }
        if (memoryProfile.traumaWeight() > 1.0D && family == AbilityFamily.SURVIVAL) {
            return AbilityMechanic.GUARDIAN_PULSE;
        }
        if (memoryProfile.aggressionWeight() > 1.0D && family == AbilityFamily.BRUTALITY) {
            return AbilityMechanic.BURST_STATE;
        }
        if (memoryProfile.disciplineWeight() > 1.2D && family == AbilityFamily.CONSISTENCY) {
            return AbilityMechanic.CHAIN_ESCALATION;
        }
        if (lineage != null) {
            EvolutionaryBiasGenome bias = lineage.evolutionaryBiasGenome();
            if (bias.tendency(LineageBiasDimension.EXPLORATION_PREFERENCE) > 0.12D && family == AbilityFamily.MOBILITY) {
                return AbilityMechanic.NAVIGATION_ANCHOR;
            }
            if (bias.tendency(LineageBiasDimension.RITUAL_PREFERENCE) > 0.12D && family == AbilityFamily.CHAOS) {
                return AbilityMechanic.RITUAL_CHANNEL;
            }
            if (bias.tendency(LineageBiasDimension.SUPPORT_PREFERENCE) > 0.12D && family == AbilityFamily.SURVIVAL) {
                return AbilityMechanic.GUARDIAN_PULSE;
            }
        }
        if (variantProfile != null && variantProfile.isAlphaVariant() && !underrepresentedUsefulMechanics.isEmpty() && random.nextDouble() < (0.42D + (specializationBias * 0.24D))) {
            return underrepresentedUsefulMechanics.get(random.nextInt(underrepresentedUsefulMechanics.size()));
        }
        if (variantProfile != null && variantProfile.isAlphaVariant() && random.nextDouble() < 0.36D) {
            return !underrepresentedUsefulMechanics.isEmpty()
                    ? underrepresentedUsefulMechanics.get(random.nextInt(underrepresentedUsefulMechanics.size()))
                    : current;
        }
        if (utilityHistory.hasUtilityHistory() && random.nextDouble() < (variantProfile != null && !variantProfile.isAlphaVariant() ? 0.68D : 0.55D)) {
            return utilityHistory.preferredMechanic(current);
        }
        if (random.nextDouble() < 0.1D && family == AbilityFamily.PRECISION) {
            return AbilityMechanic.MARK;
        }
        return current;
    }
}
