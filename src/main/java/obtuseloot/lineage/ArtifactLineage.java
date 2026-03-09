package obtuseloot.lineage;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArtifactLineage {
    private final String lineageId;
    private int generationIndex;
    private final List<Long> ancestorSeeds = new ArrayList<>();
    private final List<ArtifactAncestor> ancestors = new ArrayList<>();
    private final Map<String, Double> lineageTraits = new LinkedHashMap<>();
    private final EnumMap<GenomeTrait, Double> genomeTraits = new EnumMap<>(GenomeTrait.class);

    public ArtifactLineage(String lineageId) {
        this.lineageId = lineageId;
        for (GenomeTrait trait : GenomeTrait.values()) {
            genomeTraits.put(trait, 0.0D);
        }
        for (String trait : new String[]{"precision", "brutality", "survival", "mobility", "chaos", "consistency", "mutation", "memory"}) {
            lineageTraits.put(trait, 0.0D);
        }
    }

    public String lineageId() { return lineageId; }
    public int depth() { return generationIndex; }
    public int generationIndex() { return generationIndex; }
    public List<Long> ancestorSeeds() { return List.copyOf(ancestorSeeds); }
    public List<ArtifactAncestor> ancestors() { return ancestors; }
    public Map<String, Double> lineageTraits() { return lineageTraits; }
    public Map<GenomeTrait, Double> genomeTraits() { return Map.copyOf(genomeTraits); }

    public void addAncestor(ArtifactAncestor ancestor) {
        ancestors.add(ancestor);
        generationIndex = Math.max(generationIndex, ancestor.generationIndex());
        ancestorSeeds.add(ancestor.artifactSeed());
    }

    public void registerGenome(long ancestorSeed, ArtifactGenome genome) {
        int nextGeneration = generationIndex + 1;
        addAncestor(new ArtifactAncestor(ancestorSeed, nextGeneration));
        for (GenomeTrait trait : GenomeTrait.values()) {
            genomeTraits.put(trait, genome.trait(trait));
        }
    }

    public void applyMutation(LineageMutation mutation) {
        double current = lineageTraits.getOrDefault(mutation.trait(), 0.0D);
        lineageTraits.put(mutation.trait(), clamp(current + mutation.delta()));
    }

    private double clamp(double value) {
        return Math.max(-0.10D, Math.min(0.10D, value));
    }
}
