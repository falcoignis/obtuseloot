package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactSeedFactory;
import obtuseloot.names.ArtifactNameGenerator;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class YamlPlayerStateStore implements PlayerStateStore {
    private final File playerDataDir;
    private final ArtifactSeedFactory seedFactory = new ArtifactSeedFactory();

    public YamlPlayerStateStore(Plugin plugin) {
        this.playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }
    }

    @Override
    public void saveArtifact(UUID playerId, Artifact artifact) {
        YamlConfiguration yaml = load(playerId);
        String base = "artifact.";
        yaml.set(base + "owner-id", artifact.getOwnerId().toString());
        yaml.set(base + "artifact-seed", artifact.getArtifactSeed());
        yaml.set(base + "generated-name", artifact.getGeneratedName());
        yaml.set(base + "archetype-path", artifact.getArchetypePath());
        yaml.set(base + "evolution-path", artifact.getEvolutionPath());
        yaml.set(base + "awakening-path", artifact.getAwakeningPath());
        yaml.set(base + "fusion-path", artifact.getFusionPath());
        yaml.set(base + "drift-level", artifact.getDriftLevel());
        yaml.set(base + "total-drifts", artifact.getTotalDrifts());
        yaml.set(base + "drift-alignment", artifact.getDriftAlignment());
        yaml.set(base + "last-drift-timestamp", artifact.getLastDriftTimestamp());
        yaml.set(base + "latent-lineage", artifact.getLatentLineage());
        yaml.set(base + "current-instability-state", artifact.getCurrentInstabilityState());
        yaml.set(base + "instability-expiry", artifact.getInstabilityExpiryTimestamp());

        writeStatMap(yaml, base + "seed-affinities", Map.of(
                "precision", artifact.getSeedPrecisionAffinity(), "brutality", artifact.getSeedBrutalityAffinity(),
                "survival", artifact.getSeedSurvivalAffinity(), "mobility", artifact.getSeedMobilityAffinity(),
                "chaos", artifact.getSeedChaosAffinity(), "consistency", artifact.getSeedConsistencyAffinity()));
        writeStatMap(yaml, base + "drift-bias", artifact.getDriftBiasAdjustments());
        writeStatMap(yaml, base + "awakening-bias", artifact.getAwakeningBiasAdjustments());
        writeStatMap(yaml, base + "awakening-gain-multipliers", artifact.getAwakeningGainMultipliers());

        yaml.set(base + "drift-history", artifact.getDriftHistory());
        yaml.set(base + "lore-history", artifact.getLoreHistory());
        yaml.set(base + "notable-events", artifact.getNotableEvents());
        yaml.set(base + "awakening-traits", List.copyOf(artifact.getAwakeningTraits()));
        save(playerId, yaml);
    }

    @Override
    public Artifact loadArtifact(UUID playerId) {
        YamlConfiguration yaml = load(playerId);
        if (!yaml.isConfigurationSection("artifact")) {
            return null;
        }
        Artifact artifact = new Artifact(playerId, "");
        artifact.setOwnerId(UUID.fromString(yaml.getString("artifact.owner-id", playerId.toString())));
        artifact.setArchetypePath(yaml.getString("artifact.archetype-path", "unformed"));
        artifact.setEvolutionPath(yaml.getString("artifact.evolution-path", "base"));
        artifact.setAwakeningPath(yaml.getString("artifact.awakening-path", "dormant"));
        artifact.setFusionPath(yaml.getString("artifact.fusion-path", "none"));
        artifact.setDriftLevel(yaml.getInt("artifact.drift-level", 0));
        artifact.setTotalDrifts(yaml.getInt("artifact.total-drifts", 0));
        artifact.setLastDriftTimestamp(yaml.getLong("artifact.last-drift-timestamp", 0L));
        artifact.setInstabilityState(yaml.getString("artifact.current-instability-state", "none"), yaml.getLong("artifact.instability-expiry", 0L));

        long artifactSeed = yaml.getLong("artifact.artifact-seed", 0L);
        artifact.setArtifactSeed(artifactSeed);
        seedFactory.applySeedProfile(artifact, artifactSeed);
        artifact.setGeneratedName(ArtifactNameGenerator.generateFromSeed(artifactSeed));

        readStatMap(yaml, "artifact.drift-bias", artifact.getDriftBiasAdjustments());
        readStatMap(yaml, "artifact.awakening-bias", artifact.getAwakeningBiasAdjustments());
        readStatMap(yaml, "artifact.awakening-gain-multipliers", artifact.getAwakeningGainMultipliers());

        artifact.getDriftHistory().addAll(yaml.getStringList("artifact.drift-history"));
        artifact.getLoreHistory().addAll(yaml.getStringList("artifact.lore-history"));
        artifact.getNotableEvents().addAll(yaml.getStringList("artifact.notable-events"));
        artifact.getAwakeningTraits().addAll(yaml.getStringList("artifact.awakening-traits"));
        return artifact;
    }

    @Override
    public void saveReputation(UUID playerId, ArtifactReputation reputation) {
        YamlConfiguration yaml = load(playerId);
        String base = "reputation.";
        yaml.set(base + "precision", reputation.getPrecision());
        yaml.set(base + "brutality", reputation.getBrutality());
        yaml.set(base + "survival", reputation.getSurvival());
        yaml.set(base + "mobility", reputation.getMobility());
        yaml.set(base + "chaos", reputation.getChaos());
        yaml.set(base + "consistency", reputation.getConsistency());
        yaml.set(base + "kills", reputation.getKills());
        yaml.set(base + "boss-kills", reputation.getBossKills());
        yaml.set(base + "recent-kill-chain", reputation.getRecentKillChain());
        yaml.set(base + "last-kill-timestamp", reputation.getLastKillTimestamp());
        yaml.set(base + "last-combat-timestamp", reputation.getLastCombatTimestamp());
        yaml.set(base + "survival-streak", reputation.getSurvivalStreak());
        save(playerId, yaml);
    }

    @Override
    public ArtifactReputation loadReputation(UUID playerId) {
        YamlConfiguration yaml = load(playerId);
        if (!yaml.isConfigurationSection("reputation")) {
            return null;
        }
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(yaml.getInt("reputation.precision", 0));
        rep.setBrutality(yaml.getInt("reputation.brutality", 0));
        rep.setSurvival(yaml.getInt("reputation.survival", 0));
        rep.setMobility(yaml.getInt("reputation.mobility", 0));
        rep.setChaos(yaml.getInt("reputation.chaos", 0));
        rep.setConsistency(yaml.getInt("reputation.consistency", 0));
        rep.setKills(yaml.getInt("reputation.kills", 0));
        rep.setBossKills(yaml.getInt("reputation.boss-kills", 0));
        rep.setRecentKillChain(yaml.getInt("reputation.recent-kill-chain", 0));
        rep.setLastKillTimestamp(yaml.getLong("reputation.last-kill-timestamp", 0));
        rep.setLastCombatTimestamp(yaml.getLong("reputation.last-combat-timestamp", 0));
        rep.setSurvivalStreak(yaml.getInt("reputation.survival-streak", 0));
        return rep;
    }

    @Override
    public void saveAll(Map<UUID, Artifact> artifacts, Map<UUID, ArtifactReputation> reputations) {
        artifacts.forEach(this::saveArtifact);
        reputations.forEach(this::saveReputation);
    }

    private void writeStatMap(YamlConfiguration yaml, String path, Map<String, Double> values) {
        for (String stat : List.of("precision", "brutality", "survival", "mobility", "chaos", "consistency")) {
            yaml.set(path + "." + stat, values.getOrDefault(stat, 0.0D));
        }
    }

    private void readStatMap(YamlConfiguration yaml, String path, Map<String, Double> target) {
        for (String stat : List.of("precision", "brutality", "survival", "mobility", "chaos", "consistency")) {
            target.put(stat, yaml.getDouble(path + "." + stat, 0.0D));
        }
    }

    private YamlConfiguration load(UUID playerId) {
        return YamlConfiguration.loadConfiguration(fileFor(playerId));
    }

    private void save(UUID playerId, YamlConfiguration yaml) {
        try {
            yaml.save(fileFor(playerId));
        } catch (IOException ignored) {
        }
    }

    private File fileFor(UUID playerId) {
        return new File(playerDataDir, playerId + ".yml");
    }
}
