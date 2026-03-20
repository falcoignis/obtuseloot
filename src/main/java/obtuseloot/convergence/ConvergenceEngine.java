package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.artifacts.EquipmentRole;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.memory.MemoryInfluenceResolver;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ConvergenceEngine {
    private static final List<ConvergenceRecipe> RECIPES = buildRecipes();

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
        ConvergenceContext context = new ConvergenceContext(artifact, current, rep, memoryProfile);
        for (ConvergenceRecipe recipe : RECIPES) {
            if (!matches(recipe, context)) {
                continue;
            }

            ConvergenceExpansion expansion = recipe.expand(context);
            if (expansion == null || expansion.target() == null || expansion.target() == current) {
                continue;
            }
            assertBoundedTarget(recipe, current, expansion);

            convergencePairFound.incrementAndGet();
            Artifact replacement = createReplacement(context, recipe, expansion);
            if (player != null) {
                player.sendMessage("§6" + textResolver.compose(replacement, ArtifactTextChannel.CONVERGENCE, recipe.id()));
            }
            convergenceApplied.incrementAndGet();
            return new ArtifactIdentityTransition(artifact, replacement,
                    "artifact-convergence:" + recipe.id() + ":" + expansion.variantId());
        }

        convergencePrereqFailed.incrementAndGet();
        return null;
    }

    private boolean matches(ConvergenceRecipe recipe, ConvergenceContext context) {
        return recipe.supports(context.current())
                && context.rep().getTotalScore() >= recipe.minTotalScore()
                && context.rep().getBossKills() >= recipe.minBossKills()
                && context.artifact().getHistoryScore() >= recipe.minHistoryScore()
                && context.memoryProfile().pressure() >= recipe.minMemoryPressure()
                && !"dormant".equalsIgnoreCase(context.artifact().getAwakeningPath());
    }

    private static List<ConvergenceRecipe> buildRecipes() {
        List<ConvergenceRecipe> recipes = List.of(
                new ConvergenceRecipe("reaper-vow", EnumSet.of(EquipmentRole.WEAPON), 96, 1, 6,
                        List.of(EquipmentArchetype.NETHERITE_SWORD, EquipmentArchetype.NETHERITE_AXE, EquipmentArchetype.TRIDENT)) {
                    @Override boolean supports(EquipmentArchetype current) {
                        return current.hasRole(EquipmentRole.MELEE_WEAPON) || current.hasRole(EquipmentRole.TOOL_WEAPON_HYBRID);
                    }

                    @Override ConvergenceExpansion expand(ConvergenceContext context) {
                        return profile(context)
                                .preferHighScore(EquipmentArchetype.TRIDENT,
                                        context.rep().getChaos() + context.rep().getMobility() + (int) Math.round(context.memoryProfile().mobilityWeight() * 4.0D))
                                .preferHighScore(EquipmentArchetype.NETHERITE_AXE,
                                        context.rep().getBrutality() + memoryCount(context, ArtifactMemoryEvent.MULTIKILL_CHAIN) * 3)
                                .preferHighScore(EquipmentArchetype.NETHERITE_SWORD,
                                        context.rep().getPrecision() + context.rep().getConsistency()
                                                + memoryCount(context, ArtifactMemoryEvent.PRECISION_STREAK) * 3)
                                .identityBase("reaper")
                                .evolutionBase("converged-reaper-vow")
                                .build();
                    }
                },
                new ConvergenceRecipe("horizon-syndicate", EnumSet.of(EquipmentRole.WEAPON, EquipmentRole.MOBILITY), 92, 1, 5,
                        List.of(EquipmentArchetype.ELYTRA, EquipmentArchetype.TRIDENT)) {
                    @Override boolean supports(EquipmentArchetype current) {
                        return current.hasRole(EquipmentRole.RANGED_WEAPON);
                    }

                    @Override ConvergenceExpansion expand(ConvergenceContext context) {
                        return profile(context)
                                .preferHighScore(EquipmentArchetype.ELYTRA,
                                        context.rep().getMobility() + context.rep().getRecentKillChain()
                                                + (int) Math.round(context.memoryProfile().mobilityWeight() * 5.0D))
                                .preferHighScore(EquipmentArchetype.TRIDENT,
                                        context.rep().getPrecision() + context.rep().getBossKills() * 3
                                                + memoryCount(context, ArtifactMemoryEvent.PRECISION_STREAK) * 4)
                                .identityBase("horizon")
                                .evolutionBase("converged-horizon-syndicate")
                                .build();
                    }
                },
                new ConvergenceRecipe("sky-bastion", EnumSet.of(EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.MOBILITY), 94, 1, 5,
                        List.of(EquipmentArchetype.ELYTRA, EquipmentArchetype.NETHERITE_CHESTPLATE, EquipmentArchetype.NETHERITE_BOOTS)) {
                    @Override boolean supports(EquipmentArchetype current) {
                        return current == EquipmentArchetype.ELYTRA || current.hasRole(EquipmentRole.CHESTPLATE) || current.hasRole(EquipmentRole.BOOTS);
                    }

                    @Override ConvergenceExpansion expand(ConvergenceContext context) {
                        return profile(context)
                                .preferHighScore(EquipmentArchetype.ELYTRA,
                                        context.rep().getMobility() + context.rep().getChaos()
                                                + (int) Math.round(context.memoryProfile().mobilityWeight() * 5.0D))
                                .preferHighScore(EquipmentArchetype.NETHERITE_CHESTPLATE,
                                        context.rep().getSurvival() + context.rep().getConsistency()
                                                + memoryCount(context, ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL) * 4)
                                .preferHighScore(EquipmentArchetype.NETHERITE_BOOTS,
                                        context.rep().getMobility() + context.rep().getSurvival()
                                                + memoryCount(context, ArtifactMemoryEvent.LONG_BATTLE) * 3)
                                .identityBase("aegis-wing")
                                .evolutionBase("converged-sky-bastion")
                                .build();
                    }
                },
                new ConvergenceRecipe("citadel-heart", EnumSet.of(EquipmentRole.DEFENSIVE_ARMOR), 100, 2, 7,
                        List.of(EquipmentArchetype.NETHERITE_CHESTPLATE, EquipmentArchetype.TURTLE_HELMET, EquipmentArchetype.NETHERITE_HELMET)) {
                    @Override ConvergenceExpansion expand(ConvergenceContext context) {
                        return profile(context)
                                .preferHighScore(EquipmentArchetype.NETHERITE_CHESTPLATE,
                                        context.rep().getSurvival() + context.rep().getConsistency()
                                                + memoryCount(context, ArtifactMemoryEvent.LONG_BATTLE) * 3)
                                .preferHighScore(EquipmentArchetype.TURTLE_HELMET,
                                        context.rep().getSurvival() + context.rep().getBossKills() * 4
                                                + memoryCount(context, ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL) * 5)
                                .preferHighScore(EquipmentArchetype.NETHERITE_HELMET,
                                        context.rep().getConsistency() + context.rep().getSurvivalStreak() * 3
                                                + memoryCount(context, ArtifactMemoryEvent.FIRST_BOSS_KILL) * 4)
                                .identityBase("citadel")
                                .evolutionBase("converged-citadel-heart")
                                .build();
                    }
                },
                new ConvergenceRecipe("worldpiercer", EnumSet.of(EquipmentRole.SPEAR, EquipmentRole.MOBILITY), 102, 2, 6,
                        List.of(EquipmentArchetype.TRIDENT, EquipmentArchetype.ELYTRA)) {
                    @Override boolean supports(EquipmentArchetype current) {
                        return current == EquipmentArchetype.TRIDENT || current == EquipmentArchetype.ELYTRA;
                    }

                    @Override ConvergenceExpansion expand(ConvergenceContext context) {
                        return profile(context)
                                .preferHighScore(EquipmentArchetype.ELYTRA,
                                        context.rep().getMobility() + context.rep().getRecentKillChain()
                                                + (int) Math.round(context.memoryProfile().mobilityWeight() * 4.0D))
                                .preferHighScore(EquipmentArchetype.TRIDENT,
                                        context.rep().getPrecision() + context.rep().getBossKills() * 3
                                                + memoryCount(context, ArtifactMemoryEvent.FIRST_BOSS_KILL) * 4)
                                .identityBase("worldpiercer")
                                .evolutionBase("converged-worldpiercer")
                                .build();
                    }
                }
        );
        recipes.forEach(ConvergenceRecipe::assertSemanticIntegrity);
        return recipes;
    }

    static List<String> recipeIntegrityDiagnostics() {
        return RECIPES.stream()
                .map(ConvergenceRecipe::integritySummary)
                .toList();
    }

    private static int memoryCount(ConvergenceContext context, ArtifactMemoryEvent event) {
        return context.artifact().getMemory().count(event);
    }

    private static ExpansionBuilder profile(ConvergenceContext context) {
        return new ExpansionBuilder(context);
    }

    private void assertBoundedTarget(ConvergenceRecipe recipe,
                                     EquipmentArchetype current,
                                     ConvergenceExpansion expansion) {
        EquipmentArchetype target = ArtifactArchetypeValidator.requireValidArchetype(expansion.target().id(),
                "convergence target " + recipe.id());
        if (target == current) {
            throw new IllegalStateException("Convergence recipe " + recipe.id() + " cannot preserve the current identity target " + current.id());
        }
        if (!recipe.isValidTarget(target)) {
            throw new IllegalStateException("Convergence recipe " + recipe.id() + " selected out-of-bounds target " + target.id());
        }
        if (!recipe.supportsTarget(target)) {
            throw new IllegalStateException("Convergence recipe " + recipe.id() + " selected target " + target.id()
                    + " outside recipe semantic roles " + recipe.semanticRoles());
        }
    }

    private Artifact createReplacement(ConvergenceContext context,
                                       ConvergenceRecipe recipe,
                                       ConvergenceExpansion expansion) {
        Artifact artifact = context.artifact();
        EquipmentArchetype current = context.current();
        EquipmentArchetype target = expansion.target();
        ArtifactReputation rep = context.rep();
        ArtifactMemoryProfile memoryProfile = context.memoryProfile();
        long seed = Objects.hash(
                artifact.getArtifactSeed(),
                recipe.id(),
                target.id(),
                expansion.variantId(),
                expansion.identityShape(),
                artifact.getHistoryScore(),
                rep.getTotalScore(),
                memoryProfile.pressure(),
                artifact.getMemory().snapshot().hashCode(),
                artifact.getNotableEvents().hashCode(),
                artifact.getLoreHistory().hashCode());

        Artifact replacement = new Artifact(artifact.getOwnerId(), target);
        replacement.setPersistenceOriginTimestamp(artifact.getPersistenceOriginTimestamp());
        replacement.setIdentityBirthTimestamp(System.currentTimeMillis());
        replacement.setArtifactStorageKey(artifact.getArtifactStorageKey());
        replacement.setArtifactSeed(seed);
        replacement.setOwnerId(artifact.getOwnerId());
        replacement.setArchetypePath(expansion.archetypePath());
        replacement.setEvolutionPath(expansion.evolutionPath());
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
        replacement.setSeedPrecisionAffinity(blendedAffinity(context, expansion, "precision"));
        replacement.setSeedBrutalityAffinity(blendedAffinity(context, expansion, "brutality"));
        replacement.setSeedSurvivalAffinity(blendedAffinity(context, expansion, "survival"));
        replacement.setSeedMobilityAffinity(blendedAffinity(context, expansion, "mobility"));
        replacement.setSeedChaosAffinity(blendedAffinity(context, expansion, "chaos"));
        replacement.setSeedConsistencyAffinity(blendedAffinity(context, expansion, "consistency"));
        replacement.getDriftBiasAdjustments().putAll(artifact.getDriftBiasAdjustments());
        replacement.getAwakeningBiasAdjustments().putAll(artifact.getAwakeningBiasAdjustments());
        replacement.getAwakeningGainMultipliers().putAll(artifact.getAwakeningGainMultipliers());
        replacement.getAwakeningTraits().addAll(artifact.getAwakeningTraits());
        replacement.getMemory().restore(artifact.getMemory().snapshot());
        replacement.getDriftHistory().addAll(artifact.getDriftHistory());
        replacement.getLoreHistory().addAll(artifact.getLoreHistory());
        replacement.getNotableEvents().addAll(artifact.getNotableEvents());
        replacement.setConvergenceVariantId(expansion.variantId());
        replacement.setConvergenceIdentityShape(expansion.identityShape());
        replacement.setConvergenceLineageTrace(expansion.lineageTrace());
        replacement.setConvergenceLoreTrace(expansion.loreTrace());
        replacement.setConvergenceContinuityTrace(expansion.continuityTrace());
        replacement.setConvergenceExpressionTrace(expansion.expressionTrace());
        replacement.setConvergenceMemorySignature(expansion.memorySignature());
        replacement.setNaming(ArtifactNameResolver.initialize(replacement));
        replacement.addLoreHistory("Convergence replaced " + current.id() + " with " + target.id() + " via " + recipe.id() + " [" + expansion.variantId() + "]");
        replacement.addLoreHistory(textResolver.compose(replacement, ArtifactTextChannel.CONVERGENCE, recipe.id()));
        replacement.addLoreHistory("Convergence identity shaped as " + expansion.identityShape() + "; lore trace=" + expansion.loreTrace());
        replacement.addNotableEvent("identity.replaced." + artifact.getArtifactSeed());
        replacement.addNotableEvent("convergence." + recipe.id());
        replacement.addNotableEvent("convergence.variant." + expansion.variantId());
        replacement.addNotableEvent("convergence.lineage." + expansion.lineageTrace());
        replacement.addNotableEvent("convergence.expression." + expansion.expressionTrace());
        replacement.addNotableEvent("convergence.memory." + expansion.memorySignature());
        return replacement;
    }

    private double blendedAffinity(ConvergenceContext context, ConvergenceExpansion expansion, String stat) {
        EquipmentArchetype current = context.current();
        EquipmentArchetype target = expansion.target();
        Artifact artifact = context.artifact();
        ArtifactReputation rep = context.rep();
        ArtifactMemoryProfile memoryProfile = context.memoryProfile();
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
        double continuity = artifact.getSeedAffinity(stat) * expansion.continuityWeight(current == target);
        double memoryBias = switch (stat) {
            case "precision" -> memoryProfile.disciplineWeight() * 0.08D;
            case "brutality" -> memoryProfile.aggressionWeight() * 0.07D;
            case "survival" -> memoryProfile.survivalWeight() * 0.08D;
            case "mobility" -> memoryProfile.mobilityWeight() * 0.09D;
            case "chaos" -> memoryProfile.chaosWeight() * 0.08D;
            case "consistency" -> memoryProfile.disciplineWeight() * 0.05D + memoryProfile.bossWeight() * 0.03D;
            default -> 0.0D;
        };
        double shapeBias = expansion.shapeAffinity(stat);
        return Math.max(0.0D, Math.min(1.0D, continuity + roleBias + reputationBias + memoryBias + shapeBias));
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

    private record ConvergenceContext(
            Artifact artifact,
            EquipmentArchetype current,
            ArtifactReputation rep,
            ArtifactMemoryProfile memoryProfile
    ) {
    }

    private record CandidateScore(EquipmentArchetype target, int score) {
    }

    private record ConvergenceExpansion(
            EquipmentArchetype target,
            String variantId,
            String archetypePath,
            String evolutionPath,
            String identityShape,
            String lineageTrace,
            String loreTrace,
            String continuityTrace,
            String expressionTrace,
            String memorySignature,
            Map<String, Double> shapeAffinities
    ) {
        double continuityWeight(boolean sameIdentity) {
            return sameIdentity ? 0.54D : 0.34D + shapeAffinities.getOrDefault("continuity", 0.0D);
        }

        double shapeAffinity(String stat) {
            return shapeAffinities.getOrDefault(stat, 0.0D);
        }
    }

    private abstract static class ConvergenceRecipe {
        private final String id;
        private final EnumSet<EquipmentRole> semanticRoles;
        private final int minTotalScore;
        private final int minBossKills;
        private final int minMemoryPressure;
        private final List<EquipmentArchetype> validTargets;

        private ConvergenceRecipe(String id,
                                  EnumSet<EquipmentRole> semanticRoles,
                                  int minTotalScore,
                                  int minBossKills,
                                  int minMemoryPressure,
                                  List<EquipmentArchetype> validTargets) {
            this.id = id;
            this.semanticRoles = semanticRoles.clone();
            this.minTotalScore = minTotalScore;
            this.minBossKills = minBossKills;
            this.minMemoryPressure = minMemoryPressure;
            this.validTargets = List.copyOf(validTargets);
        }

        String id() { return id; }
        int minTotalScore() { return minTotalScore; }
        int minBossKills() { return minBossKills; }
        int minMemoryPressure() { return minMemoryPressure; }
        int minHistoryScore() { return Math.max(6, minTotalScore / 12); }

        boolean supports(EquipmentArchetype current) {
            return semanticRoles.stream().allMatch(current::hasRole);
        }

        boolean isValidTarget(EquipmentArchetype target) {
            return target != null && validTargets.contains(target);
        }

        boolean supportsTarget(EquipmentArchetype target) {
            return semanticRoles.stream().anyMatch(target::hasRole);
        }

        EnumSet<EquipmentRole> semanticRoles() {
            return semanticRoles.clone();
        }

        void assertSemanticIntegrity() {
            if (semanticRoles.isEmpty()) {
                throw new IllegalStateException("Convergence recipe " + id + " must declare at least one semantic role");
            }
            List<String> invalidTargets = validTargets.stream()
                    .filter(target -> !supportsTarget(target))
                    .map(EquipmentArchetype::id)
                    .toList();
            if (!invalidTargets.isEmpty()) {
                throw new IllegalStateException("Convergence recipe " + id + " declares targets outside semantic roles "
                        + semanticRoles + ": " + invalidTargets);
            }
        }

        String integritySummary() {
            return id + " roles=" + semanticRoles + " targets=" + validTargets.stream().map(EquipmentArchetype::id).toList();
        }

        abstract ConvergenceExpansion expand(ConvergenceContext context);
    }

    private static final class ExpansionBuilder {
        private final ConvergenceContext context;
        private static final int NOVELTY_SCORE_WINDOW = 3;

        private final List<CandidateScore> candidates = new ArrayList<>();
        private String identityBase = "converged";
        private String evolutionBase = "converged";

        private ExpansionBuilder(ConvergenceContext context) {
            this.context = context;
        }

        ExpansionBuilder preferHighScore(EquipmentArchetype target, int score) {
            if (target != null && target != context.current()) {
                candidates.add(new CandidateScore(target, score));
            }
            return this;
        }

        ExpansionBuilder identityBase(String identityBase) {
            this.identityBase = identityBase;
            return this;
        }

        ExpansionBuilder evolutionBase(String evolutionBase) {
            this.evolutionBase = evolutionBase;
            return this;
        }

        ConvergenceExpansion build() {
            if (candidates.isEmpty()) {
                return null;
            }
            int bestScore = candidates.stream().mapToInt(CandidateScore::score).max().orElse(Integer.MIN_VALUE);
            List<CandidateScore> finalistPool = candidates.stream()
                    .filter(candidate -> bestScore - candidate.score() <= NOVELTY_SCORE_WINDOW)
                    .sorted((left, right) -> {
                        int byScore = Integer.compare(right.score(), left.score());
                        if (byScore != 0) return byScore;
                        int tie = Integer.compare(noveltyTieBreaker(left.target()), noveltyTieBreaker(right.target()));
                        if (tie != 0) return tie;
                        return left.target().id().compareTo(right.target().id());
                    })
                    .toList();
            CandidateScore best = finalistPool.get(Math.floorMod(historySalt(), finalistPool.size()));
            if (best == null) {
                return null;
            }
            String vector = dominantVector();
            String cadence = convergenceCadence();
            String memorySignature = memorySignature();
            String identityShape = identityBase + "-" + vector + "-" + cadence;
            String variantId = best.target().id() + "-" + vector + "-" + cadence;
            String archetypePath = identityBase + "-" + vector;
            String evolutionPath = evolutionBase + "-" + cadence;
            String lineageTrace = vector + ":" + cadence + ":" + context.artifact().getLatentLineage();
            String loreTrace = context.artifact().getAwakeningPath().toLowerCase().replace(' ', '-') + ":" + vector + ":" + memorySignature;
            String continuityTrace = "seed=" + continuitySeed() + "|carry=" + carryOverClass();
            String expressionTrace = best.target().id() + ":" + vector + ":" + roleBlend();
            return new ConvergenceExpansion(
                    best.target(),
                    variantId,
                    archetypePath,
                    evolutionPath,
                    identityShape,
                    lineageTrace,
                    loreTrace,
                    continuityTrace,
                    expressionTrace,
                    memorySignature,
                    shapeAffinities(vector, cadence));
        }

        private int noveltyTieBreaker(EquipmentArchetype target) {
            int memory = context.memoryProfile().pressure() + context.artifact().getHistoryScore();
            int eventHash = context.artifact().getNotableEvents().hashCode() ^ context.artifact().getLoreHistory().hashCode();
            return Math.abs(Objects.hash(context.artifact().getArtifactSeed(), target.id(), memory, eventHash)) % 1000;
        }

        private int historySalt() {
            Artifact artifact = context.artifact();
            return Objects.hash(
                    artifact.getArtifactSeed(),
                    artifact.getHistoryScore(),
                    artifact.getLoreHistory().hashCode(),
                    artifact.getNotableEvents().hashCode(),
                    artifact.getMemory().snapshot().hashCode(),
                    context.rep().getTotalScore(),
                    context.memoryProfile().pressure());
        }

        private String dominantVector() {
            double precision = context.rep().getPrecision() + context.memoryProfile().disciplineWeight();
            double brutality = context.rep().getBrutality() + context.memoryProfile().aggressionWeight();
            double survival = context.rep().getSurvival() + context.memoryProfile().survivalWeight();
            double mobility = context.rep().getMobility() + context.memoryProfile().mobilityWeight();
            double chaos = context.rep().getChaos() + context.memoryProfile().chaosWeight();
            double consistency = context.rep().getConsistency() + context.memoryProfile().bossWeight();
            double max = Math.max(Math.max(Math.max(precision, brutality), Math.max(survival, mobility)), Math.max(chaos, consistency));
            if (max == precision) return "deadeye";
            if (max == brutality) return "harrow";
            if (max == survival) return "bulwark";
            if (max == mobility) return "glide";
            if (max == chaos) return "rift";
            return "vow";
        }

        private String convergenceCadence() {
            int recentIntensity = context.rep().getRecentKillChain() + context.rep().getSurvivalStreak()
                    + context.artifact().getMemory().count(ArtifactMemoryEvent.CONVERGENCE)
                    + context.artifact().getMemory().count(ArtifactMemoryEvent.FIRST_BOSS_KILL);
            if (recentIntensity >= 10) return "surge";
            if (context.memoryProfile().traumaWeight() >= 2.0D || context.rep().getChaos() > context.rep().getConsistency()) return "fracture";
            if (context.memoryProfile().disciplineWeight() >= context.memoryProfile().chaosWeight()) return "rite";
            return "wake";
        }

        private String memorySignature() {
            List<String> tags = new ArrayList<>();
            if (context.artifact().getMemory().count(ArtifactMemoryEvent.PRECISION_STREAK) > 0) tags.add("precision");
            if (context.artifact().getMemory().count(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL) > 0) tags.add("survival");
            if (context.artifact().getMemory().count(ArtifactMemoryEvent.CHAOS_RAMPAGE) > 0) tags.add("chaos");
            if (context.artifact().getMemory().count(ArtifactMemoryEvent.FIRST_BOSS_KILL) > 0) tags.add("boss");
            if (context.artifact().getMemory().count(ArtifactMemoryEvent.LONG_BATTLE) > 0) tags.add("endurance");
            return tags.isEmpty() ? "memory-muted" : String.join("+", tags);
        }

        private String continuitySeed() {
            return Long.toUnsignedString(Math.abs(Objects.hash(context.artifact().getArtifactSeed(), context.artifact().getDriftHistory().hashCode(), context.artifact().getLoreHistory().hashCode())));
        }

        private String carryOverClass() {
            if (context.memoryProfile().bossWeight() >= 2.0D && context.memoryProfile().disciplineWeight() >= 2.0D) return "lineage-heavy";
            if (context.memoryProfile().traumaWeight() >= 2.0D) return "scar-heavy";
            if (context.memoryProfile().mobilityWeight() >= 1.5D) return "motion-heavy";
            return "bounded-core";
        }

        private String roleBlend() {
            return context.current().roles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "+" + b).orElse("none").toLowerCase();
        }

        private Map<String, Double> shapeAffinities(String vector, String cadence) {
            double precision = vector.equals("deadeye") ? 0.10D : 0.0D;
            double brutality = vector.equals("harrow") ? 0.10D : 0.0D;
            double survival = vector.equals("bulwark") ? 0.10D : 0.0D;
            double mobility = vector.equals("glide") ? 0.11D : 0.0D;
            double chaos = vector.equals("rift") ? 0.10D : 0.0D;
            double consistency = vector.equals("vow") ? 0.08D : 0.0D;
            double continuity = cadence.equals("rite") ? 0.12D : cadence.equals("wake") ? 0.08D : 0.04D;
            return Map.of(
                    "precision", precision,
                    "brutality", brutality,
                    "survival", survival,
                    "mobility", mobility,
                    "chaos", chaos,
                    "consistency", consistency,
                    "continuity", continuity
            );
        }
    }
}
