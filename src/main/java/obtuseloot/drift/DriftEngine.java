package obtuseloot.drift;

import obtuseloot.artifacts.Artifact;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DriftEngine {
    public boolean shouldDrift(ArtifactReputation reputation) {
        RuntimeSettings.Snapshot s = RuntimeSettings.get();
        double chance = s.driftBaseChance() + reputation.getChaos() * s.driftChaosMultiplier()
                - reputation.getConsistency() * s.driftConsistencyReduction();
        chance = Math.max(0.0D, Math.min(s.driftMaxChance(), chance));
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public DriftMutation applyDrift(Player player, Artifact artifact, ArtifactReputation reputation) {
        DriftProfile profile = resolveProfile(artifact, reputation);
        applyBiasMutation(artifact, profile);
        artifact.incrementDriftLevel();
        artifact.incrementTotalDrifts();
        long now = System.currentTimeMillis();
        artifact.setLastDriftTimestamp(now);
        artifact.setDriftAlignment(profile.name().toLowerCase());
        String instability = maybeApplyInstability(artifact, profile);
        String msg = buildDriftMessage(profile);
        artifact.addDriftHistory(msg);
        artifact.addLoreHistory("Drift: " + msg);
        artifact.addNotableEvent("drift." + profile.name().toLowerCase());
        player.sendMessage(msg);
        return new DriftMutation(true, profile.name().toLowerCase(), msg, true, instability);
    }

    public DriftProfile resolveProfile(Artifact artifact, ArtifactReputation reputation) {
        Map<String, Integer> m = Map.of(
                "chaos", reputation.getChaos(), "brutality", reputation.getBrutality(), "precision", reputation.getPrecision(),
                "survival", reputation.getSurvival(), "mobility", reputation.getMobility());
        String top = m.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getKey).orElse("paradox");
        return switch (top) {
            case "chaos" -> DriftProfile.VOLATILE;
            case "brutality" -> DriftProfile.PREDATORY;
            case "precision" -> DriftProfile.ASCETIC;
            case "survival" -> DriftProfile.HOLLOW;
            case "mobility" -> DriftProfile.TEMPEST;
            default -> DriftProfile.PARADOX;
        };
    }

    public void applyBiasMutation(Artifact artifact, DriftProfile profile) {
        profile.biasDeltaMap().forEach((k, v) -> artifact.getDriftBiasAdjustments().merge(k, v, Double::sum));
    }

    public String maybeApplyInstability(Artifact artifact, DriftProfile profile) {
        long expiry = System.currentTimeMillis() + (RuntimeSettings.get().driftInstabilityDurationSeconds() * 1000L);
        artifact.setInstabilityState(profile.instabilityType(), expiry);
        return profile.instabilityType();
    }

    public String buildDriftMessage(DriftProfile profile) {
        return "§5Your artifact drifts: " + profile.name().toLowerCase() + " resonance awakens.";
    }
}
