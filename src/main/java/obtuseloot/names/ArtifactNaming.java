package obtuseloot.names;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArtifactNaming {
    private long namingSeed;
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

    public ArtifactNaming() {
        this.namingSeed = 0L;
        this.displayName = "";
        this.rootForm = "";
        this.namingArchetype = NamingArchetype.FORM_OF_CONCEPT;
        this.toneProfile = ToneProfile.ODD;
        this.identityTags = new ArrayList<>();
        this.affinityLexemes = new ArrayList<>();
        this.discoveryState = ArtifactDiscoveryState.OBSCURED;
    }

    public long getNamingSeed() { return namingSeed; }
    public void setNamingSeed(long namingSeed) { this.namingSeed = namingSeed; }
    public String getTrueName() { return trueName; }
    public void setTrueName(String trueName) { this.trueName = blankToNull(trueName); }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = requireNonBlank(displayName, "displayName"); }
    public String getRootForm() { return rootForm; }
    public void setRootForm(String rootForm) { this.rootForm = requireNonBlank(rootForm, "rootForm"); }
    public NamingArchetype getNamingArchetype() { return namingArchetype; }
    public void setNamingArchetype(NamingArchetype namingArchetype) { this.namingArchetype = Objects.requireNonNullElse(namingArchetype, NamingArchetype.FORM_OF_CONCEPT); }
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Artifact naming " + fieldName + " cannot be blank");
        }
        return value;
    }
}
