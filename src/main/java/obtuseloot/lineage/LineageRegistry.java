package obtuseloot.lineage;

import obtuseloot.artifacts.Artifact;
import obtuseloot.species.ArtifactPopulationSignature;
import obtuseloot.species.ArtifactSpecies;
import obtuseloot.species.LineageSpeciationEngine;
import obtuseloot.species.SpeciesRegistry;
import obtuseloot.species.SpeciesRegistrySnapshot;
import obtuseloot.species.SpeciesSignatureResolver;

import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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

    public ArtifactLineage assignLineage(Artifact artifact) {
        Random random = new Random(artifact.getArtifactSeed() ^ artifact.getOwnerId().getMostSignificantBits());
        String lineageId = artifact.getLatentLineage();
        if (lineageId == null || lineageId.isBlank() || "common".equalsIgnoreCase(lineageId)) {
            lineageId = random.nextDouble() < 0.65D
                    ? "lineage-" + UUID.nameUUIDFromBytes((artifact.getOwnerId().toString() + artifact.getArtifactSeed()).getBytes())
                    : "wild-" + Long.toUnsignedString(artifact.getArtifactSeed() & 65535L);
            artifact.setLatentLineage(lineageId);
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.LINEAGE, lineageId));
        }
        ArtifactLineage lineage = lineages.computeIfAbsent(lineageId, ArtifactLineage::new);
        if (!lineage.ancestorSeeds().contains(artifact.getArtifactSeed())) {
            lineage.addAncestor(new ArtifactAncestor(artifact.getArtifactSeed(), lineage.generationIndex() + 1));
        }
        return lineage;
    }

    public void recordDescendantBias(Artifact artifact,
                                     EvolutionaryBiasGenome observedBias,
                                     double ecologicalPressure,
                                     double mutationInfluence) {
        ArtifactLineage lineage = assignLineage(artifact);
        int beforeBranches = lineage.branches().size();
        double driftWindow = new LineageInfluenceResolver().resolveDriftWindow(lineage);
        double utilityDensity = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()).utilityDensity();
        lineage.registerDescendantBias(
                artifact.getArtifactSeed(),
                observedBias,
                ecologicalPressure,
                mutationInfluence,
                driftWindow,
                utilityDensity,
                branchingHeuristics);
        if (lineage.branches().size() > beforeBranches) {
            String branchId = lineage.dominantBranchId();
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.LINEAGE, lineage.lineageId() + " [" + branchId + "]"));
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
}
