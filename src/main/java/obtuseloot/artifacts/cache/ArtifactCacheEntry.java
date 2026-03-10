package obtuseloot.artifacts.cache;

import obtuseloot.artifacts.Artifact;

import java.util.UUID;

public final class ArtifactCacheEntry {
    private final ArtifactCacheKey key;
    private final UUID ownerId;
    private final Artifact artifact;
    private volatile boolean dirty;
    private volatile boolean onlinePinned;
    private volatile boolean subscriptionPinned;
    private volatile long generation;
    private volatile long lastAccessMs;
    private volatile long lastSavedMs;

    public ArtifactCacheEntry(ArtifactCacheKey key, UUID ownerId, Artifact artifact, long nowMs, boolean dirty) {
        this.key = key;
        this.ownerId = ownerId;
        this.artifact = artifact;
        this.dirty = dirty;
        this.lastAccessMs = nowMs;
        this.generation = 1L;
    }

    public ArtifactCacheKey key() { return key; }
    public UUID ownerId() { return ownerId; }
    public Artifact artifact() { return artifact; }
    public boolean dirty() { return dirty; }
    public boolean onlinePinned() { return onlinePinned; }
    public boolean subscriptionPinned() { return subscriptionPinned; }
    public long generation() { return generation; }
    public long lastAccessMs() { return lastAccessMs; }
    public long lastSavedMs() { return lastSavedMs; }
    public void touch(long nowMs) { this.lastAccessMs = nowMs; }
    public void setOnlinePinned(boolean onlinePinned) { this.onlinePinned = onlinePinned; }
    public void setSubscriptionPinned(boolean subscriptionPinned) { this.subscriptionPinned = subscriptionPinned; }

    public void markDirty(long nowMs) {
        this.dirty = true;
        this.lastAccessMs = nowMs;
        this.generation++;
    }

    public void markSaved(long nowMs) {
        this.dirty = false;
        this.lastSavedMs = nowMs;
        this.lastAccessMs = nowMs;
    }

    public boolean evictable(long nowMs, long idleExpiryMs) {
        if (onlinePinned || subscriptionPinned) return false;
        return idleExpiryMs <= 0 || (nowMs - lastAccessMs) >= idleExpiryMs;
    }
}
