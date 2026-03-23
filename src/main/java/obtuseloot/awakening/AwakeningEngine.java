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
import java.util.Set;

public class AwakeningEngine {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();
    private final MemoryInfluenceResolver memoryInfluenceResolver = new MemoryInfluenceResolver();

    // Profiles store base bias/multiplier values; depth scaling is applied at evaluation time.
    private final Map<String, AwakeningEffectProfile> profiles = Map.of(
            "Executioner's Oath", new AwakeningEffectProfile(
                    "Executioner's Oath",
                    Map.of("brutality", 1.6, "chaos", 0.4),
                    Map.of("brutality", 2.0),
                    Set.of("execution", "finisher", "oathbound")),
            "Stormblade", new AwakeningEffectProfile(
                    "Stormblade",
                    Map.of("precision", 1.4, "mobility", 0.8),
                    Map.of("precision", 2.0),
                    Set.of("stormstep", "linebreaker")),
            "Bulwark Ascendant", new AwakeningEffectProfile(
                    "Bulwark Ascendant",
                    Map.of("survival", 1.5, "consistency", 1.0),
                    Map.of("survival", 2.0),
                    Set.of("fortress", "anchor")),
            "Tempest Stride", new AwakeningEffectProfile(
                    "Tempest Stride",
                    Map.of("mobility", 1.5, "chaos", 0.7),
                    Map.of("mobility", 2.0),
                    Set.of("windrunner", "skirmish")),
            "Voidwake Covenant", new AwakeningEffectProfile(
                    "Voidwake Covenant",
                    Map.of("chaos", 1.8, "precision", 0.5),
                    Map.of("chaos", 2.0),
                    Set.of("voidwake", "entropy-mark")),
            "Last Survivor", new AwakeningEffectProfile(
                    "Last Survivor",
                    Map.of("survival", 1.3, "precision", 1.0),
                    Map.of("survival", 2.0),
                    Set.of("last-stand", "unyielding"))
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
        boolean convergencePreceded = !"none".equalsIgnoreCase(artifact.getConvergencePath());
        int historyScore = artifact.getHistoryScore();
        double depthMult = depthMultiplier(memoryProfile, historyScore, convergencePreceded);

        // 64-bit seed mixing from durable, evaluation-stable fields only.
        // Excludes ephemeral fields (lastUtilityHistory etc.) and mutable totals.
        long seed = mix64(
                artifact.getArtifactSeed(),
                resolution.profileId().hashCode(),
                resolution.identityShape().hashCode(),
                artifact.getConvergencePath().hashCode(),
                artifact.getConvergenceVariantId().hashCode(),
                (long) artifact.getMemory().snapshot().hashCode(),
                memoryProfile.pressure(),
                historyScore,
                artifact.getLatentLineage().hashCode()
        );

        String continuityTrace = continuityTrace(artifact, memoryProfile, historyScore, convergencePreceded);

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
        replacement.setSeedPrecisionAffinity(reblendedAffinity(artifact, resolution, "precision", depthMult));
        replacement.setSeedBrutalityAffinity(reblendedAffinity(artifact, resolution, "brutality", depthMult));
        replacement.setSeedSurvivalAffinity(reblendedAffinity(artifact, resolution, "survival", depthMult));
        replacement.setSeedMobilityAffinity(reblendedAffinity(artifact, resolution, "mobility", depthMult));
        replacement.setSeedChaosAffinity(reblendedAffinity(artifact, resolution, "chaos", depthMult));
        replacement.setSeedConsistencyAffinity(reblendedAffinity(artifact, resolution, "consistency", depthMult));
        replacement.getDriftBiasAdjustments().putAll(artifact.getDriftBiasAdjustments());
        replacement.getMemory().restore(artifact.getMemory().snapshot());
        replacement.getDriftHistory().addAll(artifact.getDriftHistory());
        replacement.getLoreHistory().addAll(artifact.getLoreHistory());
        replacement.getNotableEvents().addAll(artifact.getNotableEvents());
        replacement.getAwakeningTraits().addAll(resolution.profile().traits());
        // Depth-scaled bias adjustments and gain multipliers.
        resolution.profile().biasAdjustments().forEach((k, v) ->
                replacement.getAwakeningBiasAdjustments().put(k, v * depthMult));
        resolution.profile().reputationGainMultipliers().forEach((k, v) ->
                replacement.getAwakeningGainMultipliers().put(k, Math.min(3.0, v * depthMult)));
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
        replacement.setAwakeningContinuityTrace(continuityTrace);
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

    // Base affinity delta is scaled by awakening depth multiplier.
    private double reblendedAffinity(Artifact artifact, AwakeningResolution resolution, String stat, double depthMult) {
        double base = artifact.getSeedAffinity(stat);
        double rawDelta = resolution.profile().biasAdjustments().getOrDefault(stat, 0.0D);
        return Math.max(-4.0D, Math.min(4.0D, base + rawDelta * depthMult));
    }

    private AwakeningResolution resolve(Artifact artifact,
                                        ArtifactReputation rep,
                                        ArtifactMemoryProfile memoryProfile,
                                        boolean force) {
        int history = artifact.getHistoryScore();
        int convergencePressure = "none".equalsIgnoreCase(artifact.getConvergencePath()) ? 0 : 4;
        int lineagePressure = artifact.getLatentLineage().equalsIgnoreCase("common") ? 0 : 2;
        boolean convergencePreceded = convergencePressure > 0;
        if (!force && (memoryProfile.pressure() < 5 || history < 10 || rep.getKills() < 6)) {
            return null;
        }
        return switch (artifact.getArchetypePath()) {
            // Redundant count gates removed: aggressionWeight >= 6.0 subsumes MULTIKILL_CHAIN >= 2.
            case "ravager" -> qualify(
                    force || (rep.getBrutality() >= 14 && memoryProfile.aggressionWeight() >= 6.0D),
                    "executioners-oath", "Executioner's Oath",
                    identityShape("reaper-edge", memoryProfile, history, convergencePreceded),
                    "lineage:predatory-vow:" + artifact.getLatentLineage() + ":pressure=" + (history + convergencePressure),
                    "lore:execution-chain:boss=" + rep.getBossKills(),
                    expressionTrace(memoryProfile, rep, convergencePreceded, "exec"),
                    signature(memoryProfile, artifact));
            // disciplineWeight >= 5.0 subsumes PRECISION_STREAK >= 2.
            case "deadeye" -> qualify(
                    force || (rep.getPrecision() >= 14 && memoryProfile.disciplineWeight() >= 5.0D),
                    "stormblade", "Stormblade",
                    identityShape("tempest-sight", memoryProfile, history, convergencePreceded),
                    "lineage:discipline-vector:" + artifact.getLatentLineage() + ":pressure=" + (history + lineagePressure),
                    "lore:precision-storm:boss=" + rep.getBossKills(),
                    expressionTrace(memoryProfile, rep, convergencePreceded, "storm"),
                    signature(memoryProfile, artifact));
            // survivalWeight >= 5.0 subsumes LOW_HEALTH_SURVIVAL >= 2.
            // Identity shape renamed from citadel-heart (collides with convergence recipe id).
            case "vanguard" -> qualify(
                    force || (rep.getSurvival() >= 14 && memoryProfile.survivalWeight() >= 5.0D),
                    "bulwark-ascendant", "Bulwark Ascendant",
                    identityShape("bastion-core", memoryProfile, history, convergencePreceded),
                    "lineage:guardian-pressure:" + artifact.getLatentLineage() + ":pressure=" + (history + convergencePressure),
                    "lore:last-wall:trauma=" + (int) memoryProfile.traumaWeight(),
                    expressionTrace(memoryProfile, rep, convergencePreceded, "bulwark"),
                    signature(memoryProfile, artifact));
            // killChain threshold reduced from 4 to 2; mobilityWeight threshold from 3.0 to 2.0
            // to match the lower scalars of the contributing memory signals.
            case "strider" -> qualify(
                    force || (rep.getMobility() >= 14 && rep.getRecentKillChain() >= 2 && memoryProfile.mobilityWeight() >= 2.0D),
                    "tempest-stride", "Tempest Stride",
                    identityShape("wind-channel", memoryProfile, history, convergencePreceded),
                    "lineage:motion-breakpoint:" + artifact.getLatentLineage() + ":pressure=" + (history + rep.getRecentKillChain()),
                    "lore:velocity-cascade:chain=" + rep.getRecentKillChain(),
                    expressionTrace(memoryProfile, rep, convergencePreceded, "tempest"),
                    signature(memoryProfile, artifact));
            // chaosWeight >= 5.0 subsumes CHAOS_RAMPAGE >= 2.
            // Identity shape renamed from entropy-gate (gate/recipe connotation avoided).
            case "harbinger" -> qualify(
                    force || (rep.getChaos() >= 14 && memoryProfile.chaosWeight() >= 5.0D),
                    "voidwake-covenant", "Voidwake Covenant",
                    identityShape("void-mark", memoryProfile, history, convergencePreceded),
                    "lineage:void-pressure:" + artifact.getLatentLineage() + ":pressure=" + (history + convergencePressure + lineagePressure),
                    "lore:rampage-echo:chaos=" + rep.getChaos(),
                    expressionTrace(memoryProfile, rep, convergencePreceded, "void"),
                    signature(memoryProfile, artifact));
            // LONG_BATTLE count gate retained: not subsumed by traumaWeight (different signal source).
            case "warden" -> qualify(
                    force || (rep.getSurvival() >= 12 && rep.getConsistency() >= 12
                            && artifact.getMemory().count(ArtifactMemoryEvent.LONG_BATTLE) >= 2
                            && memoryProfile.traumaWeight() >= 2.0D),
                    "last-survivor", "Last Survivor",
                    identityShape("unyielding-guard", memoryProfile, history, convergencePreceded),
                    "lineage:endurance-knot:" + artifact.getLatentLineage() + ":pressure=" + (history + rep.getSurvivalStreak()),
                    "lore:survivor-knot:streak=" + rep.getSurvivalStreak(),
                    expressionTrace(memoryProfile, rep, convergencePreceded, "endure"),
                    signature(memoryProfile, artifact));
            // paragon removed: ArchetypeResolver never resolves "paragon", making
            // that case permanently unreachable. Removed as dead code.
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
        return new AwakeningResolution(profileId, path, identityShape, variantId, lineageTrace, loreTrace,
                expressionTrace, memorySignature, profiles.get(path));
    }

    // Derives a compound identity shape: {base}-{pressureTier}:{convergencePrefix}{dominantSignal}.
    // Pressure tier anchors depth; dominant signal reflects actual memory composition.
    private String identityShape(String base, ArtifactMemoryProfile mem, int historyScore, boolean convergencePreceded) {
        String tier = mem.pressure() >= 12 ? "crystallized" : mem.pressure() >= 8 ? "forged" : "raw";
        String signal = dominantSignal(mem);
        String prefix = convergencePreceded ? "cv-" : "";
        return base + "-" + tier + ":" + prefix + signal;
    }

    // Weight labels in a fixed, consistent order used by signal resolution.
    private static final String[] SIGNAL_LABELS =
            {"aggression", "survival", "discipline", "chaos", "boss", "trauma", "mobility"};

    /**
     * Resolves both the dominant and secondary memory-weight signals in one pass.
     * Returns a two-element array: [dominant, secondary].
     * dominant is "balanced" if the top-two weights are within 1.0 of each other.
     */
    private String[] resolveSignals(ArtifactMemoryProfile mem) {
        double[] w = {mem.aggressionWeight(), mem.survivalWeight(), mem.disciplineWeight(),
                mem.chaosWeight(), mem.bossWeight(), mem.traumaWeight(), mem.mobilityWeight()};
        int best = 0;
        for (int i = 1; i < w.length; i++) {
            if (w[i] > w[best]) best = i;
        }
        int second = (best == 0) ? 1 : 0;
        for (int i = 0; i < w.length; i++) {
            if (i != best && w[i] > w[second]) second = i;
        }
        String dominant = (w[best] - w[second]) < 1.0 ? "balanced" : SIGNAL_LABELS[best];
        return new String[]{dominant, SIGNAL_LABELS[second]};
    }

    // Returns the dominant memory weight label, or "balanced" if the top two are within 1.0.
    private String dominantSignal(ArtifactMemoryProfile mem) {
        return resolveSignals(mem)[0];
    }

    // Returns the secondary (second-highest) memory weight label.
    private String secondarySignal(ArtifactMemoryProfile mem) {
        return resolveSignals(mem)[1];
    }

    // Builds a signal-derived expression trace: {dominant}>{secondary}:{context}:{pathTag}.
    // Context reflects whether convergence preceded, memory dominates, or reputation dominates.
    private String expressionTrace(ArtifactMemoryProfile mem,
                                   ArtifactReputation rep,
                                   boolean convergencePreceded,
                                   String pathTag) {
        // Single resolveSignals call to avoid two separate O(n) weight scans.
        String[] signals = resolveSignals(mem);
        String dom = signals[0];
        String sec = signals[1];
        String context;
        if (convergencePreceded) {
            context = "convergence-inflected";
        } else if (mem.pressure() >= 10 && rep.getTotalScore() < 30) {
            context = "memory-heavy";
        } else if (rep.getTotalScore() >= 40 && mem.pressure() < 8) {
            context = "rep-heavy";
        } else {
            context = "balanced";
        }
        return dom + ">" + sec + ":" + context + ":" + pathTag;
    }

    // Reflects what was actually carried through the transition.
    // Components: owner continuity, convergence state, lineage class, drift depth,
    // memory pressure tier, history depth band.
    private String continuityTrace(Artifact artifact,
                                   ArtifactMemoryProfile mem,
                                   int historyScore,
                                   boolean convergencePreceded) {
        StringBuilder sb = new StringBuilder("owner-storage");
        if (convergencePreceded) sb.append("|convergence-inflected");
        sb.append("|lineage=").append(sanitizeLineage(artifact.getLatentLineage()));
        int totalDrifts = artifact.getTotalDrifts();
        String driftDepth = totalDrifts >= 5 ? "deep" : totalDrifts >= 2 ? "tempered" : "shallow";
        sb.append("|drift=").append(driftDepth);
        String memTier = mem.pressure() >= 12 ? "crystallized" : mem.pressure() >= 8 ? "forged" : "raw";
        sb.append("|memory=").append(memTier);
        String histBand = historyScore >= 20 ? "established" : historyScore >= 14 ? "developing" : "emergent";
        sb.append("|history=").append(histBand);
        return sb.toString();
    }

    private String sanitizeLineage(String lineage) {
        if (lineage == null || lineage.isEmpty() || lineage.equalsIgnoreCase("none")) return "none";
        String s = lineage.toLowerCase().replace(' ', '-');
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    // Depth multiplier for affinity deltas and gain multipliers.
    // Scales from 1.0 (shallow, pure) to 1.4 (deep, convergence-inflected, established history).
    private double depthMultiplier(ArtifactMemoryProfile mem, int historyScore, boolean convergencePreceded) {
        double mult = 1.0;
        if (mem.pressure() >= 12) mult += 0.2;
        else if (mem.pressure() >= 8) mult += 0.1;
        if (historyScore >= 16) mult += 0.1;
        if (convergencePreceded) mult += 0.1;
        return Math.min(1.4, mult);
    }

    // FNV-1a-inspired 64-bit mixing. Uses only durable, evaluation-stable inputs.
    private static long mix64(long... values) {
        long h = 0xcbf29ce484222325L;
        long prime = 0x00000100000001B3L;
        for (long v : values) {
            h = (h ^ v) * prime;
        }
        return h ^ (h >>> 33);
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
