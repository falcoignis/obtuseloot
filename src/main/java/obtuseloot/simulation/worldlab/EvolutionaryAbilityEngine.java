package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.BranchLifecycleState;
import obtuseloot.lineage.LineageBranchProfile;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEventType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EvolutionaryAbilityEngine {
    public static final String ENTROPY_PULSE = "evolution.entropy_pulse";
    public static final String RESOURCE_PARASITISM = "evolution.resource_parasitism";
    public static final String RITUAL_AMPLIFIER = "evolution.ritual_amplifier";
    public static final String LINEAGE_FORTIFICATION = "evolution.lineage_fortification";
    public static final String NICHE_ARCHITECT = "evolution.niche_architect";

    public record AbilityEffects(double mutationDriftBias,
                                 double utilityMultiplier,
                                 double utilityTradeoffPenalty,
                                 double crowdingPressureDelta,
                                 double nicheUtilityDelta,
                                 double collapseGraceDelta,
                                 boolean ritualCoherence,
                                 double mobilityPenalty) {
        static AbilityEffects none() {
            return new AbilityEffects(0.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, 0.0D);
        }

        AbilityEffects plus(AbilityEffects other) {
            return new AbilityEffects(
                    mutationDriftBias + other.mutationDriftBias,
                    utilityMultiplier * other.utilityMultiplier,
                    utilityTradeoffPenalty + other.utilityTradeoffPenalty,
                    crowdingPressureDelta + other.crowdingPressureDelta,
                    nicheUtilityDelta + other.nicheUtilityDelta,
                    collapseGraceDelta + other.collapseGraceDelta,
                    ritualCoherence || other.ritualCoherence,
                    mobilityPenalty + other.mobilityPenalty);
        }
    }

    public AbilityEffects apply(long generation,
                                SimulatedArtifactAgent agent,
                                List<AbilityDefinition> definitions,
                                ArtifactLineage lineage,
                                String nicheId,
                                double crowdingPenalty,
                                EcosystemTelemetryEmitter telemetryEmitter) {
        EvolutionaryAbilityRuntimeState state = agent.evolutionaryAbilityState();
        state.tick();
        boolean ritualSignal = definitions.stream().anyMatch(def -> def.id().equals(RITUAL_AMPLIFIER)
                || def.id().contains("ritual")
                || def.mechanic().name().contains("RITUAL"));
        state.observeRitual(ritualSignal);
        state.observeNiche(nicheId);

        Set<String> ids = definitions.stream().map(AbilityDefinition::id).collect(Collectors.toSet());
        AbilityEffects effects = AbilityEffects.none();
        if (ids.contains(ENTROPY_PULSE)) {
            effects = effects.plus(entropyPulse(generation, agent, lineage, nicheId, telemetryEmitter));
        }
        if (ids.contains(RESOURCE_PARASITISM)) {
            effects = effects.plus(resourceParasitism(generation, agent, crowdingPenalty, nicheId, telemetryEmitter));
        }
        if (ids.contains(RITUAL_AMPLIFIER)) {
            effects = effects.plus(ritualAmplifier(generation, agent, nicheId, telemetryEmitter));
        }
        if (ids.contains(LINEAGE_FORTIFICATION)) {
            effects = effects.plus(lineageFortification(generation, agent, lineage, nicheId, telemetryEmitter));
        }
        if (ids.contains(NICHE_ARCHITECT)) {
            effects = effects.plus(nicheArchitect(generation, agent, nicheId, telemetryEmitter));
        }
        return effects;
    }

    private AbilityEffects entropyPulse(long generation,
                                        SimulatedArtifactAgent agent,
                                        ArtifactLineage lineage,
                                        String nicheId,
                                        EcosystemTelemetryEmitter telemetryEmitter) {
        EvolutionaryAbilityRuntimeState state = agent.evolutionaryAbilityState();
        double divergence = lineage == null ? 0.0D : lineage.currentBranchDivergence();
        boolean triggered = !state.onCooldown(ENTROPY_PULSE)
                && (divergence > 0.11D || (lineage != null && lineage.repeatedDivergences() >= 2));
        if (!triggered) {
            return AbilityEffects.none();
        }
        state.setCooldown(ENTROPY_PULSE, 6);
        state.activate(ENTROPY_PULSE, 3);
        emit(telemetryEmitter, generation, agent, nicheId, ENTROPY_PULSE, "triggered",
                Map.of("trigger", "branch-divergence", "branch_divergence", String.valueOf(divergence)));
        emit(telemetryEmitter, generation, agent, nicheId, ENTROPY_PULSE, "effect",
                Map.of("mutation_drift_bias", "0.16", "window", "3"));
        emit(telemetryEmitter, generation, agent, nicheId, ENTROPY_PULSE, "tradeoff",
                Map.of("survival_penalty", "0.06", "maintenance_burden", "0.04"));
        return new AbilityEffects(0.16D, 1.0D, 0.06D, 0.04D, 0.0D, 0.0D, false, 0.0D);
    }

    private AbilityEffects resourceParasitism(long generation,
                                              SimulatedArtifactAgent agent,
                                              double crowdingPenalty,
                                              String nicheId,
                                              EcosystemTelemetryEmitter telemetryEmitter) {
        EvolutionaryAbilityRuntimeState state = agent.evolutionaryAbilityState();
        boolean triggered = !state.onCooldown(RESOURCE_PARASITISM) && crowdingPenalty > 1.08D;
        if (!triggered) {
            return AbilityEffects.none();
        }
        state.setCooldown(RESOURCE_PARASITISM, 5);
        state.activate(RESOURCE_PARASITISM, 2);
        emit(telemetryEmitter, generation, agent, nicheId, RESOURCE_PARASITISM, "triggered",
                Map.of("trigger", "dominance-pressure", "crowding_penalty", String.valueOf(crowdingPenalty)));
        emit(telemetryEmitter, generation, agent, nicheId, RESOURCE_PARASITISM, "effect",
                Map.of("dominance_siphon", "0.05", "collapse_pressure_delta", "0.06"));
        emit(telemetryEmitter, generation, agent, nicheId, RESOURCE_PARASITISM, "tradeoff",
                Map.of("parasite_risk_penalty", "0.04", "maintenance_burden", "0.03"));
        return new AbilityEffects(0.0D, 1.05D, 0.04D, 0.06D, 0.0D, 0.0D, false, 0.0D);
    }

    private AbilityEffects ritualAmplifier(long generation,
                                           SimulatedArtifactAgent agent,
                                           String nicheId,
                                           EcosystemTelemetryEmitter telemetryEmitter) {
        EvolutionaryAbilityRuntimeState state = agent.evolutionaryAbilityState();
        boolean triggered = !state.onCooldown(RITUAL_AMPLIFIER) && state.ritualStreak() >= 3;
        if (!triggered) {
            return AbilityEffects.none();
        }
        state.setCooldown(RITUAL_AMPLIFIER, 7);
        state.activate(RITUAL_AMPLIFIER, 4);
        emit(telemetryEmitter, generation, agent, nicheId, RITUAL_AMPLIFIER, "triggered",
                Map.of("trigger", "repeated-ritual-pattern", "ritual_streak", String.valueOf(state.ritualStreak())));
        emit(telemetryEmitter, generation, agent, nicheId, RITUAL_AMPLIFIER, "effect",
                Map.of("ritual_utility_bonus", "0.10", "mutation_drift_dampening", "0.08"));
        emit(telemetryEmitter, generation, agent, nicheId, RITUAL_AMPLIFIER, "tradeoff",
                Map.of("adaptability_penalty", "0.05", "mobility_penalty", "0.03"));
        return new AbilityEffects(-0.08D, 1.10D, 0.02D, 0.0D, 0.04D, 0.0D, true, 0.03D);
    }

    private AbilityEffects lineageFortification(long generation,
                                                SimulatedArtifactAgent agent,
                                                ArtifactLineage lineage,
                                                String nicheId,
                                                EcosystemTelemetryEmitter telemetryEmitter) {
        EvolutionaryAbilityRuntimeState state = agent.evolutionaryAbilityState();
        LineageBranchProfile branch = dominantBranch(lineage);
        boolean unstable = (branch != null
                && (branch.lifecycleState() == BranchLifecycleState.UNSTABLE || branch.lifecycleState() == BranchLifecycleState.COLLAPSING
                || branch.lastSurvivalScore() < 0.10D))
                || (lineage != null && lineage.currentBranchDivergence() > 0.10D);
        boolean triggered = !state.onCooldown(LINEAGE_FORTIFICATION) && unstable;
        if (!triggered) {
            return AbilityEffects.none();
        }
        state.setCooldown(LINEAGE_FORTIFICATION, 8);
        state.activate(LINEAGE_FORTIFICATION, 3);
        emit(telemetryEmitter, generation, agent, nicheId, LINEAGE_FORTIFICATION, "triggered",
                Map.of("trigger", "collapse-imminent", "survival_score", String.valueOf(branch == null ? 0.0D : branch.lastSurvivalScore())));
        emit(telemetryEmitter, generation, agent, nicheId, LINEAGE_FORTIFICATION, "effect",
                Map.of("collapse_grace_delta", "1.0", "resilience_bonus", "0.07"));
        emit(telemetryEmitter, generation, agent, nicheId, LINEAGE_FORTIFICATION, "tradeoff",
                Map.of("maintenance_burden", "0.09", "utility_penalty", "0.03"));
        return new AbilityEffects(0.0D, 1.0D, 0.03D, 0.02D, 0.0D, 1.0D, false, 0.0D);
    }

    private AbilityEffects nicheArchitect(long generation,
                                          SimulatedArtifactAgent agent,
                                          String nicheId,
                                          EcosystemTelemetryEmitter telemetryEmitter) {
        EvolutionaryAbilityRuntimeState state = agent.evolutionaryAbilityState();
        boolean triggered = !state.onCooldown(NICHE_ARCHITECT) && state.stableNicheStreak() >= 4;
        if (!triggered) {
            return AbilityEffects.none();
        }
        state.setCooldown(NICHE_ARCHITECT, 8);
        state.activate(NICHE_ARCHITECT, 5);
        emit(telemetryEmitter, generation, agent, nicheId, NICHE_ARCHITECT, "triggered",
                Map.of("trigger", "sustained-niche-occupation", "niche_stability_streak", String.valueOf(state.stableNicheStreak())));
        emit(telemetryEmitter, generation, agent, nicheId, NICHE_ARCHITECT, "effect",
                Map.of("niche_utility_density_delta", "0.08", "specialization_trajectory", "0.06"));
        emit(telemetryEmitter, generation, agent, nicheId, NICHE_ARCHITECT, "tradeoff",
                Map.of("cross_niche_mobility_penalty", "0.05", "adaptability_penalty", "0.04"));
        return new AbilityEffects(0.0D, 1.06D, 0.04D, 0.0D, 0.08D, 0.0D, false, 0.05D);
    }

    private LineageBranchProfile dominantBranch(ArtifactLineage lineage) {
        if (lineage == null || lineage.branches().isEmpty()) {
            return null;
        }
        String dominantBranchId = lineage.dominantBranchId();
        return lineage.branches().values().stream()
                .filter(branch -> branch.branchId().equals(dominantBranchId))
                .findFirst()
                .orElse(lineage.branches().values().iterator().next());
    }

    private void emit(EcosystemTelemetryEmitter emitter,
                      long generation,
                      SimulatedArtifactAgent agent,
                      String nicheId,
                      String abilityId,
                      String phase,
                      Map<String, String> attributes) {
        if (emitter == null) {
            return;
        }
        Map<String, String> out = new HashMap<>(attributes);
        out.put("generation", String.valueOf(generation));
        out.put("ability_id", abilityId);
        out.put("ability_phase", phase);
        out.put("mechanic", "EVOLUTIONARY_ABILITY");
        out.put("trigger", "ON_RUNTIME_TICK");
        out.put("execution_status", "SUCCESS");
        out.put("outcome_classification", "MEANINGFUL");
        out.put("context_tags", "evolutionary-ability");
        emitter.emit(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                agent.artifact().getArtifactSeed(),
                agent.artifact().getLatentLineage(),
                nicheId == null ? "unassigned" : nicheId,
                out);
    }
}
