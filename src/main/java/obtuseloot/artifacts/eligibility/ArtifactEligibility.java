package obtuseloot.artifacts.eligibility;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.artifacts.EquipmentRole;

public final class ArtifactEligibility {
    private ArtifactEligibility() {}

    public static boolean isEvolutionEligible(Artifact artifact) {
        return hasRealEquipmentArchetype(artifact);
    }

    public static boolean isAbilityEligible(Artifact artifact) {
        return hasRealEquipmentArchetype(artifact);
    }

    public static boolean isMemoryEligible(Artifact artifact) {
        return hasRealEquipmentArchetype(artifact);
    }

    public static boolean isWeaponEligible(Artifact artifact) {
        return hasRole(artifact, EquipmentRole.WEAPON);
    }

    public static boolean isArmorEligible(Artifact artifact) {
        return hasRole(artifact, EquipmentRole.ARMOR);
    }

    public static boolean isDefensiveArmorEligible(Artifact artifact) {
        return hasRole(artifact, EquipmentRole.DEFENSIVE_ARMOR);
    }

    public static boolean isToolEligible(Artifact artifact) {
        return hasRole(artifact, EquipmentRole.TOOL);
    }

    public static boolean isToolWeaponHybridEligible(Artifact artifact) {
        return hasRole(artifact, EquipmentRole.TOOL_WEAPON_HYBRID);
    }

    private static boolean hasRealEquipmentArchetype(Artifact artifact) {
        return artifact != null && EquipmentArchetype.isEquipment(artifact.getItemCategory());
    }

    private static boolean hasRole(Artifact artifact, EquipmentRole role) {
        return hasRealEquipmentArchetype(artifact)
                && EquipmentArchetype.fromId(artifact.getItemCategory()).hasRole(role);
    }
}
