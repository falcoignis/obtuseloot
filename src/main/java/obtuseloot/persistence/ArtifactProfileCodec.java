package obtuseloot.persistence;

import obtuseloot.artifacts.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.names.*;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** Shared artifact serialization used by both SQL and YAML per-item profiles. */
public final class ArtifactProfileCodec {
    private static final ArtifactSeedFactory seedFactory = new ArtifactSeedFactory();
    private ArtifactProfileCodec() { }
    public static String encode(Artifact artifact) {
        if (artifact.getArtifactSeed() == 0) throw new IllegalArgumentException("Zero artifact seed");
        ArtifactStorageKeys.itemId(artifact.getArtifactStorageKey());
        artifact.refreshNamingProjection();
        var yaml = new YamlConfiguration();
        writeArtifact(yaml, artifact);
        writeReputation(yaml, artifact.getItemReputation());
        yaml.set("profile-version", 4);
        yaml.set("ability-recovery.count", artifact.abilityRecoverySnapshot().size());
        int recoveryIndex = 0;
        for (var entry : artifact.abilityRecoverySnapshot().entrySet()) {
            String path = "ability-recovery.entries." + recoveryIndex++ + ".";
            yaml.set(path + "id", entry.getKey()); yaml.set(path + "ready-at", entry.getValue());
        }
        yaml.set("genome.revision", artifact.getGenomeRevision());
        yaml.set("genome.evolution-state", artifact.getGenomeEvolutionState());
        yaml.set("inheritance.parent", artifact.getParentStorageKey());
        if ((artifact.getParentStorageKey() == null) != (artifact.getParentGenomeRevision() == -1)
                || (artifact.getParentStorageKey() == null) != (artifact.getParentGenomeSeed() == 0))
            throw new IllegalArgumentException("Incomplete parent genome attribution");
        yaml.set("inheritance.parent-revision", artifact.getParentGenomeRevision());
        yaml.set("inheritance.parent-seed", artifact.getParentGenomeSeed());
        artifact.getGenomeFitnessHistory().writeTo(yaml, "genome.fitness");
        yaml.set("inheritance.generation", artifact.getInheritanceGeneration());
        for (GenomeTrait trait : GenomeTrait.values()) {
            yaml.set("genome.expressed." + trait.name(), artifact.getGenome().trait(trait));
            yaml.set("genome.latent." + trait.name(), artifact.getGenome().latentTrait(trait));
        }
        yaml.set("genome.activated", artifact.getGenome().activatedLatentTraits().stream().map(Enum::name).toList());
        return yaml.saveToString();
    }
    public static Artifact decode(String key, String encoded) {
        UUID id = ArtifactStorageKeys.itemId(key);
        try {
            var yaml = new YamlConfiguration();
            yaml.loadFromString(encoded);
            if (yaml.getInt("profile-version", 0) != 4 || !key.equals(yaml.getString("artifact.storage-key"))) {
                throw new IllegalArgumentException("Invalid per-item profile identity/version");
            }
            if (!yaml.isString("artifact.owner-id") || !yaml.isString("artifact.latent-lineage")
                    || !yaml.isInt("inheritance.generation") || !yaml.isConfigurationSection("reputation")) {
                throw new IllegalArgumentException("Incomplete per-item profile");
            }
            for (String stat : List.of("precision", "brutality", "survival", "mobility", "chaos", "consistency",
                    "kills", "boss-kills", "recent-kill-chain", "survival-streak")) {
                if (!yaml.isInt("reputation." + stat)) throw new IllegalArgumentException("Invalid reputation stat " + stat);
            }
            for (String stat : List.of("last-kill-timestamp", "last-combat-timestamp")) {
                if (!yaml.isInt("reputation." + stat) && !yaml.isLong("reputation." + stat)) throw new IllegalArgumentException("Invalid reputation time " + stat);
            }
            if (!yaml.isString("reputation.utility-experience")) throw new IllegalArgumentException("Missing utility credit");
            Artifact artifact = readArtifact(id, yaml);
            if (artifact == null || artifact.getArtifactSeed() == 0) throw new IllegalArgumentException("Missing artifact identity");
            var expressed = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
            var latent = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
            for (GenomeTrait trait : GenomeTrait.values()) {
                expressed.put(trait, requireTrait(yaml, "genome.expressed." + trait.name()));
                latent.put(trait, requireTrait(yaml, "genome.latent." + trait.name()));
            }
            var activated = EnumSet.noneOf(GenomeTrait.class);
            yaml.getStringList("genome.activated").forEach(name -> activated.add(GenomeTrait.valueOf(name)));
            artifact.setGenome(new ArtifactGenome(artifact.getArtifactSeed(), expressed, latent, activated));
            if ((!yaml.isLong("genome.revision") && !yaml.isInt("genome.revision"))
                    || !yaml.isString("genome.evolution-state")) throw new IllegalArgumentException("Missing genome evolution receipt");
            artifact.restoreGenomeEvolution(yaml.getLong("genome.revision"), yaml.getString("genome.evolution-state"));
            artifact.setParentStorageKey(yaml.getString("inheritance.parent"));
            if (artifact.getParentStorageKey() != null) ArtifactStorageKeys.itemId(artifact.getParentStorageKey());
            if (!yaml.isInt("inheritance.parent-revision") && !yaml.isLong("inheritance.parent-revision"))
                throw new IllegalArgumentException("Missing parent genome revision");
            artifact.setParentGenomeRevision(yaml.getLong("inheritance.parent-revision"));
            if (!yaml.isInt("inheritance.parent-seed") && !yaml.isLong("inheritance.parent-seed"))
                throw new IllegalArgumentException("Missing parent genome seed");
            artifact.setParentGenomeSeed(yaml.getLong("inheritance.parent-seed"));
            if ((artifact.getParentStorageKey() == null) != (artifact.getParentGenomeRevision() == -1)
                    || (artifact.getParentStorageKey() == null) != (artifact.getParentGenomeSeed() == 0))
                throw new IllegalArgumentException("Invalid parent genome attribution");
            artifact.setGenomeFitnessHistory(obtuseloot.evolution.GenomeFitnessHistory.readFrom(yaml, "genome.fitness"));
            artifact.setInheritanceGeneration(yaml.getInt("inheritance.generation"));
            var recovery = yaml.getConfigurationSection("ability-recovery.entries");
            int recoveryCount = recovery == null ? 0 : recovery.getKeys(false).size();
            if (!yaml.isInt("ability-recovery.count") || yaml.getInt("ability-recovery.count") != recoveryCount)
                throw new IllegalArgumentException("Incomplete ability recovery state");
            var ids = new HashSet<String>();
            for (String name : recovery == null ? Set.<String>of() : recovery.getKeys(false)) {
                String path = "ability-recovery.entries." + name + ".";
                String ability = yaml.getString(path + "id");
                if (ability == null || ability.isBlank() || !ids.add(ability)
                        || !yaml.isLong(path + "ready-at") && !yaml.isInt(path + "ready-at"))
                    throw new IllegalArgumentException("Invalid ability recovery state");
                long ready = yaml.getLong(path + "ready-at");
                if (ready <= 0) throw new IllegalArgumentException("Invalid stored recovery deadline");
                artifact.setAbilityReadyAt(ability, ready);
            }
            artifact.setItemReputation(readReputation(yaml));
            return artifact;
        } catch (Exception exception) {
            throw new PlayerStateLoadException(id, "Unreadable per-item artifact profile " + key, exception);
        }
    }
    private static double requireTrait(YamlConfiguration yaml, String path) {
        Object value = yaml.get(path);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())
                || number.doubleValue() < 0 || number.doubleValue() > 1) {
            throw new IllegalArgumentException("Invalid genome trait: " + path);
        }
        return number.doubleValue();
    }
    static Artifact readArtifact(UUID playerId, YamlConfiguration yaml) {
        EquipmentArchetype archetype = requirePersistedItemCategory(yaml, playerId);
        Artifact artifact = new Artifact(playerId, archetype);
        artifact.setOwnerId(UUID.fromString(yaml.getString("artifact.owner-id", playerId.toString())));
        artifact.setArtifactStorageKey(yaml.getString("artifact.storage-key", Artifact.buildDefaultStorageKey(artifact.getOwnerId())));
        artifact.setPersistenceOriginTimestamp(yaml.getLong("artifact.persistence-origin-timestamp", artifact.getPersistenceOriginTimestamp()));
        artifact.setIdentityBirthTimestamp(yaml.getLong("artifact.identity-birth-timestamp", artifact.getIdentityBirthTimestamp()));
        artifact.setArchetypePath(yaml.getString("artifact.archetype-path", "unformed"));
        artifact.setEvolutionPath(yaml.getString("artifact.evolution-path", "base"));
        artifact.setAwakeningPath(yaml.getString("artifact.awakening-path", "dormant"));
        artifact.setConvergencePath(yaml.getString("artifact.convergence-path", "none"));
        artifact.setDriftLevel(yaml.getInt("artifact.drift-level", 0));
        artifact.setTotalDrifts(yaml.getInt("artifact.total-drifts", 0));
        artifact.setLastDriftTimestamp(yaml.getLong("artifact.last-drift-timestamp", 0L));
        artifact.setInstabilityState(yaml.getString("artifact.current-instability-state", "none"), yaml.getLong("artifact.instability-expiry", 0L));

        long artifactSeed = yaml.getLong("artifact.artifact-seed", 0L);
        artifact.setArtifactSeed(artifactSeed);
        seedFactory.applySeedProfile(artifact, artifactSeed);
        java.util.Map<String, Double> persistedSeedAffinities = new java.util.LinkedHashMap<>();
        readStatMap(yaml, "artifact.seed-affinities", persistedSeedAffinities);
        artifact.setSeedPrecisionAffinity(persistedSeedAffinities.getOrDefault("precision", artifact.getSeedPrecisionAffinity()));
        artifact.setSeedBrutalityAffinity(persistedSeedAffinities.getOrDefault("brutality", artifact.getSeedBrutalityAffinity()));
        artifact.setSeedSurvivalAffinity(persistedSeedAffinities.getOrDefault("survival", artifact.getSeedSurvivalAffinity()));
        artifact.setSeedMobilityAffinity(persistedSeedAffinities.getOrDefault("mobility", artifact.getSeedMobilityAffinity()));
        artifact.setSeedChaosAffinity(persistedSeedAffinities.getOrDefault("chaos", artifact.getSeedChaosAffinity()));
        artifact.setSeedConsistencyAffinity(persistedSeedAffinities.getOrDefault("consistency", artifact.getSeedConsistencyAffinity()));
        ArtifactNaming naming = ArtifactNameResolver.initialize(artifact);
        naming.setNamingSeed(yaml.getLong("artifact.naming.naming-seed", naming.getNamingSeed()));
        naming.setTrueName(yaml.getString("artifact.naming.true-name", null));
        naming.setEpithetSeed(yaml.getInt("artifact.naming.epithet-seed", naming.getEpithetSeed()));
        naming.setTitleSeed(yaml.getInt("artifact.naming.title-seed", naming.getTitleSeed()));
        ArtifactNameResolver.refresh(artifact, naming);
        artifact.setNaming(naming);
        artifact.setLatentLineage(yaml.getString("artifact.latent-lineage", artifact.getLatentLineage()));
        artifact.setSpeciesId(yaml.getString("artifact.species-id", artifact.getSpeciesId()));
        artifact.setParentSpeciesId(yaml.getString("artifact.parent-species-id", artifact.getParentSpeciesId()));
        artifact.setLastSpeciesCompatibilityDistance(yaml.getDouble("artifact.species-compatibility-distance", 0.0D));
        artifact.setDriftAlignment(yaml.getString("artifact.drift-alignment", artifact.getDriftAlignment()));

        readStatMap(yaml, "artifact.drift-bias", artifact.getDriftBiasAdjustments());
        readStatMap(yaml, "artifact.awakening-bias", artifact.getAwakeningBiasAdjustments());
        readStatMap(yaml, "artifact.awakening-gain-multipliers", artifact.getAwakeningGainMultipliers());

        artifact.getDriftHistory().addAll(yaml.getStringList("artifact.drift-history"));
        artifact.getLoreHistory().addAll(yaml.getStringList("artifact.lore-history"));
        artifact.getNotableEvents().addAll(yaml.getStringList("artifact.notable-events"));
        artifact.getAwakeningTraits().addAll(yaml.getStringList("artifact.awakening-traits"));
        artifact.setLastAbilityBranchPath(yaml.getString("artifact.last-ability-branch-path", "[]"));
        artifact.setLastMutationHistory(yaml.getString("artifact.last-mutation-history", "[]"));
        artifact.setLastRegulatoryProfile(yaml.getString("artifact.last-regulatory-profile", "[]"));
        artifact.setLastOpenRegulatoryGates(yaml.getString("artifact.last-open-regulatory-gates", ""));
        artifact.setLastGateCandidatePool(yaml.getString("artifact.last-gate-candidate-pool", "0->0"));
        artifact.setLastTriggerProfile(yaml.getString("artifact.last-trigger-profile", ""));
        artifact.setLastMechanicProfile(yaml.getString("artifact.last-mechanic-profile", ""));
        artifact.setLastUtilityHistory(yaml.getString("artifact.last-utility-history", ""));
        artifact.setLastMemoryInfluence(yaml.getString("artifact.last-memory-influence", "none"));
        artifact.setConvergenceVariantId(yaml.getString("artifact.convergence-variant-id", "none"));
        artifact.setConvergenceIdentityShape(yaml.getString("artifact.convergence-identity-shape", "none"));
        artifact.setConvergenceLineageTrace(yaml.getString("artifact.convergence-lineage-trace", "none"));
        artifact.setConvergenceLoreTrace(yaml.getString("artifact.convergence-lore-trace", "none"));
        artifact.setConvergenceContinuityTrace(yaml.getString("artifact.convergence-continuity-trace", "none"));
        artifact.setConvergenceExpressionTrace(yaml.getString("artifact.convergence-expression-trace", "none"));
        artifact.setConvergenceMemorySignature(yaml.getString("artifact.convergence-memory-signature", "none"));
        artifact.setAwakeningVariantId(yaml.getString("artifact.awakening-variant-id", "none"));
        artifact.setAwakeningIdentityShape(yaml.getString("artifact.awakening-identity-shape", "none"));
        artifact.setAwakeningLineageTrace(yaml.getString("artifact.awakening-lineage-trace", "none"));
        artifact.setAwakeningLoreTrace(yaml.getString("artifact.awakening-lore-trace", "none"));
        artifact.setAwakeningContinuityTrace(yaml.getString("artifact.awakening-continuity-trace", "none"));
        artifact.setAwakeningExpressionTrace(yaml.getString("artifact.awakening-expression-trace", "none"));
        artifact.setAwakeningMemorySignature(yaml.getString("artifact.awakening-memory-signature", "none"));
        for (String key : yaml.getConfigurationSection("artifact.memory-events") != null ? yaml.getConfigurationSection("artifact.memory-events").getKeys(false) : java.util.Set.<String>of()) {
            try {
                obtuseloot.memory.ArtifactMemoryEvent event = obtuseloot.memory.ArtifactMemoryEvent.valueOf(key.toUpperCase());
                int count = yaml.getInt("artifact.memory-events." + key, 0);
                for (int i = 0; i < count; i++) {
                    artifact.getMemory().record(event);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        artifact.refreshNamingProjection();
        return artifact;
    }
    static ArtifactReputation readReputation(YamlConfiguration yaml) {
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
        rep.restoreUtilityExperience(yaml.getString("reputation.utility-experience", ""));
        return rep;
    }
    private static EquipmentArchetype requirePersistedItemCategory(YamlConfiguration yaml, UUID playerId) {
        String rawCategory = yaml.getString("artifact.item-category");
        if (rawCategory == null || rawCategory.isBlank()) {
            throw new IllegalStateException("[Persistence] Missing artifact.item-category for " + playerId);
        }
        return ArtifactArchetypeValidator.requireValidArchetype(rawCategory, "[Persistence] artifact.item-category for " + playerId);
    }

    static void writeArtifact(YamlConfiguration yaml, Artifact artifact) {
        String base = "artifact.";
        yaml.set(base + "owner-id", artifact.getOwnerId().toString());
        yaml.set(base + "storage-key", artifact.getArtifactStorageKey());
        yaml.set(base + "persistence-origin-timestamp", artifact.getPersistenceOriginTimestamp());
        yaml.set(base + "identity-birth-timestamp", artifact.getIdentityBirthTimestamp());
        yaml.set(base + "artifact-seed", artifact.getArtifactSeed());
        yaml.set(base + "naming.display-name", artifact.getDisplayName());
        yaml.set(base + "naming.true-name", artifact.getTrueName());
        yaml.set(base + "naming.naming-seed", artifact.getNaming().getNamingSeed());
        yaml.set(base + "naming.root-form", artifact.getNaming().getRootForm());
        yaml.set(base + "naming.archetype", artifact.getNaming().getNamingArchetype().name());
        yaml.set(base + "naming.tone", artifact.getNaming().getToneProfile().name());
        yaml.set(base + "naming.discovery", artifact.getNaming().getDiscoveryState().name());
        yaml.set(base + "naming.identity-tags", artifact.getNaming().getIdentityTags());
        yaml.set(base + "naming.affinity-lexemes", artifact.getNaming().getAffinityLexemes());
        yaml.set(base + "naming.epithet-seed", artifact.getNaming().getEpithetSeed());
        yaml.set(base + "naming.title-seed", artifact.getNaming().getTitleSeed());
        yaml.set(base + "item-category", artifact.getItemCategory());
        yaml.set(base + "archetype-path", artifact.getArchetypePath());
        yaml.set(base + "evolution-path", artifact.getEvolutionPath());
        yaml.set(base + "awakening-path", artifact.getAwakeningPath());
        yaml.set(base + "convergence-path", artifact.getConvergencePath());
        yaml.set(base + "drift-level", artifact.getDriftLevel());
        yaml.set(base + "total-drifts", artifact.getTotalDrifts());
        yaml.set(base + "drift-alignment", artifact.getDriftAlignment());
        yaml.set(base + "last-drift-timestamp", artifact.getLastDriftTimestamp());
        yaml.set(base + "latent-lineage", artifact.getLatentLineage());
        yaml.set(base + "species-id", artifact.getSpeciesId());
        yaml.set(base + "parent-species-id", artifact.getParentSpeciesId());
        yaml.set(base + "species-compatibility-distance", artifact.getLastSpeciesCompatibilityDistance());
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
        yaml.set(base + "last-ability-branch-path", artifact.getLastAbilityBranchPath());
        yaml.set(base + "last-mutation-history", artifact.getLastMutationHistory());
        yaml.set(base + "last-regulatory-profile", artifact.getLastRegulatoryProfile());
        yaml.set(base + "last-open-regulatory-gates", artifact.getLastOpenRegulatoryGates());
        yaml.set(base + "last-gate-candidate-pool", artifact.getLastGateCandidatePool());
        yaml.set(base + "last-trigger-profile", artifact.getLastTriggerProfile());
        yaml.set(base + "last-mechanic-profile", artifact.getLastMechanicProfile());
        yaml.set(base + "last-utility-history", artifact.getLastUtilityHistory());
        yaml.set(base + "last-memory-influence", artifact.getLastMemoryInfluence());
        yaml.set(base + "convergence-variant-id", artifact.getConvergenceVariantId());
        yaml.set(base + "convergence-identity-shape", artifact.getConvergenceIdentityShape());
        yaml.set(base + "convergence-lineage-trace", artifact.getConvergenceLineageTrace());
        yaml.set(base + "convergence-lore-trace", artifact.getConvergenceLoreTrace());
        yaml.set(base + "convergence-continuity-trace", artifact.getConvergenceContinuityTrace());
        yaml.set(base + "convergence-expression-trace", artifact.getConvergenceExpressionTrace());
        yaml.set(base + "convergence-memory-signature", artifact.getConvergenceMemorySignature());
        yaml.set(base + "awakening-variant-id", artifact.getAwakeningVariantId());
        yaml.set(base + "awakening-identity-shape", artifact.getAwakeningIdentityShape());
        yaml.set(base + "awakening-lineage-trace", artifact.getAwakeningLineageTrace());
        yaml.set(base + "awakening-lore-trace", artifact.getAwakeningLoreTrace());
        yaml.set(base + "awakening-continuity-trace", artifact.getAwakeningContinuityTrace());
        yaml.set(base + "awakening-expression-trace", artifact.getAwakeningExpressionTrace());
        yaml.set(base + "awakening-memory-signature", artifact.getAwakeningMemorySignature());
        artifact.getMemory().snapshot().forEach((event, count) -> yaml.set(base + "memory-events." + event.name().toLowerCase(), count));
    }

    static void writeReputation(YamlConfiguration yaml, ArtifactReputation reputation) {
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
        yaml.set(base + "utility-experience", reputation.encodeUtilityExperience());
    }

    private static void writeStatMap(YamlConfiguration yaml, String path, Map<String, Double> values) {
        for (String stat : List.of("precision", "brutality", "survival", "mobility", "chaos", "consistency")) {
            yaml.set(path + "." + stat, values.getOrDefault(stat, 0.0D));
        }
    }

    private static void readStatMap(YamlConfiguration yaml, String path, Map<String, Double> target) {
        for (String stat : List.of("precision", "brutality", "survival", "mobility", "chaos", "consistency")) {
            target.put(stat, yaml.getDouble(path + "." + stat, 0.0D));
        }
    }


}
