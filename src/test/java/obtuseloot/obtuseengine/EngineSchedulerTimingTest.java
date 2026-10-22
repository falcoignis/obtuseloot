package obtuseloot.obtuseengine;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.evolution.*;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class EngineSchedulerTimingTest {
    private final UUID owner = new UUID(0, 41);
    private final Store store = new Store();
    private final AtomicInteger profileBuilds = new AtomicInteger();
    private Server previousServer;
    private RuntimeSettings.Snapshot previousSettings;
    private ArtifactManager artifacts;
    private ReputationManager reputations;
    private ItemAbilityManager abilities;
    private EngineScheduler scheduler;
    private Artifact artifact;
    private ArtifactReputation reputation;
    private Runnable tick;
    private AbilityTemplate first;
    private AbilityTemplate evolved;
    private boolean cancelled;
    private boolean failNextRefresh;

    @BeforeEach
    void setUp() throws Exception {
        previousSettings = RuntimeSettings.get();
        first = template("test.first_effect");
        evolved = template("test.evolved_effect");
        LiveAbilityEffect effect = (definition, context, stage) -> AbilityExecutionResult.observed(
                definition, context, AbilityOutcomeType.INFORMATION, 1, null, "measured report");
        var catalog = new AbilityRegistry(List.of(new AbilityRegistration(first, effect), new AbilityRegistration(evolved, effect)));
        var tracker = new ArtifactUsageTracker();
        abilities = new ItemAbilityManager((item, rep) -> {
            profileBuilds.incrementAndGet();
            if (failNextRefresh) { failNextRefresh = false; throw new IllegalStateException("test refresh failure"); }
            return new AbilityProfile("test", List.of(definition(item.getEvolutionPath().endsWith("-formed") ? evolved : first)));
        }, catalog, tracker);
        artifacts = new ArtifactManager(store);
        reputations = new ReputationManager(store);
        artifact = artifacts.getOrCreate(owner);
        reputation = reputations.get(owner);
        Plugin plugin = api(Plugin.class, (name, args) -> switch (name) {
            case "getLogger" -> Logger.getLogger("EngineSchedulerTimingTest");
            default -> unexpected(name);
        });
        BukkitTask task = api(BukkitTask.class, (name, args) -> switch (name) {
            case "isCancelled" -> cancelled;
            case "cancel" -> { cancelled = true; yield null; }
            default -> unexpected(name);
        });
        BukkitScheduler timer = api(BukkitScheduler.class, (name, args) -> {
            if (name.equals("runTaskTimer")) { tick = (Runnable) args[1]; return task; }
            return unexpected(name);
        });
        Player player = api(Player.class, (name, args) -> switch (name) {
            case "isOnline" -> true;
            default -> unexpected(name);
        });
        Server server = api(Server.class, (name, args) -> switch (name) {
            case "getScheduler" -> timer;
            case "getPlayer" -> player;
            default -> unexpected(name);
        });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        previousServer = (Server) field.get(null);
        field.set(null, server);
        scheduler = new EngineScheduler(plugin, artifacts, reputations, new CombatContextManager(),
                new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver()), abilities);
        abilities.rebuildSubscriptions(owner, artifact, reputation, "test-initial");
        scheduler.startEvolutionTask();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (scheduler != null) scheduler.stopAll();
        Field server = Bukkit.class.getDeclaredField("server");
        server.setAccessible(true);
        server.set(null, previousServer);
        Field settings = RuntimeSettings.class.getDeclaredField("current");
        settings.setAccessible(true);
        settings.set(null, previousSettings);
    }

    @Test
    void realDispatchFeedbackAdvancesStageAndRebindsWithoutAnyKill() {
        for (int i = 0; i < 100 && reputation.getTotalScore() < 10; i++) dispatch("distinct-context-" + i);
        assertTrue(reputation.getTotalScore() >= 10, "Measured positive utility must earn existing family reputation");
        assertFalse(artifact.getEvolutionPath().endsWith("-formed"));
        assertEquals(1, profileBuilds.get());
        advance(99);
        assertEquals(1, profileBuilds.get());
        advance(1);
        assertTrue(artifact.getEvolutionPath().endsWith("-formed"));
        assertEquals(2, profileBuilds.get());
        assertEquals(evolved.id(), abilities.triggerMap(owner).bindingsFor(first.trigger()).getFirst().abilityId());
        assertEquals(0, reputation.getKills());
        assertEquals(0, reputation.getBossKills());
        advance(200);
        assertEquals(2, profileBuilds.get(), "Idle windows must not evolve or reroll");
    }

    @Test
    void quitFlushAndShutdownDoNotReplayEarnedPointsOrForceExtraSelections() {
        dispatch("observed");
        int score = reputation.getTotalScore();
        String balance = reputation.encodeUtilityExperience();
        scheduler.flushEvolution(owner);
        assertEquals(1, profileBuilds.get());
        scheduler.stopAll();
        assertEquals(score, reputation.getTotalScore());
        assertEquals(balance, reputation.encodeUtilityExperience());
        assertTrue(cancelled);
    }

    @Test
    void resetOrReplacementCannotReceiveTheOldWindow() {
        dispatch("observed");
        var replacement = new Artifact(owner, "wooden_sword");
        replacement.setArtifactSeed(42);
        artifacts.getLoadedArtifacts().put(owner, replacement);
        advance(100);
        assertEquals(1, profileBuilds.get());
        assertFalse(replacement.getEvolutionPath().endsWith("-formed"));
    }

    @Test
    void liveIntervalSettingIsReadWithoutRestartingTimer() {
        dispatch("observed");
        YamlConfiguration config = new YamlConfiguration();
        config.set("evolution.noncombat-update-interval-ticks", 2);
        RuntimeSettings.load(config);
        advance(2);
        assertEquals(2, profileBuilds.get());
        config.set("evolution.noncombat-update-interval-ticks", 0);
        assertThrows(IllegalArgumentException.class, () -> RuntimeSettings.load(config));
        assertEquals(2, RuntimeSettings.get().noncombatUpdateIntervalTicks());
    }

    @Test void twoNonMainHandItemsForTheSameHolderEvolveInTheirOwnWindows() {
        var items = new ArrayList<Artifact>();
        for (int i = 0; i < 2; i++) {
            var item = new Artifact(owner, "elytra"); item.setArtifactSeed(100 + i);
            item.setArtifactStorageKey(obtuseloot.artifacts.ArtifactStorageKeys.create());
            artifacts.registerItem(item); items.add(item);
        }
        for (var item : items) {
            for (int i = 0; i < 100 && item.getItemReputation().getTotalScore() < 10; i++) {
                abilities.resolveDispatch(new AbilityEventContext(first.trigger(), item, item.getItemReputation(), 1,
                        "distinct-" + i, AbilityRuntimeContext.intentional(AbilitySource.OTHER), null, true));
            }
            assertTrue(item.getItemReputation().getTotalScore() >= 10);
        }
        advance(100);
        for (var item : items) assertTrue(item.getEvolutionPath().endsWith("-formed"));
        assertSame(artifact, artifacts.getLoadedArtifacts().get(owner), "Equipment evolution does not replace the main-hand command target");
        assertEquals(0, reputation.getTotalScore());
    }

    @Test void failedProfileRefreshRetriesWithoutMutatingTheGenomeTwice() {
        for (int i = 0; i < 100 && reputation.getTotalScore() < 10; i++) dispatch("retry-" + i);
        failNextRefresh = true;
        advance(100);
        assertEquals(1, artifact.getGenomeRevision());
        var genome = artifact.getGenome();
        advance(100);
        assertSame(genome, artifact.getGenome());
        assertEquals(1, artifact.getGenomeRevision());
        assertEquals(3, profileBuilds.get());
        assertEquals(evolved.id(), abilities.triggerMap(owner).bindingsFor(first.trigger()).getFirst().abilityId());
    }

    private void advance(int ticks) { for (int i = 0; i < ticks; i++) tick.run(); }

    private void dispatch(String source) {
        Event event = new Event() { @Override public HandlerList getHandlers() { return new HandlerList(); } };
        var result = abilities.resolveDispatch(new AbilityEventContext(first.trigger(), artifact, reputation, 1, source,
                AbilityRuntimeContext.intentional(AbilitySource.OTHER), event, true));
        assertEquals(AbilityExecutionStatus.SUCCESS, result.executions().getFirst().status());
    }

    private AbilityTemplate template(String id) {
        var t = new AbilityRegistry().templateById("precision.material_insight");
        var metadata = AbilityMetadata.of(Set.of("information"), Set.of("intentional"), Set.of("watchful"),
                0.5, 0.5, 0.5, 0.5, 0.5, 0.5,
                new TriggerBudgetProfile(0.1, 0.1, 100, 100, 100, 100, 100, TriggerBudgetPolicy.CRITICAL_SYSTEM, true, 0));
        return new AbilityTemplate(id, id, t.category(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "", List.of(), metadata);
    }

    private AbilityDefinition definition(AbilityTemplate t) {
        return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "", List.of(), List.of(),
                t.metadata(), "1", "2", "3", "4", "5");
    }

    private static final class Store implements PlayerStateStore {
        private final Map<String, Artifact> items = new HashMap<>();
        public Artifact loadArtifactProfile(String key) { return items.get(key); }
        public void saveArtifactProfile(Artifact item) { items.put(item.getArtifactStorageKey(), item); }
        private final Map<UUID, Artifact> artifacts = new HashMap<>();
        private final Map<UUID, ArtifactReputation> reputations = new HashMap<>();
        public Artifact loadArtifact(UUID id) { return artifacts.get(id); }
        public void saveArtifact(UUID id, Artifact artifact) { artifacts.put(id, artifact); }
        public ArtifactReputation loadReputation(UUID id) { return reputations.get(id); }
        public void saveReputation(UUID id, ArtifactReputation reputation) { reputations.put(id, reputation); }
    }

    @FunctionalInterface private interface Call { Object invoke(String name, Object[] args); }
    private static <T> T api(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + " test double";
            default -> call.invoke(method.getName(), args);
        }));
    }
    private static Object unexpected(String name) { throw new AssertionError("Unexpected API call: " + name); }
}
