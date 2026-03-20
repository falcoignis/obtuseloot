package obtuseloot.significance;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.artifacts.EquipmentRole;
import obtuseloot.evolution.NichePopulationTracker;
import obtuseloot.evolution.NicheVariantProfile;
import obtuseloot.evolution.UtilityHistoryRollup;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ArtifactSignificanceResolver {
    public ArtifactSignificanceProfile resolve(Artifact artifact) {
        Objects.requireNonNull(artifact, "artifact");
        EquipmentArchetype archetype = ArtifactArchetypeValidator.requireValid(artifact, "artifact significance");
        return new ArtifactSignificanceProfile(
                deriveLineage(artifact),
                deriveFunctionalIdentity(artifact, archetype),
                deriveState(artifact),
                deriveDistinctness(artifact),
                deriveAge(artifact)
        );
    }

    private String deriveLineage(Artifact artifact) {
        List<String> parts = new ArrayList<>();
        if (present(artifact.getAwakeningLineageTrace())) {
            parts.add(prettyTrace(artifact.getAwakeningLineageTrace()));
        }
        if (present(artifact.getConvergenceLineageTrace())
                && !equalsNormalized(artifact.getConvergenceLineageTrace(), artifact.getAwakeningLineageTrace())) {
            parts.add(prettyTrace(artifact.getConvergenceLineageTrace()));
        }
        if (present(artifact.getSpeciesId()) && !"unspeciated".equalsIgnoreCase(artifact.getSpeciesId())) {
            parts.add(prettySpecies(artifact.getSpeciesId()));
        } else if (present(artifact.getParentSpeciesId()) && !"none".equalsIgnoreCase(artifact.getParentSpeciesId())) {
            parts.add(prettySpecies(artifact.getParentSpeciesId()));
        }
        if (parts.isEmpty() && present(artifact.getLatentLineage())) {
            parts.add(prettyTrace(artifact.getLatentLineage()));
        }
        if (parts.size() > 1) {
            return parts.get(0) + "-" + parts.get(1) + " line";
        }
        return parts.get(0) + " line";
    }

    private String deriveFunctionalIdentity(Artifact artifact, EquipmentArchetype archetype) {
        String role = functionalRole(archetype);
        String expression = strongestExpression(artifact);
        if (expression != null) {
            return expression + " " + role;
        }
        String affinity = dominantAffinityLabel(artifact);
        return affinity + " " + role;
    }

    private String deriveState(Artifact artifact) {
        UtilityHistoryRollup utility = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
        ArtifactMemoryProfile memory = memoryProfile(artifact);
        if (artifact.hasInstability()) {
            return "unstable";
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) && !"none".equalsIgnoreCase(artifact.getConvergencePath())) {
            return "awakened and converged";
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            return "awakened";
        }
        if (!"none".equalsIgnoreCase(artifact.getConvergencePath())) {
            return "converged";
        }
        if (artifact.getDriftLevel() >= 5 || artifact.getTotalDrifts() >= 8) {
            return "drift-worn";
        }
        if (memory.pressure() >= 8 || utility.hasUtilityHistory() && utility.meaningfulRate() >= 0.6D) {
            return "battle-marked";
        }
        return "steady";
    }

    private String deriveDistinctness(Artifact artifact) {
        UtilityHistoryRollup utility = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
        int signalCount = distinctSignalCount(artifact, utility);
        NichePopulationTracker tracker = tracker();
        NicheVariantProfile variant = tracker == null ? null : tracker.variantFor(artifact.getArtifactSeed());
        double competition = tracker == null ? 1.0D : tracker.nicheCompetitionFactor(artifact.getArtifactSeed());
        String context = variant != null
                ? compactWords(variant.childNiche()) + " offshoot"
                : competition >= 2.2D ? "crowded-context"
                : competition <= 1.15D ? "low-overlap"
                : "cross-current";
        return context + " " + signalCount + "-signal weave";
    }

    private String deriveAge(Artifact artifact) {
        long now = System.currentTimeMillis();
        long continuityAgeMs = Math.max(0L, now - artifact.getPersistenceOriginTimestamp());
        long identityAgeMs = Math.max(0L, now - artifact.getIdentityBirthTimestamp());
        String carried = describeAge("carried", continuityAgeMs);
        if (artifact.getIdentityBirthTimestamp() - artifact.getPersistenceOriginTimestamp() > 60_000L) {
            return "new shape, " + carried;
        }
        if (identityAgeMs < 60_000L) {
            return "just formed";
        }
        return carried;
    }

    private int distinctSignalCount(Artifact artifact, UtilityHistoryRollup utility) {
        int count = 0;
        count += present(artifact.getAwakeningVariantId()) && !"none".equalsIgnoreCase(artifact.getAwakeningVariantId()) ? 1 : 0;
        count += present(artifact.getConvergenceVariantId()) && !"none".equalsIgnoreCase(artifact.getConvergenceVariantId()) ? 1 : 0;
        count += present(artifact.getAwakeningIdentityShape()) && !"none".equalsIgnoreCase(artifact.getAwakeningIdentityShape()) ? 1 : 0;
        count += present(artifact.getConvergenceIdentityShape()) && !"none".equalsIgnoreCase(artifact.getConvergenceIdentityShape()) ? 1 : 0;
        count += present(artifact.getSpeciesId()) && !"unspeciated".equalsIgnoreCase(artifact.getSpeciesId()) ? 1 : 0;
        count += artifact.getAwakeningTraits().size();
        count += Math.min(4, artifact.getNotableEvents().size());
        count += Math.min(3, utility.signalByMechanicTrigger().size());
        count += (int) artifact.getMemory().snapshot().entrySet().stream().filter(entry -> entry.getValue() > 0).count();
        return Math.max(1, count);
    }

    private String strongestExpression(Artifact artifact) {
        for (String value : List.of(
                artifact.getAwakeningExpressionTrace(),
                artifact.getConvergenceExpressionTrace(),
                artifact.getAwakeningIdentityShape(),
                artifact.getConvergenceIdentityShape())) {
            if (present(value) && !"none".equalsIgnoreCase(value)) {
                return compactWords(value);
            }
        }
        return null;
    }

    private String functionalRole(EquipmentArchetype archetype) {
        if (archetype.hasRole(EquipmentRole.TRAVERSAL) || archetype.hasRole(EquipmentRole.MOBILITY)) return "traversal rig";
        if (archetype.hasRole(EquipmentRole.SPEAR)) return "reach spear";
        if (archetype.hasRole(EquipmentRole.RANGED_WEAPON)) return "ranged frame";
        if (archetype.hasRole(EquipmentRole.TOOL_WEAPON_HYBRID)) return "breach axe";
        if (archetype.hasRole(EquipmentRole.MELEE_WEAPON)) return "close blade";
        if (archetype.hasRole(EquipmentRole.HELMET)) return "watch helm";
        if (archetype.hasRole(EquipmentRole.CHESTPLATE)) return "bulwark plate";
        if (archetype.hasRole(EquipmentRole.LEGGINGS)) return "stride greaves";
        if (archetype.hasRole(EquipmentRole.BOOTS)) return "path boots";
        return compactWords(archetype.rootForm().toLowerCase(Locale.ROOT));
    }

    private String dominantAffinityLabel(Artifact artifact) {
        return Map.of(
                        "precision", artifact.getSeedPrecisionAffinity() + artifact.getAwakeningBias("precision"),
                        "brutality", artifact.getSeedBrutalityAffinity() + artifact.getAwakeningBias("brutality"),
                        "survival", artifact.getSeedSurvivalAffinity() + artifact.getAwakeningBias("survival"),
                        "mobility", artifact.getSeedMobilityAffinity() + artifact.getAwakeningBias("mobility"),
                        "chaos", artifact.getSeedChaosAffinity() + artifact.getAwakeningBias("chaos"),
                        "consistency", artifact.getSeedConsistencyAffinity() + artifact.getAwakeningBias("consistency"))
                .entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .map(this::affinityWord)
                .orElse("tempered");
    }

    private String affinityWord(String key) {
        return switch (key) {
            case "precision" -> "keen";
            case "brutality" -> "crushing";
            case "survival" -> "guarding";
            case "mobility" -> "swift";
            case "chaos" -> "volatile";
            case "consistency" -> "steady";
            default -> "tempered";
        };
    }

    private ArtifactMemoryProfile memoryProfile(Artifact artifact) {
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin == null || plugin.getArtifactMemoryEngine() == null) {
            int pressure = artifact.getMemory().pressure();
            return new ArtifactMemoryProfile(pressure, 0, 0, 0, 0, 0, 0, 0);
        }
        return plugin.getArtifactMemoryEngine().profile(artifact);
    }

    private NichePopulationTracker tracker() {
        ObtuseLoot plugin = ObtuseLoot.get();
        return plugin == null || plugin.getArtifactUsageTracker() == null ? null : plugin.getArtifactUsageTracker().nichePopulationTracker();
    }

    private String prettyTrace(String raw) {
        return compactWords(raw)
                .replace(" lineage", "")
                .replace(" trace", "");
    }

    private String prettySpecies(String raw) {
        return compactWords(raw)
                .replace(" species", "")
                .replace(" lineage", "");
    }

    private String compactWords(String raw) {
        String cleaned = raw == null ? "" : raw.replaceAll("[\\[\\]{}()]", " ")
                .replaceAll("[:_/\\\\|]+", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (cleaned.isBlank()) {
            return "formed";
        }
        StringBuilder out = new StringBuilder();
        for (String part : cleaned.split(" ")) {
            if (part.isBlank() || "none".equals(part) || "unassigned".equals(part) || "wild".equals(part)) {
                continue;
            }
            if (!out.isEmpty()) out.append(' ');
            out.append(part.length() <= 2 ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1));
            if (out.length() >= 18) {
                break;
            }
        }
        return out.isEmpty() ? "Formed" : out.toString();
    }

    private String describeAge(String prefix, long ageMs) {
        long minutes = ageMs / 60_000L;
        if (minutes < 60L) return prefix + " " + Math.max(1L, minutes) + "m";
        long hours = minutes / 60L;
        if (hours < 48L) return prefix + " " + hours + "h";
        long days = hours / 24L;
        return prefix + " " + days + "d";
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private boolean equalsNormalized(String left, String right) {
        return compactWords(left).equalsIgnoreCase(compactWords(right));
    }
}
