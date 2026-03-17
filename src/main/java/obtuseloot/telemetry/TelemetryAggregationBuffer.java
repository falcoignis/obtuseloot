package obtuseloot.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

public class TelemetryAggregationBuffer {
    private static final int DEFAULT_MAX_DIMENSION_KEYS = Integer.getInteger("world.telemetryMaxDimensionKeys", 2048);

    private final int maxPendingEvents;
    private final int maxDimensionKeys;
    private final ConcurrentLinkedQueue<EcosystemTelemetryEvent> pending = new ConcurrentLinkedQueue<>();
    private final LongAdder droppedEvents = new LongAdder();
    private final Map<String, LongAdder> nicheArtifactCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> lineageArtifactCounts = new ConcurrentHashMap<>();
    private final Map<Long, String> nicheByArtifactSeed = new ConcurrentHashMap<>();
    private final Map<Long, String> lineageByArtifactSeed = new ConcurrentHashMap<>();
    private final java.util.Set<Long> activeArtifactSeeds = ConcurrentHashMap.newKeySet();
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
    private final Map<String, DoubleAdder> survivalScoreByLineage = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> maintenanceCostByLineage = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> graceWindowByLineage = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> unstableTransitionsByLineage = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> collapsingTransitionsByLineage = new ConcurrentHashMap<>();
    private final Map<String, String> lastKnownNicheByLineage = new ConcurrentHashMap<>();
    private final LongAdder branchBirthCount = new LongAdder();
    private final LongAdder branchCollapseCount = new LongAdder();
    private final Map<String, LongAdder> competitionPressureDistribution = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> dynamicNichePopulation = new ConcurrentHashMap<>();
    private final java.util.Set<String> dynamicNiches = ConcurrentHashMap.newKeySet();
    private final LongAdder bifurcationCount = new LongAdder();
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
        this(maxPendingEvents, DEFAULT_MAX_DIMENSION_KEYS);
    }

    public TelemetryAggregationBuffer(int maxPendingEvents, int maxDimensionKeys) {
        this.maxPendingEvents = Math.max(64, maxPendingEvents);
        this.maxDimensionKeys = Math.max(64, maxDimensionKeys);
    }

    public void enqueue(EcosystemTelemetryEvent event) {
        if (pending.size() >= maxPendingEvents) {
            pending.poll();
            droppedEvents.increment();
        }
        pending.add(event);
        typeCounts.computeIfAbsent(event.type(), ignored -> new LongAdder()).increment();
        long artifactSeed = event.artifactSeed();
        if (artifactSeed > 0L) {
            activeArtifactSeeds.add(artifactSeed);
        }
        // Prefer the emitted event niche over any legacy attribute-level niche.
        // Ability telemetry may include coarse labels (e.g., "RITUAL") in attributes,
        // while the event niche contains the effective dynamic niche (e.g., "RITUAL_A1").
        String niche = normalized(event.niche(), event.attributes().get("niche"));
        String lineage = normalized(event.attributes().get("lineage_id"), event.lineageId());
        if (artifactSeed > 0L) {
            updateArtifactNamespace(artifactSeed, niche, nicheByArtifactSeed, nicheArtifactCounts);
            updateArtifactNamespace(artifactSeed, lineage, lineageByArtifactSeed, lineageArtifactCounts);
        }
        if (present(lineage) && present(niche)) {
            lastKnownNicheByLineage.put(lineage, niche);
        }

        boolean meaningfulOutcome = "true".equalsIgnoreCase(event.attributes().get("meaningful"))
                || "MEANINGFUL".equalsIgnoreCase(event.attributes().get("outcome_classification"));
        if (present(niche) && meaningfulOutcome) {
            incrementLong(meaningfulByNiche, niche);
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
        addDoubleMetric(event.attributes(), "survival_score", lineage, survivalScoreByLineage, null);
        addDoubleMetric(event.attributes(), "maintenance_cost", lineage, maintenanceCostByLineage, null);
        addDoubleMetric(event.attributes(), "grace_window_remaining", lineage, graceWindowByLineage, null);

        if (present(lineage) && present(niche)) {
            Map<String, LongAdder> lineageNicheCounts = boundedMapForLineage(lineage);
            if (lineageNicheCounts != null) {
                incrementLong(lineageNicheCounts, niche);
            }
        }
        if (event.type() == EcosystemTelemetryEventType.BRANCH_FORMATION) {
            branchBirthCount.increment();
            if (!present(niche) && present(lineage)) {
                niche = lastKnownNicheByLineage.get(lineage);
            }
            if (present(niche)) {
                incrementLong(branchContributionByNiche, niche);
            }
            if (present(lineage)) {
                incrementLong(branchCountByLineage, lineage);
            }
        }
        if (event.type() == EcosystemTelemetryEventType.LINEAGE_UPDATE
                && "branch-collapsed".equalsIgnoreCase(event.attributes().get("event"))) {
            branchCollapseCount.increment();
        }
        if (event.type() == EcosystemTelemetryEventType.LINEAGE_UPDATE) {
            if ("UNSTABLE".equalsIgnoreCase(event.attributes().get("lifecycle_state")) && present(lineage)) {
                incrementLong(unstableTransitionsByLineage, lineage);
            }
            if ("COLLAPSING".equalsIgnoreCase(event.attributes().get("lifecycle_state")) && present(lineage)) {
                incrementLong(collapsingTransitionsByLineage, lineage);
            }
        }
        if (event.type() == EcosystemTelemetryEventType.NICHE_BIFURCATION) {
            String eventType = event.attributes().get("event_type");
            if ("niche_bifurcation".equalsIgnoreCase(eventType)) {
                bifurcationCount.increment();
                String childA = event.attributes().get("child_niche_a");
                String childB = event.attributes().get("child_niche_b");
                if (present(childA)) {
                    dynamicNiches.add(childA);
                }
                if (present(childB)) {
                    dynamicNiches.add(childB);
                }
            }
            String emittedNiche = normalized(event.niche(), event.attributes().get("niche"));
            if (present(emittedNiche) && ("niche_bifurcation_child".equalsIgnoreCase(eventType) || dynamicNiches.contains(emittedNiche))) {
                dynamicNiches.add(emittedNiche);
            }
        }
        if (present(niche) && dynamicNiches.contains(niche)) {
            incrementLong(dynamicNichePopulation, niche);
        }
        double pressure = parseDouble(event.attributes().get("ecology_pressure"));
        if (!Double.isNaN(pressure)) {
            String bucket = pressure < 0.33D ? "low" : pressure < 0.66D ? "medium" : "high";
            incrementLong(competitionPressureDistribution, bucket);
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
    public Map<String, Double> survivalScoreByLineageSnapshot() { return doubleSnapshot(survivalScoreByLineage); }
    public Map<String, Double> maintenanceCostByLineageSnapshot() { return doubleSnapshot(maintenanceCostByLineage); }
    public Map<String, Double> graceWindowByLineageSnapshot() { return doubleSnapshot(graceWindowByLineage); }
    public Map<String, Long> unstableTransitionsByLineageSnapshot() { return longSnapshot(unstableTransitionsByLineage); }
    public Map<String, Long> collapsingTransitionsByLineageSnapshot() { return longSnapshot(collapsingTransitionsByLineage); }
    public long activeArtifactCountSnapshot() { return baselineActiveArtifactCount + activeArtifactSeeds.size(); }
    public long branchBirthCountSnapshot() { return baselineBranchBirthCount + branchBirthCount.sum(); }
    public long branchCollapseCountSnapshot() { return baselineBranchCollapseCount + branchCollapseCount.sum(); }
    public Map<String, Long> competitionPressureDistributionSnapshot() { return longSnapshot(competitionPressureDistribution); }
    public java.util.List<String> dynamicNichesSnapshot() { return java.util.List.copyOf(dynamicNiches); }
    public long bifurcationCountSnapshot() { return bifurcationCount.sum(); }
    public Map<String, Long> dynamicNichePopulationSnapshot() { return longSnapshot(dynamicNichePopulation); }
    public long totalRecordedEventsSnapshot() {
        return typeCountsSnapshot().values().stream().mapToLong(Long::longValue).sum();
    }

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


    private void incrementLong(Map<String, LongAdder> store, String key) {
        if (!present(key)) {
            return;
        }
        ensureCapacity(store, key);
        store.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    }

    private void updateArtifactNamespace(long artifactSeed,
                                         String namespace,
                                         Map<Long, String> namespaceByArtifact,
                                         Map<String, LongAdder> populationByNamespace) {
        if (!present(namespace)) {
            return;
        }
        String previous = namespaceByArtifact.put(artifactSeed, namespace);
        if (namespace.equals(previous)) {
            return;
        }
        if (present(previous)) {
            LongAdder previousCount = populationByNamespace.get(previous);
            if (previousCount != null) {
                previousCount.add(-1L);
            }
        }
        incrementLong(populationByNamespace, namespace);
    }

    private Map<String, LongAdder> boundedMapForLineage(String lineage) {
        if (!present(lineage)) {
            return null;
        }
        ensureCapacity(nicheDistributionByLineage, lineage);
        return nicheDistributionByLineage.computeIfAbsent(lineage, ignored -> new ConcurrentHashMap<>());
    }

    private <V> void ensureCapacity(Map<String, V> store, String incomingKey) {
        if (store.containsKey(incomingKey) || store.size() < maxDimensionKeys) {
            return;
        }
        var iterator = store.keySet().iterator();
        if (iterator.hasNext()) {
            store.remove(iterator.next());
        }
    }

    private void addDoubleMetric(Map<String, String> attrs,
                                 String key,
                                 String namespace,
                                 Map<String, DoubleAdder> store,
                                 Map<String, LongAdder> samples) {
        if (!present(namespace)) return;
        double v = parseDouble(attrs.get(key));
        if (Double.isNaN(v)) return;
        ensureCapacity(store, namespace);
        store.computeIfAbsent(namespace, ignored -> new DoubleAdder()).add(v);
        if (samples != null) {
            ensureCapacity(samples, namespace);
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
