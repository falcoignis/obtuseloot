package obtuseloot.evolution;

public final class ArtifactUsageProfile {
    private long firstSeenAt;
    private long lastSeenAt;
    private long uses;
    private long kills;
    private long discards;
    private long fusions;
    private long awakenings;

    public void markCreated(long now) {
        if (firstSeenAt == 0L) {
            firstSeenAt = now;
        }
        lastSeenAt = now;
    }

    public void recordUse(long now) {
        markCreated(now);
        uses++;
    }

    public void recordKill(long now) {
        markCreated(now);
        kills++;
    }

    public void recordDiscard(long now) {
        markCreated(now);
        discards++;
    }

    public void recordFusion(long now) {
        markCreated(now);
        fusions++;
    }

    public void recordAwakening(long now) {
        markCreated(now);
        awakenings++;
    }

    public double usageFrequency() {
        return perHour(uses);
    }

    public double lifetimeHours() {
        if (firstSeenAt == 0L || lastSeenAt == 0L || lastSeenAt <= firstSeenAt) {
            return 0.0D;
        }
        return (lastSeenAt - firstSeenAt) / 3_600_000.0D;
    }

    public double killParticipation() {
        if (uses <= 0L) {
            return 0.0D;
        }
        return (double) kills / (double) uses;
    }

    public double discardRate() {
        long totalOutcomes = uses + discards;
        if (totalOutcomes <= 0L) {
            return 0.0D;
        }
        return (double) discards / (double) totalOutcomes;
    }

    public double fusionParticipation() {
        if (uses <= 0L) {
            return 0.0D;
        }
        return (double) fusions / (double) uses;
    }

    public double awakeningRate() {
        if (uses <= 0L) {
            return 0.0D;
        }
        return (double) awakenings / (double) uses;
    }

    private double perHour(long count) {
        double life = Math.max(1.0D / 60.0D, lifetimeHours());
        return count / life;
    }
}
