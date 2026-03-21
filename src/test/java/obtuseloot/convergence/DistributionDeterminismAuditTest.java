package obtuseloot.convergence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Formal distribution and determinism audit for the convergence system.
 *
 * Scope:
 *  1. citadel-heart recipe is reachable and can select all three of its targets.
 *  2. reaper-vow can select all three of its targets (NETHERITE_SWORD, NETHERITE_AXE, TRIDENT).
 *  3. All four cadence labels (surge, fracture, rite, wake) are reachable.
 *  4. All six vector labels (deadeye, harrow, bulwark, glide, rift, vow) are reachable.
 *  5. Recipe-order invariant: a recipe earlier in the list preempts a later one for matching archetypes.
 *  6. Determinism: same artifact state + same engine always produces the same output.
 */
class DistributionDeterminismAuditTest {

    // -----------------------------------------------------------------------
    // 1. citadel-heart reachability
    // -----------------------------------------------------------------------

    @Test
    void citadelHeartFiresForHelmetArchetype() {
        // NETHERITE_HELMET has DEFENSIVE_ARMOR role but not CHESTPLATE or BOOTS,
        // so sky-bastion (which requires CHESTPLATE or BOOTS) does not match it.
        // citadel-heart (requires DEFENSIVE_ARMOR) does match.
        // Thresholds: totalScore ≥ 100, bossKills ≥ 2, pressure ≥ 7, historyScore ≥ 8.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = helmetArtifact(42L);
        ArtifactReputation rep = citadelRep();

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "NETHERITE_HELMET must match citadel-heart");
        assertEquals("citadel-heart", transition.replacement().getConvergencePath());
        assertTrue(
                Set.of(EquipmentArchetype.NETHERITE_CHESTPLATE.id(),
                        EquipmentArchetype.TURTLE_HELMET.id(),
                        EquipmentArchetype.NETHERITE_HELMET.id())
                        .contains(transition.replacement().getItemCategory()),
                "citadel-heart target must be within declared target set");
        // Identity must differ from NETHERITE_HELMET input.
        assertNotEquals(EquipmentArchetype.NETHERITE_HELMET.id(), transition.replacement().getItemCategory(),
                "Convergence must produce a distinct identity target");
        // Identity shape must carry citadel base.
        assertTrue(transition.replacement().getConvergenceIdentityShape().startsWith("citadel-"),
                "citadel-heart identity shape must use citadel base");
    }

    @Test
    void citadelHeartReachesNetheriteChestplateWithSurvivalConsistencyProfile() {
        // NETHERITE_CHESTPLATE score = survival + consistency + LONG_BATTLE*3.
        // Set survival=30, consistency=20, 3× LONG_BATTLE → chest score = 50+9=79.
        // TURTLE_HELMET score = survival + bossKills*4 + LOW_HEALTH_SURVIVAL*5 = 30+8+0=38.
        // NETHERITE_HELMET (target) score = consistency + survivalStreak*3 + FIRST_BOSS_KILL*4 = 20+0+0=20.
        // NETHERITE_CHESTPLATE wins clearly → window collapses to single winner.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = helmetArtifact(10L);
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);

        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(30);
        rep.setConsistency(20);
        rep.setKills(15);
        rep.setBossKills(2);
        // totalScore = 30+20+30+10 = 90 ... short. Add more kills.
        rep.setKills(20); // totalScore = 30+20+40+10 = 100 ✓

        // historyScore = 7 (base) + 3 (LONG_BATTLE) = 10 ≥ 8 ✓
        // pressure = 7+3 = 10 ≥ 7 ✓
        // Remove the extra three records added above; recompute.
        // Actually artifact memory already has 7 events from helmetArtifact + 3 LONG_BATTLE = 10 events.

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Must converge under citadel-heart");
        assertEquals("citadel-heart", transition.replacement().getConvergencePath());
        assertEquals(EquipmentArchetype.NETHERITE_CHESTPLATE.id(), transition.replacement().getItemCategory(),
                "Survival+consistency profile must select NETHERITE_CHESTPLATE");
    }

    @Test
    void citadelHeartReachesTurtleHelmetWithBossKillsAndLowHealthProfile() {
        // TURTLE_HELMET score = survival + bossKills*4 + LOW_HEALTH_SURVIVAL*5.
        // Set survival=30, bossKills=3, 4× LOW_HEALTH_SURVIVAL → turtle score = 30+12+20=62.
        // NETHERITE_CHESTPLATE score = survival + consistency + LONG_BATTLE*3 = 30+0+0=30.
        // NETHERITE_HELMET score = consistency + survivalStreak*3 + FIRST_BOSS_KILL*4 = 0+0+0=0.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_helmet");
        artifact.setArtifactSeed(77L);
        artifact.setAwakeningPath("Bulwark Ward");
        // 7 base events + 4 LOW_HEALTH_SURVIVAL = 11 events → pressure = 11 ≥ 7 ✓
        for (int i = 0; i < 7; i++) artifact.getMemory().record(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
        // historyScore = 11 (memory) + 1 (notable) = 12 ≥ 8 ✓
        artifact.addNotableEvent("siege.survived");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(30);
        rep.setKills(15);
        rep.setBossKills(3); // totalScore = 30+30+15 = 75... short
        rep.setKills(25);    // totalScore = 30+50+15 = 95... short
        rep.setKills(30);    // totalScore = 30+60+15 = 105 ≥ 100 ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "High LOW_HEALTH_SURVIVAL + bossKills profile must converge");
        assertEquals("citadel-heart", transition.replacement().getConvergencePath());
        assertEquals(EquipmentArchetype.TURTLE_HELMET.id(), transition.replacement().getItemCategory(),
                "Boss kill + low health survival profile must select TURTLE_HELMET");
    }

    @Test
    void citadelHeartReachesNetheriteHelmetWithConsistencyStreakProfile() {
        // To target NETHERITE_HELMET, the input must be a different armor archetype.
        // Use TURTLE_HELMET as input (it has DEFENSIVE_ARMOR role, not CHESTPLATE/BOOTS).
        // NETHERITE_HELMET score = consistency + survivalStreak*3 + FIRST_BOSS_KILL*4.
        // consistency=30, survivalStreak=4, FIRST_BOSS_KILL×3 → score = 30+12+12=54.
        // NETHERITE_CHESTPLATE score = survival + consistency + LONG_BATTLE*3 = 0+30+0=30.
        // TURTLE_HELMET skipped (it's current).
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "turtle_helmet");
        artifact.setArtifactSeed(99L);
        artifact.setAwakeningPath("Fortress Vow");
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        for (int i = 0; i < 4; i++) artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        // pressure = 7 ≥ 7 ✓
        artifact.addNotableEvent("boss.guardian-prime");
        // historyScore = 7 (memory) + 1 (event) = 8 ≥ 8 ✓

        ArtifactReputation rep = new ArtifactReputation();
        rep.setConsistency(30);
        rep.setSurvivalStreak(4);
        rep.setKills(25);
        rep.setBossKills(3); // totalScore = 30+50+15 = 95... need 100
        rep.setKills(30);    // totalScore = 30+60+15 = 105 ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Consistency + survivalStreak + FIRST_BOSS_KILL profile must converge");
        assertEquals("citadel-heart", transition.replacement().getConvergencePath());
        assertEquals(EquipmentArchetype.NETHERITE_HELMET.id(), transition.replacement().getItemCategory(),
                "Consistency + streak + boss kill profile must select NETHERITE_HELMET");
    }

    // -----------------------------------------------------------------------
    // 2. reaper-vow 3-target reachability
    // -----------------------------------------------------------------------

    @Test
    void reaperVowReachesNetheriteAxeWithBrutalityProfile() {
        // NETHERITE_AXE score = brutality + MULTIKILL_CHAIN*3.
        // brutality=40, 4× MULTIKILL_CHAIN → axe score = 40+12=52.
        // NETHERITE_SWORD score = precision + consistency + PRECISION_STREAK*3 = 0+0+0=0.
        // TRIDENT score = chaos + mobility + mobilityWeight*4 = 0+0+small ≈ 0.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_sword");
        artifact.setArtifactSeed(31L);
        artifact.setAwakeningPath("Executioner's Oath");
        for (int i = 0; i < 4; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        for (int i = 0; i < 2; i++) artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        // pressure = 6 ≥ 6 ✓
        artifact.addLoreHistory("brutal-campaign");
        artifact.addLoreHistory("chain-conquest");
        // historyScore = 6 (memory) + 2 (lore) = 8 ≥ 8 ✓

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(40);
        rep.setKills(20);
        rep.setBossKills(2); // totalScore = 40+40+10 = 90... need 96
        rep.setKills(23);    // totalScore = 40+46+10 = 96 ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Brutality-dominant MELEE_WEAPON must converge via reaper-vow");
        assertEquals("reaper-vow", transition.replacement().getConvergencePath());
        assertEquals(EquipmentArchetype.NETHERITE_AXE.id(), transition.replacement().getItemCategory(),
                "brutality + MULTIKILL_CHAIN profile must select NETHERITE_AXE");
    }

    @Test
    void reaperVowReachesNetheriteSwWordWithPrecisionConsistencyProfile() {
        // NETHERITE_SWORD score = precision + consistency + PRECISION_STREAK*3.
        // precision=40, consistency=20, 3× PRECISION_STREAK → sword score = 40+20+9=69.
        // NETHERITE_AXE score = brutality + MULTIKILL_CHAIN*3 = 0+0=0.
        // TRIDENT score ≈ 0.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_sword");
        artifact.setArtifactSeed(32L);
        artifact.setAwakeningPath("Stormblade");
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        // pressure = 6 ≥ 6 ✓
        artifact.addLoreHistory("precision-trials");
        artifact.addLoreHistory("disciplined-march");
        // historyScore = 6 (memory) + 2 (lore) = 8 ✓

        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(40);
        rep.setConsistency(20);
        rep.setKills(15);
        rep.setBossKills(2); // totalScore = 40+20+30+10 = 100 ✓ (≥ 96)

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Precision+consistency MELEE_WEAPON must converge via reaper-vow");
        assertEquals("reaper-vow", transition.replacement().getConvergencePath());
        assertEquals(EquipmentArchetype.NETHERITE_SWORD.id(), transition.replacement().getItemCategory(),
                "Precision + consistency + PRECISION_STREAK profile must select NETHERITE_SWORD");
    }

    @Test
    void reaperVowReachesTridentWithChaosAndMobilityProfile() {
        // TRIDENT score = chaos + mobility + mobilityWeight*4.
        // chaos=22, mobility=22, 3× LONG_BATTLE → mobilityWeight=3*0.25=0.75, round(0.75*4)=3 → score=47.
        // NETHERITE_AXE score = brutality + MULTIKILL_CHAIN*3 = 0+0=0.
        // NETHERITE_SWORD score = precision + consistency + PRECISION_STREAK*3 = 0+0+0=0.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_sword");
        artifact.setArtifactSeed(33L);
        artifact.setAwakeningPath("Tempest Stride");
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        // pressure = 6 ≥ 6 ✓
        artifact.addLoreHistory("chaos-march");
        artifact.addLoreHistory("storm-charge");
        // historyScore = 6 (memory) + 2 (lore) = 8 ✓

        ArtifactReputation rep = new ArtifactReputation();
        rep.setChaos(22);
        rep.setMobility(22);
        rep.setKills(15);
        rep.setBossKills(2); // totalScore = 22+22+30+10 = 84... need 96
        rep.setKills(21);    // totalScore = 22+22+42+10 = 96 ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Chaos+mobility MELEE_WEAPON must converge via reaper-vow");
        assertEquals("reaper-vow", transition.replacement().getConvergencePath());
        assertEquals(EquipmentArchetype.TRIDENT.id(), transition.replacement().getItemCategory(),
                "Chaos + mobility + LONG_BATTLE profile must select TRIDENT");
    }

    // -----------------------------------------------------------------------
    // 3. All four cadence labels reachable
    // -----------------------------------------------------------------------

    @Test
    void cadenceSurgeReachableViaBossKills() {
        // Already covered in ConvergenceEngineIdentityTransitionTest.cadenceWeightedBossKillsReachesSurge.
        // Repeated here to make the audit self-contained.
        // recentIntensity = bossKills*2 = 4*2=8 + killChain=2 = 10 ≥ 10 → surge.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(28);
        rep.setMobility(16);
        rep.setBossKills(4);
        rep.setRecentKillChain(2);
        rep.setKills(20); // totalScore = 28+16+40+20 = 104 ≥ 92

        Artifact artifact = preparedRangedArtifact(10L);
        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertTrue(transition.replacement().getConvergenceVariantId().endsWith("-surge"),
                "High boss-kill + kill-chain artifact must reach surge cadence");
    }

    @Test
    void cadenceFractureReachableViaChaosOverConsistency() {
        // fracture: traumaWeight < 2.0 AND chaos > consistency triggers fracture.
        // chaos=20, consistency=5 → chaos > consistency → fracture (intensity check first: < 10 ✓).
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setChaos(20);
        rep.setConsistency(5);
        rep.setBossKills(1);
        rep.setKills(24); // totalScore = 20+5+48+5 = 78... need 92
        rep.setMobility(10);
        // totalScore = 20+10+5+48+5 = 88... still short
        rep.setPrecision(5);
        // totalScore = 5+20+10+5+48+5 = 93 ≥ 92 ✓

        Artifact artifact = preparedRangedArtifact(20L);
        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Chaos-dominant (chaos > consistency) artifact must converge");
        assertTrue(transition.replacement().getConvergenceVariantId().endsWith("-fracture"),
                "Chaos > consistency with no trauma must yield fracture cadence; variantId="
                        + transition.replacement().getConvergenceVariantId());
    }

    @Test
    void cadenceRiteReachableViaDisciplineWeight() {
        // rite: NOT surge, NOT fracture, disciplineWeight ≥ chaosWeight.
        // 3× PRECISION_STREAK → disciplineWeight=3.6, chaosWeight=0 → disciplineWeight ≥ chaosWeight → rite.
        // fracture guard: chaos=5 ≤ consistency=10, traumaWeight=0 → not fracture.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(25);
        rep.setConsistency(10);
        rep.setChaos(5);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 25+10+5+44+5 = 89... short
        rep.setMobility(5);
        // totalScore = 25+5+10+5+44+5 = 94 ≥ 92 ✓

        Artifact artifact = new Artifact(UUID.randomUUID(), "bow");
        artifact.setArtifactSeed(30L);
        artifact.setAwakeningPath("Stormblade");
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        // pressure = 5 ≥ 5 ✓
        artifact.addLoreHistory("discipline-path");
        artifact.addLoreHistory("precision-march");
        // historyScore = 5 + 2 = 7 ≥ 7 ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Discipline-dominant artifact must converge");
        assertTrue(transition.replacement().getConvergenceVariantId().endsWith("-rite"),
                "Discipline-dominant (disciplineWeight > chaosWeight) with no fracture trigger must yield rite; variantId="
                        + transition.replacement().getConvergenceVariantId());
    }

    @Test
    void cadenceWakeReachableWhenAllOtherCadenceConditionsMiss() {
        // wake: NOT surge (intensity < 10), NOT fracture (chaos ≤ consistency, trauma < 2),
        // NOT rite (disciplineWeight < chaosWeight, pressure < 7 OR bossKills < 2).
        // 3× CHAOS_RAMPAGE → chaosWeight=4.2, disciplineWeight=0 → discipline < chaos → NOT rite by first cond.
        // bossKills=1 < 2 → NOT rite by second cond either. → wake.
        ConvergenceEngine engine = new ConvergenceEngine();
        ArtifactReputation rep = new ArtifactReputation();
        rep.setConsistency(20);
        rep.setChaos(5);        // chaos ≤ consistency → not fracture via chaos path
        rep.setBossKills(1);
        // Need totalScore ≥ 92 with kills making up the rest.
        rep.setPrecision(5);
        rep.setMobility(5);
        rep.setKills(24); // totalScore = 5+5+20+5+48+5 = 88... short
        rep.setSurvival(5);
        // totalScore = 5+5+5+20+5+48+5 = 93 ✓

        Artifact artifact = new Artifact(UUID.randomUUID(), "bow");
        artifact.setArtifactSeed(40L);
        artifact.setAwakeningPath("Skywake");
        for (int i = 0; i < 3; i++) artifact.getMemory().record(ArtifactMemoryEvent.CHAOS_RAMPAGE);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        // pressure = 5 ≥ 5 ✓; traumaWeight = CHAOS_RAMPAGE*0.4*3 ≈ 1.2 < 2.0 ✓
        artifact.addLoreHistory("adrift-path");
        artifact.addLoreHistory("wandering-storm");
        // historyScore = 5 + 2 = 7 ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition, "Wake-cadence artifact must converge");
        assertTrue(transition.replacement().getConvergenceVariantId().endsWith("-wake"),
                "No surge/fracture/rite triggers must yield wake cadence; variantId="
                        + transition.replacement().getConvergenceVariantId());
    }

    // -----------------------------------------------------------------------
    // 4. All six vector labels reachable
    // -----------------------------------------------------------------------

    @Test
    void vectorDeadeyeReachableWithPrecisionDominant() {
        // precision >> all other stats → deadeye vector.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedRangedArtifact(50L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(50);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 50+44+5 = 99 ≥ 92

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertVectorInVariantId(transition, "deadeye");
    }

    @Test
    void vectorHarrowReachableWithBrutalityDominant() {
        // Brutality >> all other stats → harrow vector.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedRangedArtifact(51L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(50);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 50+44+5 = 99 ≥ 92

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertVectorInVariantId(transition, "harrow");
    }

    @Test
    void vectorBulwarkReachableWithSurvivalDominant() {
        // survival >> all other stats → bulwark vector.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedRangedArtifact(52L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(50);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 50+44+5 = 99 ≥ 92

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertVectorInVariantId(transition, "bulwark");
    }

    @Test
    void vectorGlideReachableWithMobilityDominant() {
        // mobility >> all other stats → glide vector.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedRangedArtifact(53L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setMobility(50);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 50+44+5 = 99 ≥ 92

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertVectorInVariantId(transition, "glide");
    }

    @Test
    void vectorRiftReachableWithChaosDominant() {
        // chaos >> all other stats → rift vector.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedRangedArtifact(54L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setChaos(50);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 50+44+5 = 99 ≥ 92

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertVectorInVariantId(transition, "rift");
    }

    @Test
    void vectorVowReachableWithConsistencyDominant() {
        // consistency >> all other stats → vow vector.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedRangedArtifact(55L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setConsistency(50);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 50+44+5 = 99 ≥ 92

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertVectorInVariantId(transition, "vow");
    }

    // -----------------------------------------------------------------------
    // 5. Recipe-order invariant
    // -----------------------------------------------------------------------

    @Test
    void horizonSyndicateFiresBeforeCitadelHeartForRangedWeaponWithSufficientScoreForBoth() {
        // horizon-syndicate (RANGED_WEAPON, minScore=92) should fire before citadel-heart
        // (DEFENSIVE_ARMOR) for a BOW artifact, since BOW has RANGED_WEAPON role
        // but does NOT have DEFENSIVE_ARMOR role.
        // This test confirms the recipe list order is not the issue: they simply don't compete
        // for the same archetype. Verified by checking the convergence path.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = preparedRangedArtifact(60L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(30);
        rep.setMobility(10);
        rep.setBossKills(2);
        rep.setKills(20); // totalScore = 30+10+40+10 = 90... short
        rep.setKills(22); // totalScore = 30+10+44+10 = 94 ≥ 92 ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertEquals("horizon-syndicate", transition.replacement().getConvergencePath(),
                "BOW artifact must match horizon-syndicate, not citadel-heart");
    }

    @Test
    void skyBastionFiresBeforeCitadelHeartForChestplateArchetype() {
        // sky-bastion (CHESTPLATE or BOOTS) fires before citadel-heart (DEFENSIVE_ARMOR)
        // for DIAMOND_CHESTPLATE since that archetype has both CHESTPLATE and DEFENSIVE_ARMOR roles.
        // sky-bastion is earlier in the recipe list so it fires first when the artifact meets thresholds.
        ConvergenceEngine engine = new ConvergenceEngine();
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_chestplate");
        artifact.setArtifactSeed(61L);
        artifact.setAwakeningPath("Bulwark Ward");
        for (int i = 0; i < 5; i++) artifact.getMemory().record(ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
        artifact.addLoreHistory("bastion-trials");
        artifact.addLoreHistory("siege-survivor");
        // pressure = 5 ≥ 5 (sky-bastion threshold) ✓; historyScore = 5+2 = 7 ✓

        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(30);
        rep.setConsistency(20);
        rep.setBossKills(1);
        rep.setKills(22); // totalScore = 30+20+44+5 = 99 ≥ 94 (sky-bastion) ✓

        ArtifactIdentityTransition transition = engine.evaluateSimulation(artifact, rep);
        assertNotNull(transition);
        assertEquals("sky-bastion", transition.replacement().getConvergencePath(),
                "DIAMOND_CHESTPLATE must match sky-bastion (earlier in list) before citadel-heart");
    }

    // -----------------------------------------------------------------------
    // 6. Determinism: repeated evaluation on same state
    // -----------------------------------------------------------------------

    @Test
    void convergenceIsFullyDeterministicAcrossEngineInstances() {
        // Two engine instances evaluating identical artifact state must produce identical output.
        Artifact artifact = preparedRangedArtifact(99L);
        ArtifactReputation rep = new ArtifactReputation();
        rep.setPrecision(30);
        rep.setMobility(10);
        rep.setBossKills(1);
        rep.setKills(24); // totalScore = 30+10+48+5 = 93 ≥ 92

        ArtifactIdentityTransition t1 = new ConvergenceEngine().evaluateSimulation(artifact, rep);
        ArtifactIdentityTransition t2 = new ConvergenceEngine().evaluateSimulation(artifact, rep);

        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1.replacement().getArtifactSeed(),       t2.replacement().getArtifactSeed(),
                "seed must be identical across engine instances for same input");
        assertEquals(t1.replacement().getConvergenceVariantId(), t2.replacement().getConvergenceVariantId(),
                "variant id must be identical across engine instances");
        assertEquals(t1.replacement().getConvergenceIdentityShape(), t2.replacement().getConvergenceIdentityShape(),
                "identity shape must be identical across engine instances");
        assertEquals(t1.replacement().getItemCategory(),         t2.replacement().getItemCategory(),
                "target archetype must be identical across engine instances");
    }

    @Test
    void convergenceDeterminismHoldsForCitadelHeart() {
        Artifact artifact = helmetArtifact(88L);
        ArtifactReputation rep = citadelRep();

        ArtifactIdentityTransition t1 = new ConvergenceEngine().evaluateSimulation(artifact, rep);
        ArtifactIdentityTransition t2 = new ConvergenceEngine().evaluateSimulation(artifact, rep);

        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1.replacement().getArtifactSeed(),            t2.replacement().getArtifactSeed());
        assertEquals(t1.replacement().getConvergenceVariantId(),     t2.replacement().getConvergenceVariantId());
        assertEquals(t1.replacement().getConvergenceIdentityShape(), t2.replacement().getConvergenceIdentityShape());
    }

    @Test
    void convergenceDeterminismHoldsForReaperVow() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_sword");
        artifact.setArtifactSeed(77L);
        artifact.setAwakeningPath("Executioner's Oath");
        for (int i = 0; i < 4; i++) artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        for (int i = 0; i < 2; i++) artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.addLoreHistory("brutal-campaign");
        artifact.addLoreHistory("chain-conquest");

        ArtifactReputation rep = new ArtifactReputation();
        rep.setBrutality(40);
        rep.setKills(23);
        rep.setBossKills(2);

        ArtifactIdentityTransition t1 = new ConvergenceEngine().evaluateSimulation(artifact, rep);
        ArtifactIdentityTransition t2 = new ConvergenceEngine().evaluateSimulation(artifact, rep);

        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1.replacement().getArtifactSeed(),            t2.replacement().getArtifactSeed());
        assertEquals(t1.replacement().getConvergenceVariantId(),     t2.replacement().getConvergenceVariantId());
        assertEquals(t1.replacement().getItemCategory(),             t2.replacement().getItemCategory());
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Minimal NETHERITE_HELMET artifact that satisfies citadel-heart prerequisites.
     * pressure = 7 ≥ 7; historyScore = 7 (memory) + 1 (notable) = 8 ≥ 8.
     */
    private Artifact helmetArtifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "netherite_helmet");
        artifact.setArtifactSeed(seed);
        artifact.setAwakeningPath("Citadel Warden");
        for (int i = 0; i < 7; i++) artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.addNotableEvent("siege.endured");
        return artifact;
    }

    /**
     * Reputation that satisfies citadel-heart thresholds (totalScore ≥ 100, bossKills ≥ 2).
     * survival=30, consistency=20, kills=20, bossKills=2 → totalScore = 30+20+40+10 = 100.
     */
    private ArtifactReputation citadelRep() {
        ArtifactReputation rep = new ArtifactReputation();
        rep.setSurvival(30);
        rep.setConsistency(20);
        rep.setKills(20);
        rep.setBossKills(2);
        return rep;
    }

    /**
     * BOW artifact ready for horizon-syndicate convergence.
     * pressure = 5 ≥ 5; historyScore = 5 (memory) + 2 (lore) = 7 ≥ 7.
     */
    private Artifact preparedRangedArtifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "bow");
        artifact.setArtifactSeed(seed);
        artifact.setAwakeningPath("Stormblade");
        artifact.getMemory().record(ArtifactMemoryEvent.AWAKENING);
        artifact.getMemory().record(ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.getMemory().record(ArtifactMemoryEvent.PRECISION_STREAK);
        artifact.getMemory().record(ArtifactMemoryEvent.LONG_BATTLE);
        artifact.getMemory().record(ArtifactMemoryEvent.MULTIKILL_CHAIN);
        artifact.addLoreHistory("sky-pursuit");
        artifact.addLoreHistory("storm-arrow");
        return artifact;
    }

    /** Extracts the vector label from a variant id of the form {@code target-vector-cadence}. */
    private void assertVectorInVariantId(ArtifactIdentityTransition transition, String expectedVector) {
        String variantId = transition.replacement().getConvergenceVariantId();
        // Format: targetId-vector-cadence. The targetId itself may contain hyphens (e.g. netherite_sword
        // uses underscores, but EquipmentArchetype ids use underscores not hyphens). So split from the
        // right: last two tokens are vector and cadence.
        String[] parts = variantId.split("-");
        assertTrue(parts.length >= 3,
                "variantId must have at least 3 hyphen-separated parts; got: " + variantId);
        String cadence = parts[parts.length - 1];
        String vector  = parts[parts.length - 2];
        assertEquals(expectedVector, vector,
                "Expected vector '" + expectedVector + "' but variantId=" + variantId);
    }
}
