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
                    item_category VARCHAR(64),
                    archetype VARCHAR(64),
                    evolution_path VARCHAR(128),
                    drift_alignment VARCHAR(64),
                    awakening_path VARCHAR(128),
                    fusion_path VARCHAR(128),
                    ability_branch_path TEXT,
                    mutation_history TEXT,
                    memory_influence TEXT,
                    drift_level INT,
                    total_drifts INT,
                    last_drift_timestamp BIGINT,
                    latent_lineage VARCHAR(128),
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
        }
    }
}
