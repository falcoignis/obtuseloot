package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.memory.MemoryInfluenceResolver;
import obtuseloot.reputation.ArtifactReputation;

import java.util.Locale;

record ArtifactDisposition(
        String drive,
        Temperament temperament,
        double pressure,
        Direction direction,
        double driveIntensity,
        double restraint,
        double aggression,
        double survivalInstinct,
        double mobility,
        double chaos,
        double convergencePull,
        double awakeningPull
) {
    enum Temperament {
        RESTRAINED,
        FORCEFUL,
        WATCHFUL,
        RESTLESS,
        FRACTURED
    }

    enum Direction {
        DEEPENING,
        ASCENDING,
        SPLITTING,
        BRACING,
        SETTLING
    }

    private static final MemoryInfluenceResolver MEMORY = new MemoryInfluenceResolver();

    static ArtifactDisposition resolve(Artifact artifact, ArtifactReputation reputation) {
        ArtifactMemoryProfile memory = MEMORY.profileFor(artifact.getMemory());
        double precision = blend(artifact.getSeedPrecisionAffinity(), usageScore(reputation == null ? 0 : reputation.getPrecision(), 18.0D), memory.disciplineWeight() / 10.0D);
        double brutality = blend(artifact.getSeedBrutalityAffinity(), usageScore(reputation == null ? 0 : reputation.getBrutality(), 18.0D), memory.aggressionWeight() / 10.0D);
        double survival = blend(artifact.getSeedSurvivalAffinity(), usageScore(reputation == null ? 0 : reputation.getSurvival(), 18.0D), memory.survivalWeight() / 10.0D + memory.traumaWeight() / 14.0D);
        double mobility = blend(artifact.getSeedMobilityAffinity(), usageScore(reputation == null ? 0 : reputation.getMobility(), 18.0D), memory.mobilityWeight() / 10.0D);
        double chaos = blend(artifact.getSeedChaosAffinity(), usageScore(reputation == null ? 0 : reputation.getChaos(), 18.0D), memory.chaosWeight() / 10.0D);
        double consistency = blend(artifact.getSeedConsistencyAffinity(), usageScore(reputation == null ? 0 : reputation.getConsistency(), 18.0D), memory.disciplineWeight() / 12.0D);

        double convergencePull = shapeSignal(artifact.getConvergenceIdentityShape(), artifact.getConvergenceExpressionTrace(), artifact.getConvergencePath(),
                "split", "twin", "horizon", "echo", "borrowed", "mirror", "merge", "syndicate", "reef", "choir",
                "reaper", "bastion", "citadel", "piercer");
        double awakeningPull = shapeSignal(artifact.getAwakeningIdentityShape(), artifact.getAwakeningExpressionTrace(), artifact.getAwakeningPath(),
                "oath", "wake", "storm", "ascendant", "last", "crown", "vow", "anchor", "unyielding", "waking",
                "tempest");
        double pressure = clamp((memory.pressure() / 10.0D)
                + memory.traumaWeight() * 0.06D
                + artifact.getDriftLevel() * 0.08D
                + artifact.getTotalDrifts() * 0.03D
                + (artifact.hasInstability() ? 0.28D : 0.0D)
                + Math.abs(convergencePull - awakeningPull) * 0.25D);

        String drive = dominant(precision, brutality, survival, mobility, chaos, consistency);
        double driveIntensity = switch (drive) {
            case "precision" -> precision;
            case "brutality" -> brutality;
            case "survival" -> survival;
            case "mobility" -> mobility;
            case "chaos" -> chaos;
            default -> consistency;
        };

        Temperament temperament;
        if (chaos + pressure > 1.35D) temperament = Temperament.FRACTURED;
        else if (brutality > precision + 0.15D && brutality > survival) temperament = Temperament.FORCEFUL;
        else if (survival > brutality && survival >= precision) temperament = Temperament.WATCHFUL;
        else if (mobility > Math.max(survival, consistency)) temperament = Temperament.RESTLESS;
        else temperament = Temperament.RESTRAINED;

        Direction direction;
        if (convergencePull > awakeningPull + 0.12D) direction = Direction.SPLITTING;
        else if (awakeningPull > convergencePull + 0.12D) direction = Direction.ASCENDING;
        else if (pressure > 0.72D && survival >= Math.max(brutality, chaos)) direction = Direction.BRACING;
        else if (driveIntensity > 0.78D || artifact.getHistoryScore() >= 7) direction = Direction.DEEPENING;
        else direction = Direction.SETTLING;

        return new ArtifactDisposition(drive, temperament, pressure, direction, driveIntensity, consistency, brutality, survival, mobility, chaos, convergencePull, awakeningPull);
    }

    private static double usageScore(int value, double scale) {
        return clamp(value / scale);
    }

    private static double blend(double a, double b, double c) {
        return clamp(a * 0.45D + b * 0.30D + c * 0.25D);
    }

    private static String dominant(double precision, double brutality, double survival, double mobility, double chaos, double consistency) {
        String[] keys = {"precision", "brutality", "survival", "mobility", "chaos", "consistency"};
        double[] values = {precision, brutality, survival, mobility, chaos, consistency};
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[best]) best = i;
        }
        return keys[best];
    }

    private static double shapeSignal(String a, String b, String c, String... markers) {
        String joined = ((a == null ? "" : a) + " " + (b == null ? "" : b) + " " + (c == null ? "" : c)).toLowerCase(Locale.ROOT);
        double score = 0.0D;
        for (String marker : markers) {
            if (joined.contains(marker)) score += 0.12D;
        }
        return clamp(score);
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
