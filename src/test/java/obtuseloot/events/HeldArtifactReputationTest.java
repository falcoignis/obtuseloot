package obtuseloot.events;

import obtuseloot.artifacts.Artifact;
import obtuseloot.combat.CombatContextManager;
import org.bukkit.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class HeldArtifactReputationTest {
    private obtuseloot.config.RuntimeSettings.Snapshot previousSettings;
    @BeforeEach void configure() {
        previousSettings = obtuseloot.config.RuntimeSettings.get();
        var config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("reputation.boss-types", java.util.List.of("WITHER"));
        obtuseloot.config.RuntimeSettings.load(config);
    }
    @AfterEach void restore() throws Exception {
        var field = obtuseloot.config.RuntimeSettings.class.getDeclaredField("current");
        field.setAccessible(true); field.set(null, previousSettings);
    }
    private final UUID owner = new UUID(0, 90);
    private final Player player = api(Player.class);
    private final Entity boss = api(Entity.class);

    @Test void bossCreditFollowsTheCurrentlyResolvedItemAcrossSwitches() {
        var first = item(1); var second = item(2);
        var held = new AtomicReference<>(first);
        var listener = new ReputationFeedListener(ignored -> held.get(), new CombatContextManager());
        listener.recordKillContext(player, boss);
        assertEquals(1, first.getItemReputation().getBossKills());
        assertEquals(0, second.getItemReputation().getBossKills());
        held.set(second);
        listener.recordKillContext(player, boss);
        assertEquals(1, first.getItemReputation().getBossKills());
        assertEquals(1, second.getItemReputation().getBossKills());
    }

    @Test void deathResetAffectsOnlyTheCurrentItemAndNeverAPlayerFallback() {
        var first = item(1); var second = item(2);
        first.getItemReputation().recordSurvival(); second.getItemReputation().recordSurvival();
        var held = new AtomicReference<>(first);
        var contexts = new CombatContextManager();
        var listener = new ReputationFeedListener(ignored -> held.get(), contexts);
        listener.recordHeldDeath(player);
        assertEquals(0, first.getItemReputation().getSurvivalStreak());
        assertEquals(1, second.getItemReputation().getSurvivalStreak());
        held.set(second); listener.recordHeldDeath(player);
        assertEquals(2, first.getItemReputation().getChaos());
        assertEquals(2, second.getItemReputation().getChaos());
        held.set(null); contexts.remove(owner);
        listener.recordHeldDeath(player); listener.recordKillContext(player, boss);
        assertTrue(contexts.getLoadedContexts().isEmpty());
        assertEquals(0, first.getItemReputation().getBossKills());
        assertEquals(0, second.getItemReputation().getBossKills());
    }

    private Artifact item(long seed) {
        var item = new Artifact(owner, "wooden_sword");
        item.setArtifactSeed(seed); item.setArtifactStorageKey("artifact:" + new UUID(0, seed));
        return item;
    }
    private <T> T api(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> owner;
            case "getHealth" -> 20.0;
            case "getType" -> EntityType.WITHER;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + " fixture";
            default -> throw new AssertionError("Unexpected API call " + method.getName());
        }));
    }
}
