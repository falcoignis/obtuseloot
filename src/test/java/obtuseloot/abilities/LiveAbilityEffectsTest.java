package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.ArtifactFitnessEvaluator;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.evolution.UtilityHistoryRollup;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Executes real Bukkit event objects against stateful API doubles; no live server is started. */
class LiveAbilityEffectsTest {
    private final UUID holder = new UUID(0, 19);
    private final AbilityExecutor executor = new AbilityExecutor();
    private final List<String> messages = new ArrayList<>();
    private final List<BlockPlaceEvent> placements = new ArrayList<>();
    private Consumer<BlockPlaceEvent> protection = event -> { };
    private Server previousServer;
    private World world;
    private Player player;
    private Block block;
    private Material cropType = Material.WHEAT;
    private BlockData current;
    private ItemStack dropStack;
    private boolean dropRemoved;
    private boolean dropValid = true;
    private int originalAge = -1;
    private int x = 100;
    private long gameTick = 200;
    private GameMode gameMode = GameMode.SURVIVAL;

    @BeforeEach
    void setUp() throws Exception {
        current = data(Material.AIR, 0);
        world = api(World.class, (method, args) -> switch (method) {
            case "getEnvironment" -> World.Environment.NORMAL;
            case "isChunkLoaded" -> false;
            case "getUID" -> new UUID(0, 10);
            case "getGameTime" -> gameTick;
            case "getMinHeight" -> -64;
            case "getSpawnLocation" -> new Location(world, 0, 64, 0);
            default -> unexpected(method);
        });
        player = api(Player.class, (method, args) -> switch (method) {
            case "isOnline" -> true;
            case "getUniqueId" -> holder;
            case "getGameMode" -> gameMode;
            case "getWorld" -> world;
            case "isOp" -> false;
            case "sendMessage" -> { messages.add((String) args[0]); yield null; }
            default -> unexpected(method);
        });
        Block soil = api(Block.class, (method, args) -> switch (method) {
            case "getType" -> cropType == Material.NETHER_WART ? Material.SOUL_SAND : Material.FARMLAND;
            default -> unexpected(method);
        });
        block = api(Block.class, (method, args) -> switch (method) {
            case "getType" -> current.getMaterial();
            case "getBlockData" -> current.clone();
            case "setBlockData" -> { current = ((BlockData) args[0]).clone(); yield null; }
            case "getRelative" -> soil;
            case "getState" -> snapshot(current);
            case "getWorld" -> world;
            case "getX" -> x;
            case "getY" -> 64;
            case "getZ" -> 100;
            case "getLightLevel" -> (byte) 12;
            default -> unexpected(method);
        });
        PluginManager plugins = api(PluginManager.class, (method, args) -> {
            if (method.equals("callEvent")) {
                var event = (BlockPlaceEvent) args[0];
                placements.add(event);
                protection.accept(event);
                return null;
            }
            return unexpected(method);
        });
        Server server = api(Server.class, (method, args) -> switch (method) {
            case "isPrimaryThread" -> true;
            case "getSpawnRadius" -> 16;
            case "getPluginManager" -> plugins;
            default -> unexpected(method);
        });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        previousServer = (Server) field.get(null);
        field.set(null, server);
    }

