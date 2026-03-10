package obtuseloot.text;

import obtuseloot.artifacts.Artifact;
import obtuseloot.names.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ArtifactTextIdentityResolver {

    public ArtifactTextIdentity resolve(Artifact artifact, ArtifactNaming naming) {
        List<String> tags = naming == null || naming.getIdentityTags().isEmpty() ? fallbackTags(artifact) : naming.getIdentityTags();
        ToneProfile tone = naming == null ? ToneProfile.ODD : naming.getToneProfile();
        ArtifactRank rank = ArtifactRankResolver.resolve(artifact);
        ArtifactDiscoveryState discovery = naming == null ? ArtifactDiscoveryState.OBSCURED : naming.getDiscoveryState();
        String personality = resolvePersonality(tags, tone);
        String voice = resolveVoice(tags, tone);
        String cadence = resolveCadence(tone, discovery);
        double implication = resolveImplication(tone, rank, discovery, tags);
        List<String> motifs = resolveMotifs(tags, tone, discovery);
        NamingArchetype archetype = naming == null ? NamingArchetype.TRAIT_FORM : naming.getNamingArchetype();
        return new ArtifactTextIdentity(personality, voice, archetype, tone, implication, cadence, rank, discovery,
                List.copyOf(tags), motifs);
    }

    private List<String> fallbackTags(Artifact artifact) {
        List<String> tags = new ArrayList<>();
        if (artifact.getSeedSurvivalAffinity() > 0.66D) tags.add("defensive");
        if (artifact.getSeedChaosAffinity() > 0.66D) tags.add("chaotic");
        if (artifact.getSeedBrutalityAffinity() > 0.66D) tags.add("aggression");
        if (artifact.getSeedMobilityAffinity() > 0.66D) tags.add("mobility");
        if (artifact.getMemory().snapshot().values().stream().mapToInt(Integer::intValue).sum() > 3) tags.add("memory");
        if (tags.isEmpty()) tags.add("support");
        return tags;
    }

    private String resolvePersonality(List<String> tags, ToneProfile tone) {
        if (tags.contains("aggression") && tone == ToneProfile.RITUAL) return "courtly-predatory";
        if (tags.contains("memory") || tone == ToneProfile.ELEGIAC) return "intimate-elegiac";
        if (tags.contains("ritual") || tone == ToneProfile.RITUAL) return "liturgical";
        if (tags.contains("defensive")) return "vigilant";
        if (tags.contains("chaotic")) return "sly-volatile";
        return "patient-strange";
    }

    private String resolveVoice(List<String> tags, ToneProfile tone) {
        if (tone == ToneProfile.RITUAL) return "ceremonial";
        if (tone == ToneProfile.MARTIAL && tags.contains("aggression")) return "polite-danger";
        if (tone == ToneProfile.ELEGIAC) return "close-whisper";
        if (tone == ToneProfile.WARDING) return "measured-guardian";
        return "controlled-eerie";
    }

    private String resolveCadence(ToneProfile tone, ArtifactDiscoveryState discovery) {
        if (discovery.ordinal() >= ArtifactDiscoveryState.REVEALED.ordinal()) return "assured";
        return switch (tone) {
            case MARTIAL -> "clipped";
            case ELEGIAC -> "lingering";
            case RITUAL -> "litany";
            default -> "measured";
        };
    }

    private double resolveImplication(ToneProfile tone, ArtifactRank rank, ArtifactDiscoveryState discovery, List<String> tags) {
        double value = 0.25D + (rank.ordinal() * 0.12D) + (discovery.ordinal() * 0.1D);
        if (tone == ToneProfile.ELEGIAC || tone == ToneProfile.RITUAL) value += 0.1D;
        if (tags.contains("aggression") || tags.contains("memory")) value += 0.08D;
        return Math.min(0.95D, value);
    }

    private List<String> resolveMotifs(List<String> tags, ToneProfile tone, ArtifactDiscoveryState discovery) {
        Set<String> motifs = new LinkedHashSet<>();
        if (tags.contains("defensive")) {
            motifs.add("vigil");
            motifs.add("mercy");
        }
        if (tags.contains("aggression")) {
            motifs.add("claim");
            motifs.add("appetite");
        }
        if (tags.contains("memory")) {
            motifs.add("echo");
            motifs.add("remembrance");
        }
        if (tags.contains("chaotic")) {
            motifs.add("fracture");
            motifs.add("ruin");
        }
        if (tags.contains("mobility")) motifs.add("threshold");
        switch (tone) {
            case RITUAL -> {
                motifs.add("devotion");
                motifs.add("secrecy");
            }
            case ELEGIAC -> motifs.add("afterimage");
            case WARDING -> motifs.add("patience");
            case MARTIAL -> motifs.add("oath");
            default -> motifs.add("dusk");
        }
        if (discovery == ArtifactDiscoveryState.OBSCURED) motifs.add("hollowing");
        return List.copyOf(motifs.stream().limit(4).toList());
    }
}
