package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactSeedFactory;
import obtuseloot.names.ArtifactNameGenerator;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class YamlPlayerStateStore implements PlayerStateStore {
    private final File playerDataDir;
    private final ArtifactSeedFactory seedFactory = new ArtifactSeedFactory();
    private final Logger logger;

    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ObtuseLoot-Persistence");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<UUID, CachedPlayerState> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> latestRequestedRevision = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> latestWrittenRevision = new ConcurrentHashMap<>();
    private final AtomicLong revisionCounter = new AtomicLong();

    public YamlPlayerStateStore(Plugin plugin) {
        this.playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        this.logger = plugin.getLogger();
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }
    }

    @Override
    public void saveArtifact(UUID playerId, Artifact artifact) {
        CachedPlayerState state = stateFor(playerId);
        String yamlText;
        long revision;

        synchronized (state) {
            writeArtifactSection(state.yaml, artifact);
            state.yaml.set("dataVersion", PlayerDataMigrator.CURRENT_DATA_VERSION);
            revision = revisionCounter.incrementAndGet();
            state.lastRevision = revision;
            yamlText = state.yaml.saveToString();
        }

        enqueueWrite(playerId, revision, yamlText, "artifact");
    }

    @Override
    public Artifact loadArtifact(UUID playerId) {
        CachedPlayerState state = stateFor(playerId);
        YamlConfiguration yaml;
        synchronized (state) {
            yaml = state.yaml;
            if (!yaml.isConfigurationSection("artifact")) {
                return null;
            }
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
        String generatedName = yaml.getString("artifact.generated-name", ArtifactNameGenerator.generateFromSeed(artifactSeed));
        artifact.setGeneratedName(generatedName);
        artifact.setLatentLineage(yaml.getString("artifact.latent-lineage", artifact.getLatentLineage()));
        artifact.setDriftAlignment(yaml.getString("artifact.drift-alignment", artifact.getDriftAlignment()));

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
        CachedPlayerState state = stateFor(playerId);
        String yamlText;
        long revision;

        synchronized (state) {
            writeReputationSection(state.yaml, reputation);
            state.yaml.set("dataVersion", PlayerDataMigrator.CURRENT_DATA_VERSION);
            revision = revisionCounter.incrementAndGet();
            state.lastRevision = revision;
            yamlText = state.yaml.saveToString();
        }

        enqueueWrite(playerId, revision, yamlText, "reputation");
    }

    @Override
    public ArtifactReputation loadReputation(UUID playerId) {
        CachedPlayerState state = stateFor(playerId);
        YamlConfiguration yaml;
        synchronized (state) {
            yaml = state.yaml;
            if (!yaml.isConfigurationSection("reputation")) {
                return null;
            }
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

    @Override
    public void flushPendingWrites() {
        writeExecutor.shutdown();
        try {
            if (!writeExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("[Persistence] Timed out while flushing writes on shutdown.");
                writeExecutor.shutdownNow();
            } else {
                logger.info("[Persistence] Pending writes flushed successfully.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warning("[Persistence] Interrupted while flushing writes; forcing shutdown.");
            writeExecutor.shutdownNow();
        }
    }

    private void enqueueWrite(UUID playerId, long revision, String yamlText, String reason) {
        latestRequestedRevision.put(playerId, revision);
        writeExecutor.submit(() -> {
            long latest = latestRequestedRevision.getOrDefault(playerId, -1L);
            if (revision < latest) {
                logger.fine("[Persistence] Skipped stale " + reason + " snapshot for " + playerId + " rev=" + revision + " latest=" + latest);
                return;
            }

            try {
                Files.writeString(fileFor(playerId).toPath(), yamlText, StandardCharsets.UTF_8);
                latestWrittenRevision.put(playerId, revision);
                logger.fine("[Persistence] Wrote " + reason + " snapshot for " + playerId + " rev=" + revision);
            } catch (IOException ex) {
                logger.warning("[Persistence] Failed writing player data for " + playerId + ": " + ex.getMessage());
            }
        });
    }

    private CachedPlayerState stateFor(UUID playerId) {
        return cache.computeIfAbsent(playerId, id -> {
            YamlConfiguration yaml = loadYaml(id);
            PlayerDataMigrator.MigrationResult migration = PlayerDataMigrator.migrateToCurrent(yaml, logger, fileFor(id).getName());
            if (migration.failed()) {
                logger.warning("[Persistence] Using in-memory defaults for " + id + " due to migration/load failure.");
                return new CachedPlayerState(new YamlConfiguration());
            }
            if (migration.migrated()) {
                long revision = revisionCounter.incrementAndGet();
                enqueueWrite(id, revision, yaml.saveToString(), "migration");
            }
            return new CachedPlayerState(yaml);
        });
    }

    private void writeArtifactSection(YamlConfiguration yaml, Artifact artifact) {
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
    }

    private void writeReputationSection(YamlConfiguration yaml, ArtifactReputation reputation) {
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

    private YamlConfiguration loadYaml(UUID playerId) {
        return YamlConfiguration.loadConfiguration(fileFor(playerId));
    }

    private File fileFor(UUID playerId) {
        return new File(playerDataDir, playerId + ".yml");
    }

    private static final class CachedPlayerState {
        private final YamlConfiguration yaml;
        private long lastRevision;

        private CachedPlayerState(YamlConfiguration yaml) {
            this.yaml = yaml;
        }
    }
}
