package obtuseloot.simulation.worldlab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record EvolutionExperimentConfig(
        SimulationScenario scenario,
        int parallelBatches
) {
    public static EvolutionExperimentConfig defaults(WorldSimulationConfig world) {
        return new EvolutionExperimentConfig(SimulationScenario.defaults(world), 1);
    }

    public static EvolutionExperimentConfig load(Path path, WorldSimulationConfig defaults) {
        if (path == null || !Files.exists(path)) {
            return EvolutionExperimentConfig.defaults(defaults);
        }
        try {
            String raw = Files.readString(path);
            Map<String, String> kv = parseFlat(raw);
            SimulationScenario base = SimulationScenario.defaults(defaults);
            Map<PlayerBehaviorModel, Double> mix = readBehaviorMix(kv, base.behaviorMix());
            SimulationScenario scenario = new SimulationScenario(
                    kv.getOrDefault("name", base.name()),
                    intVal(kv, "artifact_population_size", base.artifactPopulationSize()),
                    intVal(kv, "generations", base.generations()),
                    doubleVal(kv, "mutation_intensity", base.mutationIntensity()),
                    doubleVal(kv, "competition_pressure", base.competitionPressure()),
                    doubleVal(kv, "ecology_sensitivity", base.ecologySensitivity()),
                    doubleVal(kv, "lineage_drift_window", base.lineageDriftWindow()),
                    mix);
            return new EvolutionExperimentConfig(scenario, intVal(kv, "parallel_batches", 1));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read evolution experiment config: " + path, ex);
        }
    }

    private static Map<PlayerBehaviorModel, Double> readBehaviorMix(Map<String, String> kv,
                                                                     Map<PlayerBehaviorModel, Double> defaults) {
        Map<PlayerBehaviorModel, Double> mix = new EnumMap<>(PlayerBehaviorModel.class);
        defaults.forEach(mix::put);
        for (PlayerBehaviorModel model : PlayerBehaviorModel.values()) {
            String key = "player_behavior_mix." + model.name().toLowerCase(Locale.ROOT);
            if (kv.containsKey(key)) {
                mix.put(model, parseDouble(kv.get(key), mix.getOrDefault(model, 0.0D)));
            }
        }
        double sum = mix.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0.0D) {
            return Map.copyOf(defaults);
        }
        Map<PlayerBehaviorModel, Double> normalized = new EnumMap<>(PlayerBehaviorModel.class);
        mix.forEach((k, v) -> normalized.put(k, Math.max(0.0D, v) / sum));
        return Map.copyOf(normalized);
    }

    private static Map<String, String> parseFlat(String raw) {
        String cleaned = raw.replace("{", "\n").replace("}", "\n").replace(",", "\n");
        Map<String, String> out = new HashMap<>();
        for (String line : cleaned.split("\\R")) {
            String t = line.trim();
            if (t.isBlank() || t.startsWith("#")) continue;
            String[] parts = t.split(":", 2);
            if (parts.length != 2) {
                parts = t.split("=", 2);
            }
            if (parts.length != 2) continue;
            String key = unquote(parts[0].trim());
            String value = unquote(parts[1].trim());
            if (!key.isBlank()) {
                out.put(key, value);
            }
        }
        return out;
    }

    private static String unquote(String value) {
        String t = value.trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static int intVal(Map<String, String> kv, String key, int def) {
        return (int) parseDouble(kv.get(key), def);
    }

    private static double doubleVal(Map<String, String> kv, String key, double def) {
        return parseDouble(kv.get(key), def);
    }

    private static double parseDouble(String raw, double def) {
        if (raw == null || raw.isBlank()) return def;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
