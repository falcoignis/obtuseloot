package obtuseloot.awakening;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public class AwakeningEngine {
    private final Map<String, AwakeningEffectProfile> profiles = Map.of(
            "Executioner's Oath", new AwakeningEffectProfile("Executioner's Oath", Map.of("brutality", 1.5, "chaos", 0.5), Map.of("brutality", 1.2), Set.of("execution")),
            "Stormblade", new AwakeningEffectProfile("Stormblade", Map.of("precision", 1.2, "mobility", 1.0), Map.of("precision", 1.15), Set.of("stormstep")),
            "Bulwark Ascendant", new AwakeningEffectProfile("Bulwark Ascendant", Map.of("survival", 1.4, "consistency", 0.8), Map.of("survival", 1.2), Set.of("fortress")),
            "Tempest Stride", new AwakeningEffectProfile("Tempest Stride", Map.of("mobility", 1.4, "chaos", 0.5), Map.of("mobility", 1.2), Set.of("windrunner")),
            "Voidwake Covenant", new AwakeningEffectProfile("Voidwake Covenant", Map.of("chaos", 1.6), Map.of("chaos", 1.25), Set.of("voidwake")),
            "Last Survivor", new AwakeningEffectProfile("Last Survivor", Map.of("survival", 1.2, "precision", 0.8), Map.of("survival", 1.15), Set.of("last-stand")),
            "Crown of Equilibrium", new AwakeningEffectProfile("Crown of Equilibrium", Map.of("consistency", 1.5), Map.of("consistency", 1.3), Set.of("equilibrium"))
    );

    public boolean evaluate(Player player, Artifact artifact, ArtifactReputation reputation) {
        if (!ArtifactEligibility.isAbilityEligible(artifact)) {
            return false;
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            return false;
        }

        String resolved = resolve(artifact, reputation);
        if (resolved == null) {
            return false;
        }
        return applyAwakening(player, artifact, resolved);
    }

    public boolean forceAwakening(Player player, Artifact artifact, ArtifactReputation reputation) {
        if (!ArtifactEligibility.isAbilityEligible(artifact)) {
            return false;
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            return false;
        }
        String resolved = resolve(artifact, reputation);
        if (resolved == null) {
            resolved = "Crown of Equilibrium";
        }
        return applyAwakening(player, artifact, resolved);
    }

    private boolean applyAwakening(Player player, Artifact artifact, String resolved) {
        artifact.setAwakeningPath(resolved);
        AwakeningEffectProfile profile = profiles.get(resolved);
        if (profile != null) {
            profile.biasAdjustments().forEach((k, v) -> artifact.getAwakeningBiasAdjustments().merge(k, v, Double::sum));
            profile.reputationGainMultipliers().forEach((k, v) -> artifact.getAwakeningGainMultipliers().put(k, v));
            artifact.getAwakeningTraits().addAll(profile.traits());
        }
        artifact.addLoreHistory("Awakening: " + resolved);
        artifact.addNotableEvent("awakening." + resolved.toLowerCase().replace(' ', '-'));
        player.sendMessage("§dYour artifact awakens: " + resolved);
        return true;
    }

    private String resolve(Artifact artifact, ArtifactReputation rep) {
        return switch (artifact.getArchetypePath()) {
            case "ravager" -> rep.getBrutality() >= 12 ? "Executioner's Oath" : null;
            case "deadeye" -> rep.getPrecision() >= 12 ? "Stormblade" : null;
            case "vanguard" -> rep.getSurvival() >= 12 ? "Bulwark Ascendant" : null;
            case "strider" -> rep.getMobility() >= 12 ? "Tempest Stride" : null;
            case "harbinger" -> rep.getChaos() >= 12 ? "Voidwake Covenant" : null;
            case "warden" -> rep.getSurvival() >= 10 && rep.getConsistency() >= 10 ? "Last Survivor" : null;
            default -> rep.getTotalScore() >= 80 ? "Crown of Equilibrium" : null;
        };
    }
}
