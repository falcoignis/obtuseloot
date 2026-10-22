package obtuseloot.artifacts;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.cache.ArtifactCacheEntry;
import obtuseloot.artifacts.cache.ArtifactCacheManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.persistence.PlayerLoadGuard;
import obtuseloot.persistence.PlayerStateLoadException;
import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArtifactManager {
    private final PlayerStateStore stateStore;
    private final ArtifactSeedFactory seedFactory;
    private final Map<UUID, Artifact> loadedArtifacts = new ConcurrentHashMap<>();
    private final Map<String, Artifact> itemProfiles = new ConcurrentHashMap<>();
    private final Map<String, Long> itemProfileAccess = new ConcurrentHashMap<>();
    private long itemAccessSequence;
    private final java.util.Set<String> activatedItemProfiles = ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> dirtyItemProfiles = ConcurrentHashMap.newKeySet();
    private final Map<UUID, java.util.Set<String>> equipmentPins = new ConcurrentHashMap<>();
    public void pinEquipment(UUID player, java.util.Set<String> keys) {
        if (keys.isEmpty()) equipmentPins.remove(player); else equipmentPins.put(player, java.util.Set.copyOf(keys));
    }
    public java.util.Collection<Artifact> equippedProfiles() {
        var keys = equipmentPins.values().stream().flatMap(java.util.Set::stream).collect(java.util.stream.Collectors.toSet());
        return keys.stream().map(itemProfiles::get).filter(java.util.Objects::nonNull).toList();
    }
    public void attachEquippedItem(UUID holder, Artifact artifact) {
        boolean changed = !holder.equals(artifact.getOwnerId());
        artifact.setOwnerId(holder);
        if (activatedItemProfiles.add(artifact.getArtifactStorageKey()) && ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().hydrateFromArtifact(artifact);
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(artifact);
            changed = true;
        }
        if (changed) markDirty(artifact);
    }
    private final Map<String, UUID> storageToOwner = new ConcurrentHashMap<>();
    private final ArtifactCacheManager cache;
    private final PlayerLoadGuard loadGuard;
    private final PlayerLoadGuard itemLoadGuard;
    private volatile Consumer<UUID> recoveryListener = id -> { };

    public ArtifactManager(PlayerStateStore stateStore) {
        this(stateStore, System::currentTimeMillis, PlayerLoadGuard.DEFAULT_BACKOFF_MS);
    }

    ArtifactManager(PlayerStateStore stateStore, LongSupplier clock, long loadRetryBackoffMs) {
        this.stateStore = stateStore;
        this.seedFactory = new ArtifactSeedFactory();
        this.cache = new ArtifactCacheManager(
                stateStore,
                RuntimeSettings.get().activeArtifactCache(),
                RuntimeSettings.get().activeArtifactCacheMaxEntries(),
                RuntimeSettings.get().activeArtifactCacheIdleExpireMs()
        );
        this.loadGuard = new PlayerLoadGuard("artifact", clock, loadRetryBackoffMs, ArtifactManager::logger);
        this.itemLoadGuard = new PlayerLoadGuard("artifact item", clock, loadRetryBackoffMs, ArtifactManager::logger);
    }

    /**
     * Returns the player's artifact, generating one only when storage has no record.
     *
     * @throws PlayerStateLoadException if stored state exists but cannot be read. Nothing is created,
     *                                  cached or saved for the player; a later call retries after a backoff.
     */
    public Artifact getOrCreate(UUID playerId) {
        // The loaded profile is also the retry buffer when the optional LRU cache is disabled.
        Artifact retained = loadedArtifacts.get(playerId);
        if (retained != null && (!cache.isEnabled() || ArtifactStorageKeys.isItem(retained.getArtifactStorageKey()))) return retained;
        Artifact artifact = cache.resolve(playerId, () -> {
            Artifact loaded = loadGuard.load(playerId, stateStore::loadArtifact);
            Artifact resolved = loaded != null ? loaded : ArtifactGenerator.generateFor(playerId);
            if (loaded != null && ObtuseLoot.get() != null) {
                ObtuseLoot.get().getArtifactUsageTracker().hydrateFromArtifact(resolved);
            }
            if (resolved.getNaming() == null) {
                resolved.setNaming(ArtifactNameResolver.initialize(resolved));
            }
            if (resolved.getArtifactStorageKey() == null || resolved.getArtifactStorageKey().isBlank()) {
                resolved.setArtifactStorageKey(Artifact.buildDefaultStorageKey(playerId));
            }
            if (loaded == null && ObtuseLoot.get() != null) {
                ObtuseLoot.get().getArtifactUsageTracker().trackCreated(resolved);
            }
            return resolved;
        }).artifact();
        loadedArtifacts.put(playerId, artifact);
        storageToOwner.put(artifact.getArtifactStorageKey(), playerId);
        if (loadGuard.consumeRecovered(playerId)) {
            recoveryListener.accept(playerId);
        }
        return artifact;
    }

    /** Like {@link #getOrCreate(UUID)}, but returns null instead of throwing while the player's stored artifact is unreadable. */
    public Artifact tryGet(UUID playerId) {
        try {
            return getOrCreate(playerId);
        } catch (PlayerStateLoadException ex) {
            return null;
        }
    }

    /** Called with the player id when a previously failing load succeeds, so deferred join work can run. */
    public void setRecoveryListener(Consumer<UUID> listener) {
        this.recoveryListener = listener == null ? id -> { } : listener;
    }

    public void save(UUID playerId) {
        Artifact artifact = loadedArtifacts.get(playerId);
        if (artifact != null) {
            if (ObtuseLoot.get() != null) {
                artifact.setLastUtilityHistory(ObtuseLoot.get().getArtifactUsageTracker().utilityHistoryFor(artifact).encode());
            }
            saveOrKeepDirty(playerId, artifact);
        }
        cache.saveOwner(playerId);
    }

    public void saveAll() {
        if (ObtuseLoot.get() != null) {
            loadedArtifacts.values().forEach(artifact ->
                    artifact.setLastUtilityHistory(ObtuseLoot.get().getArtifactUsageTracker().utilityHistoryFor(artifact).encode()));
        }
        loadedArtifacts.forEach(this::saveOrKeepDirty);
        for (String key : java.util.Set.copyOf(dirtyItemProfiles)) {
            Artifact profile = itemProfiles.get(key);
            if (profile != null) saveOrKeepDirty(profile.getOwnerId(), profile);
        }
        cache.saveAllDirty();
        stateStore.retryFailedWrites();
    }

    private boolean saveOrKeepDirty(UUID playerId, Artifact artifact) {
        try {
            if (ArtifactStorageKeys.isItem(artifact.getArtifactStorageKey())) {
                stateStore.saveArtifactProfile(artifact);
                synchronized (itemProfiles) {
                    dirtyItemProfiles.remove(artifact.getArtifactStorageKey());
                    trimItemCache();
                }
            } else {
                stateStore.saveArtifact(playerId, artifact);
            }
            return true;
        } catch (RuntimeException ex) {
            markDirty(artifact);
            logger().log(Level.SEVERE, "[Persistence] Failed to save artifact for " + playerId + "; will retry on next save.", ex);
            return false;
        }
    }

    public void unload(UUID playerId) {
        loadGuard.forget(playerId);
        Artifact loaded = loadedArtifacts.get(playerId);
        if (loaded != null && ArtifactStorageKeys.isItem(loaded.getArtifactStorageKey())) {
            saveOrKeepDirty(playerId, loaded);
            loadedArtifacts.remove(playerId, loaded); // the per-item retry buffer retains failed saves
            synchronized (itemProfiles) { trimItemCache(); }
            return;
        }
        if (!cache.isEnabled() && loaded != null && !saveOrKeepDirty(playerId, loaded)) return;
        cache.releaseOwner(playerId);
        Artifact removed = loadedArtifacts.remove(playerId);
        if (removed != null) {
            storageToOwner.remove(removed.getArtifactStorageKey());
        }
    }

    public Map<UUID, Artifact> getLoadedArtifacts() {
        return loadedArtifacts;
    }


    public Artifact recreateWithArchetype(UUID playerId, EquipmentArchetype archetype, String reason) {
        EquipmentArchetype validated = java.util.Objects.requireNonNull(archetype, "archetype");
        Artifact existing = loadedOrStored(playerId);
        if (existing != null && ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackDiscard(existing);
        }
        Artifact fresh = createReplacementArtifact(existing, playerId, validated, true);
        ArtifactIdentityTransition transition = ArtifactIdentityTransition.baseline(existing, fresh,
                reason == null || reason.isBlank() ? "artifact-recreate-with-archetype" : reason);
        replaceLoadedArtifact(playerId, transition, true);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(fresh);
        }
        return fresh;
    }

    public Artifact recreate(UUID playerId) {
        Artifact existing = loadedOrStored(playerId);
        if (existing != null && ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackDiscard(existing);
        }
        Artifact fresh = createReplacementArtifact(existing, playerId, null, false);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(fresh);
        }
        replaceLoadedArtifact(playerId, ArtifactIdentityTransition.recreate(existing, fresh), true);
        return fresh;
    }

    public Artifact resolveByStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        if (ArtifactStorageKeys.isItem(storageKey)) {
            ArtifactStorageKeys.itemId(storageKey);
            Artifact retained = itemProfiles.get(storageKey);
            if (retained != null) { rememberItem(retained); return retained; }
            UUID itemId = ArtifactStorageKeys.itemId(storageKey);
            Artifact loaded = itemLoadGuard.load(itemId, ignored -> {
                Artifact stored = stateStore.loadArtifactProfile(storageKey);
                if (stored == null) throw new PlayerStateLoadException(itemId, "Missing profile for tracked item " + storageKey, null);
                return stored;
            });
            itemLoadGuard.consumeRecovered(itemId);
            if (!storageKey.equals(loaded.getArtifactStorageKey()) || loaded.getArtifactSeed() == 0) {
                throw new IllegalStateException("Invalid persisted per-item identity " + storageKey);
            }
            rememberItem(loaded);
            return loaded;
        }
        UUID owner = storageToOwner.get(storageKey);
        if (owner != null) {
            return getOrCreate(owner);
        }
        Artifact fromCacheOrStore = cache.resolveByStorageKey(storageKey, () -> {
            if (storageKey.startsWith("player:")) {
                try {
                    UUID parsed = UUID.fromString(storageKey.substring("player:".length()));
                    return getOrCreate(parsed);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            return null;
        });
        if (fromCacheOrStore != null) {
            storageToOwner.put(fromCacheOrStore.getArtifactStorageKey(), fromCacheOrStore.getOwnerId());
            loadedArtifacts.put(fromCacheOrStore.getOwnerId(), fromCacheOrStore);
            return fromCacheOrStore;
        }
        if (storageKey.startsWith("player:")) {
            try {
                UUID parsed = UUID.fromString(storageKey.substring("player:".length()));
                return getOrCreate(parsed);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    /** Persist identity before an item can be stamped. Caller owns no world/item mutation here. */
    public void registerItem(Artifact artifact) {
        String key = artifact.getArtifactStorageKey();
        ArtifactStorageKeys.itemId(key);
        if (itemProfiles.containsKey(key) || stateStore.loadArtifactProfile(key) != null) {
            throw new IllegalStateException("Artifact identity already exists: " + key);
        }
        stateStore.saveArtifactProfile(artifact);
        rememberItem(artifact);
    }

    public void saveItem(Artifact artifact) {
        markDirty(artifact);
        if (!saveOrKeepDirty(artifact.getOwnerId(), artifact)) throw new IllegalStateException("Per-item save failed");
    }

    public void activateItem(UUID holder, Artifact artifact) {
        ArtifactStorageKeys.itemId(artifact.getArtifactStorageKey());
        Artifact previous = loadedArtifacts.get(holder);
        if (previous != null && previous != artifact && !saveOrKeepDirty(holder, previous)) {
            throw new IllegalStateException("Cannot switch away from an unsaved artifact");
        }
        loadedArtifacts.entrySet().removeIf(entry -> !entry.getKey().equals(holder) && entry.getValue() == artifact);
        artifact.setOwnerId(holder);
        if (activatedItemProfiles.add(artifact.getArtifactStorageKey()) && ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().hydrateFromArtifact(artifact);
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(artifact);
        }
        loadedArtifacts.put(holder, artifact);
        markDirty(artifact);
    }

    public int cachedItemProfiles() { return itemProfiles.size(); }

    private void rememberItem(Artifact artifact) {
        synchronized (itemProfiles) {
            String key = artifact.getArtifactStorageKey();
            itemProfiles.put(key, artifact);
            itemProfileAccess.put(key, ++itemAccessSequence);
            trimItemCache();
        }
    }

    /** A memory bound only; persisted artifacts and ecological eligibility are never removed here. */
    private void trimItemCache() {
        int limit = cache.isEnabled() ? Math.max(16, RuntimeSettings.get().activeArtifactCacheMaxEntries()) : 0;
        if (itemProfiles.size() <= limit) return;
        var pinned = loadedArtifacts.values().stream().map(Artifact::getArtifactStorageKey).collect(java.util.stream.Collectors.toSet());
        equipmentPins.values().forEach(pinned::addAll);
        var evictable = itemProfiles.keySet().stream().filter(key -> !pinned.contains(key) && !dirtyItemProfiles.contains(key))
                .sorted(java.util.Comparator.comparingLong(key -> itemProfileAccess.getOrDefault(key, 0L))).toList();
        for (String key : evictable) {
            if (itemProfiles.size() <= limit) break;
            itemProfiles.remove(key);
            itemProfileAccess.remove(key);
            activatedItemProfiles.remove(key);
        }
    }

    public Artifact reseed(UUID playerId, long newSeed) {
        Artifact artifact = getOrCreate(playerId);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackDiscard(artifact);
        }
        Artifact replacement = createRegeneratedArtifact(artifact, newSeed);
        replaceLoadedArtifact(playerId, ArtifactIdentityTransition.regenerate(artifact, replacement), true);
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getArtifactUsageTracker().trackCreated(replacement);
        }
        return replacement;
    }

    public Artifact replaceIdentity(UUID playerId, ArtifactIdentityTransition transition) {
        replaceLoadedArtifact(playerId, transition, true);
        return transition.replacement();
    }

    public void markDirty(UUID playerId) {
        Artifact current = loadedArtifacts.get(playerId);
        if (current != null) markDirty(current);
        cache.markDirtyByOwner(playerId);
    }

    public void markDirty(Artifact artifact) {
        if (artifact != null && ArtifactStorageKeys.isItem(artifact.getArtifactStorageKey())) {
            synchronized (itemProfiles) {
                dirtyItemProfiles.add(artifact.getArtifactStorageKey());
                rememberItem(artifact);
            }
        }
        cache.markDirty(artifact);
    }

    public void pinSubscriptions(UUID playerId, boolean pinned) {
        cache.pinSubscription(playerId, pinned);
    }

    public ArtifactCacheManager.ArtifactCacheStats cacheStats() {
        return cache.stats();
    }

    public Map<String, ArtifactCacheEntry> cacheSnapshotByStorageKey() {
        Map<String, ArtifactCacheEntry> copy = new ConcurrentHashMap<>();
        for (ArtifactCacheEntry entry : cache.snapshotEntries()) {
            copy.put(entry.key().storageKey(), entry);
        }
        return copy;
    }

    public void invalidateAll(String reason) {
        cache.invalidateAll();
        loadedArtifacts.clear();
        storageToOwner.clear();
        if (ObtuseLoot.get() != null) {
            ObtuseLoot.get().getLogger().info("[ArtifactCache] Invalidated all entries: " + reason);
        }
    }

    /**
     * The artifact being replaced. Reads storage when the player isn't loaded, so a replacement keeps the stored
     * identity's storage key and is recorded as discarding it. Throws while storage is unreadable, which aborts
     * the replacement instead of overwriting the stored artifact.
     */
    private Artifact loadedOrStored(UUID playerId) {
        Artifact loaded = loadedArtifacts.get(playerId);
        return loaded != null ? loaded : loadGuard.load(playerId, stateStore::loadArtifact);
    }

    private static Logger logger() {
        return ObtuseLoot.get() != null ? ObtuseLoot.get().getLogger() : Logger.getLogger("ObtuseLoot");
    }

    public long rollSeed() {
        return ThreadLocalRandom.current().nextLong();
    }


    private Artifact createReplacementArtifact(Artifact previous,
                                               UUID ownerId,
                                               EquipmentArchetype forcedArchetype,
                                               boolean preservePersistenceOrigin) {
        long seed = rollSeed();
        EquipmentArchetype archetype = forcedArchetype != null
                ? forcedArchetype
                : ArtifactArchetypeValidator.requireValidArchetype(ArtifactGenerator.resolveCategory(seed),
                "artifact baseline identity regeneration");

        Artifact replacement = new Artifact(ownerId, archetype);
        if (preservePersistenceOrigin && previous != null) {
            replacement.setPersistenceOriginTimestamp(previous.getPersistenceOriginTimestamp());
        }
        replacement.setIdentityBirthTimestamp(System.currentTimeMillis());
        if (previous != null && previous.getArtifactStorageKey() != null && !previous.getArtifactStorageKey().isBlank()) {
            replacement.setArtifactStorageKey(previous.getArtifactStorageKey());
        }
        replacement.setOwnerId(ownerId);
        replacement.resetMutableState();
        seedFactory.regenerateFromSeed(replacement, seed);
        replacement.setNaming(ArtifactNameResolver.initialize(replacement));
        return replacement;
    }

    private Artifact createRegeneratedArtifact(Artifact previous, long seed) {
        EquipmentArchetype archetype = ArtifactArchetypeValidator.requireValidArchetype(
                ArtifactGenerator.resolveCategory(seed),
                "artifact baseline identity regeneration");
        Artifact replacement = new Artifact(previous.getOwnerId(), archetype);
        replacement.setPersistenceOriginTimestamp(previous.getPersistenceOriginTimestamp());
        replacement.setIdentityBirthTimestamp(System.currentTimeMillis());
        replacement.setArtifactStorageKey(previous.getArtifactStorageKey());
        replacement.setOwnerId(previous.getOwnerId());
        replacement.resetMutableState();
        seedFactory.regenerateFromSeed(replacement, seed);
        replacement.setNaming(ArtifactNameResolver.initialize(replacement));
        return replacement;
    }

    private void replaceLoadedArtifact(UUID playerId, Artifact artifact, boolean dirty) {
        replaceLoadedArtifact(playerId, new ArtifactIdentityTransition(null, artifact, "artifact-load"), dirty);
    }

    private void replaceLoadedArtifact(UUID playerId, ArtifactIdentityTransition transition, boolean dirty) {
        Artifact artifact = transition.replacement();
        Artifact previous = loadedArtifacts.get(playerId);
        if (transition.discarded() != null && previous != null && previous != transition.discarded()) {
            throw new IllegalStateException("Artifact transition discarded instance does not match loaded artifact for " + playerId);
        }
        loadedArtifacts.put(playerId, artifact);
        if (previous != null) {
            storageToOwner.remove(previous.getArtifactStorageKey(), playerId);
        }
        storageToOwner.put(artifact.getArtifactStorageKey(), playerId);
        if (ArtifactStorageKeys.isItem(artifact.getArtifactStorageKey())) {
            rememberItem(artifact);
            if (ObtuseLoot.get() != null) ObtuseLoot.get().getReputationManager().bindItem(playerId, artifact);
            if (dirty) markDirty(artifact);
        } else {
            cache.put(playerId, artifact, dirty, true);
        }
    }
}
