package obtuseloot.text;

import obtuseloot.names.ArtifactDiscoveryState;
import obtuseloot.names.ArtifactRank;
import obtuseloot.names.NamingArchetype;
import obtuseloot.names.ToneProfile;

import java.util.List;

public record ArtifactTextIdentity(
        String personality,
        String voice,
        NamingArchetype namingArchetype,
        ToneProfile toneProfile,
        double implicationScore,
        String cadence,
        ArtifactRank rank,
        ArtifactDiscoveryState discoveryState,
        List<String> identityTags,
        List<String> motifs
) {
}
