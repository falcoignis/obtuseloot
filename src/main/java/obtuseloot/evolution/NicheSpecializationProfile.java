package obtuseloot.evolution;

public record NicheSpecializationProfile(
        MechanicNicheTag dominantNiche,
        String dominantSubniche,
        double specializationScore,
        double concentration
) {
}
