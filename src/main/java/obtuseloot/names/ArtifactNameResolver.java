package obtuseloot.names;

import obtuseloot.artifacts.Artifact;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ArtifactNameResolver {
    private static final ArtifactTextResolver TEXT_RESOLVER = new ArtifactTextResolver();

    private ArtifactNameResolver() {
    }

    public static ArtifactNaming initialize(Artifact artifact) {
        ArtifactNaming naming = new ArtifactNaming();
        naming.setRootForm(resolveRootForm(artifact.getItemCategory()));
        naming.setEpithetSeed((int) (artifact.getArtifactSeed() & 0x7FFFFFFF));
        naming.setTitleSeed((int) ((artifact.getArtifactSeed() >>> 32) & 0x7FFFFFFF));
        refresh(artifact, naming);
        return naming;
    }

    public static void refresh(Artifact artifact, ArtifactNaming naming) {
        ArtifactRank rank = ArtifactRankResolver.resolve(artifact);
        ArtifactDiscoveryState discovery = resolveDiscovery(artifact, rank);
        List<String> tags = identityTags(artifact);
        List<String> lexemes = ArtifactLexemeRegistry.lexemesFor(tags, artifact.getArtifactSeed() ^ artifact.getTotalDrifts());
        String trueName = naming.getTrueName();
        if (trueName == null && shouldAssignTrueName(rank, discovery, artifact.getArtifactSeed())) {
            trueName = titleCase(lexemes.isEmpty() ? "Vesper" : lexemes.getFirst());
        }

        naming.setRankAtNaming(rank);
        naming.setDiscoveryState(discovery);
        naming.setIdentityTags(tags);
        naming.setAffinityLexemes(lexemes);
        naming.setToneProfile(resolveTone(tags));
        naming.setTrueName(trueName);
        naming.setNamingArchetype(resolveArchetype(rank, discovery, trueName));
        String displayName = TEXT_RESOLVER.compose(artifact, ArtifactTextChannel.NAME, naming.getRootForm());
        naming.setDisplayName(displayName);
    }

    private static NamingArchetype resolveArchetype(ArtifactRank rank, ArtifactDiscoveryState discovery, String trueName) {
        if (trueName != null && discovery == ArtifactDiscoveryState.STORIED) {
            return NamingArchetype.TRUE_NAME_WITH_TITLE;
        }
        if (trueName != null && discovery == ArtifactDiscoveryState.REVEALED) {
            return NamingArchetype.TRUE_NAME_WITH_EPITHET;
        }
        if (trueName != null) {
            return NamingArchetype.TRUE_NAME_ONLY;
        }
        return switch (rank) {
            case BASE -> NamingArchetype.TRAIT_FORM;
            case TEMPERED -> NamingArchetype.COMPACT_COMPOUND;
            default -> NamingArchetype.FORM_OF_CONCEPT;
        };
    }

    private static boolean shouldAssignTrueName(ArtifactRank rank, ArtifactDiscoveryState discovery, long seed) {
        int probability = RuntimeSettings.get().namingTrueNameProbabilityPercentByRank().getOrDefault(rank.name(), 0);
        int bonus = discovery.ordinal() * 10;
        Random random = new Random(seed ^ 0xABC98388L ^ discovery.ordinal());
        return random.nextInt(100) < Math.min(100, probability + bonus);
    }

    private static ArtifactDiscoveryState resolveDiscovery(Artifact artifact, ArtifactRank rank) {
        int memoryCount = artifact.getMemory().snapshot().values().stream().mapToInt(Integer::intValue).sum();
        int historyScore = artifact.getLoreHistory().size() + artifact.getNotableEvents().size() + memoryCount;
        if (historyScore >= RuntimeSettings.get().namingDiscoveryStoriedThreshold()) {
            return ArtifactDiscoveryState.STORIED;
        }
        if (rank.ordinal() >= ArtifactRank.MYTHIC.ordinal() || historyScore >= RuntimeSettings.get().namingDiscoveryRevealedThreshold()) {
            return ArtifactDiscoveryState.REVEALED;
        }
        if (historyScore >= RuntimeSettings.get().namingDiscoveryKnownThreshold()) {
            return ArtifactDiscoveryState.KNOWN;
        }
        return ArtifactDiscoveryState.OBSCURED;
    }

    private static List<String> identityTags(Artifact artifact) {
        List<String> tags = new ArrayList<>();
        if (artifact.getSeedSurvivalAffinity() > 0.66D) tags.add("defensive");
        if (artifact.getSeedChaosAffinity() > 0.66D || artifact.getDriftAlignment().toLowerCase().contains("vol")) tags.add("chaotic");
        if (artifact.getSeedPrecisionAffinity() > 0.66D) tags.add("precision");
        if (artifact.getSeedBrutalityAffinity() > 0.66D) tags.add("aggression");
        if (artifact.getMemory().snapshot().values().stream().mapToInt(Integer::intValue).sum() > 4) tags.add("memory");
        if (artifact.getSeedMobilityAffinity() > 0.66D) tags.add("mobility");
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) tags.add("ritual");
        if (!"none".equalsIgnoreCase(artifact.getConvergencePath())) tags.add("control");
        if (tags.isEmpty()) tags.add("support");
        return tags;
    }

    private static String resolveRootForm(String itemCategory) {
        return switch (itemCategory == null ? "artifact" : itemCategory.toLowerCase()) {
            case "blade", "sword" -> "Blade";
            case "bell" -> "Bell";
            case "focus", "reliquary" -> "Reliquary";
            case "ward", "shield" -> "Ward";
            default -> "Artifact";
        };
    }

    private static ToneProfile resolveTone(List<String> tags) {
        if (tags.contains("ritual")) return ToneProfile.RITUAL;
        if (tags.contains("defensive")) return ToneProfile.WARDING;
        if (tags.contains("aggression")) return ToneProfile.MARTIAL;
        if (tags.contains("memory")) return ToneProfile.ELEGIAC;
        if (tags.contains("chaotic")) return ToneProfile.WILD;
        return ToneProfile.ODD;
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}
