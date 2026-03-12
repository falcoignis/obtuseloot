package obtuseloot.text;

import obtuseloot.artifacts.Artifact;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.names.ArtifactNameCompressor;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ArtifactTextComposer {
    private final ArtifactExpressionSafetyFilter safetyFilter = new ArtifactExpressionSafetyFilter();
    private final ArtifactTextToneValidator validator = new ArtifactTextToneValidator();

    public String compose(Artifact artifact, ArtifactTextIdentity identity, ArtifactTextChannel channel, String context) {
        int maxWords = maxWords(channel);
        String raw = switch (channel) {
            case NAME -> name(identity, context);
            case LORE -> lore(identity);
            case IDENTIFY -> identify(identity);
            case AWAKENING -> awakening(identity);
            case LINEAGE -> lineage(identity, context);
            case MEMORY -> memory(identity, context);
            case DRIFT -> drift(identity, context);
            case FUSION -> fusion(identity, context);
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
        String motif = cap(identity.motifs().getFirst());
        String root = context == null || context.isBlank() ? "Artifact" : cap(context);
        return switch (identity.namingArchetype()) {
            case TRAIT_FORM -> motif + " " + root;
            case COMPACT_COMPOUND -> motif + cap(lastMotif(identity));
            case FORM_OF_CONCEPT -> root + " of " + motif;
            case TRUE_NAME_ONLY -> motif;
            case TRUE_NAME_WITH_EPITHET -> motif + ", " + root + " of " + cap(lastMotif(identity));
            case TRUE_NAME_WITH_TITLE -> motif + " the " + cap(lastMotif(identity)) + " " + root;
        };
    }

    private String lore(ArtifactTextIdentity identity) {
        return phrase(identity, List.of(
                "It keeps " + article(lastMotif(identity)) + " " + lastMotif(identity) + " behind a careful hush.",
                "Its voice stays " + identity.cadence() + ", with " + identity.motifs().getFirst() + " at the edges.",
                "It does not rush what it means to keep."
        ));
    }

    private String identify(ArtifactTextIdentity identity) {
        if (identity.discoveryState().ordinal() < 2) {
            return "Its intent is felt before it is known.";
        }
        return "It answers as " + identity.voice().replace('-', ' ') + ", carrying " + lastMotif(identity) + ".";
    }

    private String awakening(ArtifactTextIdentity identity) {
        return phrase(identity, List.of(
                "Its name returns with " + identity.motifs().getFirst() + ".",
                "What was hidden now answers in " + lastMotif(identity) + ".",
                "It is no longer content to remain quiet."
        ));
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
        return "It remembers " + anchor + " not as record, but as " + identity.motifs().getFirst() + ".";
    }

    private String drift(ArtifactTextIdentity identity, String drift) {
        String direction = drift == null || drift.isBlank() ? lastMotif(identity) : drift.toLowerCase(Locale.ROOT);
        return "It leans toward " + direction + ", slowly and without apology.";
    }

    private String fusion(ArtifactTextIdentity identity, String path) {
        String fusionPath = path == null || path.isBlank() ? "a second inheritance" : path;
        return "Two intentions now share one edge: " + fusionPath + ".";
    }

    private String discovery(ArtifactTextIdentity identity) {
        return "It opens only by degrees, keeping " + identity.motifs().getFirst() + " close.";
    }

    private String event(ArtifactTextIdentity identity, String eventId) {
        return "Something in it marked " + (eventId == null ? "the passing" : eventId.replace('.', ' '))
                + " and kept the afterimage.";
    }

    private String phrase(ArtifactTextIdentity identity, List<String> options) {
        int seed = Math.abs((identity.personality() + identity.voice() + identity.motifs()).hashCode());
        return options.get(new Random(seed).nextInt(options.size()));
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
        if (value == null || value.isBlank()) return "Unknown";
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
            case FUSION -> s.textFusionMaxWords();
            case DISCOVERY, EVENT -> s.textLoreMaxWords();
        };
    }
}
