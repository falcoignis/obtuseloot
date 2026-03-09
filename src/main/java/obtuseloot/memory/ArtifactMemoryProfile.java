package obtuseloot.memory;

public record ArtifactMemoryProfile(
        int pressure,
        double chaosWeight,
        double disciplineWeight,
        double aggressionWeight,
        double survivalWeight,
        double mobilityWeight,
        double bossWeight,
        double traumaWeight
) {
}
