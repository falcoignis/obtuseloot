package obtuseloot.analytics.ecosystem;

import java.util.Map;

public record DatasetLayoutDescriptor(
        String sourceKind,
        String schemaVersion,
        Map<String, String> requiredPaths,
        Map<String, String> optionalPaths,
        Map<String, String> layoutHints
) {
}
