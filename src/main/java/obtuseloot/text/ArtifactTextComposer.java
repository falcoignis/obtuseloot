package obtuseloot.text;

import obtuseloot.artifacts.Artifact;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.names.ArtifactNameCompressor;

import java.util.List;
import java.util.Locale;

public class ArtifactTextComposer {
    private final ArtifactExpressionSafetyFilter safetyFilter = new ArtifactExpressionSafetyFilter();
    private final ArtifactTextToneValidator validator = new ArtifactTextToneValidator();

    public String compose(Artifact artifact, ArtifactTextIdentity identity, ArtifactTextChannel channel, String context) {
        int maxWords = maxWords(channel);
        String raw = switch (channel) {
            case NAME -> name(identity, context);
            case LORE -> lore(identity);
            case IDENTIFY -> identify(identity);
            case AWAKENING -> awakening(identity, context);
            case LINEAGE -> lineage(identity, context);
            case MEMORY -> memory(identity, context);
            case DRIFT -> drift(identity, context);
            case CONVERGENCE -> convergence(identity, context);
            case DISCOVERY -> discovery(identity);
            case EVENT -> event(identity, context);
        };
        String safe = safetyFilter.sanitize(raw);
        String bounded = validator.validate(channel, identity, safe, maxWords);
        return channel == ArtifactTextChannel.NAME
                ? ArtifactNameCompressor.compress(bounded, maxWords)
                : bounded;
    }

    private String name(ArtifactTextIdentity identity, String context) {
        String motif = cap(stableNameMotif(identity));
        String root = cap(context);
        String modifier = suggestiveModifier(identity);
        return switch (identity.namingArchetype()) {
            case TRAIT_FORM -> joinNonBlank(modifier, motif, root);
            case COMPACT_COMPOUND -> joinNonBlank(modifier, motif + cap(lastMotif(identity)));
            case FORM_OF_CONCEPT -> joinNonBlank(modifier, root, "of", motif);
            case TRUE_NAME_ONLY -> joinNonBlank(modifier, motif);
            case TRUE_NAME_WITH_EPITHET -> joinNonBlank(modifier, motif + ",", root, "of", cap(lastMotif(identity)));
            case TRUE_NAME_WITH_TITLE -> joinNonBlank(modifier, motif, "the", cap(lastMotif(identity)), root);
        };
    }

    private String lore(ArtifactTextIdentity identity) {
        return twoBeat(identity, "lore",
                List.of(
                        "It keeps " + article(lastMotif(identity)) + " " + lastMotif(identity) + " behind a careful hush.",
                        "Its voice stays " + identity.cadence() + ", with " + identity.motifs().getFirst() + " at the edges.",
                        "It does not rush what it means to keep."
                ),
                signalDriven(identity, "lore"));
    }

    private String identify(ArtifactTextIdentity identity) {
        if (identity.discoveryState().ordinal() < 2) {
            return "Its intent is felt before it is known.";
        }
        return "It answers as " + identity.voice().replace('-', ' ') + ", carrying " + lastMotif(identity) + ".";
    }

    private String awakening(ArtifactTextIdentity identity, String context) {
        List<String> base = List.of(
                "Its name returns with " + identity.motifs().getFirst() + ".",
                "What was hidden now answers in " + lastMotif(identity) + ".",
                "It is no longer content to remain quiet."
        );
        return twoBeat(identity, context == null ? "awakening" : context,
                base,
                signalDriven(identity, "awakening"));
    }

    private String lineage(ArtifactTextIdentity identity, String lineage) {
        String source = lineage == null || lineage.isBlank() ? "an older house" : lineage;
        String branchHint = "";
        if (source.contains("[") && source.contains("]")) {
            int start = source.indexOf("[");
            int end = source.indexOf("]", start);
            if (end > start) {
                String branch = source.substring(start + 1, end);
                source = source.substring(0, start).trim();
                branchHint = " The " + branch + " branch keeps " + lastMotif(identity) + " close.";
            }
        }
        return "Its making belongs to " + source + ", where craft ran toward " + identity.motifs().getFirst() + "." + branchHint;
    }

    private String memory(ArtifactTextIdentity identity, String event) {
        String anchor = event == null || event.isBlank() ? "a vanished moment" : event.replace('_', ' ');
        return twoBeat(identity, anchor,
                List.of("It remembers " + anchor + " not as record, but as " + identity.motifs().getFirst() + "."),
                signalDriven(identity, "memory"));
    }

    private String drift(ArtifactTextIdentity identity, String drift) {
        String direction = drift == null || drift.isBlank() ? lastMotif(identity) : drift.toLowerCase(Locale.ROOT);
        return twoBeat(identity, direction,
                List.of("It leans toward " + direction + ", slowly and without apology."),
                signalDriven(identity, "drift"));
    }

    private String convergence(ArtifactTextIdentity identity, String path) {
        String convergencePath = path == null || path.isBlank() ? "a second inheritance" : path;
        return twoBeat(identity, convergencePath,
                List.of("Two intentions now share one edge: " + convergencePath + "."),
                signalDriven(identity, "convergence"));
    }

    private String discovery(ArtifactTextIdentity identity) {
        return twoBeat(identity, "discovery",
                List.of("It opens only by degrees, keeping " + identity.motifs().getFirst() + " close."),
                signalDriven(identity, "discovery"));
    }

    private String event(ArtifactTextIdentity identity, String eventId) {
        String marker = eventId == null ? "the passing" : eventId.replace('.', ' ');
        return twoBeat(identity, marker,
                List.of("Something in it marked " + marker + " and kept the afterimage."),
                signalDriven(identity, "event"));
    }

