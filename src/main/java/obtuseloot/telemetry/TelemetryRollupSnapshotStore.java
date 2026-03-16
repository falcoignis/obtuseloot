package obtuseloot.telemetry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class TelemetryRollupSnapshotStore {
    private final Path snapshotPath;

    public TelemetryRollupSnapshotStore(Path snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public synchronized void write(TelemetryRollupSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        Properties p = new Properties();
        EcosystemSnapshot eco = snapshot.ecosystemSnapshot();
        p.setProperty("version", String.valueOf(snapshot.version()));
        p.setProperty("createdAtMs", String.valueOf(snapshot.createdAtMs()));
        p.setProperty("initializationMode", snapshot.initializationMode());
        p.setProperty("snapshot.generatedAtMs", String.valueOf(eco.generatedAtMs()));
        p.setProperty("snapshot.activeArtifactCount", String.valueOf(eco.activeArtifactCount()));
        p.setProperty("snapshot.carryingCapacityUtilization", String.valueOf(eco.carryingCapacityUtilization()));
        p.setProperty("snapshot.diversityIndex", String.valueOf(eco.diversityIndex()));
        p.setProperty("snapshot.turnoverRate", String.valueOf(eco.turnoverRate()));
        p.setProperty("snapshot.branchBirthCount", String.valueOf(eco.branchBirthCount()));
        p.setProperty("snapshot.branchCollapseCount", String.valueOf(eco.branchCollapseCount()));
        writeLongMap(p, "eventCounts.", stringifyEventCounts(eco.eventCounts()));
        writeLongMap(p, "niche.population.", eco.nichePopulationRollup().populationByNiche());
        writeLongMap(p, "niche.meaningful.", eco.nichePopulationRollup().meaningfulOutcomesByNiche());
        writeDoubleMap(p, "niche.utilityDensity.", eco.nichePopulationRollup().utilityDensityByNiche());
        writeDoubleMap(p, "niche.saturation.", eco.nichePopulationRollup().saturationPressureByNiche());
        writeDoubleMap(p, "niche.opportunity.", eco.nichePopulationRollup().opportunityShareByNiche());
        writeDoubleMap(p, "niche.specialization.", eco.nichePopulationRollup().specializationPressureByNiche());
        writeLongMap(p, "niche.branchContribution.", eco.nichePopulationRollup().branchContributionByNiche());
        writeLongMap(p, "lineage.population.", eco.lineagePopulationRollup().populationByLineage());
        writeLongMap(p, "lineage.branchCount.", eco.lineagePopulationRollup().branchCountByLineage());
        writeDoubleMap(p, "lineage.utilityDensity.", eco.lineagePopulationRollup().utilityDensityByLineage());
        writeDoubleMap(p, "lineage.momentum.", eco.lineagePopulationRollup().momentumByLineage());
        writeDoubleMap(p, "lineage.specializationTrajectory.", eco.lineagePopulationRollup().specializationTrajectoryByLineage());
        writeDoubleMap(p, "lineage.driftWindow.", eco.lineagePopulationRollup().driftWindowRemainingByLineage());
        writeDoubleMap(p, "lineage.branchDivergence.", eco.lineagePopulationRollup().branchDivergenceByLineage());
        writeLongMap(p, "competitionPressure.", eco.competitionPressureDistribution());
        p.setProperty("snapshot.bifurcationCount", String.valueOf(eco.bifurcationCount()));
        writeList(p, "dynamicNiches.", eco.dynamicNiches());
        writeLongMap(p, "dynamicNichePopulation.", eco.dynamicNichePopulation());
        writeNestedLongMap(p, "lineage.nicheDistribution.", eco.lineagePopulationRollup().nicheDistributionByLineage());

        try {
            Files.createDirectories(snapshotPath.getParent());
            try (var out = Files.newOutputStream(snapshotPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                p.store(out, "telemetry rollup snapshot");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist telemetry rollup snapshot", ex);
        }
    }

    public synchronized Optional<TelemetryRollupSnapshot> readLatest() {
        if (!Files.exists(snapshotPath)) {
            return Optional.empty();
        }
        Properties p = new Properties();
        try {
            p.load(Files.newBufferedReader(snapshotPath, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read telemetry rollup snapshot", ex);
        }

        int version = Integer.parseInt(p.getProperty("version", "0"));
        if (version <= 0 || version > TelemetryRollupSnapshot.CURRENT_VERSION) {
            return Optional.empty();
        }
        long createdAt = Long.parseLong(p.getProperty("createdAtMs", "0"));
        String mode = p.getProperty("initializationMode", "rehydrated_snapshot");
        long generatedAtMs = Long.parseLong(p.getProperty("snapshot.generatedAtMs", "0"));

        NichePopulationRollup niche = new NichePopulationRollup(
                generatedAtMs,
                readLongMap(p, "niche.population."),
                readLongMap(p, "niche.meaningful."),
                readDoubleMap(p, "niche.utilityDensity."),
                readDoubleMap(p, "niche.saturation."),
                readDoubleMap(p, "niche.opportunity."),
                readDoubleMap(p, "niche.specialization."),
                readLongMap(p, "niche.branchContribution."));
        LineagePopulationRollup lineage = new LineagePopulationRollup(
                generatedAtMs,
                readLongMap(p, "lineage.population."),
                readLongMap(p, "lineage.branchCount."),
                readDoubleMap(p, "lineage.utilityDensity."),
                readDoubleMap(p, "lineage.momentum."),
                readDoubleMap(p, "lineage.specializationTrajectory."),
                readNestedLongMap(p, "lineage.nicheDistribution."),
                readDoubleMap(p, "lineage.driftWindow."),
                readDoubleMap(p, "lineage.branchDivergence."));

        EcosystemSnapshot eco = new EcosystemSnapshot(
                generatedAtMs,
                parseEventCounts(readLongMap(p, "eventCounts.")),
                niche,
                lineage,
                Long.parseLong(p.getProperty("snapshot.activeArtifactCount", "0")),
                Double.parseDouble(p.getProperty("snapshot.carryingCapacityUtilization", "0.0")),
                Double.parseDouble(p.getProperty("snapshot.diversityIndex", "0.0")),
                Double.parseDouble(p.getProperty("snapshot.turnoverRate", "0.0")),
                Long.parseLong(p.getProperty("snapshot.branchBirthCount", "0")),
                Long.parseLong(p.getProperty("snapshot.branchCollapseCount", "0")),
                readLongMap(p, "competitionPressure."),
                readList(p, "dynamicNiches."),
                Long.parseLong(p.getProperty("snapshot.bifurcationCount", "0")),
                readLongMap(p, "dynamicNichePopulation."));

        return Optional.of(new TelemetryRollupSnapshot(version, createdAt, mode, eco));
    }

    private void writeLongMap(Properties p, String prefix, Map<String, Long> map) {
        map.forEach((k, v) -> p.setProperty(prefix + escape(k), String.valueOf(v)));
    }

    private void writeDoubleMap(Properties p, String prefix, Map<String, Double> map) {
        map.forEach((k, v) -> p.setProperty(prefix + escape(k), String.valueOf(v)));
    }


    private void writeList(Properties p, String prefix, java.util.List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            p.setProperty(prefix + i, escape(values.get(i)));
        }
    }

    private java.util.List<String> readList(Properties p, String prefix) {
        return p.stringPropertyNames().stream()
                .filter(k -> k.startsWith(prefix))
                .sorted(java.util.Comparator.comparingInt(k -> Integer.parseInt(k.substring(prefix.length()))))
                .map(k -> unescape(p.getProperty(k)))
                .toList();
    }

    private void writeNestedLongMap(Properties p, String prefix, Map<String, Map<String, Long>> nested) {
        nested.forEach((outer, inner) -> inner.forEach((innerKey, value) ->
                p.setProperty(prefix + escape(outer) + "::" + escape(innerKey), String.valueOf(value))));
    }

    private Map<String, Long> readLongMap(Properties p, String prefix) {
        Map<String, Long> out = new LinkedHashMap<>();
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith(prefix))
                .forEach(k -> out.put(unescape(k.substring(prefix.length())), Long.parseLong(p.getProperty(k))));
        return Map.copyOf(out);
    }

    private Map<String, Double> readDoubleMap(Properties p, String prefix) {
        Map<String, Double> out = new LinkedHashMap<>();
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith(prefix))
                .forEach(k -> out.put(unescape(k.substring(prefix.length())), Double.parseDouble(p.getProperty(k))));
        return Map.copyOf(out);
    }

    private Map<String, Map<String, Long>> readNestedLongMap(Properties p, String prefix) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith(prefix))
                .forEach(k -> {
                    String key = k.substring(prefix.length());
                    String[] parts = key.split("::", 2);
                    if (parts.length < 2) {
                        return;
                    }
                    String outer = unescape(parts[0]);
                    String inner = unescape(parts[1]);
                    out.computeIfAbsent(outer, ignored -> new LinkedHashMap<>()).put(inner, Long.parseLong(p.getProperty(k)));
                });
        Map<String, Map<String, Long>> copy = new LinkedHashMap<>();
        out.forEach((k, v) -> copy.put(k, Map.copyOf(v)));
        return Map.copyOf(copy);
    }

    private Map<String, Long> stringifyEventCounts(Map<EcosystemTelemetryEventType, Long> eventCounts) {
        Map<String, Long> out = new LinkedHashMap<>();
        eventCounts.forEach((k, v) -> out.put(k.name(), v));
        return out;
    }

    private Map<EcosystemTelemetryEventType, Long> parseEventCounts(Map<String, Long> raw) {
        Map<EcosystemTelemetryEventType, Long> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> out.put(EcosystemTelemetryEventType.valueOf(k), v));
        return Map.copyOf(out);
    }

    private String escape(String value) {
        return value.replace("%", "%25").replace(".", "%2E").replace(":", "%3A");
    }

    private String unescape(String value) {
        return value.replace("%3A", ":").replace("%2E", ".").replace("%25", "%");
    }
}
