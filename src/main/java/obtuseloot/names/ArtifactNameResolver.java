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
        long namingSeed = artifact.getArtifactSeed();
        naming.setNamingSeed(namingSeed);
        naming.setRootForm(resolveRootForm(artifact, namingSeed));
        naming.setEpithetSeed((int) (namingSeed & 0x7FFFFFFF));
        naming.setTitleSeed((int) ((namingSeed >>> 32) & 0x7FFFFFFF));
        artifact.setNaming(naming);
        refresh(artifact, naming);
        return naming;
    }

    public static void refresh(Artifact artifact, ArtifactNaming naming) {
        long namingSeed = naming.getNamingSeed() == 0L ? artifact.getArtifactSeed() : naming.getNamingSeed();
        naming.setNamingSeed(namingSeed);
        naming.setRootForm(resolveRootForm(artifact, namingSeed));
        ArtifactDiscoveryState discovery = ArtifactDiscoveryState.OBSCURED;
        List<String> tags = identityTags(artifact);
        List<String> lexemes = ArtifactLexemeRegistry.lexemesFor(tags, namingSeed);
        String trueName = naming.getTrueName();

        naming.setDiscoveryState(discovery);
        naming.setIdentityTags(tags);
        naming.setAffinityLexemes(lexemes);
        naming.setToneProfile(resolveTone(tags));
        naming.setTrueName(trueName);
        naming.setNamingArchetype(NamingArchetype.FORM_OF_CONCEPT);
        String displayName = TEXT_RESOLVER.compose(artifact, ArtifactTextChannel.NAME, naming.getRootForm());
        naming.setDisplayName(displayName);
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
        if (artifact.getSeedChaosAffinity() > 0.66D) tags.add("chaotic");
        if (tags.isEmpty()) {
            throw new IllegalStateException("Artifact naming tags must derive from a valid archetype: " + archetype.id());
        }
        return tags;
    }

    private static String resolveRootForm(Artifact artifact, long namingSeed) {
        EquipmentArchetype archetype = ArtifactArchetypeValidator.requireValid(artifact, "artifact naming");
        return archetype.rootForm(namingSeed);
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

}
