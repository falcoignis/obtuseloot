package obtuseloot.text;

import java.util.HashSet;
import java.util.Set;

public class ArtifactTextToneValidator {
    public String validate(ArtifactTextChannel channel, ArtifactTextIdentity identity, String text, int maxWords) {
        if (text == null || text.isBlank()) {
            return fallback(channel, identity);
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        normalized = compressRepeatedMotifs(normalized, identity);
        String[] words = normalized.split(" ");
        if (words.length > maxWords) {
            normalized = String.join(" ", java.util.Arrays.copyOf(words, maxWords));
        }
        if (channel == ArtifactTextChannel.NAME && normalized.length() > 48) {
            normalized = normalized.substring(0, 48).trim();
        }
        return normalized;
    }

    private String compressRepeatedMotifs(String text, ArtifactTextIdentity identity) {
        Set<String> seen = new HashSet<>();
        StringBuilder out = new StringBuilder();
        for (String token : text.split(" ")) {
            String clean = token.toLowerCase().replaceAll("[^a-z-]", "");
            boolean motif = identity.motifs().contains(clean);
            if (motif && !seen.add(clean)) {
                continue;
            }
            if (!out.isEmpty()) out.append(' ');
            out.append(token);
        }
        return out.toString();
    }

    private String fallback(ArtifactTextChannel channel, ArtifactTextIdentity identity) {
        return switch (channel) {
            case NAME -> "Veiled Artifact";
            case LORE -> "It keeps a careful silence.";
            case IDENTIFY -> "Its meaning opens by degrees.";
            case AWAKENING -> "Its name returns with " + identity.motifs().getFirst() + ".";
            case MEMORY -> "It remembers what others let fade.";
            case LINEAGE -> "Its making belongs to an older vow.";
            case DRIFT -> "It leans toward " + identity.motifs().getFirst() + ".";
            case FUSION -> "Two intentions now share one edge.";
            case DISCOVERY, EVENT -> "Something in it is newly known.";
        };
    }
}
