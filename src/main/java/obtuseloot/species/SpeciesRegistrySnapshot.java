package obtuseloot.species;

import java.util.LinkedHashMap;
import java.util.Map;

public record SpeciesRegistrySnapshot(Map<String, ArtifactSpecies> speciesById,
                                      Map<String, String> lineageRoots) {
    public SpeciesRegistrySnapshot {
        speciesById = new LinkedHashMap<>(speciesById);
        lineageRoots = new LinkedHashMap<>(lineageRoots);
    }
}
