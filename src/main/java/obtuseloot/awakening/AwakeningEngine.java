package obtuseloot.awakening;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class AwakeningEngine {
    private AwakeningEngine() {
    }

    public static void checkAwakening(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        if (!"dormant".equals(artifact.getAwakeningPath())) {
            return;
        }

        RuntimeSettings.Snapshot settings = RuntimeSettings.get();
        String archetype = artifact.getArchetypePath();

        switch (archetype) {
            case "ravager" -> {
                if (rep.brutality() >= settings.ravagerMinBrutality() && rep.bossKills() >= settings.ravagerMinBossKills()) {
                    artifact.setAwakeningPath("Executioner's Oath");
                }
            }
            case "deadeye" -> {
                if (rep.precision() >= settings.deadeyeMinPrecision() && rep.mobility() >= settings.deadeyeMinMobility()) {
                    artifact.setAwakeningPath("Stormblade");
                }
            }
            case "vanguard" -> {
                if (rep.survival() >= settings.vanguardMinSurvival() && rep.consistency() >= settings.vanguardMinConsistency()) {
                    artifact.setAwakeningPath("Bulwark Ascendant");
                }
            }
            case "strider" -> {
                if (rep.mobility() >= settings.striderMinMobility() && rep.precision() >= settings.striderMinPrecision()) {
                    artifact.setAwakeningPath("Tempest Stride");
                }
            }
            case "harbinger" -> {
                if (rep.chaos() >= settings.harbingerMinChaos() && rep.bossKills() >= settings.harbingerMinBossKills()) {
                    artifact.setAwakeningPath("Voidwake Covenant");
                }
            }
            case "warden" -> {
                if (rep.survival() >= settings.wardenMinSurvival() && rep.consistency() >= settings.wardenMinConsistency()) {
                    artifact.setAwakeningPath("Last Survivor");
                }
            }
            default -> {
                if (rep.score() >= settings.paragonMinScore() && rep.consistency() >= settings.paragonMinConsistency()) {
                    artifact.setAwakeningPath("Crown of Equilibrium");
                }
            }
        }
    }
}