    @AfterEach
    void restoreServer() throws Exception {
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, previousServer);
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {"WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "NETHER_WART"})
    void matureHarvestConsumesOneExistingDropAndReplantsAtAgeZero(Material type) {
        cropType = type;
        var event = harvestEvent(3);
        var result = execute("survival.gentle_harvest", event);
        assertEquals(AbilityExecutionStatus.SUCCESS, result.status());
        assertEquals(1.0, result.observedAmount());
        assertTrue(result.meaningfulOutcome());
        assertEquals(world.getUID() + ":100:64:100:200", result.observationKey());
        assertEquals(type, current.getMaterial());
        assertEquals(0, ((Ageable) current).getAge());
        assertEquals(2, dropStack.getAmount());
        assertEquals(1, placements.size());
        assertEquals(Material.AIR, placements.getFirst().getBlockReplacedState().getType());
    }

    @Test
    void anotherAbilitysRecoveryOnTheSameItemSuppressesReplantWithoutTouchingTheWorld() {
        var definition = definition("survival.gentle_harvest"); var item = artifact();
        item.setAbilityReadyAt("precision.material_insight", gameTick + 1);
        var result = executor.execute(definition, context(item, definition, harvestEvent(3)), 1);
        assertEquals(AbilityExecutionStatus.SUPPRESSED, result.status()); assertEquals("ability-recovering", result.suppressionReason());
        assertTrue(placements.isEmpty()); assertEquals(3, dropStack.getAmount());
        item.setAbilityReadyAt("precision.material_insight", gameTick);
        assertEquals(AbilityExecutionStatus.SUCCESS, executor.execute(definition, context(item, definition, harvestEvent(3)), 1).status());
    }

    @Test
    void singlePlantingItemIsRemovedRatherThanDuplicated() {
        assertEquals(AbilityExecutionStatus.SUCCESS, execute("survival.gentle_harvest", harvestEvent(1)).status());
        assertTrue(dropRemoved);
    }

    @Test
    void protectionCancellationRollsBackAndDoesNotConsumeSeed() {
        protection = event -> event.setCancelled(true);
        assertNoHarvest(harvestEvent(3));
        assertEquals(1, placements.size());
    }

    @Test
    void canBuildDenialAlsoRollsBack() {
        protection = event -> event.setBuild(false);
        assertNoHarvest(harvestEvent(3));
    }

    @Test
    void exceptionInProtectionCheckRollsBackWithoutPayment() {
        protection = event -> { throw new IllegalStateException("test denial"); };
        assertThrows(IllegalStateException.class, () -> execute("survival.gentle_harvest", harvestEvent(3)));
        assertEquals(Material.AIR, current.getMaterial());
        assertEquals(3, dropStack.getAmount());
    }

    @Test
    void laterListenerEditIsPreserved() {
        protection = event -> current = data(Material.STONE, 0);
        assertEquals(AbilityExecutionStatus.NO_OP, execute("survival.gentle_harvest", harvestEvent(3)).status());
        assertEquals(Material.STONE, current.getMaterial());
        assertEquals(3, dropStack.getAmount());
    }

    @Test
    void cancelledHarvestCannotReplant() {
        var event = harvestEvent(3);
        event.setCancelled(true);
        assertNoHarvest(event);
        assertTrue(placements.isEmpty());
    }

    @Test
    void immatureCropCannotReplant() {
        originalAge = 2;
        assertNoHarvest(harvestEvent(3));
    }

    @Test
    void unavailableDropsCannotBecomeFreeSeeds() {
        var event = harvestEvent(3);
        dropValid = false;
        assertNoHarvest(event);
    }

    @Test
    void missingDropsDoNotCreateSeeds() {
        var event = harvestEvent(3);
        event.getItems().clear();
        assertNoHarvest(event);
    }

    @Test
    void occupiedBlockIsNeverOverwritten() {
        var event = harvestEvent(3);
        current = data(Material.STONE, 0);
        assertEquals(AbilityExecutionStatus.NO_OP, execute("survival.gentle_harvest", event).status());
        assertEquals(Material.STONE, current.getMaterial());
    }

    @Test
    void spawnProtectionIsCheckedBeforeProvisionalPlacement() {
        x = 0;
        // Both horizontal coordinates must be inside the spawn radius.
        world = api(World.class, (method, args) -> switch (method) {
            case "getEnvironment" -> World.Environment.NORMAL;
            case "isChunkLoaded" -> false;
            case "getUID" -> new UUID(0, 10);
            case "getGameTime" -> gameTick;
            case "getMinHeight" -> -64;
            case "getSpawnLocation" -> new Location(world, 0, 64, 100);
            default -> unexpected(method);
        });
        assertNoHarvest(harvestEvent(3));
        assertTrue(placements.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "ADVENTURE", "SPECTATOR"})
    void onlySurvivalHarvestsCanConsumeDrops(GameMode mode) {
        gameMode = mode;
        assertNoHarvest(harvestEvent(3));
    }

    @Test
    void materialInsightSendsAnActualReportAndCountsDeliveredFacts() {
        current = data(Material.WHEAT, 4);
        var result = execute("precision.material_insight", inspection(EquipmentSlot.HAND));
        assertEquals(AbilityExecutionStatus.SUCCESS, result.status());
        assertEquals(4.0, result.observedAmount());
        assertEquals(60, result.performance().recoveryTicks());
        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().contains("growth 4/7"));
        assertTrue(messages.getFirst().contains("light 12"));
        assertEquals(4, ((Ageable) current).getAge());
    }

