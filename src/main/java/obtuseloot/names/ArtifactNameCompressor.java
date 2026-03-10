package obtuseloot.names;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ArtifactNameCompressor {
    private ArtifactNameCompressor() {
    }

    public static String compress(String raw, int maxWords) {
        String[] words = raw.trim().replaceAll("\\s+", " ").split(" ");
        Set<String> unique = new LinkedHashSet<>();
        for (String word : words) {
            if (!word.isBlank()) {
                unique.add(word);
            }
            if (unique.size() >= maxWords) {
                break;
            }
        }
        return String.join(" ", unique);
    }
}
