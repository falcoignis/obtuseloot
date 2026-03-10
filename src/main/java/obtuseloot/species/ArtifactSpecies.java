package obtuseloot.species;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArtifactSpecies {
    private final String speciesId;
    private final String parentSpeciesId;
    private final String originLineageId;
    private final long createdAtEpochMs;
    private final int createdGeneration;
    private final Map<String, Double> divergenceSnapshot;
    private final Map<String, Double> tendencyProfile;

    public ArtifactSpecies(String speciesId,
                           String parentSpeciesId,
                           String originLineageId,
                           long createdAtEpochMs,
                           int createdGeneration,
                           Map<String, Double> divergenceSnapshot,
                           Map<String, Double> tendencyProfile) {
        this.speciesId = speciesId;
        this.parentSpeciesId = parentSpeciesId;
        this.originLineageId = originLineageId;
        this.createdAtEpochMs = createdAtEpochMs;
        this.createdGeneration = createdGeneration;
        this.divergenceSnapshot = new LinkedHashMap<>(divergenceSnapshot);
        this.tendencyProfile = new LinkedHashMap<>(tendencyProfile);
    }

    public String speciesId() { return speciesId; }
    public String parentSpeciesId() { return parentSpeciesId; }
    public String originLineageId() { return originLineageId; }
    public long createdAtEpochMs() { return createdAtEpochMs; }
    public int createdGeneration() { return createdGeneration; }
    public Map<String, Double> divergenceSnapshot() { return Map.copyOf(divergenceSnapshot); }
    public Map<String, Double> tendencyProfile() { return Map.copyOf(tendencyProfile); }
}
