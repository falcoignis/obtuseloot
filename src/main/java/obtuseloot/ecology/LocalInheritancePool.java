package obtuseloot.ecology;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.ArtifactStorageKeys;
import obtuseloot.abilities.genome.GenomeRevision;
import obtuseloot.evolution.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;
import java.util.random.RandomGenerator;

/** Recent measured evidence; spatially filtered before scoring, with no global parent fallback. */
public final class LocalInheritancePool {
    private final Map<Identity, Evidence> evidence = new LinkedHashMap<>();
    private final TriggerBudgetResolver budgets = new TriggerBudgetResolver();
    private long gameTick;

    public long gameTick() { return gameTick; }
    public void advanceTick(long windowTicks) {
        gameTick = Math.addExact(gameTick, 1);
        if (gameTick % 100 == 0) prune(windowTicks);
    }
    public void record(AbilityOutcomeObservation observation, long windowTicks) {
        if (!observation.liveMeasured() || observation.context().position() == null) return;
        var artifact = observation.context().artifact();
        if (!ArtifactStorageKeys.isItem(artifact.getArtifactStorageKey())) return;
        ArtifactStorageKeys.itemId(artifact.getArtifactStorageKey());
        var budget = budgets.resolve(observation.definition(), observation.context());
        var result = observation.result();
        var outcome = new UtilityOutcomeRecord(result.abilityId(), result.mechanic(), result.trigger(), result.status(),
                result.outcomeType(), result.meaningfulOutcome(), observation.context().runtimeContext() != null
                && observation.context().runtimeContext().intentional(), 1.0, budget.triggerCost() + budget.evaluationCost(),
                observation.context().source(), gameTick, result.observationKey(), result.performance(), observation.context().genomeRevision());
        var identity = new Identity(artifact.getArtifactStorageKey(), observation.context().genomeRevision());
        Evidence current = evidence.computeIfAbsent(identity, ignored -> new Evidence(identity, artifact.getLatentLineage()));
        current.samples.addLast(new Sample(gameTick, observation.context().position(), outcome, result.holderId(),
                ActionTarget.of(observation.context().liveEvent())));
        prune(windowTicks);
    }
    public List<ParentCandidate> candidates(WorldPosition origin, int radiusChunks, long windowTicks) {
        return candidates(origin, null, radiusChunks, windowTicks);
    }
    public List<ParentCandidate> candidates(WorldPosition origin, UUID holder, int radiusChunks, long windowTicks) {
        prune(windowTicks);
        return selectionCandidates(origin, holder, radiusChunks, windowTicks, candidate -> true);
    }

    /**
     * The exact positive-fitness list {@link #choose} draws from. A null holder, or one without recent measured
     * attempts on any target the competing parents were measured on, keeps pooled fitness. Otherwise each parent's
     * fitness is its per-target fitness weighted by the holder's own recent attempt mix over those targets.
     */
    public List<ParentCandidate> selectionCandidates(WorldPosition origin, UUID holder, int radiusChunks, long windowTicks,
            java.util.function.Predicate<ParentCandidate> currentGenome) {
        var competition = competitiveEvidence(origin, radiusChunks, windowTicks, currentGenome);
        var weighted = holderWeighted(competition, holder, windowTicks);
        return (weighted == null ? competition.stream().map(LocalEvidence::individual) : weighted.stream())
                .filter(candidate -> Double.isFinite(candidate.fitness()) && candidate.fitness() > 0).toList();
    }
    /** The holder's recent measured attempts per action target, from the whole pool inside the outcome window. */
    public Map<String, Long> holderTargetAttempts(UUID holder, long windowTicks) {
        var attempts = new TreeMap<String, Long>();
        if (holder == null) return attempts;
        long cutoff = gameTick - windowTicks;
        for (Evidence parent : evidence.values()) for (Sample sample : parent.samples)
            if (sample.tick > cutoff && holder.equals(sample.holder)) attempts.merge(sample.target, 1L, Long::sum);
        return attempts;
    }
    private List<ParentCandidate> holderWeighted(List<LocalEvidence> competition, UUID holder, long windowTicks) {
        if (holder == null || competition.isEmpty()) return null;
        var measured = new HashSet<String>();
        for (var local : competition) measured.addAll(local.targetFitness().keySet());
        var mix = holderTargetAttempts(holder, windowTicks);
        mix.keySet().retainAll(measured);
        long total = mix.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return null;
        var result = new ArrayList<ParentCandidate>();
        for (var local : competition) {
            var individual = local.individual();
            double fitness = 0;
            for (var entry : mix.entrySet())
                fitness += entry.getValue() / (double) total * local.targetFitness().getOrDefault(entry.getKey(), 0.0);
            result.add(new ParentCandidate(individual.storageKey(), individual.seed(), individual.genomeRevision(),
                    individual.lineageId(), fitness, individual.attempts()));
        }
        return result;
    }
    /** A read-only census, including failures; it never advances time or prunes persisted evidence. */
    public List<LocalEvidence> recentEvidence(WorldPosition origin, int radiusChunks, long windowTicks) {
        return summarize(origin, radiusChunks, windowTicks, sample -> true, identity -> true);
    }

