package obtuseloot.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

public class TelemetryAggregationBuffer {
    private final ConcurrentLinkedQueue<EcosystemTelemetryEvent> pending = new ConcurrentLinkedQueue<>();
    private final Map<String, Set<Long>> nicheArtifacts = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> lineageArtifacts = new ConcurrentHashMap<>();
    private final Set<Long> activeArtifacts = ConcurrentHashMap.newKeySet();
    private final Map<EcosystemTelemetryEventType, LongAdder> typeCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> meaningfulByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> utilityDensityByNiche = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> utilityDensitySamplesByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> saturationPressureByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> opportunityShareByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> specializationPressureByNiche = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> branchContributionByNiche = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> branchIdsByLineage = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> utilityDensityByLineage = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> utilityDensitySamplesByLineage = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> momentumByLineage = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> specializationTrajectoryByLineage = new ConcurrentHashMap<>();
    private final Map<String, Map<String, LongAdder>> nicheDistributionByLineage = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> driftWindowByLineage = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> branchDivergenceByLineage = new ConcurrentHashMap<>();
    private final LongAdder branchBirthCount = new LongAdder();
    private final LongAdder branchCollapseCount = new LongAdder();
    private final Map<String, LongAdder> competitionPressureDistribution = new ConcurrentHashMap<>();

