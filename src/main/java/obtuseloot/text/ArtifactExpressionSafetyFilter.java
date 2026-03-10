package obtuseloot.text;

import java.util.List;

public class ArtifactExpressionSafetyFilter {
    private static final List<String> BLOCKED = List.of("fuck", "shit", "damn", "sexy", "meme", "lol", "lmao", "sus");

    public String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value;
        for (String blocked : BLOCKED) {
            sanitized = sanitized.replaceAll("(?i)\\b" + blocked + "\\b", "hushed");
        }
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        return sanitized;
    }
}
