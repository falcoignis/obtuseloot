package obtuseloot.artifacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import obtuseloot.memory.ArtifactMemory;

public class Artifact {
    private static final int MAX_HISTORY_ENTRIES = 120;

    private long artifactSeed;
    private String artifactStorageKey;
    private UUID ownerId;
    private String generatedName;
    private String itemCategory;
    private String archetypePath;
    private String evolutionPath;
    private String awakeningPath;
    private String fusionPath;

    private int driftLevel;
    private int totalDrifts;
    // Seed initialization sets the baseline drift tendency, and runtime drift mutations
    // update the same field as the current mutable drift alignment state.
    private String driftAlignment;
    private long lastDriftTimestamp;

    private String latentLineage;
    private String currentInstabilityState;
    private long instabilityExpiryTimestamp;

    private double seedPrecisionAffinity;
    private double seedBrutalityAffinity;
    private double seedSurvivalAffinity;
    private double seedMobilityAffinity;
    private double seedChaosAffinity;
    private double seedConsistencyAffinity;

    private final Map<String, Double> driftBiasAdjustments;
    private final Map<String, Double> awakeningBiasAdjustments;
    private final Map<String, Double> awakeningGainMultipliers;

    private final List<String> driftHistory;
    private final List<String> loreHistory;
    private final List<String> notableEvents;
    private final Set<String> awakeningTraits;
    private final ArtifactMemory memory;

    private String lastAbilityBranchPath;
    private String lastMutationHistory;
    private String lastMemoryInfluence;
    private String lastRegulatoryProfile;
    private String lastOpenRegulatoryGates;
    private String lastGateCandidatePool;

    public Artifact(UUID ownerId, String generatedName) {
        this.ownerId = ownerId;
        this.artifactStorageKey = buildDefaultStorageKey(ownerId);
        this.generatedName = generatedName;
        this.itemCategory = "artifact";
        this.archetypePath = "unformed";
        this.evolutionPath = "base";
        this.awakeningPath = "dormant";
        this.fusionPath = "none";
        this.driftAlignment = "stable";
        this.latentLineage = "common";
        this.currentInstabilityState = "none";
        this.driftBiasAdjustments = new HashMap<>();
        this.awakeningBiasAdjustments = new HashMap<>();
        this.awakeningGainMultipliers = new HashMap<>();
        this.driftHistory = new ArrayList<>();
        this.loreHistory = new ArrayList<>();
        this.notableEvents = new ArrayList<>();
        this.awakeningTraits = new HashSet<>();
        this.memory = new ArtifactMemory();
        this.lastAbilityBranchPath = "[]";
        this.lastMutationHistory = "[]";
        this.lastMemoryInfluence = "none";
        this.lastRegulatoryProfile = "[]";
        this.lastOpenRegulatoryGates = "";
        this.lastGateCandidatePool = "0->0";
    }

    public void resetMutableState() {
        driftBiasAdjustments.clear();
        awakeningBiasAdjustments.clear();
        awakeningGainMultipliers.clear();
        driftLevel = 0;
        totalDrifts = 0;
        driftAlignment = "stable";
        lastDriftTimestamp = 0L;
        archetypePath = "unformed";
        evolutionPath = "base";
        awakeningPath = "dormant";
        fusionPath = "none";
        currentInstabilityState = "none";
        instabilityExpiryTimestamp = 0L;
        driftHistory.clear();
        loreHistory.clear();
        notableEvents.clear();
        awakeningTraits.clear();
        lastAbilityBranchPath = "[]";
        lastMutationHistory = "[]";
        lastMemoryInfluence = "none";
        lastRegulatoryProfile = "[]";
        lastOpenRegulatoryGates = "";
        lastGateCandidatePool = "0->0";
    }

    public long getArtifactSeed() { return artifactSeed; }
    public void setArtifactSeed(long artifactSeed) { this.artifactSeed = artifactSeed; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        if (artifactStorageKey == null || artifactStorageKey.isBlank()) {
            artifactStorageKey = buildDefaultStorageKey(ownerId);
        }
    }
    public String getArtifactStorageKey() { return artifactStorageKey; }
    public void setArtifactStorageKey(String artifactStorageKey) {
        if (artifactStorageKey == null || artifactStorageKey.isBlank()) {
            this.artifactStorageKey = buildDefaultStorageKey(ownerId);
            return;
        }
        this.artifactStorageKey = artifactStorageKey;
    }
    public String getGeneratedName() { return generatedName; }
    public String getItemCategory() { return itemCategory; }
    public String getName() { return generatedName; }
    public void setGeneratedName(String generatedName) { this.generatedName = generatedName; }
    public void setItemCategory(String itemCategory) { this.itemCategory = itemCategory; }
    public String getArchetypePath() { return archetypePath; }
    public void setArchetypePath(String archetypePath) { this.archetypePath = archetypePath; }
    public String getEvolutionPath() { return evolutionPath; }
    public void setEvolutionPath(String evolutionPath) { this.evolutionPath = evolutionPath; }
    public String getAwakeningPath() { return awakeningPath; }
    public void setAwakeningPath(String awakeningPath) { this.awakeningPath = awakeningPath; }
    public String getFusionPath() { return fusionPath; }
    public void setFusionPath(String fusionPath) { this.fusionPath = fusionPath; }
    public int getDriftLevel() { return driftLevel; }
    public void setDriftLevel(int driftLevel) { this.driftLevel = driftLevel; }
    public int getTotalDrifts() { return totalDrifts; }
    public void setTotalDrifts(int totalDrifts) { this.totalDrifts = totalDrifts; }
    public String getDriftAlignment() { return driftAlignment; }
    public void setDriftAlignment(String driftAlignment) { this.driftAlignment = driftAlignment; }
    public long getLastDriftTimestamp() { return lastDriftTimestamp; }
    public void setLastDriftTimestamp(long lastDriftTimestamp) { this.lastDriftTimestamp = lastDriftTimestamp; }
    public String getLatentLineage() { return latentLineage; }
    public void setLatentLineage(String latentLineage) { this.latentLineage = latentLineage; }
    public String getCurrentInstabilityState() { return currentInstabilityState; }
    public long getInstabilityExpiryTimestamp() { return instabilityExpiryTimestamp; }
    public double getSeedPrecisionAffinity() { return seedPrecisionAffinity; }
    public void setSeedPrecisionAffinity(double value) { this.seedPrecisionAffinity = value; }
    public double getSeedBrutalityAffinity() { return seedBrutalityAffinity; }
    public void setSeedBrutalityAffinity(double value) { this.seedBrutalityAffinity = value; }
    public double getSeedSurvivalAffinity() { return seedSurvivalAffinity; }
    public void setSeedSurvivalAffinity(double value) { this.seedSurvivalAffinity = value; }
    public double getSeedMobilityAffinity() { return seedMobilityAffinity; }
    public void setSeedMobilityAffinity(double value) { this.seedMobilityAffinity = value; }
    public double getSeedChaosAffinity() { return seedChaosAffinity; }
    public void setSeedChaosAffinity(double value) { this.seedChaosAffinity = value; }
    public double getSeedConsistencyAffinity() { return seedConsistencyAffinity; }
    public void setSeedConsistencyAffinity(double value) { this.seedConsistencyAffinity = value; }

