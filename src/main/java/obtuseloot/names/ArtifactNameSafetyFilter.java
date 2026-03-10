package obtuseloot.names;

import java.util.List;
import java.util.Locale;

public final class ArtifactNameSafetyFilter {
    private static final List<String> BLOCKED = List.of(
            "fuck", "shit", "cunt", "dick", "cock", "pussy", "penis", "vagina", "slut", "whore", "cum", "nude"
    );

    public boolean isSafe(String candidate, double implicationScore) {
        String normalized = candidate.toLowerCase(Locale.ROOT);
        for (String blocked : BLOCKED) {
            if (normalized.contains(blocked)) {
                return false;
            }
        }
        int loaded = countLoadedSignals(normalized);
        if (loaded >= 4 && implicationScore > 0.55D) {
            return false;
        }
        if (normalized.contains("lol") || normalized.contains("69") || normalized.contains("bro")) {
            return false;
        }
        return !normalized.contains("  ");
    }

    private int countLoadedSignals(String normalized) {
        int count = 0;
        for (String token : List.of("hush", "velvet", "mouth", "hunger", "tender", "caress", "mercy", "hollow", "bind", "secret", "ache")) {
            if (normalized.contains(token)) count++;
        }
        return count;
    }
}
