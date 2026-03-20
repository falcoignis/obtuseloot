package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.artifacts.EquipmentRole;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.memory.MemoryInfluenceResolver;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ConvergenceEngine {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();
    private final MemoryInfluenceResolver memoryInfluenceResolver = new MemoryInfluenceResolver();

    private final AtomicInteger convergenceAttempted = new AtomicInteger();
    private final AtomicInteger convergenceBlocked = new AtomicInteger();
    private final AtomicInteger convergencePrereqFailed = new AtomicInteger();
    private final AtomicInteger convergencePairFound = new AtomicInteger();
    private final AtomicInteger convergenceApplied = new AtomicInteger();

    public ArtifactIdentityTransition evaluate(Player player, Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(player, artifact, rep);
    }

    public ArtifactIdentityTransition evaluateSimulation(Artifact artifact, ArtifactReputation rep) {
        return evaluateInternal(null, artifact, rep);
    }

    private ArtifactIdentityTransition evaluateInternal(Player player, Artifact artifact, ArtifactReputation rep) {
        convergenceAttempted.incrementAndGet();

        EquipmentArchetype current = ArtifactArchetypeValidator.requireValid(artifact, "convergence evaluation");
        if (!"none".equalsIgnoreCase(artifact.getConvergencePath())) {
            convergenceBlocked.incrementAndGet();
            return null;
        }

        ArtifactMemoryProfile memoryProfile = memoryInfluenceResolver.profileFor(artifact.getMemory());
        for (ConvergenceRecipe recipe : recipes()) {
            if (!matches(recipe, current, artifact, rep, memoryProfile)) {
                continue;
            }

            convergencePairFound.incrementAndGet();
            EquipmentArchetype target = recipe.resolveTarget(current, artifact, rep);
            Artifact replacement = createReplacement(artifact, current, target, recipe, rep, memoryProfile);
            if (player != null) {
                player.sendMessage("§6" + textResolver.compose(replacement, ArtifactTextChannel.CONVERGENCE, recipe.id()));
            }
            convergenceApplied.incrementAndGet();
            return new ArtifactIdentityTransition(artifact, replacement, "artifact-convergence:" + recipe.id());
        }

        convergencePrereqFailed.incrementAndGet();
        return null;
    }

    private boolean matches(ConvergenceRecipe recipe,
                            EquipmentArchetype current,
                            Artifact artifact,
                            ArtifactReputation rep,
                            ArtifactMemoryProfile memoryProfile) {
        return recipe.supports(current)
                && rep.getTotalScore() >= recipe.minTotalScore()
                && rep.getBossKills() >= recipe.minBossKills()
                && artifact.getHistoryScore() >= recipe.minHistoryScore()
                && memoryProfile.pressure() >= recipe.minMemoryPressure()
                && !"dormant".equalsIgnoreCase(artifact.getAwakeningPath());
    }

    private List<ConvergenceRecipe> recipes() {
        return List.of(
                new ConvergenceRecipe("reaper-vow", EnumSet.of(EquipmentRole.WEAPON, EquipmentRole.MELEE_WEAPON), 96, 1, 6) {
                    @Override EquipmentArchetype resolveTarget(EquipmentArchetype current, Artifact artifact, ArtifactReputation rep) {
                        return rep.getMobility() >= rep.getPrecision() ? EquipmentArchetype.NETHERITE_AXE : EquipmentArchetype.NETHERITE_SWORD;
                    }

                    @Override String evolutionPath(Artifact artifact, ArtifactReputation rep) { return "converged-reaper-vow"; }
                    @Override String archetypePath(Artifact artifact, ArtifactReputation rep) { return "reaper"; }
                },
                new ConvergenceRecipe("horizon-syndicate", EnumSet.of(EquipmentRole.RANGED_WEAPON), 92, 1, 5) {
                    @Override EquipmentArchetype resolveTarget(EquipmentArchetype current, Artifact artifact, ArtifactReputation rep) {
                        return current == EquipmentArchetype.CROSSBOW ? EquipmentArchetype.BOW : EquipmentArchetype.CROSSBOW;
                    }

                    @Override String evolutionPath(Artifact artifact, ArtifactReputation rep) { return "converged-horizon-syndicate"; }
                    @Override String archetypePath(Artifact artifact, ArtifactReputation rep) { return "horizon"; }
                },
                new ConvergenceRecipe("sky-bastion", EnumSet.of(EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.MOBILITY), 94, 1, 5) {
                    @Override boolean supports(EquipmentArchetype current) {
                        return current == EquipmentArchetype.ELYTRA || current.hasRole(EquipmentRole.CHESTPLATE) || current.hasRole(EquipmentRole.BOOTS);
                    }

                    @Override EquipmentArchetype resolveTarget(EquipmentArchetype current, Artifact artifact, ArtifactReputation rep) {
                        return EquipmentArchetype.ELYTRA;
                    }

                    @Override String evolutionPath(Artifact artifact, ArtifactReputation rep) { return "converged-sky-bastion"; }
                    @Override String archetypePath(Artifact artifact, ArtifactReputation rep) { return "aegis-wing"; }
                },
                new ConvergenceRecipe("citadel-heart", EnumSet.of(EquipmentRole.DEFENSIVE_ARMOR), 100, 2, 7) {
                    @Override EquipmentArchetype resolveTarget(EquipmentArchetype current, Artifact artifact, ArtifactReputation rep) {
                        if (current.hasRole(EquipmentRole.HELMET)) return EquipmentArchetype.NETHERITE_HELMET;
                        if (current.hasRole(EquipmentRole.CHESTPLATE)) return EquipmentArchetype.NETHERITE_CHESTPLATE;
                        if (current.hasRole(EquipmentRole.LEGGINGS)) return EquipmentArchetype.NETHERITE_LEGGINGS;
                        return EquipmentArchetype.NETHERITE_BOOTS;
                    }

                    @Override String evolutionPath(Artifact artifact, ArtifactReputation rep) { return "converged-citadel-heart"; }
                    @Override String archetypePath(Artifact artifact, ArtifactReputation rep) { return "citadel"; }
                },
                new ConvergenceRecipe("worldpiercer", EnumSet.of(EquipmentRole.SPEAR, EquipmentRole.MOBILITY), 102, 2, 6) {
                    @Override boolean supports(EquipmentArchetype current) {
                        return current == EquipmentArchetype.TRIDENT || current == EquipmentArchetype.ELYTRA;
                    }

                    @Override EquipmentArchetype resolveTarget(EquipmentArchetype current, Artifact artifact, ArtifactReputation rep) {
                        return rep.getMobility() >= rep.getSurvival() ? EquipmentArchetype.ELYTRA : EquipmentArchetype.TRIDENT;
                    }

                    @Override String evolutionPath(Artifact artifact, ArtifactReputation rep) { return "converged-worldpiercer"; }
                    @Override String archetypePath(Artifact artifact, ArtifactReputation rep) { return "worldpiercer"; }
                }
        );
    }

    private Artifact createReplacement(Artifact artifact,
                                       EquipmentArchetype current,
                                       EquipmentArchetype target,
                                       ConvergenceRecipe recipe,
                                       ArtifactReputation rep,
                                       ArtifactMemoryProfile memoryProfile) {
        long seed = Objects.hash(
                artifact.getArtifactSeed(),
                recipe.id(),
                target.id(),
                artifact.getHistoryScore(),
                rep.getTotalScore(),
                memoryProfile.pressure(),
                artifact.getMemory().snapshot().hashCode());

        Artifact replacement = new Artifact(artifact.getOwnerId(), target);
        replacement.setArtifactStorageKey(artifact.getArtifactStorageKey());
        replacement.setArtifactSeed(seed);
        replacement.setOwnerId(artifact.getOwnerId());
        replacement.setArchetypePath(recipe.archetypePath(artifact, rep));
        replacement.setEvolutionPath(recipe.evolutionPath(artifact, rep));
        replacement.setAwakeningPath(artifact.getAwakeningPath());
        replacement.setConvergencePath(recipe.id());
        replacement.setDriftLevel(artifact.getDriftLevel());
        replacement.setTotalDrifts(artifact.getTotalDrifts());
        replacement.setDriftAlignment(artifact.getDriftAlignment());
        replacement.setLastDriftTimestamp(artifact.getLastDriftTimestamp());
        replacement.setLatentLineage(artifact.getLatentLineage());
        replacement.setSpeciesId(artifact.getSpeciesId());
        replacement.setParentSpeciesId(artifact.getParentSpeciesId());
        replacement.setLastSpeciesCompatibilityDistance(artifact.getLastSpeciesCompatibilityDistance());
        replacement.setInstabilityState(artifact.getCurrentInstabilityState(), artifact.getInstabilityExpiryTimestamp());
        replacement.setSeedPrecisionAffinity(blendedAffinity(artifact, rep, "precision", current, target));
        replacement.setSeedBrutalityAffinity(blendedAffinity(artifact, rep, "brutality", current, target));
        replacement.setSeedSurvivalAffinity(blendedAffinity(artifact, rep, "survival", current, target));
        replacement.setSeedMobilityAffinity(blendedAffinity(artifact, rep, "mobility", current, target));
        replacement.setSeedChaosAffinity(blendedAffinity(artifact, rep, "chaos", current, target));
        replacement.setSeedConsistencyAffinity(blendedAffinity(artifact, rep, "consistency", current, target));
        replacement.getDriftBiasAdjustments().putAll(artifact.getDriftBiasAdjustments());
        replacement.getAwakeningBiasAdjustments().putAll(artifact.getAwakeningBiasAdjustments());
        replacement.getAwakeningGainMultipliers().putAll(artifact.getAwakeningGainMultipliers());
        replacement.getAwakeningTraits().addAll(artifact.getAwakeningTraits());
        replacement.getMemory().restore(artifact.getMemory().snapshot());
        replacement.getDriftHistory().addAll(artifact.getDriftHistory());
        replacement.getLoreHistory().addAll(artifact.getLoreHistory());
        replacement.getNotableEvents().addAll(artifact.getNotableEvents());
        replacement.setLastAbilityBranchPath(artifact.getLastAbilityBranchPath());
        replacement.setLastMutationHistory(artifact.getLastMutationHistory());
        replacement.setLastMemoryInfluence(artifact.getLastMemoryInfluence());
        replacement.setLastRegulatoryProfile(artifact.getLastRegulatoryProfile());
        replacement.setLastOpenRegulatoryGates(artifact.getLastOpenRegulatoryGates());
        replacement.setLastGateCandidatePool(artifact.getLastGateCandidatePool());
        replacement.setLastTriggerProfile(artifact.getLastTriggerProfile());
        replacement.setLastMechanicProfile(artifact.getLastMechanicProfile());
        replacement.setLastInterferenceEffects(artifact.getLastInterferenceEffects());
        replacement.setLastLatentActivationRate(artifact.getLastLatentActivationRate());
        replacement.setLastActivatedLatentTraits(artifact.getLastActivatedLatentTraits());
        replacement.setLastUtilityHistory(artifact.getLastUtilityHistory());
        replacement.setNaming(ArtifactNameResolver.initialize(replacement));
        replacement.addLoreHistory("Convergence replaced " + current.id() + " with " + target.id() + " via " + recipe.id());
        replacement.addLoreHistory(textResolver.compose(replacement, ArtifactTextChannel.CONVERGENCE, recipe.id()));
        replacement.addNotableEvent("identity.replaced." + artifact.getArtifactSeed());
        replacement.addNotableEvent("convergence." + recipe.id());
        return replacement;
    }

    private double blendedAffinity(Artifact artifact, ArtifactReputation rep, String stat, EquipmentArchetype current, EquipmentArchetype target) {
        double roleBias = target.roles().stream().mapToDouble(role -> switch (role) {
            case WEAPON, MELEE_WEAPON -> "brutality".equals(stat) || "precision".equals(stat) ? 0.18D : 0.0D;
            case TOOL, TOOL_WEAPON_HYBRID -> "mobility".equals(stat) || "consistency".equals(stat) ? 0.12D : 0.0D;
            case DEFENSIVE_ARMOR, ARMOR, HELMET, CHESTPLATE, LEGGINGS, BOOTS -> "survival".equals(stat) || "consistency".equals(stat) ? 0.18D : 0.0D;
            case MOBILITY, TRAVERSAL -> "mobility".equals(stat) ? 0.22D : 0.0D;
            case RANGED_WEAPON, SPEAR -> "precision".equals(stat) || "mobility".equals(stat) ? 0.15D : 0.0D;
        }).sum();
        double reputationBias = switch (stat) {
            case "precision" -> rep.getPrecision() / 80.0D;
            case "brutality" -> rep.getBrutality() / 80.0D;
            case "survival" -> rep.getSurvival() / 80.0D;
            case "mobility" -> rep.getMobility() / 80.0D;
            case "chaos" -> rep.getChaos() / 80.0D;
            case "consistency" -> rep.getConsistency() / 80.0D;
            default -> 0.0D;
        };
        double continuity = artifact.getSeedAffinity(stat) * (current == target ? 0.58D : 0.42D);
        return Math.max(0.0D, Math.min(1.0D, continuity + roleBias + reputationBias));
    }

    public Map<String, Integer> diagnosticCounters() {
        return Map.of(
                "convergence_attempted", convergenceAttempted.get(),
                "convergence_blocked", convergenceBlocked.get(),
                "convergence_prereq_failed", convergencePrereqFailed.get(),
                "convergence_pair_found", convergencePairFound.get(),
                "convergence_applied", convergenceApplied.get()
        );
    }

    private abstract static class ConvergenceRecipe {
        private final String id;
        private final EnumSet<EquipmentRole> semanticRoles;
        private final int minTotalScore;
        private final int minBossKills;
        private final int minMemoryPressure;

        private ConvergenceRecipe(String id, EnumSet<EquipmentRole> semanticRoles, int minTotalScore, int minBossKills, int minMemoryPressure) {
            this.id = id;
            this.semanticRoles = semanticRoles.clone();
            this.minTotalScore = minTotalScore;
            this.minBossKills = minBossKills;
            this.minMemoryPressure = minMemoryPressure;
        }

        String id() { return id; }
        int minTotalScore() { return minTotalScore; }
        int minBossKills() { return minBossKills; }
        int minMemoryPressure() { return minMemoryPressure; }
        int minHistoryScore() { return Math.max(6, minTotalScore / 12); }

        boolean supports(EquipmentArchetype current) {
            return semanticRoles.stream().allMatch(current::hasRole);
        }

        abstract EquipmentArchetype resolveTarget(EquipmentArchetype current, Artifact artifact, ArtifactReputation rep);
        abstract String evolutionPath(Artifact artifact, ArtifactReputation rep);
        abstract String archetypePath(Artifact artifact, ArtifactReputation rep);
    }
}