    public Map<String, Double> getDriftBiasAdjustments() { return driftBiasAdjustments; }
    public Map<String, Double> getAwakeningBiasAdjustments() { return awakeningBiasAdjustments; }
    public Map<String, Double> getAwakeningGainMultipliers() { return awakeningGainMultipliers; }
    public List<String> getDriftHistory() { return driftHistory; }
    public List<String> getLoreHistory() { return loreHistory; }
    public List<String> getNotableEvents() { return notableEvents; }
    public Set<String> getAwakeningTraits() { return awakeningTraits; }
    public ArtifactMemory getMemory() { return memory; }
    public String getLastAbilityBranchPath() { return lastAbilityBranchPath; }
    public void setLastAbilityBranchPath(String lastAbilityBranchPath) { this.lastAbilityBranchPath = lastAbilityBranchPath; }
    public String getLastMutationHistory() { return lastMutationHistory; }
    public void setLastMutationHistory(String lastMutationHistory) { this.lastMutationHistory = lastMutationHistory; }
    public String getLastMemoryInfluence() { return lastMemoryInfluence; }
    public void setLastMemoryInfluence(String lastMemoryInfluence) { this.lastMemoryInfluence = lastMemoryInfluence; }
    public String getLastRegulatoryProfile() { return lastRegulatoryProfile; }
    public void setLastRegulatoryProfile(String lastRegulatoryProfile) { this.lastRegulatoryProfile = lastRegulatoryProfile; }
    public String getLastOpenRegulatoryGates() { return lastOpenRegulatoryGates; }
    public void setLastOpenRegulatoryGates(String lastOpenRegulatoryGates) { this.lastOpenRegulatoryGates = lastOpenRegulatoryGates; }
    public String getLastGateCandidatePool() { return lastGateCandidatePool; }
    public void setLastGateCandidatePool(String lastGateCandidatePool) { this.lastGateCandidatePool = lastGateCandidatePool; }

    public double getSeedAffinity(String statKey) {
        return switch (statKey) {
            case "precision" -> seedPrecisionAffinity;
            case "brutality" -> seedBrutalityAffinity;
            case "survival" -> seedSurvivalAffinity;
            case "mobility" -> seedMobilityAffinity;
            case "chaos" -> seedChaosAffinity;
            case "consistency" -> seedConsistencyAffinity;
            default -> 0.0D;
        };
    }

    public double getDriftBias(String statKey) { return driftBiasAdjustments.getOrDefault(statKey, 0.0D); }
    public double getAwakeningBias(String statKey) { return awakeningBiasAdjustments.getOrDefault(statKey, 0.0D); }
    public double getAwakeningGainMultiplier(String statKey) { return awakeningGainMultipliers.getOrDefault(statKey, 1.0D); }
    public void addDriftHistory(String entry) { addHistoryEntry(driftHistory, entry); }
    public void addLoreHistory(String entry) { addHistoryEntry(loreHistory, entry); }
    public void addNotableEvent(String entry) { addHistoryEntry(notableEvents, entry); }
    public void incrementDriftLevel() { driftLevel++; }
    public void incrementTotalDrifts() { totalDrifts++; }
    public boolean hasInstability() { return !"none".equalsIgnoreCase(currentInstabilityState); }
    public void clearInstability() { currentInstabilityState = "none"; instabilityExpiryTimestamp = 0L; }
    public void setInstabilityState(String state, long expiryTimestamp) { currentInstabilityState = state; instabilityExpiryTimestamp = expiryTimestamp; }
    public boolean isInstabilityExpired(long now) { return hasInstability() && instabilityExpiryTimestamp > 0L && now >= instabilityExpiryTimestamp; }

    private void addHistoryEntry(List<String> history, String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }

        if (!history.isEmpty() && entry.equals(history.get(history.size() - 1))) {
            return;
        }

        history.add(entry);
        if (history.size() > MAX_HISTORY_ENTRIES) {
            history.remove(0);
        }
    }

    public static String buildDefaultStorageKey(UUID ownerId) {
        return "player:" + ownerId;
    }
}
