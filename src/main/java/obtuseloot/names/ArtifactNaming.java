package obtuseloot.names;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArtifactNaming {
    private String trueName;
    private String displayName;
    private String rootForm;
    private NamingArchetype namingArchetype;
    private ToneProfile toneProfile;
    private List<String> identityTags;
    private List<String> affinityLexemes;
    private int epithetSeed;
    private int titleSeed;
    private ArtifactDiscoveryState discoveryState;
    private ArtifactRank rankAtNaming;

    public ArtifactNaming() {
        this.displayName = "Nameless Artifact";
        this.rootForm = "Artifact";
        this.namingArchetype = NamingArchetype.TRAIT_FORM;
        this.toneProfile = ToneProfile.ODD;
        this.identityTags = new ArrayList<>();
        this.affinityLexemes = new ArrayList<>();
        this.discoveryState = ArtifactDiscoveryState.OBSCURED;
        this.rankAtNaming = ArtifactRank.BASE;
    }

    public String getTrueName() { return trueName; }
    public void setTrueName(String trueName) { this.trueName = blankToNull(trueName); }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = Objects.requireNonNullElse(displayName, "Nameless Artifact"); }
    public String getRootForm() { return rootForm; }
    public void setRootForm(String rootForm) { this.rootForm = Objects.requireNonNullElse(rootForm, "Artifact"); }
    public NamingArchetype getNamingArchetype() { return namingArchetype; }
    public void setNamingArchetype(NamingArchetype namingArchetype) { this.namingArchetype = Objects.requireNonNullElse(namingArchetype, NamingArchetype.TRAIT_FORM); }
    public ToneProfile getToneProfile() { return toneProfile; }
    public void setToneProfile(ToneProfile toneProfile) { this.toneProfile = Objects.requireNonNullElse(toneProfile, ToneProfile.ODD); }
    public List<String> getIdentityTags() { return identityTags; }
    public void setIdentityTags(List<String> identityTags) { this.identityTags = identityTags == null ? new ArrayList<>() : new ArrayList<>(identityTags); }
    public List<String> getAffinityLexemes() { return affinityLexemes; }
    public void setAffinityLexemes(List<String> affinityLexemes) { this.affinityLexemes = affinityLexemes == null ? new ArrayList<>() : new ArrayList<>(affinityLexemes); }
    public int getEpithetSeed() { return epithetSeed; }
    public void setEpithetSeed(int epithetSeed) { this.epithetSeed = epithetSeed; }
    public int getTitleSeed() { return titleSeed; }
    public void setTitleSeed(int titleSeed) { this.titleSeed = titleSeed; }
    public ArtifactDiscoveryState getDiscoveryState() { return discoveryState; }
    public void setDiscoveryState(ArtifactDiscoveryState discoveryState) { this.discoveryState = Objects.requireNonNullElse(discoveryState, ArtifactDiscoveryState.OBSCURED); }
    public ArtifactRank getRankAtNaming() { return rankAtNaming; }
    public void setRankAtNaming(ArtifactRank rankAtNaming) { this.rankAtNaming = Objects.requireNonNullElse(rankAtNaming, ArtifactRank.BASE); }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
