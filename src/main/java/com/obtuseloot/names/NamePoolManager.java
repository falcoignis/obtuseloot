package com.obtuseloot.names;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns persisted name pools under the plugin data folder.
 *
 * <p>On startup, missing files are created from bundled defaults so operators can tune
 * naming without recompiling. Loaded lists are kept as immutable snapshots.
 */
public final class NamePoolManager {
    private static volatile List<String> prefixes = List.of();
    private static volatile List<String> suffixes = List.of();
    private static volatile List<String> generic = List.of();

    private NamePoolManager() {
    }

    public static void initialize(Plugin plugin) {
        Path namesDir = plugin.getDataFolder().toPath().resolve("names");

        try {
            Files.createDirectories(namesDir);
            prefixes = loadOrCreate(namesDir.resolve("prefixes.txt"), Prefixes.get());
            suffixes = loadOrCreate(namesDir.resolve("suffixes.txt"), Suffixes.get());
            generic = loadOrCreate(namesDir.resolve("generic.txt"), Generic.get());
            plugin.getLogger().info("Loaded name pools: " + prefixes.size() + " prefixes, "
                    + suffixes.size() + " suffixes, " + generic.size() + " generic names.");
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to initialize name pool files; using in-memory defaults. Cause: "
                    + exception.getMessage());
            prefixes = Prefixes.get();
            suffixes = Suffixes.get();
            generic = Generic.get();
        }
    }

    public static List<String> prefixes() {
        return prefixes;
    }

    public static List<String> suffixes() {
        return suffixes;
    }

    public static List<String> generic() {
        return generic;
    }

    private static List<String> loadOrCreate(Path file, List<String> defaults) throws IOException {
        if (Files.notExists(file)) {
            Files.write(file, defaults, StandardCharsets.UTF_8);
            return List.copyOf(defaults);
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> cleaned = new ArrayList<>(lines.size());
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                cleaned.add(trimmed);
            }
        }

        if (cleaned.isEmpty()) {
            Files.write(file, defaults, StandardCharsets.UTF_8);
            return List.copyOf(defaults);
        }

        return List.copyOf(cleaned);
    }
}
