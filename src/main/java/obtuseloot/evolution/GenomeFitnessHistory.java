package obtuseloot.evolution;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.abilities.genome.GenomeRevision;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Durable measured fitness by genome; lifetime reputation and ability history remain separate. */
public final class GenomeFitnessHistory {
    private final Map<GenomeRevision, Map<String, OutcomeUtilityProfile>> revisions = new LinkedHashMap<>();
    private final ValidatedOutcomeClassifier classifier = new ValidatedOutcomeClassifier();

    public void record(UtilityOutcomeRecord outcome) {
        var revision = Objects.requireNonNull(outcome.genomeRevision(), "Missing genome attribution");
        String key = outcome.mechanic().name() + "@" + outcome.trigger().name();
        revisions.computeIfAbsent(revision, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(key, OutcomeUtilityProfile::new).ingest(outcome, classifier);
    }

    public Set<GenomeRevision> revisions() { return Set.copyOf(revisions.keySet()); }

    public Map<String, MechanicUtilitySignal> signals(GenomeRevision revision) {
        var result = new LinkedHashMap<String, MechanicUtilitySignal>();
        revisions.getOrDefault(revision, Map.of()).forEach((key, profile) -> result.put(key, profile.snapshot()));
        return Collections.unmodifiableMap(result);
    }

    public ArtifactUsageProfile profile(GenomeRevision revision) {
        var result = new ArtifactUsageProfile();
        result.restoreUtilitySignals(signals(revision));
        return result;
    }

    public void writeTo(ConfigurationSection yaml, String path) {
        yaml.set(path + ".revision-count", revisions.size());
        int index = 0;
        for (var entry : revisions.entrySet()) {
            String base = path + ".revisions." + index++;
            yaml.set(base + ".seed", entry.getKey().seed()); yaml.set(base + ".revision", entry.getKey().revision());
            yaml.set(base + ".profile-count", entry.getValue().size());
            int profileIndex = 0;
            for (var profile : entry.getValue().entrySet()) {
                String profilePath = base + ".profiles." + profileIndex++;
                yaml.set(profilePath + ".key", profile.getKey());
                yaml.createSection(profilePath + ".state", profile.getValue().state());
            }
        }
    }

    public static GenomeFitnessHistory readFrom(ConfigurationSection yaml, String path) {
        var result = new GenomeFitnessHistory();
        var list = yaml.getConfigurationSection(path + ".revisions");
        requireCount(yaml, path + ".revision-count", list);
        for (String entry : list == null ? Set.<String>of() : list.getKeys(false)) {
            String base = path + ".revisions." + entry;
            var revision = new GenomeRevision(integer(yaml, base + ".seed"), integer(yaml, base + ".revision"));
            if (result.revisions.containsKey(revision)) throw new IllegalArgumentException("Duplicate genome fitness revision");
            var profiles = Objects.requireNonNull(yaml.getConfigurationSection(base + ".profiles"));
            requireCount(yaml, base + ".profile-count", profiles);
            if (profiles.getKeys(false).isEmpty()) throw new IllegalArgumentException("Empty fitness revision");
            var restored = new LinkedHashMap<String, OutcomeUtilityProfile>();
            for (String name : profiles.getKeys(false)) {
                String profilePath = base + ".profiles." + name;
                String key = Objects.requireNonNull(yaml.getString(profilePath + ".key"));
                String[] parts = key.split("@", -1);
                if (parts.length != 2) throw new IllegalArgumentException("Invalid fitness mechanic key");
                AbilityMechanic.valueOf(parts[0]); AbilityTrigger.valueOf(parts[1]);
                var state = Objects.requireNonNull(yaml.getConfigurationSection(profilePath + ".state"));
                if (restored.put(key, OutcomeUtilityProfile.fromState(key, state.getValues(false))) != null)
                    throw new IllegalArgumentException("Duplicate fitness mechanic");
            }
            result.revisions.put(revision, restored);
        }
        return result;
    }

    private static void requireCount(ConfigurationSection yaml, String path, ConfigurationSection entries) {
        if (!yaml.isInt(path) || yaml.getInt(path) < 0 || yaml.getInt(path) != (entries == null ? 0 : entries.getKeys(false).size()))
            throw new IllegalArgumentException("Incomplete genome fitness history");
    }

    private static long integer(ConfigurationSection yaml, String path) {
        if (!yaml.isInt(path) && !yaml.isLong(path)) throw new IllegalArgumentException("Invalid genome fitness identity");
        return yaml.getLong(path);
    }
}