    private List<String> signalDriven(ArtifactTextIdentity identity, String salt) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (identity.signalTags().contains("convergence")) {
            lines.add("It keeps both inheritances near the same hunger.");
            lines.add("Neither half yields; each draws the other nearer.");
            lines.add("A second claim settles in without asking leave.");
        }
        if (identity.signalTags().contains("awakening")) {
            lines.add("What it held back now presses against the hush.");
            lines.add("Release suits it like a vow finally loosened.");
            lines.add("It wears becoming like restraint gone warm.");
        }
        if (identity.signalTags().contains("instability")) {
            lines.add("Its grip slips just enough to feel dangerous.");
            lines.add("Control trembles, but does not quite break.");
            lines.add("Strain leaves the edge eager for another test.");
        }
        if (identity.signalTags().contains("aggression")) {
            lines.add("Force gathers in it with unsettling manners.");
            lines.add("It favors pressure over noise.");
            lines.add("Impact is how it insists on being answered.");
        }
        if (identity.signalTags().contains("mobility")) {
            lines.add("It prefers the distance just before contact.");
            lines.add("Pursuit becomes a kind of closeness in its wake.");
            lines.add("It moves as if drawn by what keeps retreating.");
        }
        if (identity.signalTags().contains("memory")) {
            lines.add("What touched it once does not leave cleanly.");
            lines.add("Old marks return whenever the light turns intimate.");
            lines.add("Repetition has taught it where to linger.");
        }
        if (lines.isEmpty()) {
            lines.addAll(List.of(
                    "It keeps its meaning close.",
                    "It makes restraint sound deliberate.",
                    "It knows how to wait without softening."
            ));
        }
        return lines;
    }

    private String twoBeat(ArtifactTextIdentity identity, String salt, List<String> base, List<String> extensionPool) {
        String first = pick(identity, salt + ":first", base);
        boolean addSecond = identity.implicationScore() >= 0.45D || identity.signalTags().size() >= 3;
        if (!addSecond) {
            return first;
        }
        String second = pick(identity, salt + ":second", extensionPool);
        if (second.equals(first)) {
            return first;
        }
        return first + " " + second;
    }


    private String stableNameMotif(ArtifactTextIdentity identity) {
        if (identity.identityTags().contains("aggression"))
            return pickStable(identity, "aggression", List.of("oath", "claim", "edge", "mark"));
        if (identity.identityTags().contains("weapon"))
            return pickStable(identity, "weapon", List.of("oath", "edge", "mark", "vigil"));
        if (identity.identityTags().contains("defensive"))
            return pickStable(identity, "defensive", List.of("vigil", "bulwark", "ward", "hold"));
        if (identity.identityTags().contains("mobility") || identity.identityTags().contains("traversal"))
            return pickStable(identity, "mobility", List.of("draft", "current", "reach", "trace"));
        if (identity.toneProfile() == obtuseloot.names.ToneProfile.RITUAL) return "devotion";
        if (identity.toneProfile() == obtuseloot.names.ToneProfile.ELEGIAC) return "echo";
        return identity.motifs().getFirst();
    }

    // Selects deterministically from a pool using only stable identity dimensions (tags + tone).
    // Excludes signalTags and toneLayers which shift with runtime state mutations.
    private String pickStable(ArtifactTextIdentity identity, String salt, List<String> options) {
        int seed = Math.abs((identity.identityTags().toString() + identity.toneProfile().name() + salt).hashCode());
        return options.get(seed % options.size());
    }

    private String suggestiveModifier(ArtifactTextIdentity identity) {
        if (identity.implicationScore() < 0.55D) {
            return "";
        }
        List<String> options = new java.util.ArrayList<>();
        if (identity.identityTags().contains("defensive")) options.add("Hushed");
        if (identity.identityTags().contains("mobility") || identity.identityTags().contains("traversal")) options.add("Near");
        if (identity.identityTags().contains("aggression")) options.add("Restless");
        if (identity.toneProfile() == obtuseloot.names.ToneProfile.RITUAL) options.add("Veiled");
        if (options.isEmpty() || !shouldUseModifier(identity)) {
            return "";
        }
        return pick(identity, "name-modifier", options);
    }

    private boolean shouldUseModifier(ArtifactTextIdentity identity) {
        int seed = Math.abs((identity.personality() + identity.voice() + identity.identityTags() + identity.toneProfile()).hashCode());
        return seed % 3 == 0;
    }

    private String pick(ArtifactTextIdentity identity, String salt, List<String> options) {
        int seed = Math.abs((identity.personality() + identity.voice() + identity.motifs() + identity.signalTags()
                + identity.toneLayers() + salt).hashCode());
        return options.get(seed % options.size());
    }

    private String joinNonBlank(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElseThrow();
    }

    private String lastMotif(ArtifactTextIdentity identity) {
        return identity.motifs().size() > 1 ? identity.motifs().get(1) : identity.motifs().getFirst();
    }

    private String article(String value) {
        if (value == null || value.isBlank()) return "a";
        char c = Character.toLowerCase(value.charAt(0));
        return "aeiou".indexOf(c) >= 0 ? "an" : "a";
    }

    private String cap(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Artifact text requires non-blank value");
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private int maxWords(ArtifactTextChannel channel) {
        RuntimeSettings.Snapshot s = RuntimeSettings.get();
        return switch (channel) {
            case NAME -> s.textNameMaxWords();
            case LORE -> s.textLoreMaxWords();
            case IDENTIFY -> s.textIdentifyMaxWords();
            case AWAKENING -> s.textAwakeningMaxWords();
            case LINEAGE -> s.textLineageMaxWords();
            case MEMORY -> s.textMemoryMaxWords();
            case DRIFT -> s.textDriftMaxWords();
            case CONVERGENCE -> s.textConvergenceMaxWords();
            case DISCOVERY, EVENT -> s.textLoreMaxWords();
        };
    }
}
