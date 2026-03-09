package obtuseloot.analytics;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;

import java.util.*;

public class TraitInteractionAnalyzer {
    private static final double GENOME_ACTIVITY_THRESHOLD = 0.55D;

    private final GenomeResolver genomeResolver = new GenomeResolver();

    public TraitCorrelationMatrix analyze(Collection<Artifact> artifacts,
                                          Collection<ArtifactLineage> lineages) {
        TraitCorrelationMatrix matrix = new TraitCorrelationMatrix();

        for (Artifact artifact : artifacts) {
            matrix.incrementPairs(extractArtifactGenomeTraits(artifact));
            matrix.incrementPairs(extractAbilityExpressionTraits(artifact));
        }

        for (ArtifactLineage lineage : lineages) {
            matrix.incrementPairs(extractLineageGenomeTraits(lineage));
        }

        return matrix;
    }

    private Set<String> extractArtifactGenomeTraits(Artifact artifact) {
        ArtifactGenome genome = genomeResolver.resolve(artifact.getArtifactSeed());
        Set<String> traits = new LinkedHashSet<>();
        for (GenomeTrait trait : GenomeTrait.values()) {
            if (genome.trait(trait) >= GENOME_ACTIVITY_THRESHOLD) {
                traits.add("genome." + trait.name().toLowerCase(Locale.ROOT));
            }
        }
        return traits;
    }

    private Set<String> extractLineageGenomeTraits(ArtifactLineage lineage) {
        Set<String> traits = new LinkedHashSet<>();
        for (Map.Entry<GenomeTrait, Double> entry : lineage.genomeTraits().entrySet()) {
            if (entry.getValue() >= GENOME_ACTIVITY_THRESHOLD) {
                traits.add("lineage." + entry.getKey().name().toLowerCase(Locale.ROOT));
            }
        }
        return traits;
    }

    private Set<String> extractAbilityExpressionTraits(Artifact artifact) {
        Set<String> expressions = new LinkedHashSet<>();
        parseExpression(artifact.getLastAbilityBranchPath(), expressions);
        parseExpression(artifact.getLastMutationHistory(), expressions);
        parseExpression(artifact.getLastMemoryInfluence(), expressions);
        for (String awakeningTrait : artifact.getAwakeningTraits()) {
            parseExpression(awakeningTrait, expressions);
        }
        return expressions.stream().map(value -> "ability." + value).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void parseExpression(String value, Set<String> out) {
        if (value == null || value.isBlank()) {
            return;
        }
        String[] tokens = value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() >= 3) {
                out.add(token);
            }
        }
    }
}