    @Test
    void explicitlyBoundOffhandInspectionWorksAndHonorsFinalItemDenial() {
        current = data(Material.WHEAT, 4);
        var definition = definition("precision.material_insight");
        var event = inspection(EquipmentSlot.OFF_HAND);
        var context = context(artifact(), definition, event).withActivation(new AbilityActivation(EquipmentSlot.OFF_HAND,
                AbilityInput.secondary(EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_BLOCK), java.util.Set.of(definition.id())));
        assertEquals(AbilityExecutionStatus.SUCCESS, executor.execute(definition, context, 1).status());
        assertEquals(1, messages.size());
        event.setUseItemInHand(Event.Result.DENY);
        assertEquals(AbilityExecutionStatus.NO_OP, executor.execute(definition, context, 1).status());
        assertEquals(1, messages.size());
        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    void replantDoesNotCancelOrReplaceTheOriginalHarvestEvent() {
        var event = harvestEvent(3);
        assertFalse(event.isCancelled());
        assertEquals(AbilityExecutionStatus.SUCCESS, execute("survival.gentle_harvest", event).status());
        assertFalse(event.isCancelled(), "Native harvest/XP listeners keep the original event");
        assertSame(player, event.getPlayer());
        assertEquals(1, placements.size(), "Protection checks still see the paid replant");
    }

    @Test
    void actualInspectionsDistinguishTargetsButPenalizeAnUnchangedRepeatedReport() {
        var definition = definition("precision.material_insight");
        var varied = artifact();
        var repeated = artifact();
        var variedTracker = new ArtifactUsageTracker();
        var repeatedTracker = new ArtifactUsageTracker();
        var variedReputation = new ArtifactReputation();
        var repeatedReputation = new ArtifactReputation();
        current = data(Material.WHEAT, 4);
        String originalKey = execute(definition.id(), inspection(EquipmentSlot.HAND)).observationKey();
        assertNotNull(originalKey);
        assertEquals(originalKey, execute(definition.id(), inspection(EquipmentSlot.HAND)).observationKey());
        for (int i = 0; i < 30; i++) {
            gameTick += 120;
            x = 100 + i;
            var context = context(varied, definition, inspection(EquipmentSlot.HAND));
            var result = executor.execute(definition, context, 1);
            variedReputation.recordUtilityExperience(definition.family(),
                    variedTracker.trackAbilityExecution(varied, context, result, definition));
            x = 100;
            context = context(repeated, definition, inspection(EquipmentSlot.HAND));
            result = executor.execute(definition, context, 1);
            repeatedReputation.recordUtilityExperience(definition.family(),
                    repeatedTracker.trackAbilityExecution(repeated, context, result, definition));
        }
        assertTrue(variedReputation.getTotalScore() >= 10, "Different actual targets can reach the first existing stage");
        assertTrue(repeatedReputation.getTotalScore() < 10, "Repeating one unchanged report must retain its redundancy penalty");
        current = data(Material.WHEAT, 5);
        assertNotEquals(originalKey, execute(definition.id(), inspection(EquipmentSlot.HAND)).observationKey());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void indexedAndFullScanDispatchApplyEffectOnceAndBudgetBlocksRepeat(boolean indexed) {
        var definition = definition("survival.gentle_harvest");
        var manager = new ItemAbilityManager((artifact, reputation) -> new AbilityProfile("live", List.of(definition)));
        manager.setTriggerSubscriptionIndexingEnabled(indexed);
        var artifact = artifact();
        var first = manager.resolveDispatch(context(artifact, definition, harvestEvent(3)));
        assertTrue(first.hasSuccessfulMechanic(AbilityMechanic.HARVEST_RELAY));
        current = data(Material.AIR, 0);
        var second = manager.resolveDispatch(context(artifact, definition, harvestEvent(3)));
        assertEquals(AbilityExecutionStatus.SUPPRESSED, second.executions().getFirst().status());
        assertEquals(Material.AIR, current.getMaterial());
        assertEquals(3, dropStack.getAmount());
        assertEquals(1, placements.size());
    }

    @Test
    void unimplementedLiveEffectNeverReceivesClassificationCredit() {
        var definition = definition("mobility.compass_stories");
        var result = executor.execute(definition, context(artifact(), definition, null), 1);
        assertEquals(AbilityExecutionStatus.NO_OP, result.status());
        assertNull(result.observedAmount());
        assertFalse(result.meaningfulOutcome());
    }

    @ParameterizedTest
    @ValueSource(strings = {"survival.gentle_harvest", "precision.material_insight"})
    void mutationsCannotExecuteTheOriginalEffectUnderAnotherMechanicOrTrigger(String id) {
        var original = definition(id);
        for (var variant : List.of(variant(original, original.trigger(), AbilityMechanic.PULSE),
                variant(original, AbilityTrigger.ON_MEMORY_EVENT, original.mechanic()))) {
            Event event = id.equals("survival.gentle_harvest") ? harvestEvent(3) : inspection(EquipmentSlot.HAND);
            var result = executor.execute(variant, context(artifact(), variant, event), 1);
            assertEquals(AbilityExecutionStatus.NO_OP, result.status());
            assertEquals("effect-variant-not-implemented", result.suppressionReason());
            assertNull(result.observedAmount());
            assertEquals(Material.AIR, current.getMaterial());
            assertTrue(messages.isEmpty());
            assertTrue(placements.isEmpty());
        }
    }

    @Test
    void seedRemovedDuringProtectionCallbackCannotPayForPlanting() {
        protection = event -> dropValid = false;
        assertNoHarvest(harvestEvent(3));
    }

    @Test
    void deniedInspectionAndOffhandCannotReceiveCredit() {
        var denied = inspection(EquipmentSlot.HAND);
        denied.setUseInteractedBlock(Event.Result.DENY);
        assertEquals(AbilityExecutionStatus.NO_OP, execute("precision.material_insight", denied).status());
        assertEquals(AbilityExecutionStatus.NO_OP, execute("precision.material_insight", inspection(EquipmentSlot.OFF_HAND)).status());
        assertTrue(messages.isEmpty());
    }

    @Test
    void observedSuccessAndDenialReachSelectionHistoryAndFitnessImmediately() {
        Artifact artifact = artifact();
        var tracker = new ArtifactUsageTracker();
        var telemetry = new ArrayList<java.util.Map<String, String>>();
        tracker.setTelemetryEmitter(new obtuseloot.telemetry.EcosystemTelemetryEmitter(null) {
            @Override public void emit(obtuseloot.telemetry.EcosystemTelemetryEventType type, long seed,
                                       String lineage, String niche, java.util.Map<String, String> attributes) {
                if (type == obtuseloot.telemetry.EcosystemTelemetryEventType.ABILITY_EXECUTION) telemetry.add(attributes);
            }
        });
        var definition = definition("survival.gentle_harvest");
        var context = context(artifact, definition, harvestEvent(3));
        var result = executor.execute(definition, context, 1);
        tracker.trackAbilityExecution(artifact, context, result, definition);
        double fitnessAfterSuccess = new ArtifactFitnessEvaluator().evaluate(tracker.profileFor(artifact));
        assertEquals(1, UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()).attempts());
        assertTrue(UtilityHistoryRollup.parse(artifact.getLastUtilityHistory()).validatedUtility() > 0);
        double selectionAfterSuccess = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory())
                .utilityScoreForTemplate(definition.mechanic(), definition.trigger());

        current = data(Material.AIR, 0);
        protection = event -> event.setCancelled(true);
        context = context(artifact, definition, harvestEvent(3));
        result = executor.execute(definition, context, 1);
        tracker.trackAbilityExecution(artifact, context, result, definition);
        var history = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
        assertEquals(2, history.attempts());
        assertEquals(0.5, history.meaningfulRate());
        var ecologyRollups = tracker.nichePopulationTracker().rollups();
        assertFalse(ecologyRollups.isEmpty());
        ecologyRollups.values().forEach(rollup -> {
            assertEquals(2, rollup.attempts(), "Ecology must see the second outcome despite its snapshot cache");
            assertEquals(1, rollup.meaningfulOutcomes());
        });
        assertTrue(new ArtifactFitnessEvaluator().evaluate(tracker.profileFor(artifact)) < fitnessAfterSuccess);
        assertEquals("observed", telemetry.getFirst().get("measurement_basis"));
        assertEquals("1.0", telemetry.getFirst().get("observed_amount"));
        assertEquals("0.0", telemetry.getLast().get("observed_amount"));
        assertTrue(history.utilityScoreForTemplate(definition.mechanic(), definition.trigger()) < selectionAfterSuccess);
    }

