package obtuseloot.persistence;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.logging.Logger;

public final class PlayerDataMigrator {
    public static final int CURRENT_DATA_VERSION = 3;

    private static final List<DataMigration> MIGRATIONS = List.of(
            new LegacyToV1Migration(),
            new V1ToV2Migration(),
            new V2ToV3Migration()
    );

    private PlayerDataMigrator() {
    }

    public static MigrationResult migrateToCurrent(YamlConfiguration yaml, Logger logger, String playerFileName) {
        int sourceVersion = yaml.contains("dataVersion") ? yaml.getInt("dataVersion", 0) : 0;
        if (sourceVersion > CURRENT_DATA_VERSION) {
            logger.warning("[Persistence] Refusing to load future data version " + sourceVersion
                    + " for " + playerFileName + " (current=" + CURRENT_DATA_VERSION + ").");
            return MigrationResult.futureVersion(sourceVersion);
        }

        if (sourceVersion == CURRENT_DATA_VERSION) {
            return MigrationResult.noop(sourceVersion);
        }

        int workingVersion = sourceVersion;
        boolean migrated = false;
        while (workingVersion < CURRENT_DATA_VERSION) {
            DataMigration migration = findMigration(workingVersion);
            if (migration == null) {
                logger.warning("[Persistence] Missing migration step from version " + workingVersion
                        + " for " + playerFileName + ".");
                return MigrationResult.failed(sourceVersion, workingVersion);
            }
            migration.migrate(yaml);
            workingVersion = migration.toVersion();
            migrated = true;
        }

        yaml.set("dataVersion", CURRENT_DATA_VERSION);

        if (migrated) {
            logger.info("[Persistence] Migrated " + playerFileName + " dataVersion " + sourceVersion
                    + " -> " + CURRENT_DATA_VERSION + ".");
        }

        return MigrationResult.migrated(sourceVersion, CURRENT_DATA_VERSION);
    }

    private static DataMigration findMigration(int fromVersion) {
        for (DataMigration migration : MIGRATIONS) {
            if (migration.fromVersion() == fromVersion) {
                return migration;
            }
        }
        return null;
    }

    private interface DataMigration {
        int fromVersion();

        int toVersion();

        void migrate(YamlConfiguration yaml);
    }

    private static final class LegacyToV1Migration implements DataMigration {
        @Override
        public int fromVersion() {
            return 0;
        }

        @Override
        public int toVersion() {
            return 1;
        }

        @Override
        public void migrate(YamlConfiguration yaml) {
            moveIfAbsent(yaml, "artifact.seed", "artifact.artifact-seed");
            moveIfAbsent(yaml, "artifact.name", "artifact.naming.display-name");
            moveIfAbsent(yaml, "artifact.archetype", "artifact.archetype-path");
            moveIfAbsent(yaml, "artifact.evolution", "artifact.evolution-path");
            moveIfAbsent(yaml, "artifact.awakening", "artifact.awakening-path");
            moveIfAbsent(yaml, "artifact.fusion", "artifact.convergence-path");
            moveIfAbsent(yaml, "artifact.drift", "artifact.drift-level");
            moveIfAbsent(yaml, "artifact.drifts", "artifact.total-drifts");
            moveIfAbsent(yaml, "artifact.lineage", "artifact.latent-lineage");
        }
    }

    private static final class V1ToV2Migration implements DataMigration {
        @Override
        public int fromVersion() {
            return 1;
        }

        @Override
        public int toVersion() {
            return 2;
        }

        @Override
        public void migrate(YamlConfiguration yaml) {
            if (yaml.isConfigurationSection("artifact")) {
                if (!yaml.contains("artifact.naming.display-name") && yaml.contains("artifact.name")) {
                    yaml.set("artifact.naming.display-name", yaml.getString("artifact.name"));
                }
                if (!yaml.contains("artifact.current-instability-state")) {
                    yaml.set("artifact.current-instability-state", "none");
                }
                if (!yaml.contains("artifact.instability-expiry")) {
                    yaml.set("artifact.instability-expiry", 0L);
                }
            }
        }
    }

    private static final class V2ToV3Migration implements DataMigration {
        @Override
        public int fromVersion() {
            return 2;
        }

        @Override
        public int toVersion() {
            return 3;
        }

        @Override
        public void migrate(YamlConfiguration yaml) {
        }
    }

    private static void moveIfAbsent(YamlConfiguration yaml, String from, String to) {
        if (!yaml.contains(to) && yaml.contains(from)) {
            yaml.set(to, yaml.get(from));
        }
    }

    public record MigrationResult(boolean migrated, boolean failed, boolean futureVersion, int fromVersion,
                                  int resolvedVersion) {
        static MigrationResult noop(int version) {
            return new MigrationResult(false, false, false, version, version);
        }

        static MigrationResult migrated(int from, int to) {
            return new MigrationResult(true, false, false, from, to);
        }

        static MigrationResult failed(int from, int resolvedVersion) {
            return new MigrationResult(false, true, false, from, resolvedVersion);
        }

        static MigrationResult futureVersion(int from) {
            return new MigrationResult(false, true, true, from, from);
        }
    }
}
