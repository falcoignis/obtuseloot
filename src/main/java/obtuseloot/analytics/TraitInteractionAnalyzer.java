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
    private final Set<String> knownTraits = Arrays.stream(GenomeTrait.values())
            .map(trait -> toCamelCase(trait.name()))
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    public TraitCorrelationMatrix analyze(Collection<Artifact> artifacts,
                                          Collection<ArtifactLineage> lineages) {
        return analyze(artifacts, lineages, List.of());
    }

    public TraitCorrelationMatrix analyze(Collection<Artifact> artifacts,
                                          Collection<ArtifactLineage> lineages,
                                          Collection<Map<String, Object>> worldSnapshots) {
        TraitCorrelationMatrix matrix = new TraitCorrelationMatrix();

        for (Artifact artifact : artifacts) {
            matrix.incrementPairs(extractArtifactGenomeTraits(artifact));
            matrix.incrementPairs(extractAbilityExpressionTraits(artifact));
        }

        for (ArtifactLineage lineage : lineages) {
            matrix.incrementPairs(extractLineageGenomeTraits(lineage));
        }

        for (Map<String, Object> snapshot : worldSnapshots) {
            matrix.incrementPairs(extractSnapshotTraits(snapshot));
        }

        return matrix;
    }

    private Set<String> extractArtifactGenomeTraits(Artifact artifact) {
        ArtifactGenome genome = genomeResolver.resolve(artifact.getArtifactSeed());
        Set<String> traits = new LinkedHashSet<>();
        for (GenomeTrait trait : GenomeTrait.values()) {
            if (genome.trait(trait) >= GENOME_ACTIVITY_THRESHOLD) {
                traits.add(toCamelCase(trait.name()));
            }
        }
        return traits;
    }

    private Set<String> extractLineageGenomeTraits(ArtifactLineage lineage) {
        Set<String> traits = new LinkedHashSet<>();
        for (Map.Entry<GenomeTrait, Double> entry : lineage.genomeTraits().entrySet()) {
            if (entry.getValue() >= GENOME_ACTIVITY_THRESHOLD) {
                traits.add(toCamelCase(entry.getKey().name()));
            }
        }
        return traits;
    }

    private Set<String> extractSnapshotTraits(Map<String, Object> snapshot) {
        Set<String> traits = new LinkedHashSet<>();
        parseExpression(String.valueOf(snapshot.getOrDefault("mutations", "")), traits);
        parseExpression(String.valueOf(snapshot.getOrDefault("branches", "")), traits);
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
        return expressions;
    }

    private void parseExpression(String value, Set<String> out) {
        if (value == null || value.isBlank()) {
            return;
        }
        String[] tokens = value.split("[^A-Za-z0-9]+");
        for (String token : tokens) {
            String camel = toCamelToken(token);
            if (knownTraits.contains(camel)) {
                out.add(camel);
            }
        }
    }

    private static String toCamelCase(String upperSnake) {
        String[] parts = upperSnake.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            builder.append(Character.toUpperCase(parts[index].charAt(0))).append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private String toCamelToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String normalized = token.trim();
        if (normalized.contains("_")) {
            return toCamelCase(normalized.toUpperCase(Locale.ROOT));
        }
        return Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
