package obtuseloot.text;

import obtuseloot.artifacts.Artifact;
import obtuseloot.names.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ArtifactTextIdentityResolver {

    public ArtifactTextIdentity resolve(Artifact artifact, ArtifactNaming naming) {
        if (naming == null) {
            throw new IllegalStateException("Artifact naming must be initialized before text resolution");
        }
        if (naming.getIdentityTags().isEmpty()) {
            throw new IllegalStateException("Artifact naming identity tags must be present before text resolution");
        }
        List<String> tags = naming.getIdentityTags();
        ToneProfile tone = naming.getToneProfile();
        ArtifactDiscoveryState discovery = naming.getDiscoveryState();
        List<String> signalTags = resolveSignalTags(artifact, tags);
        List<String> toneLayers = resolveToneLayers(signalTags, tags, tone, discovery);
        String personality = resolvePersonality(tags, tone, signalTags);
        String voice = resolveVoice(tags, tone, signalTags);
        String cadence = resolveCadence(tone, discovery, signalTags);
        double implication = resolveImplication(tone, discovery, tags, signalTags, toneLayers);
        List<String> motifs = resolveMotifs(tags, tone, discovery, signalTags, toneLayers);
        NamingArchetype archetype = naming.getNamingArchetype();
        return new ArtifactTextIdentity(personality, voice, archetype, tone, implication, cadence, discovery,
                List.copyOf(tags), motifs, signalTags, toneLayers);
    }

    private String resolvePersonality(List<String> tags, ToneProfile tone, List<String> signalTags) {
        if (signalTags.contains("convergence") && signalTags.contains("aggression")) return "hungry-dual";
        if (tags.contains("aggression") && signalTags.contains("awakening")) return "roused-predatory";
        if (signalTags.contains("awakening") && signalTags.contains("release")) return "newly-unbound";
        if (tags.contains("aggression") && tone == ToneProfile.RITUAL) return "courtly-predatory";
        if (signalTags.contains("memory") && signalTags.contains("imprint")) return "obsessive-elegiac";
        if (tags.contains("memory") || tone == ToneProfile.ELEGIAC) return "intimate-elegiac";
        if (tags.contains("ritual") || tone == ToneProfile.RITUAL) return "liturgical";
        if (tags.contains("mobility") || tags.contains("traversal")) return "windborne";
        if (tags.contains("defensive")) return "vigilant";
        if (tags.contains("chaotic")) return "sly-volatile";
        if (tags.contains("weapon")) return "disciplined-martial";
        return "patient-strange";
    }

    private String resolveVoice(List<String> tags, ToneProfile tone, List<String> signalTags) {
        if (signalTags.contains("convergence") && signalTags.contains("pull")) return "drawn-close";
        if (tone == ToneProfile.RITUAL) return "ceremonial";
        if (tags.contains("mobility") || tags.contains("traversal")) return "aerial-sure";
        if (tone == ToneProfile.MARTIAL && tags.contains("aggression")) return "polite-danger";
        if (signalTags.contains("instability")) return "held-breath";
        if (tone == ToneProfile.MARTIAL) return "steady-edge";
        if (tone == ToneProfile.ELEGIAC) return "close-whisper";
        if (tone == ToneProfile.WARDING) return "measured-guardian";
        return "controlled-eerie";
    }

    private String resolveCadence(ToneProfile tone, ArtifactDiscoveryState discovery, List<String> signalTags) {
        if (signalTags.contains("instability")) return "tightened";
        if (discovery.ordinal() >= ArtifactDiscoveryState.REVEALED.ordinal()) return "assured";
        return switch (tone) {
            case MARTIAL -> "clipped";
            case ELEGIAC -> "lingering";
            case RITUAL -> "litany";
            default -> "measured";
        };
    }

    private double resolveImplication(ToneProfile tone, ArtifactDiscoveryState discovery, List<String> tags,
                                      List<String> signalTags, List<String> toneLayers) {
        double value = 0.25D + (discovery.ordinal() * 0.1D);
        if (tone == ToneProfile.ELEGIAC || tone == ToneProfile.RITUAL) value += 0.1D;
        if (tags.contains("aggression") || tags.contains("memory")) value += 0.08D;
        if (signalTags.contains("convergence") || signalTags.contains("awakening")) value += 0.1D;
        if (signalTags.contains("instability")) value += 0.06D;
        if (toneLayers.contains("pull") || toneLayers.contains("restraint")) value += 0.05D;
        return Math.min(0.95D, value);
    }

    private List<String> resolveMotifs(List<String> tags, ToneProfile tone, ArtifactDiscoveryState discovery,
                                       List<String> signalTags, List<String> toneLayers) {
        Set<String> motifs = new LinkedHashSet<>();
        if (tags.contains("defensive")) {
            motifs.add("vigil");
            motifs.add("bulwark");
        }
        if (tags.contains("aggression")) {
            motifs.add("claim");
            motifs.add("appetite");
        }
        if (signalTags.contains("memory")) {
            motifs.add("echo");
            motifs.add("remembrance");
        }
        if (tags.contains("chaotic") || signalTags.contains("instability")) {
            motifs.add("fracture");
            motifs.add("ruin");
        }
        if (tags.contains("mobility") || tags.contains("traversal")) {
            motifs.add("draft");
            motifs.add("glide");
        }
        if (signalTags.contains("convergence")) {
            motifs.add("inheritance");
            motifs.add("braid");
        }
        if (signalTags.contains("awakening")) {
            motifs.add("release");
            motifs.add("becoming");
        }
        if (toneLayers.contains("pull")) motifs.add("gravity");
        if (toneLayers.contains("restraint")) motifs.add("hush");
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
        return List.copyOf(motifs.stream().limit(5).toList());
    }

    private List<String> resolveSignalTags(Artifact artifact, List<String> tags) {
        Set<String> signals = new LinkedHashSet<>(tags);
        if (!"none".equalsIgnoreCase(artifact.getConvergencePath()) || !"none".equalsIgnoreCase(artifact.getConvergenceIdentityShape())) {
            signals.add("convergence");
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) || !"none".equalsIgnoreCase(artifact.getAwakeningIdentityShape())) {
            signals.add("awakening");
            signals.add("release");
        }
        if (artifact.getDriftLevel() > 0 || artifact.getTotalDrifts() > 0 || !"stable".equalsIgnoreCase(artifact.getDriftAlignment())
                || !"none".equalsIgnoreCase(artifact.getCurrentInstabilityState())) {
            signals.add("instability");
        }
        if (!artifact.getLoreHistory().isEmpty() || !artifact.getNotableEvents().isEmpty() || artifact.getMemory().pressure() > 0) {
            signals.add("memory");
            signals.add("history");
            signals.add("imprint");
        }
        if (tags.contains("mobility") || tags.contains("traversal")) signals.add("mobility");
        if (tags.contains("aggression")) signals.add("aggression");
        return List.copyOf(signals);
    }

    private List<String> resolveToneLayers(List<String> signalTags, List<String> tags, ToneProfile tone,
                                           ArtifactDiscoveryState discovery) {
        Set<String> layers = new LinkedHashSet<>();
        if (signalTags.contains("convergence")) {
            layers.add("inheritance");
            layers.add(signalTags.contains("mobility") ? "pull" : "merging");
        }
        if (signalTags.contains("awakening")) {
            layers.add("emergence");
            layers.add("restraint");
        }
        if (signalTags.contains("instability")) {
            layers.add("slippage");
            layers.add("strain");
        }
        if (signalTags.contains("aggression")) {
            layers.add("pressure");
            layers.add("dominance");
        }
        if (signalTags.contains("mobility")) {
            layers.add("pursuit");
            layers.add("closeness");
        }
        if (signalTags.contains("memory")) {
            layers.add("imprint");
            layers.add("repetition");
        }
        if (tone == ToneProfile.WARDING || tags.contains("defensive")) layers.add("restraint");
        if (discovery.ordinal() >= ArtifactDiscoveryState.REVEALED.ordinal()) layers.add("assurance");
        return List.copyOf(layers);
    }
}
