package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.ecology.WorldPosition;
import obtuseloot.persistence.ArtifactProfileCodec;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UtilityAbilityEffectsTest {
    static final String FOOT = "mobility.footprint_memory", WEATHER = "survival.weather_omen",
            HERD = "survival.herd_instinct", REDSTONE = "engineering.redstone_sympathy";

    @Test void allFourHandlersAreSelectableAndTheirGenesExpressBenefitsAndRecovery() throws Exception {
        try (var f = new Fixture()) {
            var catalog = new AbilityRegistry().implementedOnly();
            assertEquals(7, catalog.templates().size());
            for (String id : List.of(FOOT, WEATHER, HERD, REDSTONE)) {
                var registration = catalog.registrationById(id);
                assertNotNull(registration.liveEffect(), id);
                var low = registration.performanceModel().express(f.item(0).getGenome());
                var high = registration.performanceModel().express(f.item(1).getGenome());
                assertTrue(high.output(100) > low.output(100), id);
                assertTrue(high.recoveryTicks(100) > low.recoveryTicks(100), id);
            }
        }
    }

    @Test void waypointMarkSurvivesSerializationTradeAndSameIdentityReplacement() throws Exception {
        try (var f = new Fixture()) {
            var item = f.item(.5); f.sneaking = true;
            var mark = f.execute(FOOT, item, 100, f.blockEvent(EquipmentSlot.HAND));
            assertEquals(AbilityOutcomeType.MEMORY_MARK, mark.outcomeType());
            assertEquals(1, mark.observedAmount());
            assertEquals(60, mark.performance().recoveryTicks());
            var remembered = item.abilityWaypoint(FOOT);
            var restored = ArtifactProfileCodec.decode(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item));
            f.holder = new UUID(0, 99); restored.setOwnerId(f.holder);
            var replacement = f.item(.5); replacement.setArtifactStorageKey(restored.getArtifactStorageKey());
            ArtifactIdentityTransition.regenerate(restored, replacement);
            assertEquals(remembered, replacement.abilityWaypoint(FOOT));
            assertEquals(160, replacement.abilityReadyAt(FOOT));
            f.sneaking = true; f.x = -100; f.y = 60;
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.execute(FOOT, replacement, 159, f.blockEvent(EquipmentSlot.HAND, BlockFace.NORTH)).status());
            var recall = f.execute(FOOT, replacement, 160, f.blockEvent(EquipmentSlot.HAND, BlockFace.NORTH));
            assertEquals(AbilityOutcomeType.NAVIGATION_HINT, recall.outcomeType());
            assertTrue(recall.outputText().contains("east, 100 blocks; 4 blocks up"));
            var newIdentity = f.item(.5); ArtifactIdentityTransition.regenerate(replacement, newIdentity);
            assertNull(newIdentity.abilityWaypoint(FOOT));
            assertTrue(newIdentity.abilityRecoverySnapshot().isEmpty());
        }
    }

    @Test void inheritedRecallRangeIsRealAndNeverCrossesWorldsOrLoadsTheAnchor() throws Exception {
        try (var f = new Fixture()) {
            f.sneaking = true;
            var low = f.item(0); var high = f.item(1);
            var point = new ArtifactWaypoint(Fixture.WORLD, 200, 64, 0);
            low.setAbilityWaypoint(FOOT, point); high.setAbilityWaypoint(FOOT, point);
            assertEquals(0, f.execute(FOOT, low, 100, f.blockEvent(EquipmentSlot.HAND, BlockFace.NORTH)).observedAmount());
            assertEquals(0, low.abilityReadyAt(FOOT));
            var delivered = f.execute(FOOT, high, 100, f.blockEvent(EquipmentSlot.HAND, BlockFace.NORTH));
            assertEquals(1, delivered.observedAmount()); assertEquals(90, delivered.performance().recoveryTicks());
            high.setAbilityWaypoint(FOOT, new ArtifactWaypoint(new UUID(0, 123), 0, 64, 0));
            assertEquals(0, f.execute(FOOT, high, 190, f.blockEvent(EquipmentSlot.HAND, BlockFace.NORTH)).observedAmount());
        }
    }

    @Test void failedMarkDeliveryRollsBackOwnedStateRecoveryAndExecutionLease() throws Exception {
        try (var f = new Fixture()) {
            var item = f.item(.5); var previous = new ArtifactWaypoint(Fixture.WORLD, 5, 70, 5);
            item.setAbilityWaypoint(FOOT, previous); f.sneaking = true; f.failDelivery = true;
            assertThrows(IllegalStateException.class, () -> f.execute(FOOT, item, 100, f.blockEvent(EquipmentSlot.HAND)));
            assertEquals(previous, item.abilityWaypoint(FOOT)); assertEquals(0, item.abilityReadyAt(FOOT));
            f.failDelivery = false;
            assertEquals(AbilityExecutionStatus.SUCCESS, f.execute(FOOT, item, 100, f.blockEvent(EquipmentSlot.HAND)).status());
        }
    }

    @Test void zeroRecoveryStillRejectsReentrancyAfterSameItemReplacement() throws Exception {
        try (var f = new Fixture()) {
            var config = new YamlConfiguration(); config.set("abilities.utility.footprint-memory.recovery-ticks", 0); RuntimeSettings.load(config);
            var item = f.item(.5); var replacement = f.item(.5); replacement.setArtifactStorageKey(item.getArtifactStorageKey());
            var nested = new ArrayList<AbilityExecutionResult>(); f.sneaking = true;
            f.onMessage = () -> {
                ArtifactIdentityTransition.regenerate(item, replacement);
                nested.add(f.execute(FOOT, replacement, 100, f.blockEvent(EquipmentSlot.HAND)));
            };
            assertEquals(AbilityExecutionStatus.SUCCESS, f.execute(FOOT, item, 100, f.blockEvent(EquipmentSlot.HAND)).status());
            assertEquals(AbilityExecutionStatus.SUPPRESSED, nested.getFirst().status());
            assertEquals(1, f.messages.size()); assertEquals(item.abilityWaypoint(FOOT), replacement.abilityWaypoint(FOOT));
            f.onMessage = () -> { };
            assertEquals(AbilityExecutionStatus.SUCCESS, f.execute(FOOT, replacement, 101, f.blockEvent(EquipmentSlot.HAND)).status());
        }
    }

    @Test void anotherAbilitysRecoveryBlocksEveryNewEffectOnTheSameItem() throws Exception {
        try (var f = new Fixture()) {
            for (String id : List.of(FOOT, WEATHER, HERD, REDSTONE)) {
                f.sneaking = id.equals(FOOT);
                var item = f.item(.5); item.setAbilityReadyAt("precision.material_insight", 200);
                var result = f.execute(id, item, 199, f.eventFor(id));
                assertEquals(AbilityExecutionStatus.SUPPRESSED, result.status(), id);
                assertNull(result.observedAmount()); assertEquals(0, item.abilityReadyAt(id));
            }
            assertTrue(f.messages.isEmpty());
        }
    }

    @Test void normalInspectionIsNotStolenByTheSameItemsWaypointAbility() throws Exception {
        try (var f = new Fixture()) {
            var item = f.item(.5); item.setAbilityWaypoint(FOOT, new ArtifactWaypoint(Fixture.WORLD, 0, 64, 0));
            assertFalse(f.executor.eligible(f.definition(FOOT), f.context(f.definition(FOOT), item, 100, f.blockEvent(EquipmentSlot.HAND))));
            var waypoint = f.execute(FOOT, item, 100, f.blockEvent(EquipmentSlot.HAND));
            assertEquals(AbilityExecutionStatus.SUPPRESSED, waypoint.status());
            assertNull(waypoint.observedAmount()); assertTrue(item.abilityRecoverySnapshot().isEmpty());
            assertEquals(AbilityExecutionStatus.SUCCESS, f.execute(REDSTONE, item, 100, f.blockEvent(EquipmentSlot.HAND)).status());
        }
    }

    @Test void denialPermissionsOffhandAndStaleRevisionCannotProduceEffectsOrRecovery() throws Exception {
        try (var f = new Fixture()) {
            var item = f.item(1);
            var denied = f.blockEvent(EquipmentSlot.HAND); denied.setUseInteractedBlock(Event.Result.DENY);
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.execute(REDSTONE, item, 100, denied).status());
            assertEquals(Event.Result.DENY, denied.useInteractedBlock());
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.execute(REDSTONE, item, 100, f.blockEvent(EquipmentSlot.OFF_HAND)).status());
            f.permitted = false;
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.execute(REDSTONE, item, 100, f.blockEvent(EquipmentSlot.HAND)).status());
            f.permitted = true;
            var definition = f.definition(REDSTONE); var context = f.context(definition, item, 100, f.blockEvent(EquipmentSlot.HAND));
            item.setEvolutionPath("strider-formed"); new obtuseloot.evolution.GenomeEvolutionEngine().advance(item);
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.executor.execute(definition, context, 1).status());
            assertTrue(f.messages.isEmpty()); assertTrue(item.abilityRecoverySnapshot().isEmpty());
        }
    }

    @Test void redstoneReadsRealTargetStateAndDepthCostsMoreRecovery() throws Exception {
        try (var f = new Fixture()) {
            var low = f.execute(REDSTONE, f.item(0), 100, f.blockEvent(EquipmentSlot.HAND));
            var high = f.execute(REDSTONE, f.item(1), 100, f.blockEvent(EquipmentSlot.HAND));
            assertEquals(2, low.observedAmount()); assertEquals(40, low.performance().recoveryTicks());
            assertEquals(5, high.observedAmount()); assertEquals(120, high.performance().recoveryTicks());
            assertTrue(high.outputText().contains("delay 3 redstone ticks")); assertTrue(high.outputText().contains("locked true"));
            assertTrue(high.outputText().contains("facing north"));
            assertEquals(high.observedAmount(), high.performance().benefit());
        }
    }

    @Test void anUnloadedClickedChunkIsNotReadOrLoadedForDiagnostics() throws Exception {
        try (var f = new Fixture()) {
            f.loaded = false; var item = f.item(1);
            assertEquals(0, f.execute(REDSTONE, item, 100, f.blockEvent(EquipmentSlot.HAND)).observedAmount());
            assertTrue(f.messages.isEmpty()); assertTrue(item.abilityRecoverySnapshot().isEmpty());
        }
    }

    @Test void committedUtilityFeedsRevisionFitnessAndLocalInheritanceWithoutCopyingWaypoints() throws Exception {
        try (var f = new Fixture()) {
            for (String id : List.of(FOOT, WEATHER, HERD, REDSTONE)) {
                var item = f.item(1); f.sneaking = id.equals(FOOT);
                var definition = f.definition(id); var context = f.context(definition, item, 100, f.eventFor(id));
                var result = f.executor.execute(definition, context, 1);
                assertEquals(AbilityExecutionStatus.SUCCESS, result.status(), id);
                var tracker = new obtuseloot.evolution.ArtifactUsageTracker();
                tracker.trackAbilityExecution(item, context, result, definition);
                assertFalse(item.getLastUtilityHistory().isBlank(), id);
                assertTrue(item.getGenomeFitnessHistory().revisions().contains(context.genomeRevision()), id);
                var store = new MemoryStore(); var manager = new ArtifactManager(store); manager.registerItem(item);
                var pool = new obtuseloot.ecology.LocalInheritancePool();
                pool.record(new AbilityOutcomeObservation(context, result, definition, 0), 6000);
                assertEquals(1, pool.candidates(context.position(), 20, 6000).size(), id);
                var child = new obtuseloot.ecology.LocalEcosystem(store, manager, pool)
                        .createEquipment(f.holder, EquipmentArchetype.IRON_AXE, context.position(), new Random(42));
                assertEquals(item.getArtifactStorageKey(), child.getParentStorageKey(), id);
                assertTrue(child.abilityWaypointsSnapshot().isEmpty(), id);
                assertTrue(child.abilityRecoverySnapshot().isEmpty(), id);
                assertTrue(child.getGenomeFitnessHistory().revisions().isEmpty(), id);
            }
        }
    }

    @Test void deliveryDuringEvolutionCreditsTheProducingGenesAndCannotBreedTheNewUntestedRevision() throws Exception {
        try (var f = new Fixture()) {
            var item = f.item(1); var definition = f.definition(REDSTONE);
            var context = f.context(definition, item, 100, f.blockEvent(EquipmentSlot.HAND));
            f.onMessage = () -> { item.setEvolutionPath("strider-formed"); new obtuseloot.evolution.GenomeEvolutionEngine().advance(item); };
            var result = f.executor.execute(definition, context, 1);
            assertEquals(0, context.genomeRevision().revision()); assertEquals(1, item.getGenomeRevision());
            assertEquals(5, result.observedAmount()); assertEquals(120, result.performance().recoveryTicks());
            var tracker = new obtuseloot.evolution.ArtifactUsageTracker(); tracker.trackAbilityExecution(item, context, result, definition);
            assertTrue(item.getGenomeFitnessHistory().revisions().contains(context.genomeRevision()));
            var store = new MemoryStore(); var manager = new ArtifactManager(store); manager.registerItem(item);
            var pool = new obtuseloot.ecology.LocalInheritancePool(); pool.record(new AbilityOutcomeObservation(context, result, definition, 0), 6000);
            var child = new obtuseloot.ecology.LocalEcosystem(store, manager, pool)
                    .createEquipment(f.holder, EquipmentArchetype.IRON_AXE, context.position(), new Random(42));
            assertNull(child.getParentStorageKey());
        }
    }

    @Test void animalInspectionUsesBaseEntityInteractionAndPreservesDenialsAndVisibility() throws Exception {
        try (var f = new Fixture()) {
            var low = f.execute(HERD, f.item(0), 100, f.animalEvent());
            var high = f.execute(HERD, f.item(1), 100, f.animalEvent());
            assertEquals(2, low.observedAmount()); assertEquals(5, high.observedAmount());
            assertTrue(high.outputText().contains("health 7.5")); assertTrue(high.outputText().contains("breeding ready true"));
            var denied = f.animalEvent(); denied.setCancelled(true);
            assertEquals(AbilityExecutionStatus.SUPPRESSED, f.execute(HERD, f.item(1), 100, denied).status());
            assertTrue(denied.isCancelled()); f.visible = false;
            assertEquals(0, f.execute(HERD, f.item(1), 100, f.animalEvent()).observedAmount());
        }
    }

    @Test void animalCountdownDoesNotEvadeRepeatPenaltyButConditionChangesAreDistinct() throws Exception {
        try (var f = new Fixture()) {
            f.adult = false; f.age = -1000;
            var first = f.execute(HERD, f.item(1), 100, f.animalEvent());
            f.age = -800;
            var second = f.execute(HERD, f.item(1), 300, f.animalEvent());
            assertNotEquals(first.outputText(), second.outputText()); assertEquals(first.observationKey(), second.observationKey());
            f.adult = true;
            assertNotEquals(second.observationKey(), f.execute(HERD, f.item(1), 500, f.animalEvent()).observationKey());
        }
    }

    @Test void factsNotDeliveredToAShallowReaderCannotCreateNoveltyCredit() throws Exception {
        try (var f = new Fixture()) {
            var animal = f.execute(HERD, f.item(0), 100, f.animalEvent()); f.adult = false;
            var hiddenAge = f.execute(HERD, f.item(0), 300, f.animalEvent());
            assertEquals(animal.outputText(), hiddenAge.outputText()); assertEquals(animal.observationKey(), hiddenAge.observationKey());
            var weather = f.execute(WEATHER, f.item(0), 100, null); f.weatherTimer = 9000; f.cycle = false;
            var hiddenForecast = f.execute(WEATHER, f.item(0), 500, null);
            assertEquals(weather.outputText(), hiddenForecast.outputText()); assertEquals(weather.observationKey(), hiddenForecast.observationKey());
        }
    }

    @Test void weatherReportsTargetApiTimersAndPausesWithoutInventedForecasts() throws Exception {
        try (var f = new Fixture()) {
            var report = f.execute(WEATHER, f.item(1), 100, null);
            assertEquals(5, report.observedAmount()); assertTrue(report.outputText().contains("current weather timer 1000 ticks"));
            assertEquals(300, report.performance().recoveryTicks());
            f.cycle = false;
            var paused = f.execute(WEATHER, f.item(1), 500, null);
            assertEquals(3, paused.observedAmount()); assertTrue(paused.outputText().contains("paused"));
            assertFalse(paused.outputText().contains("ticks remaining"));
            f.environment = World.Environment.NETHER;
            assertEquals(0, f.execute(WEATHER, f.item(1), 1000, null).observedAmount());
        }
    }

    @Test void weatherCountdownKeepsSameObservationIdentityUntilTheForecastChanges() throws Exception {
        try (var f = new Fixture()) {
            var first = f.execute(WEATHER, f.item(1), 100, null);
            f.worldTime += 200; f.weatherTimer -= 200; f.thunderTimer -= 200;
            var second = f.execute(WEATHER, f.item(1), 300, null);
            assertNotEquals(first.outputText(), second.outputText()); assertEquals(first.observationKey(), second.observationKey());
            f.storm = true;
            assertNotEquals(second.observationKey(), f.execute(WEATHER, f.item(1), 500, null).observationKey());
        }
    }

    @Test void waypointPayloadIsRequiredValidatedAndPreservesUnknownAbilityState() throws Exception {
        try (var f = new Fixture()) {
            var item = f.item(.5); item.setAbilityWaypoint("removed.ability", new ArtifactWaypoint(Fixture.WORLD, -3.5, 64, 2));
            String encoded = ArtifactProfileCodec.encode(item);
            assertEquals(item.abilityWaypointsSnapshot(), ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded).abilityWaypointsSnapshot());
            var yaml = new YamlConfiguration(); yaml.loadFromString(encoded); yaml.set("ability-waypoints.count", null);
            assertThrows(RuntimeException.class, () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), yaml.saveToString()));
            yaml.loadFromString(encoded); yaml.set("ability-waypoints.entries.0.x", Double.NaN);
            assertThrows(RuntimeException.class, () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), yaml.saveToString()));
            yaml.loadFromString(encoded); yaml.set("profile-version", 4);
            assertThrows(RuntimeException.class, () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), yaml.saveToString()));
        }
    }

    @Test void invalidUtilityConfigurationDoesNotReplaceTheActiveSnapshot() throws Exception {
        try (var f = new Fixture()) {
            var previous = RuntimeSettings.get();
            var yaml = new YamlConfiguration(); yaml.set("abilities.utility.herd-instinct.base-output", -1);
            assertThrows(IllegalArgumentException.class, () -> RuntimeSettings.load(yaml)); assertSame(previous, RuntimeSettings.get());
            yaml.set("abilities.utility.herd-instinct.base-output", 4); yaml.set("abilities.utility.herd-instinct.recovery-ticks", 1.2);
            assertThrows(IllegalArgumentException.class, () -> RuntimeSettings.load(yaml)); assertSame(previous, RuntimeSettings.get());
        }
    }

    static final class Fixture implements AutoCloseable {
        static final UUID OWNER = new UUID(0, 901), WORLD = new UUID(0, 902), ANIMAL = new UUID(0, 903);
        UUID holder = OWNER;
        double x, y = 64, z;
        boolean sneaking, failDelivery, permitted = true, visible = true, adult = true, cycle = true, storm, loaded = true;
        int age, weatherTimer = 1000, thunderTimer = 2000;
        long worldTime = 100;
        World.Environment environment = World.Environment.NORMAL;
        Runnable onMessage = () -> { };
        final List<String> messages = new ArrayList<>();
        final AbilityExecutor executor;
        final World world;
        final Player player;
        final Block block;
        final Animals animal;
        final Field serverField, settingsField;
        final Object previousServer, previousSettings;
        Fixture() throws Exception {
            serverField = Bukkit.class.getDeclaredField("server"); serverField.setAccessible(true); previousServer = serverField.get(null);
            settingsField = RuntimeSettings.class.getDeclaredField("current"); settingsField.setAccessible(true); previousSettings = settingsField.get(null);
            RuntimeSettings.load(new YamlConfiguration());
            var registry = new AbilityRegistry();
            executor = new AbilityExecutor(new AbilityRegistry(registry.registrations().stream()
                    .map(r -> r.template().id().equals(WEATHER) ? r.withLiveEffect(new WeatherOmenEffect(w -> cycle)) : r).toList()));
            world = api(World.class, (name, args) -> switch (name) {
                case "getUID" -> WORLD; case "getEnvironment" -> environment; case "hasStorm" -> storm;
                case "isThundering" -> false; case "getGameRuleValue" -> cycle; case "getClearWeatherDuration" -> 0;
                case "getWeatherDuration" -> weatherTimer; case "getThunderDuration" -> thunderTimer; case "getGameTime" -> worldTime;
                case "isChunkLoaded" -> loaded;
                default -> unexpected(name);
            });
            player = api(Player.class, (name, args) -> switch (name) {
                case "getUniqueId" -> holder; case "isOnline" -> true; case "isDead" -> false; case "getWorld" -> world;
                case "getLocation" -> new Location(world, x, y, z); case "isSneaking" -> sneaking;
                case "hasPermission" -> permitted; case "isOp" -> true; case "canSee" -> visible;
                case "sendMessage" -> { if (failDelivery) throw new IllegalStateException("delivery failed"); onMessage.run(); messages.add((String) args[0]); yield null; }
                default -> unexpected(name);
            });
            var data = api(Repeater.class, (name, args) -> switch (name) {
                case "isPowered", "isLocked" -> true; case "getDelay" -> 3; case "getFacing" -> BlockFace.NORTH;
                default -> unexpected(name);
            });
            block = api(Block.class, (name, args) -> switch (name) {
                case "getWorld" -> world; case "getType" -> Material.REPEATER;
                case "getBlockData" -> { if (!loaded) throw new AssertionError("Unloaded block read"); yield data; }
                case "getX", "getZ" -> 0; case "getY" -> 64; default -> unexpected(name);
            });
            animal = api(Animals.class, (name, args) -> switch (name) {
                case "getWorld" -> world; case "getUniqueId" -> ANIMAL; case "getType" -> EntityType.COW;
                case "isDead" -> false; case "isValid", "canBreed" -> true; case "getHealth" -> 7.5;
                case "isAdult" -> adult; case "getAge" -> age; case "getLoveModeTicks" -> 0;
                default -> unexpected(name);
            });
            serverField.set(null, api(Server.class, (name, args) -> switch (name) {
                case "isPrimaryThread" -> true; case "getPlayer" -> player; default -> unexpected(name);
            }));
        }
        Artifact item(double gene) {
            var item = new Artifact(holder, EquipmentArchetype.DIAMOND_AXE);
            item.setArtifactStorageKey(ArtifactStorageKeys.create()); item.setArtifactSeed(19);
            var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
            for (var trait : GenomeTrait.values()) traits.put(trait, gene);
            item.setGenome(new ArtifactGenome(19, traits)); return item;
        }
        AbilityDefinition definition(String id) {
            var t = new AbilityRegistry().templateById(id);
            return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), t.effectPattern(),
                    "", "", "", "", "", List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
        }
        PlayerInteractEvent blockEvent(EquipmentSlot hand) { return blockEvent(hand, BlockFace.UP); }
        PlayerInteractEvent blockEvent(EquipmentSlot hand, BlockFace face) { return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, face, hand); }
        PlayerInteractEntityEvent animalEvent() { return new PlayerInteractEntityEvent(player, animal, EquipmentSlot.HAND); }
        Event eventFor(String id) { return id.equals(WEATHER) ? null : id.equals(HERD) ? animalEvent() : blockEvent(EquipmentSlot.HAND); }
        AbilityEventContext context(AbilityDefinition definition, Artifact item, long tick, Event event) {
            return new AbilityEventContext(definition.trigger(), item, item.getItemReputation(), 1, "modeled-utility",
                    AbilityRuntimeContext.intentional(AbilitySource.OTHER), event, true, new WorldPosition(WORLD, x, z)).withGameTick(tick);
        }
        AbilityExecutionResult execute(String id, Artifact item, long tick, Event event) {
            var definition = definition(id); return executor.execute(definition, context(definition, item, tick, event), 1);
        }
        @Override public void close() throws Exception { serverField.set(null, previousServer); settingsField.set(null, previousSettings); }
        interface Calls { Object call(String name, Object[] args); }
        static <T> T api(Class<T> type, Calls calls) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
                case "equals" -> proxy == args[0]; case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> type.getSimpleName() + " utility fixture"; default -> calls.call(method.getName(), args);
            }));
        }
        static Object unexpected(String name) { throw new AssertionError("Unexpected API access: " + name); }
    }
    private static final class MemoryStore implements obtuseloot.persistence.PlayerStateStore {
        final Map<String, String> profiles = new HashMap<>();
        public void saveArtifactProfile(Artifact item) { profiles.put(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item)); }
        public Artifact loadArtifactProfile(String key) { return profiles.containsKey(key) ? ArtifactProfileCodec.decode(key, profiles.get(key)) : null; }
        public String loadEcologyState() { return null; }
        public void saveEcologyState(String value) { }
        public void saveArtifact(UUID id, Artifact item) { throw new UnsupportedOperationException(); }
        public Artifact loadArtifact(UUID id) { return null; }
        public void saveReputation(UUID id, obtuseloot.reputation.ArtifactReputation reputation) { throw new UnsupportedOperationException(); }
        public obtuseloot.reputation.ArtifactReputation loadReputation(UUID id) { return null; }
    }
}
