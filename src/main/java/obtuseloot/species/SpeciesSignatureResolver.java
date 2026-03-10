package obtuseloot.species;

import obtuseloot.artifacts.Artifact;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpeciesSignatureResolver {
    public ArtifactPopulationSignature fromArtifact(Artifact artifact) {
        return new ArtifactPopulationSignature(
                Map.of(
                        "precision", artifact.getSeedPrecisionAffinity(),
                        "brutality", artifact.getSeedBrutalityAffinity(),
                        "survival", artifact.getSeedSurvivalAffinity(),
                        "mobility", artifact.getSeedMobilityAffinity(),
                        "chaos", artifact.getSeedChaosAffinity(),
                        "consistency", artifact.getSeedConsistencyAffinity()),
                parseListProfile(artifact.getLastAbilityBranchPath()),
                parseCsvProfile(artifact.getLastTriggerProfile()),
                parseCsvProfile(artifact.getLastMechanicProfile()),
                parseCsvProfile(artifact.getLastOpenRegulatoryGates()),
                parseEnvironmentProfile(artifact.getLastMemoryInfluence())
        );
    }

    private Map<String, Double> parseCsvProfile(String value) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (value == null || value.isBlank()) {
            return out;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .forEach(v -> out.merge(v, 1.0D, Double::sum));
        return out;
    }

    private Map<String, Double> parseListProfile(String value) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (value == null || value.isBlank()) {
            return out;
        }
        String cleaned = value.replace("[", "").replace("]", "");
        Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .forEach(v -> {
                    String branch = v.contains("->") ? v.substring(v.indexOf("->") + 2) : v;
                    out.merge(branch, 1.0D, Double::sum);
                });
        return out;
    }

    private Map<String, Double> parseEnvironmentProfile(String memoryInfluence) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (memoryInfluence == null || memoryInfluence.isBlank()) {
            return out;
        }
        for (String token : memoryInfluence.split(",")) {
            String trimmed = token.trim();
            if (trimmed.startsWith("pressure=")) {
                out.put("pressure", parseDouble(trimmed.substring("pressure=".length())));
            } else if (trimmed.startsWith("chaos=")) {
                out.put("chaos", parseDouble(trimmed.substring("chaos=".length())));
            } else if (trimmed.startsWith("survival=")) {
                out.put("survival", parseDouble(trimmed.substring("survival=".length())));
            } else if (trimmed.startsWith("mobility=")) {
                out.put("mobility", parseDouble(trimmed.substring("mobility=".length())));
            }
        }
        return out;
    }

    private double parseDouble(String raw) {
        try {
            return Math.max(0.0D, Double.parseDouble(raw));
        } catch (NumberFormatException ex) {
            return 0.0D;
        }
    }
}
