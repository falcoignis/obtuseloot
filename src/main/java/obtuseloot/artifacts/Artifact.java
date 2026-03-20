package obtuseloot.artifacts;

import obtuseloot.memory.ArtifactMemory;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.names.ArtifactNaming;

import java.util.*;

public class Artifact {
    private static final int MAX_HISTORY_ENTRIES = 120;

    private long artifactSeed;
    private String artifactStorageKey;
    private UUID ownerId;
    private long persistenceOriginTimestamp;
    private long identityBirthTimestamp;
    private ArtifactNaming naming;
    private String generatedName;
    private String itemCategory;
    private String archetypePath;
    private String evolutionPath;
    private String awakeningPath;
    private String convergencePath;

    private int driftLevel;
    private int totalDrifts;
    private String driftAlignment;
    private long lastDriftTimestamp;

    private String latentLineage;
    private String speciesId;
    private String parentSpeciesId;
    private double lastSpeciesCompatibilityDistance;
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
    private String lastTriggerProfile;
    private String lastMechanicProfile;
    private String lastInterferenceEffects;
    private double lastLatentActivationRate;
    private String lastActivatedLatentTraits;
    private String lastUtilityHistory;

    private String convergenceVariantId;
    private String convergenceIdentityShape;
    private String convergenceLineageTrace;
    private String convergenceLoreTrace;
    private String convergenceContinuityTrace;
    private String convergenceExpressionTrace;
    private String convergenceMemorySignature;

    private String awakeningVariantId;
    private String awakeningIdentityShape;
    private String awakeningLineageTrace;
    private String awakeningLoreTrace;
    private String awakeningContinuityTrace;
    private String awakeningExpressionTrace;
    private String awakeningMemorySignature;

    public Artifact(UUID ownerId, String itemCategory) {
        this(ownerId, ArtifactArchetypeValidator.requireValidArchetype(itemCategory, "artifact construction"));
    }

    public Artifact(UUID ownerId, EquipmentArchetype archetype) {
        long now = System.currentTimeMillis();
        this.ownerId = ownerId;
        this.persistenceOriginTimestamp = now;
        this.identityBirthTimestamp = now;
        this.artifactStorageKey = buildDefaultStorageKey(ownerId);
        this.itemCategory = Objects.requireNonNull(archetype, "archetype").id();
        this.archetypePath = "unformed";
        this.evolutionPath = "base";
        this.awakeningPath = "dormant";
        this.convergencePath = "none";
        this.driftAlignment = "stable";
        this.latentLineage = "common";
        this.speciesId = "unspeciated";
        this.parentSpeciesId = "none";
        this.lastSpeciesCompatibilityDistance = 0.0D;
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
        this.lastTriggerProfile = "";
        this.lastMechanicProfile = "";
        this.lastInterferenceEffects = "none";
        this.lastLatentActivationRate = 0.0D;
        this.lastActivatedLatentTraits = "[]";
        this.lastUtilityHistory = "";
        this.convergenceVariantId = "none";
        this.convergenceIdentityShape = "none";
        this.convergenceLineageTrace = "none";
        this.convergenceLoreTrace = "none";
        this.convergenceContinuityTrace = "none";
        this.convergenceExpressionTrace = "none";
        this.convergenceMemorySignature = "none";
        this.awakeningVariantId = "none";
        this.awakeningIdentityShape = "none";
        this.awakeningLineageTrace = "none";
        this.awakeningLoreTrace = "none";
        this.awakeningContinuityTrace = "none";
        this.awakeningExpressionTrace = "none";
        this.awakeningMemorySignature = "none";
        this.naming = ArtifactNameResolver.initialize(this);
        this.generatedName = this.naming.getDisplayName();
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
        convergencePath = "none";
        currentInstabilityState = "none";
        speciesId = "unspeciated";
        parentSpeciesId = "none";
        lastSpeciesCompatibilityDistance = 0.0D;
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
        lastTriggerProfile = "";
        lastMechanicProfile = "";
        lastInterferenceEffects = "none";
        lastLatentActivationRate = 0.0D;
        lastActivatedLatentTraits = "[]";
        lastUtilityHistory = "";
        convergenceVariantId = "none";
        convergenceIdentityShape = "none";
        convergenceLineageTrace = "none";
        convergenceLoreTrace = "none";
        convergenceContinuityTrace = "none";
        convergenceExpressionTrace = "none";
        convergenceMemorySignature = "none";
        awakeningVariantId = "none";
        awakeningIdentityShape = "none";
        awakeningLineageTrace = "none";
        awakeningLoreTrace = "none";
        awakeningContinuityTrace = "none";
        awakeningExpressionTrace = "none";
        awakeningMemorySignature = "none";
    }

