package obtuseloot.memory;

public class MemoryInfluenceResolver {
    public ArtifactMemoryProfile profileFor(ArtifactMemory memory) {
        int pressure = memory.pressure();
        double chaos = memory.count(ArtifactMemoryEvent.CHAOS_RAMPAGE) + (memory.count(ArtifactMemoryEvent.PLAYER_DEATH_WHILE_BOUND) * 0.5D);
        double discipline = memory.count(ArtifactMemoryEvent.PRECISION_STREAK) + memory.count(ArtifactMemoryEvent.LONG_BATTLE);
        return new ArtifactMemoryProfile(pressure, chaos, discipline);
    }
}