    /** Find the nearest supported measured column before scoring. Failures participate; old genes do not. */
    public List<LocalEvidence> competitiveEvidence(WorldPosition origin, int radiusChunks, long windowTicks,
            java.util.function.Predicate<ParentCandidate> currentGenome) {
        var current = new HashSet<Identity>();
        for (var local : recentEvidence(origin, radiusChunks, windowTicks)) {
            var candidate = local.individual();
            if (currentGenome.test(candidate)) current.add(new Identity(candidate.storageKey(),
                    new GenomeRevision(candidate.seed(), candidate.genomeRevision())));
        }
        double nearest = Double.POSITIVE_INFINITY;
        long cutoff = gameTick - windowTicks;
        for (Evidence parent : evidence.values()) {
            if (!current.contains(parent.identity)) continue;
            for (Sample sample : parent.samples) {
                if (sample.tick <= cutoff || !sample.position.within(origin, radiusChunks)) continue;
                nearest = Math.min(nearest, habitatDistance(origin.habitat(), sample.position.habitat()));
            }
        }
        if (!Double.isFinite(nearest)) return List.of();
        double distance = nearest;
        return summarize(origin, radiusChunks, windowTicks,
                sample -> habitatDistance(origin.habitat(), sample.position.habitat()) == distance, current::contains);
    }
    private static double habitatDistance(HabitatContext target, HabitatContext sample) {
        if (target == null || sample == null) return target == sample ? 0 : Double.POSITIVE_INFINITY;
        return target.distanceTo(sample);
    }
    private List<LocalEvidence> summarize(WorldPosition origin, int radiusChunks, long windowTicks,
            java.util.function.Predicate<Sample> relevant, java.util.function.Predicate<Identity> current) {
        Objects.requireNonNull(origin);
        if (radiusChunks <= 0 || windowTicks <= 0) throw new IllegalArgumentException("Positive radius/window required");
        var result = new ArrayList<LocalEvidence>();
        long cutoff = gameTick - windowTicks;
        for (Evidence parent : evidence.values()) {
            if (!current.test(parent.identity)) continue;
            var profiles = new LinkedHashMap<String, OutcomeUtilityProfile>();
            var performance = new LinkedHashMap<String, PerformanceAccumulator>();
            var targetProfiles = new TreeMap<String, Map<String, OutcomeUtilityProfile>>();
            int attempts = 0;
            for (Sample sample : parent.samples) {
                if (sample.tick <= cutoff || !sample.position.within(origin, radiusChunks) || !relevant.test(sample)) continue;
                String key = sample.outcome.mechanic() + "@" + sample.outcome.trigger();
                profiles.computeIfAbsent(key, OutcomeUtilityProfile::new).ingest(sample.outcome, new ValidatedOutcomeClassifier());
                targetProfiles.computeIfAbsent(sample.target, ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(key, OutcomeUtilityProfile::new).ingest(sample.outcome, new ValidatedOutcomeClassifier());
                if (sample.outcome.performance() != null)
                    performance.computeIfAbsent(key, ignored -> new PerformanceAccumulator()).add(sample.outcome.performance());
                attempts++;
            }
            if (attempts == 0) continue;
            var activities = new LinkedHashMap<String, MechanicUtilitySignal>();
            profiles.forEach((key, profile) -> activities.put(key, profile.snapshot()));
            // Fitness is the total measured utility in the window; an attempt divisor would turn recovery into a benefit.
            double utility = activities.values().stream().mapToDouble(MechanicUtilitySignal::validatedUtility).sum();
            var measured = new LinkedHashMap<String, PerformanceSummary>();
            performance.forEach((key, value) -> measured.put(key, value.snapshot()));
            var targetFitness = new LinkedHashMap<String, Double>();
            targetProfiles.forEach((target, byKey) -> targetFitness.put(target, byKey.values().stream()
                    .mapToDouble(profile -> profile.snapshot().validatedUtility()).sum()));
            result.add(new LocalEvidence(new ParentCandidate(parent.identity.key(), parent.identity.genome().seed(),
                    parent.identity.genome().revision(), parent.lineage, utility, attempts), activities, measured, targetFitness));
        }
        return List.copyOf(result);
    }

    public ParentCandidate choose(WorldPosition origin, int radiusChunks, long windowTicks, RandomGenerator random) {
        return choose(origin, radiusChunks, windowTicks, random, candidate -> true);
    }
    public ParentCandidate choose(WorldPosition origin, int radiusChunks, long windowTicks, RandomGenerator random,
                                  java.util.function.Predicate<ParentCandidate> eligible) {
        return choose(origin, null, radiusChunks, windowTicks, random, eligible);
    }
    public ParentCandidate choose(WorldPosition origin, UUID holder, int radiusChunks, long windowTicks, RandomGenerator random,
                                  java.util.function.Predicate<ParentCandidate> eligible) {
        prune(windowTicks);
        List<ParentCandidate> candidates = selectionCandidates(origin, holder, radiusChunks, windowTicks, eligible);
        double total = candidates.stream().mapToDouble(ParentCandidate::fitness).sum();
        if (candidates.isEmpty()) return null;
        double roll = random.nextDouble() * total;
        for (ParentCandidate candidate : candidates) {
            roll -= candidate.fitness;
            if (roll < 0) return candidate;
        }
        return candidates.getLast();
    }
    public void forget(String key) { evidence.keySet().removeIf(identity -> identity.key().equals(key)); }
    private void prune(long windowTicks) {
        if (windowTicks <= 0) throw new IllegalArgumentException("Outcome window must be positive game ticks");
        long cutoff = gameTick - windowTicks;
        for (Evidence parent : evidence.values()) {
            while (!parent.samples.isEmpty() && parent.samples.getFirst().tick <= cutoff) parent.samples.removeFirst();
        }
        evidence.values().removeIf(parent -> parent.samples.isEmpty());
    }
    public String encode() {
        var yaml = new YamlConfiguration();
        yaml.set("version", 6); yaml.set("game-tick", gameTick); yaml.set("parent-count", evidence.size());
        int index = 0;
        for (Evidence parent : evidence.values()) {
            String base = "parents." + index++ + ".";
            yaml.set(base + "key", parent.identity.key()); yaml.set(base + "seed", parent.identity.genome().seed());
            yaml.set(base + "genome-revision", parent.identity.genome().revision()); yaml.set(base + "lineage", parent.lineage);
            yaml.set(base + "sample-count", parent.samples.size());
            int attempt = 0;
            for (Sample sample : parent.samples) {
                String path = base + "samples." + attempt++ + ".";
                yaml.set(path + "tick", sample.tick); yaml.set(path + "world", sample.position.worldId().toString());
                yaml.set(path + "x", sample.position.x()); yaml.set(path + "z", sample.position.z());
                yaml.set(path + "holder", sample.holder.toString()); yaml.set(path + "target", sample.target);
                var habitat = sample.position.habitat();
                yaml.set(path + "habitat.kind", habitat == null ? "unknown" : habitat.columnHeightBlocks() == null ? "sky" : "roofed");
                if (habitat != null && habitat.columnHeightBlocks() != null) yaml.set(path + "habitat.column-height-blocks", habitat.columnHeightBlocks());
                var outcome = sample.outcome;
                yaml.set(path + "ability", outcome.abilityId()); yaml.set(path + "mechanic", outcome.mechanic().name());
                yaml.set(path + "trigger", outcome.trigger().name()); yaml.set(path + "status", outcome.status().name());
                yaml.set(path + "type", outcome.outcomeType().name()); yaml.set(path + "meaningful", outcome.meaningfulOutcome());
                yaml.set(path + "intentional", outcome.intentional()); yaml.set(path + "budget", outcome.budgetCost());
                yaml.set(path + "source", outcome.source()); yaml.set(path + "observation-key", outcome.observationKey());
                if (outcome.performance() != null) {
                    yaml.set(path + "performance.benefit", outcome.performance().benefit());
                    yaml.set(path + "performance.recovery-ticks", outcome.performance().recoveryTicks());
                }
            }
        }
        return yaml.saveToString();
    }
    public static LocalInheritancePool decode(String encoded) {
        var pool = new LocalInheritancePool();
        if (encoded == null) return pool;
        try {
            var yaml = new YamlConfiguration(); yaml.loadFromString(encoded);
            if (yaml.getInt("version", 0) != 6 || !yaml.isLong("game-tick") && !yaml.isInt("game-tick")) throw new IllegalArgumentException("Invalid ecology version/clock");
            pool.gameTick = yaml.getLong("game-tick");
            if (pool.gameTick < 0) throw new IllegalArgumentException("Negative game tick");
            var parents = yaml.getConfigurationSection("parents");
            int parentCount = parents == null ? 0 : parents.getKeys(false).size();
            if (!yaml.isInt("parent-count") || yaml.getInt("parent-count") != parentCount) throw new IllegalArgumentException("Incomplete ecology parent state");
            for (String name : parents == null ? Set.<String>of() : parents.getKeys(false)) {
                String base = "parents." + name + ".";
                String key = Objects.requireNonNull(yaml.getString(base + "key"));
                ArtifactStorageKeys.itemId(key);
                if (!yaml.isLong(base + "seed") && !yaml.isInt(base + "seed"))
                    throw new IllegalArgumentException("Missing reproductive genome seed");
                long seed = yaml.getLong(base + "seed");
                if (!yaml.isLong(base + "genome-revision") && !yaml.isInt(base + "genome-revision"))
                    throw new IllegalArgumentException("Missing reproductive genome revision");
                var identity = new Identity(key, new GenomeRevision(seed, yaml.getLong(base + "genome-revision")));
                if (pool.evidence.containsKey(identity)) throw new IllegalArgumentException("Duplicate ecology identity");
                var parent = new Evidence(identity, Objects.requireNonNull(yaml.getString(base + "lineage")));
                var samples = Objects.requireNonNull(yaml.getConfigurationSection(base + "samples"));
                if (parent.lineage.isBlank() || !yaml.isInt(base + "sample-count") || yaml.getInt(base + "sample-count") != samples.getKeys(false).size()) throw new IllegalArgumentException("Incomplete ecology evidence");
                long previous = -1;
                for (String item : samples.getKeys(false)) {
                    String path = base + "samples." + item + ".";
                    if (!yaml.isInt(path + "tick") && !yaml.isLong(path + "tick")) throw new IllegalArgumentException("Invalid evidence clock");
                    long tick = yaml.getLong(path + "tick", -1);
                    if (tick < 0 || tick < previous || tick > pool.gameTick) throw new IllegalArgumentException("Invalid evidence time");
                    previous = tick;
                    if (!yaml.isDouble(path + "x") && !yaml.isInt(path + "x") || !yaml.isDouble(path + "z") && !yaml.isInt(path + "z")) throw new IllegalArgumentException("Missing evidence position");
                    String kind = Objects.requireNonNull(yaml.getString(path + "habitat.kind"));
                    HabitatContext habitat = switch (kind) {
                        case "unknown", "sky" -> {
                            if (yaml.contains(path + "habitat.column-height-blocks")) throw new IllegalArgumentException("Unexpected column-height evidence");
                            yield kind.equals("sky") ? new HabitatContext(null) : null;
                        }
                        case "roofed" -> {
                            if (!yaml.isInt(path + "habitat.column-height-blocks")) throw new IllegalArgumentException("Missing column-height evidence");
                            yield new HabitatContext(yaml.getInt(path + "habitat.column-height-blocks"));
                        }
                        default -> throw new IllegalArgumentException("Unknown habitat kind");
                    };
                    var position = new WorldPosition(UUID.fromString(yaml.getString(path + "world")), yaml.getDouble(path + "x"), yaml.getDouble(path + "z"), habitat);
                    var status = AbilityExecutionStatus.valueOf(yaml.getString(path + "status"));
                    if (!yaml.isBoolean(path + "meaningful") || !yaml.isBoolean(path + "intentional")) throw new IllegalArgumentException("Missing outcome flags");
                    boolean meaningful = yaml.getBoolean(path + "meaningful");
                    double budget = yaml.getDouble(path + "budget", Double.NaN);
                    if (!Double.isFinite(budget) || budget < 0 || status != AbilityExecutionStatus.SUCCESS && status != AbilityExecutionStatus.NO_OP
                            || meaningful != (status == AbilityExecutionStatus.SUCCESS)) throw new IllegalArgumentException("Invalid measured evidence");
                    AbilityPerformanceMeasurement performance = null;
                    if (yaml.contains(path + "performance")) {
                        if (!(yaml.get(path + "performance.benefit") instanceof Number benefit)
                                || !(yaml.get(path + "performance.recovery-ticks") instanceof Long
                                || yaml.get(path + "performance.recovery-ticks") instanceof Integer))
                            throw new IllegalArgumentException("Incomplete performance evidence");
                        performance = new AbilityPerformanceMeasurement(benefit.doubleValue(), yaml.getLong(path + "performance.recovery-ticks"));
                        if ((performance.benefit() > 0) != meaningful) throw new IllegalArgumentException("Inconsistent performance evidence");
                    }
                    var outcome = new UtilityOutcomeRecord(Objects.requireNonNull(yaml.getString(path + "ability")),
                            AbilityMechanic.valueOf(yaml.getString(path + "mechanic")), AbilityTrigger.valueOf(yaml.getString(path + "trigger")),
                            status, AbilityOutcomeType.valueOf(yaml.getString(path + "type")), meaningful, yaml.getBoolean(path + "intentional"),
                            1.0, budget, Objects.requireNonNull(yaml.getString(path + "source")), tick, yaml.getString(path + "observation-key"), performance, identity.genome());
                    var holder = UUID.fromString(Objects.requireNonNull(yaml.getString(path + "holder"), "Missing evidence holder"));
                    String target = Objects.requireNonNull(yaml.getString(path + "target"), "Missing action target");
                    if (!ActionTarget.valid(target)) throw new IllegalArgumentException("Invalid action target");
                    parent.samples.addLast(new Sample(tick, position, outcome, holder, target));
                }
                pool.evidence.put(identity, parent);
            }
            return pool;
        } catch (Exception exception) { throw new IllegalStateException("Unreadable local ecology state; refusing a silent reset", exception); }
    }
    public record ParentCandidate(String storageKey, long seed, long genomeRevision, String lineageId, double fitness, int attempts) { }
    public record PerformanceSummary(long samples, double meanBenefit, double meanRecoveryTicks) { }
    /** Target fitness uses the pooled formula restricted to samples measured on each action target. */
    public record LocalEvidence(ParentCandidate individual, Map<String, MechanicUtilitySignal> activities,
                                Map<String, PerformanceSummary> performance, Map<String, Double> targetFitness) {
        public LocalEvidence {
            activities = Collections.unmodifiableMap(new LinkedHashMap<>(activities));
            performance = Collections.unmodifiableMap(new LinkedHashMap<>(performance));
            targetFitness = Collections.unmodifiableMap(new LinkedHashMap<>(targetFitness));
        }
    }
    private static final class PerformanceAccumulator {
        long count; double benefit, recovery;
        void add(AbilityPerformanceMeasurement sample) {
            count++;
            benefit += (sample.benefit() - benefit) / count;
            recovery += (sample.recoveryTicks() - recovery) / count;
        }
        PerformanceSummary snapshot() { return new PerformanceSummary(count, benefit, recovery); }
    }
    private record Identity(String key, GenomeRevision genome) { }
    private record Sample(long tick, WorldPosition position, UtilityOutcomeRecord outcome, UUID holder, String target) {
        Sample { Objects.requireNonNull(holder); Objects.requireNonNull(target); }
    }
    private static final class Evidence {
        final Identity identity; final String lineage; final Deque<Sample> samples = new ArrayDeque<>();
        Evidence(Identity identity, String lineage) { this.identity = identity; this.lineage = lineage; }
    }
}
