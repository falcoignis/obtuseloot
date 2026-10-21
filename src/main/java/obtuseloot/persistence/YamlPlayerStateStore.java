package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.species.ArtifactSpecies;
import obtuseloot.species.SpeciesRegistrySnapshot;
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
    private final java.util.Set<UUID> failedWrites = ConcurrentHashMap.newKeySet();

    public YamlPlayerStateStore(Plugin plugin) {
        this.playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        this.logger = plugin.getLogger();
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }
    }

    @Override
    public void saveArtifactProfile(Artifact artifact) {
        var id = obtuseloot.artifacts.ArtifactStorageKeys.itemId(artifact.getArtifactStorageKey());
        writeAtomically(new File(playerDataDir.getParentFile(), "artifacts/" + id + ".yml").toPath(), ArtifactProfileCodec.encode(artifact));
    }

    @Override
    public Artifact loadArtifactProfile(String storageKey) {
        var id = obtuseloot.artifacts.ArtifactStorageKeys.itemId(storageKey);
        String payload = readStrictly(new File(playerDataDir.getParentFile(), "artifacts/" + id + ".yml").toPath());
        return payload == null ? null : ArtifactProfileCodec.decode(storageKey, payload);
    }

    @Override public void saveEcologyState(String state) {
        writeAtomically(new File(playerDataDir.getParentFile(), "ecology-state.yml").toPath(), state);
    }
    @Override public String loadEcologyState() {
        return readStrictly(new File(playerDataDir.getParentFile(), "ecology-state.yml").toPath());
    }

    private String readStrictly(java.nio.file.Path path) {
        if (!Files.exists(path)) return null;
        try { return Files.readString(path, StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new IllegalStateException("Unreadable state file " + path.getFileName(), exception); }
    }

    private void writeAtomically(java.nio.file.Path path, String payload) {
        java.nio.file.Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".pending");
            Files.writeString(temporary, payload, StandardCharsets.UTF_8);
            Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed writing state file " + path.getFileName(), exception);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); }
            catch (IOException exception) { logger.warning("Unable to remove pending state file " + temporary.getFileName()); }
        }
    }

    @Override
    public void saveArtifact(UUID playerId, Artifact artifact) {
        artifact.refreshNamingProjection();
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

        return ArtifactProfileCodec.readArtifact(playerId, yaml);
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

        return ArtifactProfileCodec.readReputation(yaml);
    }

    @Override
    public void saveAll(Map<UUID, Artifact> artifacts, Map<UUID, ArtifactReputation> reputations) {
        artifacts.forEach(this::saveArtifact);
        reputations.forEach(this::saveReputation);
    }

    @Override
    public void flushPendingWrites() {
        retryFailedWrites();
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
                failedWrites.add(playerId);
                logger.severe("[Persistence] Failed writing player data for " + playerId + " rev=" + revision
                        + "; will retry on next save: " + ex.getMessage());
                return;
            }
            failedWrites.remove(playerId);
        });
    }

    @Override
    public void retryFailedWrites() {
        for (UUID playerId : List.copyOf(failedWrites)) {
            CachedPlayerState state = cache.get(playerId);
            if (state == null) {
                continue;
            }
            String yamlText;
            long revision;
            synchronized (state) {
                revision = revisionCounter.incrementAndGet();
                state.lastRevision = revision;
                yamlText = state.yaml.saveToString();
            }
            enqueueWrite(playerId, revision, yamlText, "retry");
        }
    }

    private CachedPlayerState stateFor(UUID playerId) {
        return cache.computeIfAbsent(playerId, id -> {
            YamlConfiguration yaml = loadYaml(id);
            PlayerDataMigrator.MigrationResult migration = PlayerDataMigrator.migrateToCurrent(yaml, logger, fileFor(id).getName());
            if (migration.failed()) {
                throw new PlayerStateLoadException(id, "[Persistence] Refusing to load " + id + " due to migration failure.", null);
            }
            if (migration.migrated()) {
                long revision = revisionCounter.incrementAndGet();
                enqueueWrite(id, revision, yaml.saveToString(), "migration");
            }
            return new CachedPlayerState(yaml);
        });
    }


    @Override
    public void saveSpeciesSnapshot(SpeciesRegistrySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        File file = new File(playerDataDir.getParentFile(), "species-registry.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        snapshot.speciesById().forEach((id, species) -> {
            String base = "species." + id + ".";
            yaml.set(base + "parent-species-id", species.parentSpeciesId());
            yaml.set(base + "origin-lineage-id", species.originLineageId());
            yaml.set(base + "created-at", species.createdAtEpochMs());
            yaml.set(base + "created-generation", species.createdGeneration());
            yaml.set(base + "divergence-snapshot", species.divergenceSnapshot());
            yaml.set(base + "tendency-profile", species.tendencyProfile());
        });
        yaml.set("lineage-roots", snapshot.lineageRoots());
        try {
            yaml.save(file);
        } catch (IOException ex) {
            logger.warning("[Persistence] Failed to save species snapshot: " + ex.getMessage());
        }
    }

    @Override
    public SpeciesRegistrySnapshot loadSpeciesSnapshot() {
        File file = new File(playerDataDir.getParentFile(), "species-registry.yml");
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        java.util.Map<String, ArtifactSpecies> species = new java.util.LinkedHashMap<>();
        if (yaml.getConfigurationSection("species") != null) {
            for (String id : yaml.getConfigurationSection("species").getKeys(false)) {
                String base = "species." + id + ".";
                java.util.Map<String, Double> divergence = mapFromObject(yaml.getConfigurationSection(base + "divergence-snapshot") != null ? yaml.getConfigurationSection(base + "divergence-snapshot").getValues(false) : java.util.Map.of());
                java.util.Map<String, Double> tendencies = mapFromObject(yaml.getConfigurationSection(base + "tendency-profile") != null ? yaml.getConfigurationSection(base + "tendency-profile").getValues(false) : java.util.Map.of());
                species.put(id, new ArtifactSpecies(
                        id,
                        yaml.getString(base + "parent-species-id", "none"),
                        yaml.getString(base + "origin-lineage-id", "unknown"),
                        yaml.getLong(base + "created-at", 0L),
                        yaml.getInt(base + "created-generation", 0),
                        divergence,
                        tendencies
                ));
            }
        }
        java.util.Map<String, String> lineageRoots = new java.util.LinkedHashMap<>();
        if (yaml.getConfigurationSection("lineage-roots") != null) {
            for (String key : yaml.getConfigurationSection("lineage-roots").getKeys(false)) {
                lineageRoots.put(key, yaml.getString("lineage-roots." + key, ""));
            }
        }
        return new SpeciesRegistrySnapshot(species, lineageRoots);
    }


    private void writeArtifactSection(YamlConfiguration yaml, Artifact artifact) {
        ArtifactProfileCodec.writeArtifact(yaml, artifact);
    }
    private void writeReputationSection(YamlConfiguration yaml, ArtifactReputation reputation) {
        ArtifactProfileCodec.writeReputation(yaml, reputation);
    }

    private Map<String, Double> mapFromObject(Map<String, Object> raw) {
        Map<String, Double> out = new java.util.LinkedHashMap<>();
        raw.forEach((k, v) -> {
            if (v instanceof Number n) {
                out.put(k, n.doubleValue());
            }
        });
        return out;
    }

    private YamlConfiguration loadYaml(UUID playerId) {
        File file = fileFor(playerId);
        YamlConfiguration yaml = new YamlConfiguration();
        if (!file.exists()) {
            return yaml;
        }
        // YamlConfiguration.loadConfiguration swallows parse/IO errors and returns an empty config,
        // which would read as "no record" and later overwrite the unreadable file.
        try {
            yaml.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
            throw new PlayerStateLoadException(playerId, "Unreadable player data file " + file.getName() + ": " + ex.getMessage(), ex);
        }
        return yaml;
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
