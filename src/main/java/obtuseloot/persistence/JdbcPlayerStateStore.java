package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactSeedFactory;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.species.ArtifactSpecies;
import obtuseloot.species.SpeciesRegistrySnapshot;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class JdbcPlayerStateStore implements PlayerStateStore {
    public enum Dialect { SQLITE, MYSQL }
    private final DatabaseManager database;
    private final ArtifactSeedFactory seedFactory = new ArtifactSeedFactory();
    private final Plugin plugin;
    private final Dialect dialect;

    public JdbcPlayerStateStore(DatabaseManager database, Plugin plugin, Dialect dialect) {
        this.database = database;
        this.plugin = plugin;
        this.dialect = dialect;
    }

    @Override
    public void saveArtifact(UUID playerId, Artifact artifact) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            upsertPlayer(connection, playerId);
            try (PreparedStatement ps = connection.prepareStatement(artifactUpsertSql())) {
                bindArtifact(ps, playerId, artifact);
                ps.executeUpdate();
            }
            try (PreparedStatement memDelete = connection.prepareStatement("DELETE FROM artifact_memories WHERE player_uuid=?")) {
                memDelete.setString(1, playerId.toString());
                memDelete.executeUpdate();
            }
            try (PreparedStatement memInsert = connection.prepareStatement("INSERT INTO artifact_memories(player_uuid,event_key,event_count) VALUES (?,?,?)")) {
                for (Map.Entry<ArtifactMemoryEvent, Integer> entry : artifact.getMemory().snapshot().entrySet()) {
                    memInsert.setString(1, playerId.toString());
                    memInsert.setString(2, entry.getKey().name());
                    memInsert.setInt(3, entry.getValue());
                    memInsert.addBatch();
                }
                memInsert.executeBatch();
            }
            try (PreparedStatement mut = connection.prepareStatement(mutationUpsertSql())) {
                mut.setString(1, playerId.toString());
                mut.setString(2, artifact.getLastMutationHistory());
                mut.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            plugin.getLogger().warning("[Persistence] SQL saveArtifact failed for " + playerId + ": " + ex.getMessage());
        }
    }

    @Override
    public Artifact loadArtifact(UUID playerId) { /* unchanged logic */
        try (Connection connection = database.openConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM artifacts WHERE player_uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Artifact artifact = new Artifact(playerId);
                artifact.setOwnerId(UUID.fromString(rs.getString("owner_uuid")));
                artifact.setArtifactSeed(rs.getLong("artifact_seed"));
                seedFactory.applySeedProfile(artifact, artifact.getArtifactSeed());
                artifact.setNaming(ArtifactNameResolver.initialize(artifact));
                artifact.setDisplayName(nullToDefault(rs.getString("generated_name"), artifact.getDisplayName()));
                artifact.setItemCategory(nullToDefault(rs.getString("item_category"), "artifact"));
                artifact.setArchetypePath(nullToDefault(rs.getString("archetype"), "unformed"));
                artifact.setEvolutionPath(nullToDefault(rs.getString("evolution_path"), "base"));
                artifact.setDriftAlignment(nullToDefault(rs.getString("drift_alignment"), "stable"));
                artifact.setAwakeningPath(nullToDefault(rs.getString("awakening_path"), "dormant"));
                artifact.setConvergencePath(nullToDefault(rs.getString("convergence_path"), "none"));
                artifact.setLastAbilityBranchPath(nullToDefault(rs.getString("ability_branch_path"), "[]"));
                artifact.setLastMutationHistory(nullToDefault(rs.getString("mutation_history"), "[]"));
                artifact.setLastMemoryInfluence(nullToDefault(rs.getString("memory_influence"), "none"));
                artifact.setLastRegulatoryProfile(nullToDefault(rs.getString("regulatory_profile"), "[]"));
                artifact.setLastOpenRegulatoryGates(nullToDefault(rs.getString("open_regulatory_gates"), ""));
                artifact.setLastGateCandidatePool(nullToDefault(rs.getString("gate_candidate_pool"), "0->0"));
                artifact.setLastTriggerProfile(nullToDefault(rs.getString("trigger_profile"), ""));
                artifact.setLastMechanicProfile(nullToDefault(rs.getString("mechanic_profile"), ""));
                artifact.setLastUtilityHistory(nullToDefault(rs.getString("utility_history"), ""));
                artifact.setDriftLevel(rs.getInt("drift_level"));
                artifact.setTotalDrifts(rs.getInt("total_drifts"));
                artifact.setLastDriftTimestamp(rs.getLong("last_drift_timestamp"));
                artifact.setLatentLineage(nullToDefault(rs.getString("latent_lineage"), "unassigned"));
                artifact.setSpeciesId(nullToDefault(rs.getString("species_id"), "unspeciated"));
                artifact.setParentSpeciesId(nullToDefault(rs.getString("parent_species_id"), "none"));
                artifact.setLastSpeciesCompatibilityDistance(rs.getDouble("species_compatibility_distance"));
                artifact.setInstabilityState(nullToDefault(rs.getString("instability_state"), "none"), rs.getLong("instability_expiry"));
                decodeMap(rs.getString("drift_bias_json"), artifact.getDriftBiasAdjustments());
                decodeMap(rs.getString("awakening_bias_json"), artifact.getAwakeningBiasAdjustments());
                decodeMap(rs.getString("awakening_gain_json"), artifact.getAwakeningGainMultipliers());
                artifact.getDriftHistory().addAll(decodeList(rs.getString("drift_history_json")));
                artifact.getLoreHistory().addAll(decodeList(rs.getString("lore_history_json")));
                artifact.getNotableEvents().addAll(decodeList(rs.getString("notable_events_json")));
                artifact.getAwakeningTraits().addAll(decodeList(rs.getString("awakening_traits_json")));
                try (PreparedStatement memPs = connection.prepareStatement("SELECT event_key,event_count FROM artifact_memories WHERE player_uuid=?")) {
                    memPs.setString(1, playerId.toString());
                    try (ResultSet mem = memPs.executeQuery()) {
                        while (mem.next()) {
                            ArtifactMemoryEvent event = ArtifactMemoryEvent.valueOf(mem.getString("event_key"));
                            for (int i = 0; i < mem.getInt("event_count"); i++) artifact.getMemory().record(event);
                        }
                    }
                }
                return artifact;
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[Persistence] SQL loadArtifact failed for " + playerId + ": " + ex.getMessage());
            return null;
        }
    }

    @Override
    public void saveReputation(UUID playerId, ArtifactReputation rep) {
        try (Connection connection = database.openConnection()) {
            upsertPlayer(connection, playerId);
            try (PreparedStatement ps = connection.prepareStatement(reputationUpsertSql())) {
                ps.setString(1, playerId.toString());
                ps.setInt(2, rep.getPrecision()); ps.setInt(3, rep.getBrutality()); ps.setInt(4, rep.getSurvival());
                ps.setInt(5, rep.getMobility()); ps.setInt(6, rep.getChaos()); ps.setInt(7, rep.getConsistency());
                ps.setInt(8, rep.getKills()); ps.setInt(9, rep.getBossKills()); ps.setInt(10, rep.getRecentKillChain());
                ps.setLong(11, rep.getLastKillTimestamp()); ps.setLong(12, rep.getLastCombatTimestamp()); ps.setInt(13, rep.getSurvivalStreak());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("[Persistence] SQL saveReputation failed for " + playerId + ": " + ex.getMessage());
        }
    }

    @Override
    public ArtifactReputation loadReputation(UUID playerId) {
        try (Connection connection = database.openConnection(); PreparedStatement ps = connection.prepareStatement("SELECT * FROM reputation WHERE player_uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ArtifactReputation rep = new ArtifactReputation();
                rep.setPrecision(rs.getInt("precision")); rep.setBrutality(rs.getInt("brutality")); rep.setSurvival(rs.getInt("survival"));
                rep.setMobility(rs.getInt("mobility")); rep.setChaos(rs.getInt("chaos")); rep.setConsistency(rs.getInt("consistency"));
                rep.setKills(rs.getInt("kills")); rep.setBossKills(rs.getInt("boss_kills")); rep.setRecentKillChain(rs.getInt("recent_kill_chain"));
                rep.setLastKillTimestamp(rs.getLong("last_kill_timestamp")); rep.setLastCombatTimestamp(rs.getLong("last_combat_timestamp")); rep.setSurvivalStreak(rs.getInt("survival_streak"));
                return rep;
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("[Persistence] SQL loadReputation failed for " + playerId + ": " + ex.getMessage());
            return null;
        }
    }

    private void upsertPlayer(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(playerUpsertSql())) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private String playerUpsertSql() { return dialect == Dialect.MYSQL
            ? "INSERT INTO players(player_uuid,updated_at) VALUES (?,?) ON DUPLICATE KEY UPDATE updated_at=VALUES(updated_at)"
            : "INSERT INTO players(player_uuid,updated_at) VALUES (?,?) ON CONFLICT(player_uuid) DO UPDATE SET updated_at=excluded.updated_at"; }


    @Override
    public void saveSpeciesSnapshot(SpeciesRegistrySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement clearA = connection.prepareStatement("DELETE FROM species_registry");
                 PreparedStatement clearB = connection.prepareStatement("DELETE FROM lineage_species_roots")) {
                clearA.executeUpdate();
                clearB.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO species_registry(species_id,parent_species_id,origin_lineage_id,created_at,created_generation,divergence_snapshot,tendency_profile) VALUES (?,?,?,?,?,?,?)")) {
                for (ArtifactSpecies species : snapshot.speciesById().values()) {
                    insert.setString(1, species.speciesId());
                    insert.setString(2, species.parentSpeciesId());
                    insert.setString(3, species.originLineageId());
                    insert.setLong(4, species.createdAtEpochMs());
                    insert.setInt(5, species.createdGeneration());
                    insert.setString(6, encodeMap(species.divergenceSnapshot()));
                    insert.setString(7, encodeMap(species.tendencyProfile()));
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            try (PreparedStatement insertRoot = connection.prepareStatement("INSERT INTO lineage_species_roots(lineage_id,root_species_id) VALUES (?,?)")) {
                for (Map.Entry<String, String> e : snapshot.lineageRoots().entrySet()) {
                    insertRoot.setString(1, e.getKey());
                    insertRoot.setString(2, e.getValue());
                    insertRoot.addBatch();
                }
                insertRoot.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            plugin.getLogger().warning("[Persistence] SQL saveSpeciesSnapshot failed: " + ex.getMessage());
        }
    }

    @Override
    public SpeciesRegistrySnapshot loadSpeciesSnapshot() {
        Map<String, ArtifactSpecies> species = new java.util.LinkedHashMap<>();
        Map<String, String> roots = new java.util.LinkedHashMap<>();
        try (Connection connection = database.openConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM species_registry");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Double> divergence = new java.util.LinkedHashMap<>();
                    Map<String, Double> tendency = new java.util.LinkedHashMap<>();
                    decodeMap(rs.getString("divergence_snapshot"), divergence);
                    decodeMap(rs.getString("tendency_profile"), tendency);
                    String id = rs.getString("species_id");
                    species.put(id, new ArtifactSpecies(id,
                            nullToDefault(rs.getString("parent_species_id"), "none"),
                            nullToDefault(rs.getString("origin_lineage_id"), "unknown"),
                            rs.getLong("created_at"),
                            rs.getInt("created_generation"),
                            divergence,
                            tendency));
                }
            }
            try (PreparedStatement ps = connection.prepareStatement("SELECT lineage_id, root_species_id FROM lineage_species_roots");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roots.put(rs.getString("lineage_id"), rs.getString("root_species_id"));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("[Persistence] SQL loadSpeciesSnapshot failed: " + ex.getMessage());
            return null;
        }
        return new SpeciesRegistrySnapshot(species, roots);
    }

    private String mutationUpsertSql() { return dialect == Dialect.MYSQL
            ? "INSERT INTO artifact_mutations(player_uuid,mutation_history) VALUES (?,?) ON DUPLICATE KEY UPDATE mutation_history=VALUES(mutation_history)"
            : "INSERT INTO artifact_mutations(player_uuid,mutation_history) VALUES (?,?) ON CONFLICT(player_uuid) DO UPDATE SET mutation_history=excluded.mutation_history"; }
    private String reputationUpsertSql() { return dialect == Dialect.MYSQL
            ? "INSERT INTO reputation(player_uuid,precision,brutality,survival,mobility,chaos,consistency,kills,boss_kills,recent_kill_chain,last_kill_timestamp,last_combat_timestamp,survival_streak) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE precision=VALUES(precision), brutality=VALUES(brutality), survival=VALUES(survival), mobility=VALUES(mobility), chaos=VALUES(chaos), consistency=VALUES(consistency), kills=VALUES(kills), boss_kills=VALUES(boss_kills), recent_kill_chain=VALUES(recent_kill_chain), last_kill_timestamp=VALUES(last_kill_timestamp), last_combat_timestamp=VALUES(last_combat_timestamp), survival_streak=VALUES(survival_streak)"
            : "INSERT INTO reputation(player_uuid,precision,brutality,survival,mobility,chaos,consistency,kills,boss_kills,recent_kill_chain,last_kill_timestamp,last_combat_timestamp,survival_streak) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET precision=excluded.precision, brutality=excluded.brutality, survival=excluded.survival, mobility=excluded.mobility, chaos=excluded.chaos, consistency=excluded.consistency, kills=excluded.kills, boss_kills=excluded.boss_kills, recent_kill_chain=excluded.recent_kill_chain, last_kill_timestamp=excluded.last_kill_timestamp, last_combat_timestamp=excluded.last_combat_timestamp, survival_streak=excluded.survival_streak"; }
    private String artifactUpsertSql() { return dialect == Dialect.MYSQL
            ? "INSERT INTO artifacts(player_uuid,artifact_seed,owner_uuid,generated_name,item_category,archetype,evolution_path,drift_alignment,awakening_path,convergence_path,ability_branch_path,mutation_history,memory_influence,regulatory_profile,open_regulatory_gates,gate_candidate_pool,trigger_profile,mechanic_profile,utility_history,drift_level,total_drifts,last_drift_timestamp,latent_lineage,species_id,parent_species_id,species_compatibility_distance,instability_state,instability_expiry,seed_affinities_json,drift_bias_json,awakening_bias_json,awakening_gain_json,drift_history_json,lore_history_json,notable_events_json,awakening_traits_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE artifact_seed=VALUES(artifact_seed),owner_uuid=VALUES(owner_uuid),generated_name=VALUES(generated_name),item_category=VALUES(item_category),archetype=VALUES(archetype),evolution_path=VALUES(evolution_path),drift_alignment=VALUES(drift_alignment),awakening_path=VALUES(awakening_path),convergence_path=VALUES(convergence_path),ability_branch_path=VALUES(ability_branch_path),mutation_history=VALUES(mutation_history),memory_influence=VALUES(memory_influence),regulatory_profile=VALUES(regulatory_profile),open_regulatory_gates=VALUES(open_regulatory_gates),gate_candidate_pool=VALUES(gate_candidate_pool),trigger_profile=VALUES(trigger_profile),mechanic_profile=VALUES(mechanic_profile),utility_history=VALUES(utility_history),drift_level=VALUES(drift_level),total_drifts=VALUES(total_drifts),last_drift_timestamp=VALUES(last_drift_timestamp),latent_lineage=VALUES(latent_lineage),species_id=VALUES(species_id),parent_species_id=VALUES(parent_species_id),species_compatibility_distance=VALUES(species_compatibility_distance),instability_state=VALUES(instability_state),instability_expiry=VALUES(instability_expiry),seed_affinities_json=VALUES(seed_affinities_json),drift_bias_json=VALUES(drift_bias_json),awakening_bias_json=VALUES(awakening_bias_json),awakening_gain_json=VALUES(awakening_gain_json),drift_history_json=VALUES(drift_history_json),lore_history_json=VALUES(lore_history_json),notable_events_json=VALUES(notable_events_json),awakening_traits_json=VALUES(awakening_traits_json)"
            : "INSERT INTO artifacts(player_uuid,artifact_seed,owner_uuid,generated_name,item_category,archetype,evolution_path,drift_alignment,awakening_path,convergence_path,ability_branch_path,mutation_history,memory_influence,regulatory_profile,open_regulatory_gates,gate_candidate_pool,trigger_profile,mechanic_profile,utility_history,drift_level,total_drifts,last_drift_timestamp,latent_lineage,species_id,parent_species_id,species_compatibility_distance,instability_state,instability_expiry,seed_affinities_json,drift_bias_json,awakening_bias_json,awakening_gain_json,drift_history_json,lore_history_json,notable_events_json,awakening_traits_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET artifact_seed=excluded.artifact_seed,owner_uuid=excluded.owner_uuid,generated_name=excluded.generated_name,item_category=excluded.item_category,archetype=excluded.archetype,evolution_path=excluded.evolution_path,drift_alignment=excluded.drift_alignment,awakening_path=excluded.awakening_path,convergence_path=excluded.convergence_path,ability_branch_path=excluded.ability_branch_path,mutation_history=excluded.mutation_history,memory_influence=excluded.memory_influence,regulatory_profile=excluded.regulatory_profile,open_regulatory_gates=excluded.open_regulatory_gates,gate_candidate_pool=excluded.gate_candidate_pool,trigger_profile=excluded.trigger_profile,mechanic_profile=excluded.mechanic_profile,utility_history=excluded.utility_history,drift_level=excluded.drift_level,total_drifts=excluded.total_drifts,last_drift_timestamp=excluded.last_drift_timestamp,latent_lineage=excluded.latent_lineage,species_id=excluded.species_id,parent_species_id=excluded.parent_species_id,species_compatibility_distance=excluded.species_compatibility_distance,instability_state=excluded.instability_state,instability_expiry=excluded.instability_expiry,seed_affinities_json=excluded.seed_affinities_json,drift_bias_json=excluded.drift_bias_json,awakening_bias_json=excluded.awakening_bias_json,awakening_gain_json=excluded.awakening_gain_json,drift_history_json=excluded.drift_history_json,lore_history_json=excluded.lore_history_json,notable_events_json=excluded.notable_events_json,awakening_traits_json=excluded.awakening_traits_json"; }

    private void bindArtifact(PreparedStatement ps, UUID playerId, Artifact artifact) throws SQLException { /* same */
        ps.setString(1, playerId.toString()); ps.setLong(2, artifact.getArtifactSeed()); ps.setString(3, artifact.getOwnerId().toString());
        ps.setString(4, artifact.getDisplayName()); ps.setString(5, artifact.getItemCategory()); ps.setString(6, artifact.getArchetypePath());
        ps.setString(7, artifact.getEvolutionPath()); ps.setString(8, artifact.getDriftAlignment()); ps.setString(9, artifact.getAwakeningPath());
        ps.setString(10, artifact.getConvergencePath()); ps.setString(11, artifact.getLastAbilityBranchPath()); ps.setString(12, artifact.getLastMutationHistory());
        ps.setString(13, artifact.getLastMemoryInfluence()); ps.setString(14, artifact.getLastRegulatoryProfile()); ps.setString(15, artifact.getLastOpenRegulatoryGates());
        ps.setString(16, artifact.getLastGateCandidatePool()); ps.setString(17, artifact.getLastTriggerProfile()); ps.setString(18, artifact.getLastMechanicProfile());
        ps.setString(19, artifact.getLastUtilityHistory());
        ps.setInt(20, artifact.getDriftLevel()); ps.setInt(21, artifact.getTotalDrifts()); ps.setLong(22, artifact.getLastDriftTimestamp());
        ps.setString(23, artifact.getLatentLineage()); ps.setString(24, artifact.getSpeciesId()); ps.setString(25, artifact.getParentSpeciesId());
        ps.setDouble(26, artifact.getLastSpeciesCompatibilityDistance()); ps.setString(27, artifact.getCurrentInstabilityState()); ps.setLong(28, artifact.getInstabilityExpiryTimestamp());
        ps.setString(29, encodeMap(Map.of("precision", artifact.getSeedPrecisionAffinity(), "brutality", artifact.getSeedBrutalityAffinity(), "survival", artifact.getSeedSurvivalAffinity(), "mobility", artifact.getSeedMobilityAffinity(), "chaos", artifact.getSeedChaosAffinity(), "consistency", artifact.getSeedConsistencyAffinity())));
        ps.setString(30, encodeMap(artifact.getDriftBiasAdjustments())); ps.setString(31, encodeMap(artifact.getAwakeningBiasAdjustments()));
        ps.setString(32, encodeMap(artifact.getAwakeningGainMultipliers())); ps.setString(33, encodeList(artifact.getDriftHistory()));
        ps.setString(34, encodeList(artifact.getLoreHistory())); ps.setString(35, encodeList(artifact.getNotableEvents()));
        ps.setString(36, encodeList(new ArrayList<>(artifact.getAwakeningTraits())));
    }
    private String encodeMap(Map<String, Double> map) { return map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining("|")); }
    private void decodeMap(String encoded, Map<String, Double> out) { out.clear(); if (encoded == null || encoded.isBlank()) return; for (String part : encoded.split("\\|")) { String[] kv = part.split(":", 2); if (kv.length == 2) try { out.put(kv[0], Double.parseDouble(kv[1])); } catch (NumberFormatException ignored) {} } }
    private String encodeList(List<String> list) { return list.stream().map(v -> Base64.getEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8))).collect(Collectors.joining(",")); }
    private List<String> decodeList(String encoded) { if (encoded == null || encoded.isBlank()) return List.of(); List<String> out = new ArrayList<>(); for (String p : encoded.split(",")) out.add(new String(Base64.getDecoder().decode(p), StandardCharsets.UTF_8)); return out; }
    private String nullToDefault(String value, String fallback) { return value == null ? fallback : value; }
}
