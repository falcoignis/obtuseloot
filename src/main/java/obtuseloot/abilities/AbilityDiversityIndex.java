package obtuseloot.abilities;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.MechanicNicheTag;
import obtuseloot.evolution.NicheVariantProfile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public final class AbilityDiversityIndex {
    private static final int RECENT_SIGNATURE_LIMIT = 256;
    private static final AbilityDiversityIndex INSTANCE = new AbilityDiversityIndex();
    private final Deque<AbilitySignature> recent = new ConcurrentLinkedDeque<>();

    public static AbilityDiversityIndex instance() {
        return INSTANCE;
    }

    public List<AbilitySignature> activePool(long excludeSeed) {
        List<AbilitySignature> out = new ArrayList<>();
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin != null && plugin.getArtifactManager() != null) {
            for (Artifact artifact : plugin.getArtifactManager().getLoadedArtifacts().values()) {
                if (artifact == null || artifact.getArtifactSeed() == excludeSeed) {
                    continue;
                }
                out.addAll(fromArtifact(artifact));
            }
        }
        for (AbilitySignature signature : recent) {
            if (signature.artifactSeed() != excludeSeed) {
                out.add(signature);
            }
        }
        return out;
    }

    public void record(long artifactSeed,
                       String lineageId,
                       MechanicNicheTag niche,
                       NicheVariantProfile variant,
                       Collection<AbilityTemplate> templates) {
        if (templates == null) {
            return;
        }
        for (AbilityTemplate template : templates) {
            push(fromTemplate(artifactSeed, lineageId, niche, variant, template));
        }
    }

    public void recordDefinitions(long artifactSeed,
                                  String lineageId,
                                  MechanicNicheTag niche,
                                  NicheVariantProfile variant,
                                  Collection<AbilityDefinition> definitions) {
        if (definitions == null) {
            return;
        }
        for (AbilityDefinition definition : definitions) {
            push(fromDefinition(artifactSeed, lineageId, niche, variant, definition));
        }
    }

    public AbilitySignature motifAnchor(List<AbilitySignature> activePool,
                                        MechanicNicheTag niche,
                                        String lineageId,
                                        NicheVariantProfile variant) {
        return activePool.stream()
                .filter(sig -> niche == null || sig.niche() == niche)
                .filter(sig -> lineageId == null || lineageId.equals(sig.lineageId()))
                .filter(sig -> variant == null || Objects.equals(sig.variantAlpha(), variant.isAlphaVariant()))
                .max(Comparator.comparingDouble(AbilitySignature::cohesionScore))
                .orElse(activePool.stream()
                        .filter(sig -> niche == null || sig.niche() == niche)
                        .max(Comparator.comparingDouble(AbilitySignature::cohesionScore))
                        .orElse(null));
    }

    private void push(AbilitySignature signature) {
        recent.addLast(signature);
        while (recent.size() > RECENT_SIGNATURE_LIMIT) {
            recent.pollFirst();
        }
    }

    private List<AbilitySignature> fromArtifact(Artifact artifact) {
        List<AbilitySignature> out = new ArrayList<>();
        List<String> triggers = csv(artifact.getLastTriggerProfile());
        List<String> mechanics = csv(artifact.getLastMechanicProfile());
        List<String> branches = csvish(artifact.getLastAbilityBranchPath());
        int count = Math.max(branches.size(), Math.max(triggers.size(), mechanics.size()));
        for (int i = 0; i < count; i++) {
            String branch = i < branches.size() ? branches.get(i) : "artifact.unknown";
            String id = branch.contains("->") ? branch.substring(0, branch.indexOf("->")) : branch;
            AbilityTrigger trigger = parseTrigger(i < triggers.size() ? triggers.get(i) : "ON_WORLD_SCAN");
            AbilityMechanic mechanic = parseMechanic(i < mechanics.size() ? mechanics.get(i) : "PULSE");
            Set<String> effectTokens = tokenSet(id + " " + branch);
            out.add(new AbilitySignature(artifact.getArtifactSeed(), artifact.getLatentLineage(), null, null,
                    categoryForId(id), familyForId(id), trigger, mechanic, effectTokens, effectTokens, new double[6], id));
        }
        return out;
    }

    public AbilitySignature fromTemplate(long artifactSeed, String lineageId, MechanicNicheTag niche, NicheVariantProfile variant, AbilityTemplate template) {
        return new AbilitySignature(artifactSeed, lineageId, niche, variant == null ? null : variant.isAlphaVariant(),
                template.category(), template.family(), template.trigger(), template.mechanic(),
                tokenSet(template.effectPattern() + " " + String.join(" ", template.metadata().utilityDomains())),
                mechanicSet(template),
                statVector(template.metadata()), template.id());
    }

    public AbilitySignature fromDefinition(long artifactSeed, String lineageId, MechanicNicheTag niche, NicheVariantProfile variant, AbilityDefinition definition) {
        return new AbilitySignature(artifactSeed, lineageId, niche, variant == null ? null : variant.isAlphaVariant(),
                categoryForId(definition.id()), definition.family(), definition.trigger(), definition.mechanic(),
                tokenSet(definition.effectPattern()),
                new LinkedHashSet<>(definition.metadata() == null ? Set.of() : definition.metadata().affinities()),
                definition.metadata() == null ? new double[6] : statVector(definition.metadata()), definition.id());
    }

    private static Set<String> mechanicSet(AbilityTemplate template) {
        Set<String> out = new LinkedHashSet<>();
        out.add(template.mechanic().name().toLowerCase(Locale.ROOT));
        out.addAll(template.metadata().affinities());
        out.addAll(template.metadata().triggerClasses());
        return out;
    }

    public static double similarity(AbilitySignature a, AbilitySignature b) {
        double family = a.family() == b.family() ? 1.0D : 0.0D;
        double category = a.category() == b.category() ? 1.0D : 0.0D;
        double trigger = a.trigger() == b.trigger() ? 1.0D : 0.0D;
        double mechanic = a.mechanic() == b.mechanic() ? 1.0D : 0.0D;
        double effects = jaccard(a.effectTokens(), b.effectTokens());
        double signature = jaccard(a.mechanicSignature(), b.mechanicSignature());
        double stats = 1.0D - normalizedDistance(a.statVector(), b.statVector());
        return (category * 0.08D)
                + (family * 0.05D)
                + (trigger * 0.09D)
                + (mechanic * 0.11D)
                + (effects * 0.20D)
                + (signature * 0.16D)
                + (stats * 0.13D);
    }

    private static double normalizedDistance(double[] a, double[] b) {
        double sum = 0.0D;
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            double av = i < a.length ? a[i] : 0.0D;
            double bv = i < b.length ? b[i] : 0.0D;
            sum += Math.abs(av - bv);
        }
        return Math.max(0.0D, Math.min(1.0D, sum / Math.max(1.0D, n)));
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0D;
        }
        long intersection = a.stream().filter(b::contains).count();
        long union = new LinkedHashSet<String>() {{ addAll(a); addAll(b); }}.size();
        return union == 0 ? 0.0D : intersection / (double) union;
    }

    private static double[] statVector(AbilityMetadata metadata) {
        return new double[]{metadata.discoveryValue(), metadata.explorationValue(), metadata.informationValue(), metadata.ritualValue(), metadata.socialValue(), metadata.worldUtilityValue()};
    }

    private static Set<String> tokenSet(String input) {
        return java.util.Arrays.stream((input == null ? "" : input).toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<String> csv(String input) {
        if (input == null || input.isBlank()) return List.of();
        return java.util.Arrays.stream(input.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private static List<String> csvish(String input) {
        if (input == null || input.isBlank()) return List.of();
        String cleaned = input.replace("[", "").replace("]", "");
        return csv(cleaned);
    }

    private static AbilityFamily familyForId(String id) {
        String prefix = id.contains(".") ? id.substring(0, id.indexOf('.')) : id;
        return switch (prefix) {
            case "precision" -> AbilityFamily.PRECISION;
            case "brutality" -> AbilityFamily.BRUTALITY;
            case "survival", "gathering" -> AbilityFamily.SURVIVAL;
            case "mobility", "exploration" -> AbilityFamily.MOBILITY;
            case "chaos", "ritual", "environment" -> AbilityFamily.CHAOS;
            case "consistency", "social" -> AbilityFamily.CONSISTENCY;
            default -> AbilityFamily.CONSISTENCY;
        };
    }

    private static AbilityCategory categoryForId(String id) {
        String prefix = id.contains(".") ? id.substring(0, id.indexOf('.')) : id;
        return switch (prefix) {
            case "precision", "sensing" -> AbilityCategory.SENSING_INFORMATION;
            case "mobility", "exploration" -> AbilityCategory.TRAVERSAL_MOBILITY;
            case "survival", "environment" -> AbilityCategory.SURVIVAL_ADAPTATION;
            case "tactical" -> AbilityCategory.COMBAT_TACTICAL_CONTROL;
            case "warding", "consistency" -> AbilityCategory.DEFENSE_WARDING;
            case "gathering", "logistics" -> AbilityCategory.RESOURCE_FARMING_LOGISTICS;
            case "engineering" -> AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION;
            case "social", "support" -> AbilityCategory.SOCIAL_SUPPORT_COORDINATION;
            case "ritual", "chaos", "evolution" -> AbilityCategory.RITUAL_STRANGE_UTILITY;
            case "stealth" -> AbilityCategory.STEALTH_TRICKERY_DISRUPTION;
            default -> AbilityCategory.SENSING_INFORMATION;
        };
    }

    private static AbilityTrigger parseTrigger(String input) { try { return AbilityTrigger.valueOf(input); } catch (Exception ex) { return AbilityTrigger.ON_WORLD_SCAN; } }
    private static AbilityMechanic parseMechanic(String input) { try { return AbilityMechanic.valueOf(input); } catch (Exception ex) { return AbilityMechanic.PULSE; } }

    public record AbilitySignature(long artifactSeed, String lineageId, MechanicNicheTag niche, Boolean variantAlpha,
                                   AbilityCategory category,
                                   AbilityFamily family, AbilityTrigger trigger, AbilityMechanic mechanic,
                                   Set<String> effectTokens, Set<String> mechanicSignature, double[] statVector, String sourceId) {
        public double cohesionScore() {
            return (mechanicSignature.size() * 0.08D) + effectTokens.size() * 0.04D + (family == AbilityFamily.CHAOS ? 0.05D : 0.0D)
                    + (category == AbilityCategory.RITUAL_STRANGE_UTILITY || category == AbilityCategory.STEALTH_TRICKERY_DISRUPTION ? 0.03D : 0.0D);
        }
    }
}