    public void enqueue(EcosystemTelemetryEvent event) {
        pending.add(event);
        typeCounts.computeIfAbsent(event.type(), ignored -> new LongAdder()).increment();
        if (event.artifactSeed() > 0L) {
            activeArtifacts.add(event.artifactSeed());
        }
        if (present(event.niche())) {
            nicheArtifacts.computeIfAbsent(event.niche(), ignored -> ConcurrentHashMap.newKeySet()).add(event.artifactSeed());
        }
        if (present(event.lineageId())) {
            lineageArtifacts.computeIfAbsent(event.lineageId(), ignored -> ConcurrentHashMap.newKeySet()).add(event.artifactSeed());
        }
        String niche = normalized(event.attributes().get("niche"), event.niche());
        String lineage = normalized(event.attributes().get("lineage_id"), event.lineageId());
        if (present(niche) && "true".equalsIgnoreCase(event.attributes().get("meaningful"))) {
            meaningfulByNiche.computeIfAbsent(niche, ignored -> new LongAdder()).increment();
        }
        addDoubleMetric(event.attributes(), "utility_density", niche, utilityDensityByNiche, utilityDensitySamplesByNiche);
        addDoubleMetric(event.attributes(), "ecology_pressure", niche, saturationPressureByNiche, null);
        addDoubleMetric(event.attributes(), "opportunity_share", niche, opportunityShareByNiche, null);
        addDoubleMetric(event.attributes(), "specialization_pressure", niche, specializationPressureByNiche, null);
        addDoubleMetric(event.attributes(), "utility_density", lineage, utilityDensityByLineage, utilityDensitySamplesByLineage);
        addDoubleMetric(event.attributes(), "lineage_momentum", lineage, momentumByLineage, null);
        addDoubleMetric(event.attributes(), "specialization_trajectory", lineage, specializationTrajectoryByLineage, null);
        addDoubleMetric(event.attributes(), "drift_window_remaining", lineage, driftWindowByLineage, null);
        addDoubleMetric(event.attributes(), "branch_divergence", lineage, branchDivergenceByLineage, null);

        if (present(lineage) && present(niche)) {
            nicheDistributionByLineage
                    .computeIfAbsent(lineage, ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(niche, ignored -> new LongAdder())
                    .increment();
        }
        if (event.type() == EcosystemTelemetryEventType.BRANCH_FORMATION) {
            branchBirthCount.increment();
            if (present(niche)) {
                branchContributionByNiche.computeIfAbsent(niche, ignored -> new LongAdder()).increment();
            }
            String branchId = event.attributes().get("branch_id");
            if (present(lineage) && present(branchId)) {
                branchIdsByLineage.computeIfAbsent(lineage, ignored -> ConcurrentHashMap.newKeySet()).add(branchId);
            }
        }
        if (event.type() == EcosystemTelemetryEventType.LINEAGE_UPDATE
                && "branch-collapsed".equalsIgnoreCase(event.attributes().get("event"))) {
            branchCollapseCount.increment();
        }
        double pressure = parseDouble(event.attributes().get("ecology_pressure"));
        if (!Double.isNaN(pressure)) {
            String bucket = pressure < 0.33D ? "low" : pressure < 0.66D ? "medium" : "high";
            competitionPressureDistribution.computeIfAbsent(bucket, ignored -> new LongAdder()).increment();
        }
    }

    public List<EcosystemTelemetryEvent> drain(int maxEvents) {
        List<EcosystemTelemetryEvent> out = new ArrayList<>(Math.max(1, maxEvents));
        for (int i = 0; i < maxEvents; i++) {
            EcosystemTelemetryEvent event = pending.poll();
            if (event == null) {
                break;
            }
            out.add(event);
        }
        return out;
    }

    public int pendingCount() { return pending.size(); }
    public Map<String, Long> nichePopulationSnapshot() { return sizeSnapshot(nicheArtifacts); }
    public Map<String, Long> lineagePopulationSnapshot() { return sizeSnapshot(lineageArtifacts); }
    public Map<EcosystemTelemetryEventType, Long> typeCountsSnapshot() {
        Map<EcosystemTelemetryEventType, Long> out = new ConcurrentHashMap<>();
        typeCounts.forEach((k, v) -> out.put(k, v.sum()));
        return Map.copyOf(out);
    }
    public Map<String, Long> meaningfulByNicheSnapshot() { return longSnapshot(meaningfulByNiche); }
    public Map<String, Double> utilityDensityByNicheSnapshot() { return avgSnapshot(utilityDensityByNiche, utilityDensitySamplesByNiche); }
    public Map<String, Double> saturationPressureByNicheSnapshot() { return doubleSnapshot(saturationPressureByNiche); }
    public Map<String, Double> opportunityShareByNicheSnapshot() { return doubleSnapshot(opportunityShareByNiche); }
    public Map<String, Double> specializationPressureByNicheSnapshot() { return doubleSnapshot(specializationPressureByNiche); }
    public Map<String, Long> branchContributionByNicheSnapshot() { return longSnapshot(branchContributionByNiche); }
    public Map<String, Long> branchCountByLineageSnapshot() {
        Map<String, Long> out = new ConcurrentHashMap<>();
        branchIdsByLineage.forEach((k, v) -> out.put(k, (long) v.size()));
        return Map.copyOf(out);
    }
    public Map<String, Double> utilityDensityByLineageSnapshot() { return avgSnapshot(utilityDensityByLineage, utilityDensitySamplesByLineage); }
    public Map<String, Double> momentumByLineageSnapshot() { return doubleSnapshot(momentumByLineage); }
    public Map<String, Double> specializationTrajectoryByLineageSnapshot() { return doubleSnapshot(specializationTrajectoryByLineage); }
    public Map<String, Map<String, Long>> nicheDistributionByLineageSnapshot() {
        Map<String, Map<String, Long>> out = new ConcurrentHashMap<>();
        nicheDistributionByLineage.forEach((lineage, map) -> out.put(lineage, longSnapshot(map)));
        return Map.copyOf(out);
    }
    public Map<String, Double> driftWindowByLineageSnapshot() { return doubleSnapshot(driftWindowByLineage); }
    public Map<String, Double> branchDivergenceByLineageSnapshot() { return doubleSnapshot(branchDivergenceByLineage); }
    public long activeArtifactCountSnapshot() { return activeArtifacts.size(); }
    public long branchBirthCountSnapshot() { return branchBirthCount.sum(); }
    public long branchCollapseCountSnapshot() { return branchCollapseCount.sum(); }
    public Map<String, Long> competitionPressureDistributionSnapshot() { return longSnapshot(competitionPressureDistribution); }

    private void addDoubleMetric(Map<String, String> attrs,
                                 String key,
                                 String namespace,
                                 Map<String, DoubleAdder> store,
                                 Map<String, LongAdder> samples) {
        if (!present(namespace)) return;
        double v = parseDouble(attrs.get(key));
        if (Double.isNaN(v)) return;
        store.computeIfAbsent(namespace, ignored -> new DoubleAdder()).add(v);
        if (samples != null) {
            samples.computeIfAbsent(namespace, ignored -> new LongAdder()).increment();
        }
    }

    private Map<String, Long> sizeSnapshot(Map<String, Set<Long>> source) {
        Map<String, Long> out = new ConcurrentHashMap<>();
        source.forEach((k, v) -> out.put(k, (long) v.size()));
        return Map.copyOf(out);
    }

    private Map<String, Long> longSnapshot(Map<String, LongAdder> source) {
        Map<String, Long> out = new ConcurrentHashMap<>();
        source.forEach((k, v) -> out.put(k, v.sum()));
        return Map.copyOf(out);
    }

    private Map<String, Double> doubleSnapshot(Map<String, DoubleAdder> source) {
        Map<String, Double> out = new ConcurrentHashMap<>();
        source.forEach((k, v) -> out.put(k, v.sum()));
        return Map.copyOf(out);
    }

    private Map<String, Double> avgSnapshot(Map<String, DoubleAdder> sums, Map<String, LongAdder> samples) {
        Map<String, Double> out = new ConcurrentHashMap<>();
        sums.forEach((k, v) -> out.put(k, v.sum() / Math.max(1L, samples.getOrDefault(k, new LongAdder()).sum())));
        return Map.copyOf(out);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank() && !TelemetryFieldContract.NOT_APPLICABLE.equalsIgnoreCase(value);
    }

    private String normalized(String value, String fallback) {
        return present(value) ? value : fallback;
    }

    private double parseDouble(String value) {
        if (!present(value)) return Double.NaN;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private static final class DoubleAdder {
        private final java.util.concurrent.atomic.DoubleAdder delegate = new java.util.concurrent.atomic.DoubleAdder();
        void add(double value) { delegate.add(value); }
        double sum() { return delegate.sum(); }
    }
}
