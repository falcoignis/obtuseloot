package obtuseloot.names;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.artifacts.EquipmentRole;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.ArrayList;
import java.util.List;

public final class ArtifactNameResolver {
    private static final ArtifactTextResolver TEXT_RESOLVER = new ArtifactTextResolver();

    private ArtifactNameResolver() {
    }

    public static ArtifactNaming initialize(Artifact artifact) {
        ArtifactNaming naming = new ArtifactNaming();
        naming.setRootForm(resolveRootForm(artifact));
        naming.setEpithetSeed((int) (artifact.getArtifactSeed() & 0x7FFFFFFF));
        naming.setTitleSeed((int) ((artifact.getArtifactSeed() >>> 32) & 0x7FFFFFFF));
        artifact.setNaming(naming);
        refresh(artifact, naming);
        return naming;
    }

    public static void refresh(Artifact artifact, ArtifactNaming naming) {
        naming.setRootForm(resolveRootForm(artifact));
        ArtifactDiscoveryState discovery = resolveDiscovery(artifact);
        List<String> tags = identityTags(artifact);
        List<String> lexemes = ArtifactLexemeRegistry.lexemesFor(tags, artifact.getArtifactSeed() ^ artifact.getTotalDrifts());
        String trueName = naming.getTrueName();
        if (trueName == null && shouldAssignTrueName(artifact)) {
            trueName = titleCase(lexemes.isEmpty() ? "Vesper" : lexemes.getFirst());
        }

        naming.setDiscoveryState(discovery);
        naming.setIdentityTags(tags);
        naming.setAffinityLexemes(lexemes);
        naming.setToneProfile(resolveTone(tags));
        naming.setTrueName(trueName);
        naming.setNamingArchetype(NamingArchetype.FORM_OF_CONCEPT);
        String displayName = TEXT_RESOLVER.compose(artifact, ArtifactTextChannel.NAME, naming.getRootForm());
        naming.setDisplayName(displayName);
    }

    private static boolean shouldAssignTrueName(Artifact artifact) {
        return hasAwakeningPath(artifact) || hasConvergencePath(artifact) || artifact.getHistoryScore() >= 60;
    }

    private static ArtifactDiscoveryState resolveDiscovery(Artifact artifact) {
        int historyScore = artifact.getHistoryScore();
        if (historyScore >= 80) {
            return ArtifactDiscoveryState.STORIED;
        }
        if (hasAwakeningPath(artifact) || hasConvergencePath(artifact) || historyScore >= 40) {
            return ArtifactDiscoveryState.REVEALED;
        }
        if (historyScore >= 20) {
            return ArtifactDiscoveryState.KNOWN;
        }
        return ArtifactDiscoveryState.OBSCURED;
    }

    private static boolean hasAwakeningPath(Artifact artifact) {
        String awakeningPath = artifact.getAwakeningPath();
        return awakeningPath != null && !"dormant".equalsIgnoreCase(awakeningPath);
    }

    private static boolean hasConvergencePath(Artifact artifact) {
        String convergencePath = artifact.getConvergencePath();
        return convergencePath != null && !"none".equalsIgnoreCase(convergencePath);
    }

    private static List<String> identityTags(Artifact artifact) {
        EquipmentArchetype archetype = ArtifactArchetypeValidator.requireValid(artifact, "artifact naming identity tags");
        List<String> tags = new ArrayList<>();
        if (archetype.hasRole(EquipmentRole.WEAPON)) tags.add("weapon");
        if (archetype.hasRole(EquipmentRole.DEFENSIVE_ARMOR)) tags.add("defensive");
        if (archetype.hasRole(EquipmentRole.MOBILITY)) tags.add("mobility");
        if (archetype.hasRole(EquipmentRole.TRAVERSAL)) tags.add("traversal");
        if (archetype.hasRole(EquipmentRole.RANGED_WEAPON)) tags.add("ranged");
        if (archetype.hasRole(EquipmentRole.SPEAR)) tags.add("reach");
        if (archetype.hasRole(EquipmentRole.TOOL)) tags.add("utility");
        if (artifact.getSeedPrecisionAffinity() > 0.66D) tags.add("precision");
        if (artifact.getSeedBrutalityAffinity() > 0.66D && archetype.hasRole(EquipmentRole.WEAPON)) tags.add("aggression");
        if (artifact.getMemory().snapshot().values().stream().mapToInt(Integer::intValue).sum() > 4) tags.add("memory");
        if (artifact.getSeedChaosAffinity() > 0.66D || artifact.getDriftAlignment().toLowerCase().contains("vol")) tags.add("chaotic");
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) tags.add("ritual");
        if (!"none".equalsIgnoreCase(artifact.getConvergencePath())) tags.add("control");
        if (tags.isEmpty()) {
            throw new IllegalStateException("Artifact naming tags must derive from a valid archetype: " + archetype.id());
        }
        return tags;
    }

    private static String resolveRootForm(Artifact artifact) {
        EquipmentArchetype archetype = ArtifactArchetypeValidator.requireValid(artifact, "artifact naming");
        return archetype.rootForm(artifact.getArtifactSeed());
    }

    private static ToneProfile resolveTone(List<String> tags) {
        if (tags.contains("ritual")) return ToneProfile.RITUAL;
        if (tags.contains("mobility") || tags.contains("traversal")) return ToneProfile.ODD;
        if (tags.contains("defensive")) return ToneProfile.WARDING;
        if (tags.contains("aggression") || tags.contains("weapon")) return ToneProfile.MARTIAL;
        if (tags.contains("memory")) return ToneProfile.ELEGIAC;
        if (tags.contains("chaotic")) return ToneProfile.WILD;
        if (tags.contains("utility") || tags.contains("precision")) return ToneProfile.ODD;
        throw new IllegalStateException("Unable to resolve naming tone from tags: " + tags);
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Naming value cannot be blank");
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}
