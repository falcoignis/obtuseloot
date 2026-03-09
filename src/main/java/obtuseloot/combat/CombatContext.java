package obtuseloot.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CombatContext {
    private long lastCombatTimestamp;
    private long lastMovementTimestamp;
    private double recentMovementDistance;
    private final Set<UUID> recentTargets = new HashSet<>();
    private final Deque<Long> recentKillTimestamps = new ArrayDeque<>();
    private String lastWeaponCategory = "unknown";
    private boolean lowHealthFlag;
    private long lowHealthEnteredAt;
    private double lastKnownHealth = 20.0D;

    public void markCombat() { lastCombatTimestamp = System.currentTimeMillis(); }
    public void addMovement(double distance) { recentMovementDistance += distance; lastMovementTimestamp = System.currentTimeMillis(); }
    public void consumeMovement(double distance) { recentMovementDistance = Math.max(0D, recentMovementDistance - distance); }
    public void addTarget(UUID entityId) { recentTargets.add(entityId); }
    public void addKillTimestamp(long now) { recentKillTimestamps.addLast(now); }
    public int countKillsWithinWindow(long now, long windowMs) { pruneOldEntries(now, windowMs); return recentKillTimestamps.size(); }
    public boolean isInCombatWindow(long now, long windowMs) { return now - lastCombatTimestamp <= windowMs; }

    public void pruneOldEntries(long now, long killWindowMs) {
        while (!recentKillTimestamps.isEmpty() && now - recentKillTimestamps.peekFirst() > killWindowMs) {
            recentKillTimestamps.removeFirst();
        }
    }

    public void resetTransient() {
        recentMovementDistance = 0D;
        recentTargets.clear();
        recentKillTimestamps.clear();
        lastWeaponCategory = "unknown";
        lowHealthFlag = false;
        lowHealthEnteredAt = 0L;
        lastKnownHealth = 20.0D;
        lastMovementTimestamp = 0L;
    }

    public long getLastCombatTimestamp() { return lastCombatTimestamp; }
    public long getLastMovementTimestamp() { return lastMovementTimestamp; }
    public double getRecentMovementDistance() { return recentMovementDistance; }
    public Set<UUID> getRecentTargets() { return recentTargets; }
    public Deque<Long> getRecentKillTimestamps() { return recentKillTimestamps; }
    public String getLastWeaponCategory() { return lastWeaponCategory; }
    public void setLastWeaponCategory(String lastWeaponCategory) { this.lastWeaponCategory = lastWeaponCategory; }
    public boolean isLowHealthFlag() { return lowHealthFlag; }
    public void setLowHealthFlag(boolean lowHealthFlag) { this.lowHealthFlag = lowHealthFlag; }
    public long getLowHealthEnteredAt() { return lowHealthEnteredAt; }
    public void setLowHealthEnteredAt(long lowHealthEnteredAt) { this.lowHealthEnteredAt = lowHealthEnteredAt; }
    public double getLastKnownHealth() { return lastKnownHealth; }
    public void setLastKnownHealth(double lastKnownHealth) { this.lastKnownHealth = lastKnownHealth; }
}
