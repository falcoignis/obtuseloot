package obtuseloot.species;

import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SpeciesRegistry {
    private final Map<String, ArtifactSpecies> speciesById = new LinkedHashMap<>();
    private final Map<String, String> lineageRoots = new LinkedHashMap<>();

    public ArtifactSpecies resolveSpecies(Artifact artifact, ArtifactLineage lineage) {
        String speciesId = artifact.getSpeciesId();
        if (speciesId != null && !speciesId.isBlank() && speciesById.containsKey(speciesId)) {
            return speciesById.get(speciesId);
        }

        String lineageId = lineage.lineageId();
        String rootId = lineageRoots.computeIfAbsent(lineageId, l -> "species-root-" + lineageId);
        ArtifactSpecies root = speciesById.computeIfAbsent(rootId, id -> new ArtifactSpecies(
                id,
                "none",
                lineageId,
                System.currentTimeMillis(),
                lineage.generationIndex(),
                Map.of("compatibility", 0.0D),
                Map.of("gateAffinity", 0.5D, "branchBias", 0.5D, "environmentBias", 0.5D)
        ));
        artifact.setSpeciesId(root.speciesId());
        artifact.setParentSpeciesId(root.parentSpeciesId());
        return root;
    }

    public ArtifactSpecies registerSplit(Artifact artifact,
                                         ArtifactLineage lineage,
                                         ArtifactSpecies parent,
                                         Map<String, Double> divergenceSnapshot,
                                         Map<String, Double> tendencyProfile) {
        String speciesId = "species-" + UUID.nameUUIDFromBytes((lineage.lineageId() + ":" + artifact.getArtifactSeed() + ":" + lineage.generationIndex()).getBytes());
        ArtifactSpecies species = new ArtifactSpecies(
                speciesId,
                parent.speciesId(),
                lineage.lineageId(),
                System.currentTimeMillis(),
                lineage.generationIndex(),
                divergenceSnapshot,
                tendencyProfile);
        speciesById.put(speciesId, species);
        artifact.setSpeciesId(speciesId);
        artifact.setParentSpeciesId(parent.speciesId());
        return species;
    }

    public ArtifactSpecies byId(String speciesId) {
        return speciesById.get(speciesId);
    }

    public Map<String, ArtifactSpecies> allSpecies() {
        return Map.copyOf(speciesById);
    }

    public SpeciesRegistrySnapshot snapshot() {
        return new SpeciesRegistrySnapshot(speciesById, lineageRoots);
    }

    public void restore(SpeciesRegistrySnapshot snapshot) {
        speciesById.clear();
        lineageRoots.clear();
        if (snapshot == null) {
            return;
        }
        speciesById.putAll(snapshot.speciesById());
        lineageRoots.putAll(snapshot.lineageRoots());
    }
}
