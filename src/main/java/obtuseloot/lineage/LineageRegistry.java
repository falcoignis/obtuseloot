package obtuseloot.lineage;

import obtuseloot.artifacts.Artifact;
import obtuseloot.species.ArtifactPopulationSignature;
import obtuseloot.species.ArtifactSpecies;
import obtuseloot.species.LineageSpeciationEngine;
import obtuseloot.species.SpeciesRegistry;
import obtuseloot.species.SpeciesRegistrySnapshot;
import obtuseloot.species.SpeciesSignatureResolver;

import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEventType;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import obtuseloot.evolution.AdaptiveSupportBudget;
import obtuseloot.evolution.LineageCompetitionModel;
import obtuseloot.evolution.LineageMomentumPool;
import obtuseloot.evolution.UtilityHistoryRollup;

public class LineageRegistry {
    private final Map<String, ArtifactLineage> lineages = new LinkedHashMap<>();
    private final Map<String, LinkedList<ArtifactPopulationSignature>> speciesSignatures = new LinkedHashMap<>();
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();
    private final SpeciesRegistry speciesRegistry = new SpeciesRegistry();
    private final LineageSpeciationEngine speciationEngine = new LineageSpeciationEngine();
    private final SpeciesSignatureResolver signatureResolver = new SpeciesSignatureResolver();
    private final InheritanceBranchingHeuristics branchingHeuristics = new InheritanceBranchingHeuristics();
    private final Map<Long, String> telemetryNicheContextByArtifact = new ConcurrentHashMap<>();
    private volatile int driftWindowDurationTicks = 5;
    private volatile EcosystemTelemetryEmitter telemetryEmitter;

    public void setTelemetryEmitter(EcosystemTelemetryEmitter telemetryEmitter) {
        this.telemetryEmitter = telemetryEmitter;
    }

    public void setDriftWindowDurationTicks(int driftWindowDurationTicks) {
        this.driftWindowDurationTicks = Math.max(1, driftWindowDurationTicks);
    }

    public void updateTelemetryNicheContext(long artifactSeed, String nicheId) {
        if (artifactSeed <= 0L) {
            return;
        }
        if (nicheId == null || nicheId.isBlank()) {
            telemetryNicheContextByArtifact.remove(artifactSeed);
            return;
        }
        telemetryNicheContextByArtifact.put(artifactSeed, nicheId);
    }

