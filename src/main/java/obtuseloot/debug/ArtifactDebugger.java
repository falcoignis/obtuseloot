package obtuseloot.debug;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.drift.DriftEngine;
import obtuseloot.reputation.ArtifactReputation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ArtifactDebugger {
    private ArtifactDebugger() {
    }

    public static String describe(UUID playerId) {
        return String.join(" | ", describeLines(playerId));
    }

    public static List<String> describeLines(UUID playerId) {
        Artifact artifact = ObtuseLoot.get().getArtifactManager().getOrCreate(playerId);
        ArtifactReputation rep = ObtuseLoot.get().getReputationManager().get(playerId);
        List<String> lines = new ArrayList<>();
        lines.add("id=" + artifact.getArtifactSeed() + ", owner=" + artifact.getOwnerId());
        lines.add("name=\"" + artifact.getName() + "\", archetype=" + artifact.getArchetypePath()
                + ", evolution=" + artifact.getEvolutionPath());
        lines.add("awakening=" + artifact.getAwakeningPath() + ", fusion=" + artifact.getFusionPath()
                + ", drift=" + artifact.getDriftLevel() + " (total=" + artifact.getTotalDrifts() + ", align=" + artifact.getDriftAlignment() + ")");
        lines.add("reputation={precision=" + rep.precision() + ", brutality=" + rep.brutality() + ", survival=" + rep.survival()
                + ", mobility=" + rep.mobility() + ", chaos=" + rep.chaos() + ", consistency=" + rep.consistency() + "}");
        lines.add("driftNow=" + new DriftEngine().shouldDrift(rep) + ", lineage=" + artifact.getLatentLineage()
                + ", instability=" + artifact.getCurrentInstabilityState());
        lines.add("awakeningTraits=" + artifact.getAwakeningTraits());
        lines.add("recentLore=" + tail(artifact.getLoreHistory(), 3) + ", recentEvents=" + tail(artifact.getNotableEvents(), 3));
        return lines;
    }

    private static List<String> tail(List<String> values, int count) {
        if (values.size() <= count) {
            return values;
        }
        return values.subList(values.size() - count, values.size());
    }
}
