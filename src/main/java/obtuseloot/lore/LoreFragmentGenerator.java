package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.names.ArtifactNaming;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.significance.ArtifactSignificanceProfile;
import obtuseloot.significance.ArtifactSignificanceResolver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class LoreFragmentGenerator {
    private static final int MAX_EPITHET_WORDS_PER_SENTENCE = 10;
    private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("(?<=[a-z])(?=[A-Z])");

    private final ArtifactSignificanceResolver significanceResolver = new ArtifactSignificanceResolver();

    public String loreFragment(Artifact artifact) {
        return loreFragment(artifact, null);
    }

    public String loreFragment(Artifact artifact, ArtifactReputation reputation) {
        ArtifactDisposition disposition = ArtifactDisposition.resolve(artifact, reputation);
        ArtifactSignificanceProfile significance = significanceResolver.resolve(artifact);
        List<String> beats = new ArrayList<>();

        String transition = transitionBeat(artifact, disposition);
        if (transition != null) {
            beats.add(transition);
        }

        String identity = identityBeat(artifact, significance, disposition);
        String memory = memoryBeat(artifact, disposition);
        String mergedIdentity = mergeIdentityAndMemory(identity, memory);
        if (mergedIdentity != null) {
            beats.add(mergedIdentity);
        } else if (memory != null) {
            beats.add(memory);
        }

        String state = stateBeat(artifact, significance, disposition);
        if (state != null) {
            beats.add(state);
        }

        if (beats.isEmpty()) {
            return "Its history is still too quiet to retell.";
        }

        if (beats.size() == 1) {
            return beats.getFirst();
        }
        return beats.get(0) + " " + beats.get(1);
    }

    public String epithetFragment(Artifact artifact) {
        return epithetFragment(artifact, null);
    }

    public String epithetFragment(Artifact artifact, ArtifactReputation reputation) {
        ArtifactDisposition disposition = ArtifactDisposition.resolve(artifact, reputation);
        ArtifactNaming naming = Objects.requireNonNull(artifact.getNaming(), "artifact naming");
        int seed = naming.getEpithetSeed();
        List<String> sentences = new ArrayList<>();
        sentences.add(limitWords(epithetLead(artifact, disposition, seed)));
        String closer = epithetCloser(artifact, disposition, seed);
        if (closer != null && !closer.isBlank()) {
            sentences.add(limitWords(closer));
        }
        return String.join(" ", sentences);
    }

    public String driftFragment(Artifact artifact) {
        if (artifact.getDriftLevel() <= 0 && artifact.getTotalDrifts() <= 0) {
            return "It keeps a steady hand.";
        }
        String alignment = naturalize(artifact.getDriftAlignment());
        if (artifact.getDriftLevel() >= 5 || artifact.getTotalDrifts() >= 8) {
            return "Too much drift left it " + alignment + ".";
        }
        return "Drift taught it a " + alignment + " habit.";
    }

    public String awakeningFragment(Artifact artifact) {
        if (!present(artifact.getAwakeningPath()) || "dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            return "Its awakening still sleeps.";
        }
        List<String> anchors = collectNaturalSignals(
                artifact.getAwakeningPath(),
                artifact.getAwakeningIdentityShape(),
                artifact.getAwakeningExpressionTrace(),
                artifact.getAwakeningContinuityTrace());
        if (anchors.isEmpty()) {
            return "It woke into a new discipline.";
        }
        return "Its awakening bent toward " + joinWithAnd(anchors.subList(0, Math.min(2, anchors.size()))) + ".";
    }

    public String lineageFragment(Artifact artifact) {
        List<String> lineage = collectNaturalSignals(
                artifact.getAwakeningLineageTrace(),
                artifact.getConvergenceLineageTrace(),
                artifact.getSpeciesId(),
                artifact.getParentSpeciesId(),
                artifact.getLatentLineage());
        if (lineage.isEmpty()) {
            return "No line has claimed it yet.";
        }
        return "Its line still answers to " + joinWithAnd(lineage.subList(0, Math.min(2, lineage.size()))) + ".";
    }

    public String identifyFragment(Artifact artifact) {
        List<String> identities = collectNaturalSignals(
                artifact.getArchetypePath(),
                artifact.getConvergenceIdentityShape(),
                artifact.getAwakeningIdentityShape(),
                artifact.getLastAbilityBranchPath());
        if (identities.isEmpty()) {
            return "It has not settled into a calling.";
        }
        return "It now carries itself like " + identities.getFirst() + ".";
    }

    public String memoryFragment(Artifact artifact) {
        String event = latestNaturalEvent(artifact);
        if (event == null) {
            return "It remembers in small, private ways.";
        }
        return "It still remembers " + event + ".";
    }

    public String convergenceFragment(Artifact artifact) {
        if (!present(artifact.getConvergencePath()) || "none".equalsIgnoreCase(artifact.getConvergencePath())) {
            return "It has not converged.";
        }
        List<String> anchors = collectNaturalSignals(
                artifact.getConvergencePath(),
                artifact.getConvergenceIdentityShape(),
                artifact.getConvergenceExpressionTrace(),
                artifact.getConvergenceContinuityTrace());
        if (anchors.isEmpty()) {
            return "A second inheritance settled into it.";
        }
        return "Convergence left it speaking in " + joinWithAnd(anchors.subList(0, Math.min(2, anchors.size()))) + ".";
    }

    public String instabilityFragment(Artifact artifact) {
        if (!artifact.hasInstability()) {
            return "Stable.";
        }
        return "Instability still tastes of " + naturalize(artifact.getCurrentInstabilityState()) + ".";
    }

    private String transitionBeat(Artifact artifact, ArtifactDisposition disposition) {
        List<String> beats = new ArrayList<>();
        if (present(artifact.getAwakeningPath()) && !"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            beats.add("awakening");
        }
        if (present(artifact.getConvergencePath()) && !"none".equalsIgnoreCase(artifact.getConvergencePath())) {
            beats.add("convergence");
        }
        if (beats.isEmpty()) {
            return null;
        }

        List<String> anchors = collectNaturalSignals(
                artifact.getAwakeningPath(),
                artifact.getConvergencePath(),
                artifact.getAwakeningLoreTrace(),
                artifact.getConvergenceLoreTrace(),
                artifact.getAwakeningContinuityTrace(),
                artifact.getConvergenceContinuityTrace());
        String anchorText = anchors.isEmpty() ? "its own changing nature" : joinWithAnd(anchors.subList(0, Math.min(2, anchors.size())));
        String verb = switch (disposition.direction()) {
            case BRACING -> "held it toward ";
            case SPLITTING -> "pulled it apart toward ";
            case ASCENDING -> "lifted it toward ";
            default -> "pulled it toward ";
        };
        if (beats.size() == 2) {
            return "Awakening and convergence " + verb + anchorText + ".";
        }
        return capitalize(beats.getFirst()) + " " + verb + anchorText + ".";
    }

    private String memoryBeat(Artifact artifact, ArtifactDisposition disposition) {
        List<String> events = naturalEvents(artifact);
        if (events.isEmpty()) {
            return null;
        }
        if (events.size() >= 2) {
            return "Its retelling " + memoryVerb(disposition) + " on " + events.get(0) + " and " + events.get(1) + ".";
        }
        return "Its retelling " + memoryVerb(disposition) + " on " + events.getFirst() + ".";
    }

    private String identityBeat(Artifact artifact, ArtifactSignificanceProfile significance, ArtifactDisposition disposition) {
        List<String> anchors = collectNaturalSignals(
                significance.functionalIdentity(),
                artifact.getAwakeningIdentityShape(),
                artifact.getConvergenceIdentityShape(),
                artifact.getAwakeningExpressionTrace(),
                artifact.getConvergenceExpressionTrace());
        if (anchors.isEmpty()) {
            return null;
        }
        return "Now it " + identityVerb(disposition) + " as " + joinWithAnd(anchors.subList(0, Math.min(2, anchors.size()))) + ".";
    }


    private String mergeIdentityAndMemory(String identity, String memory) {
        if (identity == null) {
            return null;
        }
        if (memory == null) {
            return identity;
        }
        String identityCore = identity.replaceAll("[.!?]+$", "");
        String memoryCore = memory.replaceFirst("^It still remembers\s+", "")
                .replaceAll("[.!?]+$", "");
        return identityCore + ", still shadowed by " + memoryCore + ".";
    }

    private String stateBeat(Artifact artifact, ArtifactSignificanceProfile significance, ArtifactDisposition disposition) {
        if (artifact.hasInstability()) {
            return disposition.pressure() > 0.75D ? "Even now, it strains at the edges." : "Even now, it shakes at the edges.";
        }
        if (artifact.getHistoryScore() >= 6) {
            return "It wears that past like " + (disposition.direction() == ArtifactDisposition.Direction.BRACING ? "resolve" : "intent") + ".";
        }
        if (present(significance.state()) && !"steady".equalsIgnoreCase(significance.state())) {
            return "It stands " + stateAdverb(disposition) + " " + naturalize(significance.state()) + ".";
        }
        return null;
    }

    private String epithetLead(Artifact artifact, ArtifactDisposition disposition, int seed) {
        List<String> toneOptions = new ArrayList<>();
        if (present(artifact.getConvergenceIdentityShape()) && !"none".equalsIgnoreCase(artifact.getConvergenceIdentityShape())) {
            toneOptions.add(disposition.direction() == ArtifactDisposition.Direction.SPLITTING
                    ? "Twin-made, still pulling against itself."
                    : "Twin-made, never wholly tame.");
            toneOptions.add(disposition.temperament() == ArtifactDisposition.Temperament.RESTRAINED
                    ? "It keeps two inheritances under measured lock."
                    : "It courts two hungers without apology.");
        }
        if (present(artifact.getAwakeningPath()) && !"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            toneOptions.add(disposition.direction() == ArtifactDisposition.Direction.ASCENDING
                    ? "Awake enough to lean into its next shape."
                    : "Awake enough to choose its witness.");
            toneOptions.add(disposition.drive().equals("precision")
                    ? "It learned to seek a sharper line."
                    : "It learned to want a sharper fate.");
        }
        if (artifact.hasInstability()) {
            toneOptions.add(disposition.pressure() > 0.75D ? "Unsteady, still pressing forward." : "Unsteady, but never shy.");
            toneOptions.add(disposition.survivalInstinct() > disposition.aggression()
                    ? "It trembles like something refusing to yield."
                    : "It trembles like a promise kept too long.");
        }
        if (artifact.getSeedMobilityAffinity() >= 0.75D) {
            toneOptions.add(disposition.temperament() == ArtifactDisposition.Temperament.RESTLESS ? "Quick hands keep reaching for it." : "Quick hands lose sleep over it.");
        }
        if (artifact.getSeedBrutalityAffinity() >= 0.75D) {
            toneOptions.add(disposition.temperament() == ArtifactDisposition.Temperament.FORCEFUL ? "It presses for obedience at reach." : "It prefers obedience delivered at reach.");
        }
        if (toneOptions.isEmpty()) {
            toneOptions.add(disposition.temperament() == ArtifactDisposition.Temperament.RESTRAINED ? "It keeps its own measure." : "It keeps its own counsel.");
            toneOptions.add(disposition.pressure() > 0.55D ? "Quiet things still keep pressure." : "Quiet things still leave marks.");
        }
        return toneOptions.get(Math.floorMod(seed, toneOptions.size()));
    }

    private String epithetCloser(Artifact artifact, ArtifactDisposition disposition, int seed) {
        List<String> closers = new ArrayList<>();
        String event = latestNaturalEvent(artifact);
        if (event != null) {
            closers.add((disposition.pressure() > 0.6D ? "It still carries " : "It still tastes of ") + event + ".");
        }
        List<String> identity = collectNaturalSignals(
                artifact.getAwakeningExpressionTrace(),
                artifact.getConvergenceExpressionTrace(),
                artifact.getAwakeningIdentityShape(),
                artifact.getConvergenceIdentityShape(),
                artifact.getLatentLineage());
        if (!identity.isEmpty()) {
            closers.add("Its edge bends toward " + identity.getFirst() + ".");
        }
        if (present(artifact.getConvergencePath()) && !"none".equalsIgnoreCase(artifact.getConvergencePath())) {
            closers.add(disposition.direction() == ArtifactDisposition.Direction.SPLITTING ? "Two inheritances pull beneath the polish." : "Two inheritances settle beneath the polish.");
        }
        if (present(artifact.getAwakeningPath()) && !"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            closers.add(disposition.direction() == ArtifactDisposition.Direction.ASCENDING ? "It remembers the moment it turned upward." : "It remembers the moment it answered back.");
        }
        if (closers.isEmpty()) {
            return null;
        }
        return closers.get(Math.floorMod(seed >>> 3, closers.size()));
    }


    private String memoryVerb(ArtifactDisposition disposition) {
        return switch (disposition.temperament()) {
            case FORCEFUL -> "fixes";
            case WATCHFUL -> "holds";
            case RESTLESS -> "returns";
            case FRACTURED -> "catches";
            case RESTRAINED -> "lingers";
        };
    }

    private String identityVerb(ArtifactDisposition disposition) {
        return switch (disposition.direction()) {
            case ASCENDING -> "leans";
            case SPLITTING -> "divides";
            case BRACING -> "sets itself";
            case DEEPENING, SETTLING -> "answers";
        };
    }

    private String stateAdverb(ArtifactDisposition disposition) {
        return switch (disposition.temperament()) {
            case FORCEFUL -> "openly";
            case WATCHFUL -> "carefully";
            case RESTLESS -> "restlessly";
            case FRACTURED -> "tensely";
            case RESTRAINED -> "quietly";
        };
    }

    private List<String> naturalEvents(Artifact artifact) {
        List<String> events = new ArrayList<>();
        List<String> source = artifact.getNotableEvents();
        for (int i = source.size() - 1; i >= 0 && events.size() < 2; i--) {
            String natural = naturalEvent(source.get(i));
            if (natural != null && !events.contains(natural)) {
                events.add(natural);
            }
        }
        if (events.isEmpty()) {
            List<String> history = artifact.getLoreHistory();
            for (int i = history.size() - 1; i >= 0 && events.size() < 2; i--) {
                String natural = naturalEvent(history.get(i));
                if (natural != null && !events.contains(natural)) {
                    events.add(natural);
                }
            }
        }
        return events;
    }

    private String latestNaturalEvent(Artifact artifact) {
        List<String> events = naturalEvents(artifact);
        return events.isEmpty() ? null : events.getFirst();
    }

    private String naturalEvent(String raw) {
        if (!present(raw)) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.startsWith("awakening.")) {
            return "the waking called " + naturalize(normalized.substring("awakening.".length()));
        }
        if (normalized.startsWith("convergence.variant.")) {
            return "the converged turn toward " + naturalize(normalized.substring("convergence.variant.".length()));
        }
        if (normalized.startsWith("convergence.")) {
            return "the convergence with " + naturalize(normalized.substring("convergence.".length()));
        }
        if (normalized.startsWith("memory.")) {
            return "the memory of " + naturalize(normalized.substring("memory.".length()));
        }
        if (looksDebugLike(normalized)) {
            return null;
        }
        return naturalize(normalized);
    }

    private List<String> collectNaturalSignals(String... values) {
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String natural = naturalSignal(value);
            if (natural != null) {
                unique.add(natural);
            }
        }
        return new ArrayList<>(unique);
    }

    private String naturalSignal(String raw) {
        if (!present(raw)) {
            return null;
        }
        String normalized = raw.trim();
        if ("none".equalsIgnoreCase(normalized)
                || "dormant".equalsIgnoreCase(normalized)
                || "unformed".equalsIgnoreCase(normalized)
                || "base".equalsIgnoreCase(normalized)
                || "common".equalsIgnoreCase(normalized)
                || "unspeciated".equalsIgnoreCase(normalized)
                || "[]".equals(normalized)) {
            return null;
        }
        if (looksDebugLike(normalized)) {
            return null;
        }
        return naturalize(normalized);
    }

    private boolean looksDebugLike(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("trace=")
                || lower.contains("debug")
                || lower.startsWith("artifact-")
                || lower.contains("->")
                || lower.matches(".*:[0-9a-f]{6,}.*");
    }

    private String naturalize(String value) {
        String normalized = CAMEL_CASE_BOUNDARY.matcher(value).replaceAll(" ");
        normalized = normalized.replace('[', ' ').replace(']', ' ')
                .replace('{', ' ').replace('}', ' ')
                .replace('/', ' ').replace(':', ' ')
                .replace('_', ' ').replace('-', ' ').replace('.', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    private String joinWithAnd(List<String> parts) {
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return parts.get(0) + " and " + parts.get(1);
    }

    private String limitWords(String sentence) {
        String normalized = sentence == null ? "" : sentence.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "It keeps its own counsel.";
        }
        String punctuation = normalized.endsWith("!") ? "!" : normalized.endsWith("?") ? "?" : ".";
        String core = normalized.replaceAll("[.!?]+$", "");
        String[] words = core.split(" ");
        if (words.length <= MAX_EPITHET_WORDS_PER_SENTENCE) {
            return core + punctuation;
        }
        return String.join(" ", java.util.Arrays.copyOf(words, MAX_EPITHET_WORDS_PER_SENTENCE)) + punctuation;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String capitalize(String value) {
        if (!present(value)) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
