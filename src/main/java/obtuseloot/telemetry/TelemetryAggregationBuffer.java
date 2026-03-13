package obtuseloot.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

public class TelemetryAggregationBuffer {
    private final int maxPendingEvents;
    private final ConcurrentLinkedQueue<EcosystemTelemetryEvent> pending = new ConcurrentLinkedQueue<>();
    private final LongAdder droppedEvents = new LongAdder();
    private final Map<String, LongAdder> nicheArtifactCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> lineageArtifactCounts = new ConcurrentHashMap<>();
    private final LongAdder activeArtifactCount = new LongAdder();
    private final Map<EcosystemTelemetryEventType, LongAdder> typeCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> meaningfulByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> utilityDensityByNiche = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> utilityDensitySamplesByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> saturationPressureByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> opportunityShareByNiche = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> specializationPressureByNiche = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> branchContributionByNiche = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> branchCountByLineage = new ConcurrentHashMap<>();
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
    private final Map<String, Long> baselineNichePopulation = new ConcurrentHashMap<>();
    private final Map<String, Long> baselineLineagePopulation = new ConcurrentHashMap<>();
    private final Map<EcosystemTelemetryEventType, Long> baselineTypeCounts = new ConcurrentHashMap<>();
    private volatile long baselineActiveArtifactCount;
    private volatile long baselineBranchBirthCount;
    private volatile long baselineBranchCollapseCount;

    public TelemetryAggregationBuffer() {
        this(4096);
    }

    public TelemetryAggregationBuffer(int maxPendingEvents) {
        this.maxPendingEvents = Math.max(64, maxPendingEvents);
    }

    public void enqueue(EcosystemTelemetryEvent event) {
        if (pending.size() >= maxPendingEvents) {
            pending.poll();
            droppedEvents.increment();
        }
        pending.add(event);
        typeCounts.computeIfAbsent(event.type(), ignored -> new LongAdder()).increment();
        if (event.artifactSeed() > 0L) {
            activeArtifactCount.increment();
        }
        if (present(event.niche())) {
            nicheArtifactCounts.computeIfAbsent(event.niche(), ignored -> new LongAdder()).increment();
        }
        if (present(event.lineageId())) {
            lineageArtifactCounts.computeIfAbsent(event.lineageId(), ignored -> new LongAdder()).increment();
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
            if (present(lineage)) {
                branchCountByLineage.computeIfAbsent(lineage, ignored -> new LongAdder()).increment();
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
    public int maxPendingEvents() { return maxPendingEvents; }
    public long droppedEvents() { return droppedEvents.sum(); }
    public Map<String, Long> nichePopulationSnapshot() { return mergeLongMaps(baselineNichePopulation, longSnapshot(nicheArtifactCounts)); }
    public Map<String, Long> lineagePopulationSnapshot() { return mergeLongMaps(baselineLineagePopulation, longSnapshot(lineageArtifactCounts)); }
    public Map<EcosystemTelemetryEventType, Long> typeCountsSnapshot() {
        Map<EcosystemTelemetryEventType, Long> out = new ConcurrentHashMap<>();
        out.putAll(baselineTypeCounts);
        typeCounts.forEach((k, v) -> out.merge(k, v.sum(), Long::sum));
        return Map.copyOf(out);
    }
    public Map<String, Long> meaningfulByNicheSnapshot() { return longSnapshot(meaningfulByNiche); }
    public Map<String, Double> utilityDensityByNicheSnapshot() { return avgSnapshot(utilityDensityByNiche, utilityDensitySamplesByNiche); }
    public Map<String, Double> saturationPressureByNicheSnapshot() { return doubleSnapshot(saturationPressureByNiche); }
    public Map<String, Double> opportunityShareByNicheSnapshot() { return doubleSnapshot(opportunityShareByNiche); }
    public Map<String, Double> specializationPressureByNicheSnapshot() { return doubleSnapshot(specializationPressureByNiche); }
    public Map<String, Long> branchContributionByNicheSnapshot() { return longSnapshot(branchContributionByNiche); }
    public Map<String, Long> branchCountByLineageSnapshot() { return longSnapshot(branchCountByLineage); }
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
    public long activeArtifactCountSnapshot() { return baselineActiveArtifactCount + activeArtifactCount.sum(); }
    public long branchBirthCountSnapshot() { return baselineBranchBirthCount + branchBirthCount.sum(); }
    public long branchCollapseCountSnapshot() { return baselineBranchCollapseCount + branchCollapseCount.sum(); }
    public Map<String, Long> competitionPressureDistributionSnapshot() { return longSnapshot(competitionPressureDistribution); }

    public synchronized void rehydrateFrom(EcosystemSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        baselineNichePopulation.clear();
        baselineLineagePopulation.clear();
        baselineTypeCounts.clear();
        baselineNichePopulation.putAll(snapshot.nichePopulationRollup().populationByNiche());
        baselineLineagePopulation.putAll(snapshot.lineagePopulationRollup().populationByLineage());
        baselineTypeCounts.putAll(snapshot.eventCounts());
        baselineActiveArtifactCount = snapshot.activeArtifactCount();
        baselineBranchBirthCount = snapshot.branchBirthCount();
        baselineBranchCollapseCount = snapshot.branchCollapseCount();
    }

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

    private <K> Map<K, Long> mergeLongMaps(Map<K, Long> baseline, Map<K, Long> live) {
        Map<K, Long> out = new ConcurrentHashMap<>(baseline);
        live.forEach((k, v) -> out.merge(k, v, Long::sum));
        return Map.copyOf(out);
    }

    private <K> Map<K, Long> longSnapshot(Map<K, LongAdder> source) {
        Map<K, Long> out = new ConcurrentHashMap<>();
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
