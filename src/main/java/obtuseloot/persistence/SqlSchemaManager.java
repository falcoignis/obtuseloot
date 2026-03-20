package obtuseloot.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlSchemaManager {
    public void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    updated_at BIGINT NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS artifacts (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    artifact_seed BIGINT NOT NULL,
                    owner_uuid VARCHAR(36) NOT NULL,
                    generated_name TEXT,
                    naming_seed BIGINT,
                    true_name TEXT,
                    root_form VARCHAR(64),
                    naming_archetype VARCHAR(64),
                    tone_profile VARCHAR(64),
                    discovery_state VARCHAR(64),
                    identity_tags_json TEXT,
                    affinity_lexemes_json TEXT,
                    epithet_seed INT,
                    title_seed INT,
                    item_category VARCHAR(64),
                    archetype VARCHAR(64),
                    evolution_path VARCHAR(128),
                    drift_alignment VARCHAR(64),
                    awakening_path VARCHAR(128),
                    convergence_path VARCHAR(128),
                    ability_branch_path TEXT,
                    mutation_history TEXT,
                    memory_influence TEXT,
                    regulatory_profile TEXT,
                    open_regulatory_gates TEXT,
                    gate_candidate_pool TEXT,
                    trigger_profile TEXT,
                    mechanic_profile TEXT,
                    utility_history TEXT,
                    drift_level INT,
                    total_drifts INT,
                    last_drift_timestamp BIGINT,
                    latent_lineage VARCHAR(128),
                    species_id VARCHAR(128),
                    parent_species_id VARCHAR(128),
                    species_compatibility_distance DOUBLE,
                    instability_state VARCHAR(64),
                    instability_expiry BIGINT,
                    seed_affinities_json TEXT,
                    drift_bias_json TEXT,
                    awakening_bias_json TEXT,
                    awakening_gain_json TEXT,
                    drift_history_json TEXT,
                    lore_history_json TEXT,
                    notable_events_json TEXT,
                    awakening_traits_json TEXT,
                    FOREIGN KEY (player_uuid) REFERENCES players(player_uuid)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS species_registry (
                    species_id VARCHAR(128) PRIMARY KEY,
                    parent_species_id VARCHAR(128),
                    origin_lineage_id VARCHAR(128) NOT NULL,
                    created_at BIGINT NOT NULL,
                    created_generation INT NOT NULL,
                    divergence_snapshot TEXT,
                    tendency_profile TEXT
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lineage_species_roots (
                    lineage_id VARCHAR(128) PRIMARY KEY,
                    root_species_id VARCHAR(128) NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS artifact_memories (
                    player_uuid VARCHAR(36) NOT NULL,
                    event_key VARCHAR(64) NOT NULL,
                    event_count INT NOT NULL,
                    PRIMARY KEY (player_uuid, event_key)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS artifact_mutations (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    mutation_history TEXT
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reputation (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    precision INT,
                    brutality INT,
                    survival INT,
                    mobility INT,
                    chaos INT,
                    consistency INT,
                    kills INT,
                    boss_kills INT,
                    recent_kill_chain INT,
                    last_kill_timestamp BIGINT,
                    last_combat_timestamp BIGINT,
                    survival_streak INT,
                    FOREIGN KEY (player_uuid) REFERENCES players(player_uuid)
                )
                """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_artifact_seed ON artifacts(artifact_seed)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_artifact_owner ON artifacts(owner_uuid)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_artifact_archetype ON artifacts(archetype)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_artifact_evolution ON artifacts(evolution_path)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_artifact_drift_alignment ON artifacts(drift_alignment)");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN naming_seed BIGINT");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN true_name TEXT");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN root_form VARCHAR(64)");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN naming_archetype VARCHAR(64)");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN tone_profile VARCHAR(64)");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN discovery_state VARCHAR(64)");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN identity_tags_json TEXT");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN affinity_lexemes_json TEXT");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN epithet_seed INT");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN title_seed INT");
            tryAddColumn(statement, "ALTER TABLE artifacts ADD COLUMN utility_history TEXT");
        }
    }

    private void tryAddColumn(Statement statement, String ddl) {
        try {
            statement.executeUpdate(ddl);
        } catch (SQLException ignored) {
            // Column likely already exists on upgraded installs.
        }
    }
}
