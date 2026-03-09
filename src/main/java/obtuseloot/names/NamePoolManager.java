package obtuseloot.names;

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
    public static final String POOL_PREFIXES = "prefixes";
    public static final String POOL_SUFFIXES = "suffixes";
    public static final String POOL_GENERIC = "generic";

    private static volatile List<String> prefixes = List.of();
    private static volatile List<String> suffixes = List.of();
    private static volatile List<String> generic = List.of();
    private static volatile Path namesDirectory;

    private NamePoolManager() {
    }

    public static void initialize(Plugin plugin) {
        Path namesDir = plugin.getDataFolder().toPath().resolve("names");
        namesDirectory = namesDir;

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

    public static List<String> allPoolNames() {
        return List.of(POOL_PREFIXES, POOL_SUFFIXES, POOL_GENERIC);
    }

    public static List<String> getPool(String pool) {
        return switch (normalizePool(pool)) {
            case POOL_PREFIXES -> prefixes;
            case POOL_SUFFIXES -> suffixes;
            case POOL_GENERIC -> generic;
            default -> List.of();
        };
    }

    public static boolean contains(String pool, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim();
        for (String current : getPool(pool)) {
            if (current.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static boolean addName(String pool, String value) throws IOException {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalizedPool = normalizePool(pool);
        if (normalizedPool == null || contains(normalizedPool, value)) {
            return false;
        }

        List<String> updated = new ArrayList<>(getPool(normalizedPool));
        updated.add(value.trim());
        persistPool(normalizedPool, updated);
        updateInMemoryPool(normalizedPool, updated);
        return true;
    }

    public static boolean removeName(String pool, String value) throws IOException {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalizedPool = normalizePool(pool);
        if (normalizedPool == null) {
            return false;
        }

        List<String> current = new ArrayList<>(getPool(normalizedPool));
        String normalizedValue = value.trim();
        String matched = null;
        for (String candidate : current) {
            if (candidate.equalsIgnoreCase(normalizedValue)) {
                matched = candidate;
                break;
            }
        }

        if (matched == null || current.size() <= 1) {
            return false;
        }

        current.remove(matched);
        persistPool(normalizedPool, current);
        updateInMemoryPool(normalizedPool, current);
        return true;
    }

    public static String normalizePool(String pool) {
        if (pool == null) {
            return null;
        }

        String normalized = pool.trim().toLowerCase();
        if (POOL_PREFIXES.equals(normalized) || POOL_SUFFIXES.equals(normalized) || POOL_GENERIC.equals(normalized)) {
            return normalized;
        }

        return null;
    }

    private static void persistPool(String pool, List<String> values) throws IOException {
        if (namesDirectory == null) {
            throw new IOException("Name pool directory has not been initialized yet.");
        }

        Files.write(namesDirectory.resolve(pool + ".txt"), values, StandardCharsets.UTF_8);
    }

    private static void updateInMemoryPool(String pool, List<String> values) {
        List<String> immutable = List.copyOf(values);
        switch (pool) {
            case POOL_PREFIXES -> prefixes = immutable;
            case POOL_SUFFIXES -> suffixes = immutable;
            case POOL_GENERIC -> generic = immutable;
            default -> {
            }
        }
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