    public ArtifactLineage assignLineage(Artifact artifact) {
        Random random = new Random(artifact.getArtifactSeed() ^ artifact.getOwnerId().getMostSignificantBits());
        String lineageId = artifact.getLatentLineage();
        if (lineageId == null || lineageId.isBlank() || "unassigned".equalsIgnoreCase(lineageId)) {
            lineageId = random.nextDouble() < 0.65D
                    ? "lineage-" + UUID.nameUUIDFromBytes((artifact.getOwnerId().toString() + artifact.getArtifactSeed()).getBytes())
                    : "wild-" + Long.toUnsignedString(artifact.getArtifactSeed() & 65535L);
            artifact.setLatentLineage(lineageId);
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.LINEAGE, lineageId));
        }
        ArtifactLineage lineage = lineages.computeIfAbsent(lineageId, ArtifactLineage::new);
        if (!lineage.ancestorSeeds().contains(artifact.getArtifactSeed())) {
            lineage.addAncestor(new ArtifactAncestor(artifact.getArtifactSeed(), lineage.generationIndex() + 1));
            emit(EcosystemTelemetryEventType.LINEAGE_UPDATE, artifact, lineageId, Map.of("event", "ancestor-added", "context_tags", "lineage-update"));
        }
        return lineage;
    }

    public void recordDescendantBias(Artifact artifact,
                                     EvolutionaryBiasGenome observedBias,
                                     double ecologicalPressure,
                                     double mutationInfluence) {
        ArtifactLineage lineage = assignLineage(artifact);
        int beforeBranches = lineage.branches().size();
        double driftWindow = new LineageInfluenceResolver().resolveDriftWindow(lineage, driftWindowDurationTicks);
        double utilityDensity = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()).utilityDensity();
        lineage.registerDescendantBias(
                artifact.getArtifactSeed(),
                observedBias,
                ecologicalPressure,
                mutationInfluence,
                driftWindow,
                utilityDensity,
                branchingHeuristics);
        emit(EcosystemTelemetryEventType.MUTATION_EVENT, artifact, lineage.lineageId(), Map.of(
                "driftWindow", String.valueOf(driftWindow),
                "drift_window_remaining", String.valueOf(driftWindow),
                "utilityDensity", String.valueOf(utilityDensity),
                "utility_density", String.valueOf(utilityDensity),
                "ecologicalPressure", String.valueOf(ecologicalPressure),
                "ecology_pressure", String.valueOf(ecologicalPressure),
                "mutationInfluence", String.valueOf(mutationInfluence),
                "mutation_influence", String.valueOf(mutationInfluence)
        ));
        if (lineage.branches().size() > beforeBranches) {
            String branchId = lineage.dominantBranchId();
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.LINEAGE, lineage.lineageId() + " [" + branchId + "]"));
            emit(EcosystemTelemetryEventType.BRANCH_FORMATION, artifact, lineage.lineageId(), Map.of("branchId", branchId, "branch_id", branchId));
        }
        for (BranchLifecycleTransition transition : lineage.consumeRecentBranchTransitions()) {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("event", transition.collapsed() ? "branch-collapsed" : "branch-lifecycle-transition");
            attrs.put("branch_id", transition.branchId());
            attrs.put("lifecycle_from", transition.from().name());
            attrs.put("lifecycle_state", transition.to().name());
            attrs.put("grace_window_remaining", String.valueOf(transition.graceWindowRemaining()));
            attrs.put("survival_score", String.valueOf(transition.survivalScore()));
            attrs.put("maintenance_cost", String.valueOf(transition.maintenanceCost()));
            attrs.put("collapse_reason", transition.reason());
            attrs.put("context_tags", transition.collapsed() ? "branch-collapse" : "branch-lifecycle");
            emit(EcosystemTelemetryEventType.LINEAGE_UPDATE, artifact, lineage.lineageId(), attrs);
        }
    }

    public ArtifactLineage lineageFor(String lineageId) {
        return lineages.get(lineageId);
    }

    public ArtifactSpecies resolveSpecies(Artifact artifact) {
        ArtifactLineage lineage = assignLineage(artifact);
        return speciesRegistry.resolveSpecies(artifact, lineage);
    }

    public ArtifactSpecies evaluateSpeciation(Artifact artifact) {
        ArtifactLineage lineage = assignLineage(artifact);
        ArtifactSpecies currentSpecies = speciesRegistry.resolveSpecies(artifact, lineage);
        ArtifactPopulationSignature currentSignature = signatureResolver.fromArtifact(artifact);
        ArtifactPopulationSignature baseline = baselineFor(currentSpecies.speciesId(), currentSignature);
        ArtifactSpecies resolved = speciationEngine.evaluate(artifact, lineage, speciesRegistry, currentSignature, baseline);
        pushSignature(resolved.speciesId(), currentSignature);
        return resolved;
    }

    private ArtifactPopulationSignature baselineFor(String speciesId, ArtifactPopulationSignature fallback) {
        List<ArtifactPopulationSignature> history = speciesSignatures.get(speciesId);
        if (history == null || history.isEmpty()) {
            return fallback;
        }
        return history.get(history.size() - 1);
    }

    private void pushSignature(String speciesId, ArtifactPopulationSignature signature) {
        LinkedList<ArtifactPopulationSignature> history = speciesSignatures.computeIfAbsent(speciesId, ignored -> new LinkedList<>());
        history.add(signature);
        while (history.size() > 20) {
            history.removeFirst();
        }
    }


    public LineageMomentumPool momentumSnapshot(AdaptiveSupportBudget budget) {
        return new LineageCompetitionModel().evaluate(lineages, budget);
    }

    public Map<String, ArtifactLineage> lineages() {
        return lineages;
    }

    public SpeciesRegistry speciesRegistry() {
        return speciesRegistry;
    }

    public SpeciesRegistrySnapshot speciesSnapshot() {
        return speciesRegistry.snapshot();
    }

    public void restoreSpeciesSnapshot(SpeciesRegistrySnapshot snapshot) {
        speciesRegistry.restore(snapshot);
    }

    private void emit(EcosystemTelemetryEventType type, Artifact artifact, String lineageId, Map<String, String> attrs) {
        EcosystemTelemetryEmitter emitter = telemetryEmitter;
        if (emitter != null && artifact != null) {
            Map<String, String> enriched = new LinkedHashMap<>(attrs);
            ArtifactLineage lineage = lineages.get(lineageId);
            if (lineage != null) {
                enriched.put("generation", String.valueOf(lineage.generationIndex()));
                if (type == EcosystemTelemetryEventType.LINEAGE_UPDATE
                        || type == EcosystemTelemetryEventType.MUTATION_EVENT
                        || type == EcosystemTelemetryEventType.BRANCH_FORMATION) {
                    enriched.put("branch_divergence", String.valueOf(lineage.currentBranchDivergence()));
                }
                if (type == EcosystemTelemetryEventType.LINEAGE_UPDATE
                        || type == EcosystemTelemetryEventType.MUTATION_EVENT) {
                    enriched.put("specialization_trajectory", String.valueOf(lineage.specializationTrajectoryDelta()));
                }
            }
            String niche = enriched.getOrDefault("niche", telemetryNicheContextByArtifact.getOrDefault(artifact.getArtifactSeed(), ""));
            emitter.emit(type, artifact.getArtifactSeed(), lineageId, niche, enriched);
        }
    }
}