    private void assertNoHarvest(BlockDropItemEvent event) {
        var result = execute("survival.gentle_harvest", event);
        assertEquals(AbilityExecutionStatus.NO_OP, result.status());
        assertEquals(0.0, result.observedAmount());
        assertFalse(result.meaningfulOutcome());
        assertEquals(Material.AIR, current.getMaterial());
        assertEquals(3, dropStack.getAmount());
        assertFalse(dropRemoved);
    }

    private BlockDropItemEvent harvestEvent(int amount) {
        Material seed = switch (cropType) {
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> Material.WHEAT_SEEDS;
        };
        dropStack = stack(seed, amount);
        Item item = api(Item.class, (method, args) -> switch (method) {
            case "isValid" -> dropValid && !dropRemoved;
            case "isDead" -> dropRemoved;
            case "getItemStack" -> dropStack;
            case "setItemStack" -> { dropStack = (ItemStack) args[0]; yield null; }
            case "remove" -> { dropRemoved = true; yield null; }
            default -> unexpected(method);
        });
        return new BlockDropItemEvent(block, snapshot(data(cropType, originalAge < 0 ? maximumAge(cropType) : originalAge)), player,
                new ArrayList<>(List.of(item)));
    }

    // ItemStack's public constructors require the server's registry. This double stores actual counts.
    private ItemStack stack(Material material, int initialAmount) {
        return new ItemStack() {
            private int amount = initialAmount;
            @Override public Material getType() { return material; }
            @Override public int getAmount() { return amount; }
            @Override public void setAmount(int value) { amount = value; }
            @Override public boolean hasItemMeta() { return false; }
            @Override public ItemStack clone() { return stack(material, amount); }
        };
    }

