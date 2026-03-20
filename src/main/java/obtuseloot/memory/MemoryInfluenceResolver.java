package obtuseloot.memory;

public class MemoryInfluenceResolver {
    public ArtifactMemoryProfile profileFor(ArtifactMemory memory) {
        int pressure = memory.pressure();
        double aggression = weighted(memory.count(ArtifactMemoryEvent.MULTIKILL_CHAIN), 1.2D)
                + weighted(memory.count(ArtifactMemoryEvent.FIRST_KILL), 0.35D);
        double survival = weighted(memory.count(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL), 1.25D)
                + weighted(memory.count(ArtifactMemoryEvent.LONG_BATTLE), 0.5D);
        double boss = weighted(memory.count(ArtifactMemoryEvent.FIRST_BOSS_KILL), 1.4D)
                + weighted(memory.count(ArtifactMemoryEvent.AWAKENING), 0.45D)
                + weighted(memory.count(ArtifactMemoryEvent.CONVERGENCE), 0.45D);
        double mobility = weighted(memory.count(ArtifactMemoryEvent.LONG_BATTLE), 0.25D)
                + weighted(memory.count(ArtifactMemoryEvent.MULTIKILL_CHAIN), 0.2D);
        double discipline = weighted(memory.count(ArtifactMemoryEvent.PRECISION_STREAK), 1.2D)
                + weighted(memory.count(ArtifactMemoryEvent.LONG_BATTLE), 0.8D)
                + weighted(memory.count(ArtifactMemoryEvent.FIRST_BOSS_KILL), 0.35D);
        double trauma = weighted(memory.count(ArtifactMemoryEvent.PLAYER_DEATH_WHILE_BOUND), 1.6D)
                + weighted(memory.count(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL), 0.3D);
        double chaos = weighted(memory.count(ArtifactMemoryEvent.CHAOS_RAMPAGE), 1.4D)
                + trauma * 0.4D
                + weighted(memory.count(ArtifactMemoryEvent.CONVERGENCE), 0.25D);

        return new ArtifactMemoryProfile(pressure, chaos, discipline, aggression, survival, mobility, boss, trauma);
    }

    private double weighted(int count, double scalar) {
        return count * scalar;
    }
}