    public long getArtifactSeed() { return artifactSeed; }
    public void setArtifactSeed(long artifactSeed) { this.artifactSeed = artifactSeed; refreshNamingProjection(); }
    public UUID getOwnerId() { return ownerId; }
    public long getPersistenceOriginTimestamp() { return persistenceOriginTimestamp; }
    public void setPersistenceOriginTimestamp(long persistenceOriginTimestamp) {
        this.persistenceOriginTimestamp = persistenceOriginTimestamp <= 0L ? System.currentTimeMillis() : persistenceOriginTimestamp;
    }
    public long getIdentityBirthTimestamp() { return identityBirthTimestamp; }
    public void setIdentityBirthTimestamp(long identityBirthTimestamp) {
        this.identityBirthTimestamp = identityBirthTimestamp <= 0L ? System.currentTimeMillis() : identityBirthTimestamp;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        if (artifactStorageKey == null || artifactStorageKey.isBlank()) {
            artifactStorageKey = buildDefaultStorageKey(ownerId);
        }
    }

    public String getDisplayName() { return naming.getDisplayName(); }

    public void setDisplayName(String displayName) {
        naming.setDisplayName(displayName);
        generatedName = naming.getDisplayName();
    }

    public String getTrueName() { return naming.getTrueName(); }
    public ArtifactNaming getNaming() { return naming; }

