package obtuseloot.awakening;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.memory.MemoryInfluenceResolver;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AwakeningEngine {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();
    private final MemoryInfluenceResolver memoryInfluenceResolver = new MemoryInfluenceResolver();
    private final Map<String, AwakeningEffectProfile> profiles = Map.of(
            "Executioner's Oath", new AwakeningEffectProfile("Executioner's Oath", Map.of("brutality", 1.6, "chaos", 0.4), Map.of("brutality", 2.0), Set.of("execution", "finisher", "oathbound")),
            "Stormblade", new AwakeningEffectProfile("Stormblade", Map.of("precision", 1.4, "mobility", 0.8), Map.of("precision", 2.0), Set.of("stormstep", "linebreaker")),
            "Bulwark Ascendant", new AwakeningEffectProfile("Bulwark Ascendant", Map.of("survival", 1.5, "consistency", 1.0), Map.of("survival", 2.0), Set.of("fortress", "anchor")),
            "Tempest Stride", new AwakeningEffectProfile("Tempest Stride", Map.of("mobility", 1.5, "chaos", 0.7), Map.of("mobility", 2.0), Set.of("windrunner", "skirmish")),
            "Voidwake Covenant", new AwakeningEffectProfile("Voidwake Covenant", Map.of("chaos", 1.8, "precision", 0.5), Map.of("chaos", 2.0), Set.of("voidwake", "entropy-mark")),
            "Last Survivor", new AwakeningEffectProfile("Last Survivor", Map.of("survival", 1.3, "precision", 1.0), Map.of("survival", 2.0), Set.of("last-stand", "unyielding")),
            "Crown of Equilibrium", new AwakeningEffectProfile("Crown of Equilibrium", Map.of("consistency", 1.5, "survival", 0.6), Map.of("consistency", 2.0), Set.of("equilibrium", "harmonic"))
    );

    public ArtifactIdentityTransition evaluate(Player player, Artifact artifact, ArtifactReputation reputation) {
        return evaluateInternal(player, artifact, reputation, false);
    }

    public ArtifactIdentityTransition evaluateSimulation(Artifact artifact, ArtifactReputation reputation) {
        return evaluateInternal(null, artifact, reputation, false);
    }

    public ArtifactIdentityTransition forceAwakening(Player player, Artifact artifact, ArtifactReputation reputation) {
        return evaluateInternal(player, artifact, reputation, true);
    }

    private ArtifactIdentityTransition evaluateInternal(Player player,
                                                        Artifact artifact,
                                                        ArtifactReputation reputation,
                                                        boolean force) {
        ArtifactArchetypeValidator.requireValid(artifact, "awakening evaluation");
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            return null;
        }
        ArtifactMemoryProfile memoryProfile = memoryInfluenceResolver.profileFor(artifact.getMemory());
        AwakeningResolution resolution = resolve(artifact, reputation, memoryProfile, force);
        if (resolution == null) {
            return null;
        }
        Artifact replacement = createReplacement(artifact, reputation, memoryProfile, resolution);
        if (player != null) {
            player.sendMessage("§d" + textResolver.compose(replacement, ArtifactTextChannel.AWAKENING, resolution.path()));
        }
        return new ArtifactIdentityTransition(artifact, replacement,
                "artifact-awakening:" + resolution.profileId() + ":" + resolution.variantId());
    }

    private Artifact createReplacement(Artifact artifact,
                                       ArtifactReputation rep,
                                       ArtifactMemoryProfile memoryProfile,
                                       AwakeningResolution resolution) {
        long seed = Objects.hash(
                artifact.getArtifactSeed(),
                resolution.profileId(),
                resolution.variantId(),
                resolution.identityShape(),
                artifact.getLastUtilityHistory(),
                artifact.getConvergencePath(),
                artifact.getConvergenceVariantId(),
                artifact.getNotableEvents().hashCode(),
                artifact.getMemory().snapshot().hashCode(),
                rep.getTotalScore(),
                memoryProfile.pressure());
        Artifact replacement = new Artifact(artifact.getOwnerId(), artifact.getItemCategory());
        replacement.setPersistenceOriginTimestamp(artifact.getPersistenceOriginTimestamp());
        replacement.setIdentityBirthTimestamp(System.currentTimeMillis());
        replacement.setArtifactStorageKey(artifact.getArtifactStorageKey());
        replacement.setArtifactSeed(seed);
        replacement.setOwnerId(artifact.getOwnerId());
        replacement.setArchetypePath(resolution.archetypePath());
        replacement.setEvolutionPath("awakened-" + resolution.profileId());
        replacement.setAwakeningPath(resolution.path());
        replacement.setConvergencePath(artifact.getConvergencePath());
        replacement.setDriftLevel(artifact.getDriftLevel());
        replacement.setTotalDrifts(artifact.getTotalDrifts());
        replacement.setDriftAlignment(artifact.getDriftAlignment());
        replacement.setLastDriftTimestamp(artifact.getLastDriftTimestamp());
        replacement.setLatentLineage(artifact.getLatentLineage());
        replacement.setSpeciesId(artifact.getSpeciesId());
        replacement.setParentSpeciesId(artifact.getParentSpeciesId());
        replacement.setLastSpeciesCompatibilityDistance(artifact.getLastSpeciesCompatibilityDistance());
        replacement.setInstabilityState(artifact.getCurrentInstabilityState(), artifact.getInstabilityExpiryTimestamp());
        replacement.setSeedPrecisionAffinity(reblendedAffinity(artifact, resolution, "precision"));
        replacement.setSeedBrutalityAffinity(reblendedAffinity(artifact, resolution, "brutality"));
        replacement.setSeedSurvivalAffinity(reblendedAffinity(artifact, resolution, "survival"));
        replacement.setSeedMobilityAffinity(reblendedAffinity(artifact, resolution, "mobility"));
        replacement.setSeedChaosAffinity(reblendedAffinity(artifact, resolution, "chaos"));
        replacement.setSeedConsistencyAffinity(reblendedAffinity(artifact, resolution, "consistency"));
        replacement.getDriftBiasAdjustments().putAll(artifact.getDriftBiasAdjustments());
        replacement.getMemory().restore(artifact.getMemory().snapshot());
        replacement.getDriftHistory().addAll(artifact.getDriftHistory());
        replacement.getLoreHistory().addAll(artifact.getLoreHistory());
        replacement.getNotableEvents().addAll(artifact.getNotableEvents());
        replacement.getAwakeningTraits().addAll(resolution.profile().traits());
        resolution.profile().biasAdjustments().forEach((k, v) -> replacement.getAwakeningBiasAdjustments().put(k, v));
        resolution.profile().reputationGainMultipliers().forEach((k, v) -> replacement.getAwakeningGainMultipliers().put(k, v));
        replacement.setConvergenceVariantId(artifact.getConvergenceVariantId());
        replacement.setConvergenceIdentityShape(artifact.getConvergenceIdentityShape());
        replacement.setConvergenceLineageTrace(artifact.getConvergenceLineageTrace());
        replacement.setConvergenceLoreTrace(artifact.getConvergenceLoreTrace());
        replacement.setConvergenceContinuityTrace(artifact.getConvergenceContinuityTrace());
        replacement.setConvergenceExpressionTrace(artifact.getConvergenceExpressionTrace());
        replacement.setConvergenceMemorySignature(artifact.getConvergenceMemorySignature());
        replacement.setAwakeningVariantId(resolution.variantId());
        replacement.setAwakeningIdentityShape(resolution.identityShape());
        replacement.setAwakeningLineageTrace(resolution.lineageTrace());
        replacement.setAwakeningLoreTrace(resolution.loreTrace());
        replacement.setAwakeningContinuityTrace("owner-storage|memory-imprint|lineage-thread|bounded-history");
        replacement.setAwakeningExpressionTrace(resolution.expressionTrace());
        replacement.setAwakeningMemorySignature(resolution.memorySignature());
        replacement.setNaming(ArtifactNameResolver.initialize(replacement));
        replacement.addLoreHistory("Awakening replaced " + artifact.getItemCategory() + " identity with " + resolution.path() + " [" + resolution.variantId() + "]");
        replacement.addLoreHistory(textResolver.compose(replacement, ArtifactTextChannel.AWAKENING, resolution.path()));
        replacement.addLoreHistory("Awakening identity shaped as " + resolution.identityShape() + "; lore trace=" + resolution.loreTrace());
        replacement.addNotableEvent("identity.replaced." + artifact.getArtifactSeed());
        replacement.addNotableEvent("awakening." + resolution.profileId());
        replacement.addNotableEvent("awakening.variant." + resolution.variantId());
        replacement.addNotableEvent("awakening.lineage." + resolution.lineageTrace());
        replacement.addNotableEvent("awakening.expression." + resolution.expressionTrace());
        replacement.addNotableEvent("awakening.memory." + resolution.memorySignature());
        return replacement;
    }

    private double reblendedAffinity(Artifact artifact, AwakeningResolution resolution, String stat) {
        double base = artifact.getSeedAffinity(stat);
        double delta = resolution.profile().biasAdjustments().getOrDefault(stat, 0.0D);
        return Math.max(-4.0D, Math.min(4.0D, base + delta));
    }

    private AwakeningResolution resolve(Artifact artifact, ArtifactReputation rep, ArtifactMemoryProfile memoryProfile, boolean force) {
        int history = artifact.getHistoryScore();
        int convergencePressure = "none".equalsIgnoreCase(artifact.getConvergencePath()) ? 0 : 4;
        int lineagePressure = artifact.getLatentLineage().equalsIgnoreCase("common") ? 0 : 2;
        if (!force && (memoryProfile.pressure() < 5 || history < 10 || rep.getKills() < 6)) {
            return null;
        }
        return switch (artifact.getArchetypePath()) {
            case "ravager" -> qualify(force || (rep.getBrutality() >= 14 && memoryProfile.aggressionWeight() >= 6.0D
                    && artifact.getMemory().count(ArtifactMemoryEvent.MULTIKILL_CHAIN) >= 2),
                    "executioners-oath", "Executioner's Oath", "reaper-edge",
                    "lineage:predatory-vow:" + artifact.getLatentLineage() + ":pressure=" + (history + convergencePressure),
                    "lore:execution-chain:boss=" + rep.getBossKills(),
                    "execution|brutality|aggression", signature(memoryProfile, artifact));
            case "deadeye" -> qualify(force || (rep.getPrecision() >= 14 && artifact.getMemory().count(ArtifactMemoryEvent.PRECISION_STREAK) >= 2
                    && memoryProfile.disciplineWeight() >= 5.0D),
                    "stormblade", "Stormblade", "tempest-sight",
                    "lineage:discipline-vector:" + artifact.getLatentLineage() + ":pressure=" + (history + lineagePressure),
                    "lore:precision-storm:boss=" + rep.getBossKills(),
                    "precision|mobility|discipline", signature(memoryProfile, artifact));
            case "vanguard" -> qualify(force || (rep.getSurvival() >= 14 && artifact.getMemory().count(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL) >= 2
                    && memoryProfile.survivalWeight() >= 5.0D),
                    "bulwark-ascendant", "Bulwark Ascendant", "citadel-heart",
                    "lineage:guardian-pressure:" + artifact.getLatentLineage() + ":pressure=" + (history + convergencePressure),
                    "lore:last-wall:trauma=" + (int) memoryProfile.traumaWeight(),
                    "survival|consistency|anchor", signature(memoryProfile, artifact));
            case "strider" -> qualify(force || (rep.getMobility() >= 14 && rep.getRecentKillChain() >= 4 && memoryProfile.mobilityWeight() >= 3.0D),
                    "tempest-stride", "Tempest Stride", "wind-channel",
                    "lineage:motion-breakpoint:" + artifact.getLatentLineage() + ":pressure=" + (history + rep.getRecentKillChain()),
                    "lore:velocity-cascade:chain=" + rep.getRecentKillChain(),
                    "mobility|chaos|skirmish", signature(memoryProfile, artifact));
            case "harbinger" -> qualify(force || (rep.getChaos() >= 14 && artifact.getMemory().count(ArtifactMemoryEvent.CHAOS_RAMPAGE) >= 2
                    && memoryProfile.chaosWeight() >= 5.0D),
                    "voidwake-covenant", "Voidwake Covenant", "entropy-gate",
                    "lineage:void-pressure:" + artifact.getLatentLineage() + ":pressure=" + (history + convergencePressure + lineagePressure),
                    "lore:rampage-echo:chaos=" + rep.getChaos(),
                    "chaos|memory|voidwake", signature(memoryProfile, artifact));
            case "warden" -> qualify(force || (rep.getSurvival() >= 12 && rep.getConsistency() >= 12
                    && artifact.getMemory().count(ArtifactMemoryEvent.LONG_BATTLE) >= 2 && memoryProfile.traumaWeight() >= 2.0D),
                    "last-survivor", "Last Survivor", "unyielding-guard",
                    "lineage:endurance-knot:" + artifact.getLatentLineage() + ":pressure=" + (history + rep.getSurvivalStreak()),
                    "lore:survivor-knot:streak=" + rep.getSurvivalStreak(),
                    "survival|precision|unyielding", signature(memoryProfile, artifact));
            case "paragon" -> qualify(force || (rep.getTotalScore() >= 84 && memoryProfile.pressure() >= 8 && history >= 16
                    && rep.getConsistency() >= 12 && rep.getSurvival() >= 12),
                    "crown-of-equilibrium", "Crown of Equilibrium", "harmonic-axis",
                    "lineage:equilibrium-bridge:" + artifact.getLatentLineage() + ":pressure=" + (history + convergencePressure + lineagePressure),
                    "lore:balanced-breach:score=" + rep.getTotalScore(),
                    "consistency|survival|harmonic", signature(memoryProfile, artifact));
            default -> null;
        };
    }

    private AwakeningResolution qualify(boolean matched,
                                        String profileId,
                                        String path,
                                        String identityShape,
                                        String lineageTrace,
                                        String loreTrace,
                                        String expressionTrace,
                                        String memorySignature) {
        if (!matched) {
            return null;
        }
        String variantId = profileId + "::" + identityShape + "::" + Integer.toHexString(memorySignature.hashCode());
        return new AwakeningResolution(profileId, path, identityShape, variantId, lineageTrace, loreTrace, expressionTrace,
                memorySignature, profiles.get(path));
    }

    private String signature(ArtifactMemoryProfile memoryProfile, Artifact artifact) {
        return "pressure=" + memoryProfile.pressure()
                + "|agg=" + Math.round(memoryProfile.aggressionWeight() * 10.0D)
                + "|surv=" + Math.round(memoryProfile.survivalWeight() * 10.0D)
                + "|disc=" + Math.round(memoryProfile.disciplineWeight() * 10.0D)
                + "|chaos=" + Math.round(memoryProfile.chaosWeight() * 10.0D)
                + "|events=" + artifact.getNotableEvents().size();
    }

    private record AwakeningResolution(String profileId,
                                       String path,
                                       String identityShape,
                                       String variantId,
                                       String lineageTrace,
                                       String loreTrace,
                                       String expressionTrace,
                                       String memorySignature,
                                       AwakeningEffectProfile profile) {
        private String archetypePath() {
            return "awakened-" + profileId;
        }
    }
}
