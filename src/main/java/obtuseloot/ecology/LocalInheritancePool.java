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
        current.samples.addLast(new Sample(gameTick, observation.context().position(), outcome));
        prune(windowTicks);
    }
    public List<ParentCandidate> candidates(WorldPosition origin, int radiusChunks, long windowTicks) {
        prune(windowTicks);
        var candidates = new ArrayList<ParentCandidate>();
        for (LocalEvidence local : recentEvidence(origin, radiusChunks, windowTicks)) {
            var candidate = local.individual();
            if (Double.isFinite(candidate.fitness()) && candidate.fitness() > 0) candidates.add(candidate);
        }
        return List.copyOf(candidates);
    }
    /** A read-only census, including failures; it never advances time or prunes persisted evidence. */
    public List<LocalEvidence> recentEvidence(WorldPosition origin, int radiusChunks, long windowTicks) {
        Objects.requireNonNull(origin);
        if (radiusChunks <= 0 || windowTicks <= 0) throw new IllegalArgumentException("Positive radius/window required");
        var result = new ArrayList<LocalEvidence>();
        long cutoff = gameTick - windowTicks;
        for (Evidence parent : evidence.values()) {
            var profiles = new LinkedHashMap<String, OutcomeUtilityProfile>();
            var performance = new LinkedHashMap<String, PerformanceAccumulator>();
            int attempts = 0;
            for (Sample sample : parent.samples) {
                if (sample.tick <= cutoff || !sample.position.within(origin, radiusChunks)) continue;
                String key = sample.outcome.mechanic() + "@" + sample.outcome.trigger();
                profiles.computeIfAbsent(key, OutcomeUtilityProfile::new).ingest(sample.outcome, new ValidatedOutcomeClassifier());
                if (sample.outcome.performance() != null)
                    performance.computeIfAbsent(key, ignored -> new PerformanceAccumulator()).add(sample.outcome.performance());
                attempts++;
            }
            if (attempts == 0) continue;
            var activities = new LinkedHashMap<String, MechanicUtilitySignal>();
            profiles.forEach((key, profile) -> activities.put(key, profile.snapshot()));
            double utility = activities.values().stream().mapToDouble(MechanicUtilitySignal::validatedUtility).sum();
            var measured = new LinkedHashMap<String, PerformanceSummary>();
            performance.forEach((key, value) -> measured.put(key, value.snapshot()));
            result.add(new LocalEvidence(new ParentCandidate(parent.identity.key(), parent.identity.genome().seed(),
                    parent.identity.genome().revision(), parent.lineage, utility / attempts, attempts), activities, measured));
        }
        return List.copyOf(result);
    }

    public ParentCandidate choose(WorldPosition origin, int radiusChunks, long windowTicks, RandomGenerator random) {
        return choose(origin, radiusChunks, windowTicks, random, candidate -> true);
    }
    public ParentCandidate choose(WorldPosition origin, int radiusChunks, long windowTicks, RandomGenerator random,
                                  java.util.function.Predicate<ParentCandidate> eligible) {
        List<ParentCandidate> candidates = candidates(origin, radiusChunks, windowTicks).stream().filter(eligible).toList();
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
        yaml.set("version", 3); yaml.set("game-tick", gameTick); yaml.set("parent-count", evidence.size());
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
            if (yaml.getInt("version", 0) != 3 || !yaml.isLong("game-tick") && !yaml.isInt("game-tick")) throw new IllegalArgumentException("Invalid ecology version/clock");
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
                    var position = new WorldPosition(UUID.fromString(yaml.getString(path + "world")), yaml.getDouble(path + "x"), yaml.getDouble(path + "z"));
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
                    parent.samples.addLast(new Sample(tick, position, outcome));
                }
                pool.evidence.put(identity, parent);
            }
            return pool;
        } catch (Exception exception) { throw new IllegalStateException("Unreadable local ecology state; refusing a silent reset", exception); }
    }
    public record ParentCandidate(String storageKey, long seed, long genomeRevision, String lineageId, double fitness, int attempts) { }
    public record PerformanceSummary(long samples, double meanBenefit, double meanRecoveryTicks) { }
    public record LocalEvidence(ParentCandidate individual, Map<String, MechanicUtilitySignal> activities,
                                Map<String, PerformanceSummary> performance) {
        public LocalEvidence {
            activities = Collections.unmodifiableMap(new LinkedHashMap<>(activities));
            performance = Collections.unmodifiableMap(new LinkedHashMap<>(performance));
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
    private record Sample(long tick, WorldPosition position, UtilityOutcomeRecord outcome) { }
    private static final class Evidence {
        final Identity identity; final String lineage; final Deque<Sample> samples = new ArrayDeque<>();
        Evidence(Identity identity, String lineage) { this.identity = identity; this.lineage = lineage; }
    }
}