    public void setNaming(ArtifactNaming naming) {
        this.naming = Objects.requireNonNull(naming, "naming");
        this.generatedName = this.naming.getDisplayName();
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
    public String getName() { return naming.getDisplayName(); }
    public String getArchetypePath() { return archetypePath; }
    public void setArchetypePath(String archetypePath) { this.archetypePath = archetypePath; }
    public String getEvolutionPath() { return evolutionPath; }
    public void setEvolutionPath(String evolutionPath) { this.evolutionPath = evolutionPath; }
    public String getAwakeningPath() { return awakeningPath; }
    public void setAwakeningPath(String awakeningPath) { this.awakeningPath = awakeningPath; }
    public String getConvergencePath() { return convergencePath; }
    public void setConvergencePath(String convergencePath) { this.convergencePath = convergencePath; }
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
    public String getSpeciesId() { return speciesId; }
    public void setSpeciesId(String speciesId) { this.speciesId = speciesId; }
    public String getParentSpeciesId() { return parentSpeciesId; }
    public void setParentSpeciesId(String parentSpeciesId) { this.parentSpeciesId = parentSpeciesId; }
    public double getLastSpeciesCompatibilityDistance() { return lastSpeciesCompatibilityDistance; }
    public void setLastSpeciesCompatibilityDistance(double lastSpeciesCompatibilityDistance) { this.lastSpeciesCompatibilityDistance = lastSpeciesCompatibilityDistance; }
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
    public String getLastTriggerProfile() { return lastTriggerProfile; }
    public void setLastTriggerProfile(String lastTriggerProfile) { this.lastTriggerProfile = lastTriggerProfile; }
    public String getLastMechanicProfile() { return lastMechanicProfile; }
    public void setLastMechanicProfile(String lastMechanicProfile) { this.lastMechanicProfile = lastMechanicProfile; }
    public String getLastInterferenceEffects() { return lastInterferenceEffects; }
    public void setLastInterferenceEffects(String lastInterferenceEffects) { this.lastInterferenceEffects = lastInterferenceEffects; }
    public double getLastLatentActivationRate() { return lastLatentActivationRate; }
    public void setLastLatentActivationRate(double lastLatentActivationRate) { this.lastLatentActivationRate = lastLatentActivationRate; }
    public String getLastActivatedLatentTraits() { return lastActivatedLatentTraits; }
    public void setLastActivatedLatentTraits(String lastActivatedLatentTraits) { this.lastActivatedLatentTraits = lastActivatedLatentTraits; }
    public String getLastUtilityHistory() { return lastUtilityHistory; }
    public void setLastUtilityHistory(String lastUtilityHistory) { this.lastUtilityHistory = lastUtilityHistory; }
    public String getConvergenceVariantId() { return convergenceVariantId; }
    public void setConvergenceVariantId(String convergenceVariantId) { this.convergenceVariantId = convergenceVariantId; }
    public String getConvergenceIdentityShape() { return convergenceIdentityShape; }
    public void setConvergenceIdentityShape(String convergenceIdentityShape) { this.convergenceIdentityShape = convergenceIdentityShape; }
    public String getConvergenceLineageTrace() { return convergenceLineageTrace; }
    public void setConvergenceLineageTrace(String convergenceLineageTrace) { this.convergenceLineageTrace = convergenceLineageTrace; }
    public String getConvergenceLoreTrace() { return convergenceLoreTrace; }
    public void setConvergenceLoreTrace(String convergenceLoreTrace) { this.convergenceLoreTrace = convergenceLoreTrace; }
    public String getConvergenceContinuityTrace() { return convergenceContinuityTrace; }
    public void setConvergenceContinuityTrace(String convergenceContinuityTrace) { this.convergenceContinuityTrace = convergenceContinuityTrace; }
    public String getConvergenceExpressionTrace() { return convergenceExpressionTrace; }
    public void setConvergenceExpressionTrace(String convergenceExpressionTrace) { this.convergenceExpressionTrace = convergenceExpressionTrace; }
    public String getConvergenceMemorySignature() { return convergenceMemorySignature; }
    public void setConvergenceMemorySignature(String convergenceMemorySignature) { this.convergenceMemorySignature = convergenceMemorySignature; }
    public String getAwakeningVariantId() { return awakeningVariantId; }
    public void setAwakeningVariantId(String awakeningVariantId) { this.awakeningVariantId = awakeningVariantId; }
    public String getAwakeningIdentityShape() { return awakeningIdentityShape; }
    public void setAwakeningIdentityShape(String awakeningIdentityShape) { this.awakeningIdentityShape = awakeningIdentityShape; }
    public String getAwakeningLineageTrace() { return awakeningLineageTrace; }
    public void setAwakeningLineageTrace(String awakeningLineageTrace) { this.awakeningLineageTrace = awakeningLineageTrace; }
    public String getAwakeningLoreTrace() { return awakeningLoreTrace; }
    public void setAwakeningLoreTrace(String awakeningLoreTrace) { this.awakeningLoreTrace = awakeningLoreTrace; }
    public String getAwakeningContinuityTrace() { return awakeningContinuityTrace; }
    public void setAwakeningContinuityTrace(String awakeningContinuityTrace) { this.awakeningContinuityTrace = awakeningContinuityTrace; }
    public String getAwakeningExpressionTrace() { return awakeningExpressionTrace; }
    public void setAwakeningExpressionTrace(String awakeningExpressionTrace) { this.awakeningExpressionTrace = awakeningExpressionTrace; }
    public String getAwakeningMemorySignature() { return awakeningMemorySignature; }
    public void setAwakeningMemorySignature(String awakeningMemorySignature) { this.awakeningMemorySignature = awakeningMemorySignature; }

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
    public int getHistoryScore() { return loreHistory.size() + notableEvents.size() + memory.snapshot().values().stream().mapToInt(Integer::intValue).sum(); }
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

    public void refreshNamingProjection() {
        if (naming == null || itemCategory == null || itemCategory.isBlank()) {
            return;
        }
        ArtifactNameResolver.refresh(this, naming);
        generatedName = naming.getDisplayName();
    }

    public static String buildDefaultStorageKey(UUID ownerId) {
        return "player:" + ownerId;
    }
}