    private PlayerInteractEvent inspection(EquipmentSlot hand) {
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block,
                org.bukkit.block.BlockFace.UP, hand);
    }

    private AbilityExecutionResult execute(String id, Event event) {
        var definition = definition(id);
        return executor.execute(definition, context(artifact(), definition, event), 1);
    }

    private Artifact artifact() {
        Artifact artifact = new Artifact(holder, "wooden_sword");
        artifact.setArtifactSeed(19);
        var traits = new java.util.EnumMap<>(artifact.getGenome().traits());
        traits.put(obtuseloot.abilities.genome.GenomeTrait.PRECISION_AFFINITY, .5);
        artifact.setGenome(new obtuseloot.abilities.genome.ArtifactGenome(19, traits));
        return artifact;
    }

    private AbilityEventContext context(Artifact artifact, AbilityDefinition definition, Event event) {
        return new AbilityEventContext(definition.trigger(), artifact, new ArtifactReputation(), 1,
                "test-live", AbilityRuntimeContext.intentional(AbilitySource.OTHER), event).withGameTick(gameTick);
    }

    private AbilityDefinition definition(String id) {
        var t = new AbilityRegistry().templates().stream().filter(template -> template.id().equals(id)).findFirst().orElseThrow();
        return new AbilityDefinition(t.id(), "Localized name", t.family(), t.trigger(), t.mechanic(),
                t.effectPattern(), t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.convergenceVariant(),
                t.memoryVariant(), t.supportModifiers(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }

    private AbilityDefinition variant(AbilityDefinition original, AbilityTrigger trigger, AbilityMechanic mechanic) {
        return new AbilityDefinition(original.id(), original.name(), original.family(), trigger, mechanic,
                original.effectPattern(), original.evolutionVariant(), original.driftVariant(), original.awakeningVariant(),
                original.convergenceVariant(), original.memoryVariant(), original.supportModifiers(), original.effects(),
                original.metadata(), original.stage1(), original.stage2(), original.stage3(), original.stage4(), original.stage5());
    }

    private BlockState snapshot(BlockData value) {
        BlockData copy = value.clone();
        return api(BlockState.class, (method, args) -> switch (method) {
            case "getType" -> copy.getMaterial();
            case "getBlockData" -> copy.clone();
            default -> unexpected(method);
        });
    }

    private BlockData data(Material material, int initialAge) {
        int[] age = {initialAge};
        return api(Ageable.class, (method, args) -> switch (method) {
            case "getMaterial" -> material;
            case "getAge" -> age[0];
            case "setAge" -> { age[0] = (int) args[0]; yield null; }
            case "getMaximumAge" -> maximumAge(material);
            case "clone" -> data(material, age[0]);
            case "matches" -> args[0] instanceof Ageable other && other.getMaterial() == material && other.getAge() == age[0];
            default -> unexpected(method);
        });
    }

    private int maximumAge(Material material) {
        return material == Material.NETHER_WART || material == Material.BEETROOTS ? 3 : 7;
    }

    @FunctionalInterface private interface Call { Object invoke(String method, Object[] args); }

    private static <T> T api(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + " test double";
            default -> call.invoke(method.getName(), args);
        }));
    }

    private static Object unexpected(String method) { throw new AssertionError("Unexpected API call: " + method); }
}
