package obtuseloot.simulation.worldlab;

import java.util.HashMap;
import java.util.Map;

public class EvolutionaryAbilityRuntimeState {
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private final Map<String, Integer> activeWindows = new HashMap<>();
    private String lastNiche = "unassigned";
    private int stableNicheStreak;
    private int ritualStreak;

    public void tick() {
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        cooldowns.entrySet().removeIf(e -> e.getValue() <= 0);
        activeWindows.replaceAll((k, v) -> Math.max(0, v - 1));
        activeWindows.entrySet().removeIf(e -> e.getValue() <= 0);
    }

    public boolean onCooldown(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0) > 0;
    }

    public void setCooldown(String abilityId, int turns) {
        cooldowns.put(abilityId, Math.max(0, turns));
    }

    public void activate(String abilityId, int turns) {
        activeWindows.put(abilityId, Math.max(0, turns));
    }

    public boolean active(String abilityId) {
        return activeWindows.getOrDefault(abilityId, 0) > 0;
    }

    public void observeNiche(String nicheId) {
        String safe = nicheId == null || nicheId.isBlank() ? "unassigned" : nicheId;
        if (safe.equals(lastNiche)) {
            stableNicheStreak++;
        } else {
            stableNicheStreak = 0;
        }
        lastNiche = safe;
    }

    public void observeRitual(boolean ritualSignal) {
        if (ritualSignal) {
            ritualStreak++;
        } else {
            ritualStreak = Math.max(0, ritualStreak - 1);
        }
    }

    public int stableNicheStreak() {
        return stableNicheStreak;
    }

    public int ritualStreak() {
        return ritualStreak;
    }
}
