package obtuseloot.persistence;

import obtuseloot.abilities.AbilityFamily;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UtilityExperiencePersistenceTest {
    @TempDir Path temp;

    @Test
    void sqliteRoundTripPreservesFractionalCreditAndPenaltyDebt() throws Exception {
        var database = new DatabaseManager("jdbc:sqlite:" + temp.resolve("utility.db"), null, null);
        try (var connection = database.openConnection()) { new SqlSchemaManager().ensureSchema(connection); }
        roundTrip(() -> new JdbcPlayerStateStore(database, null, JdbcPlayerStateStore.Dialect.SQLITE));
    }

    @Test
    void yamlRoundTripPreservesFractionalCreditAndPenaltyDebt() {
        roundTrip(() -> new YamlPlayerStateStore(plugin()));
        var loaded = new YamlPlayerStateStore(plugin()).loadReputation(new UUID(0, 1));
        assertEquals(1, loaded.getPrecision());
        assertTrue(loaded.encodeUtilityExperience().contains("SURVIVAL=-0.5"));
    }

    @Test
    void malformedExperienceCannotPartiallyReplaceAnExistingBalance() {
        var rep = new ArtifactReputation();
        rep.recordUtilityExperience(AbilityFamily.PRECISION, 0.5);
        String original = rep.encodeUtilityExperience();
        for (String invalid : new String[]{"PRECISION=NaN", "SURVIVAL=Infinity", "PRECISION=1.5", "PRECISION=0.2;PRECISION=0.3", "broken"}) {
            assertThrows(IllegalArgumentException.class, () -> rep.restoreUtilityExperience(invalid));
            assertEquals(original, rep.encodeUtilityExperience());
        }
    }

    private void roundTrip(java.util.function.Supplier<PlayerStateStore> stores) {
        PlayerStateStore store = stores.get();
        var owner = new UUID(0, 1);
        var rep = new ArtifactReputation();
        rep.recordUtilityExperience(AbilityFamily.PRECISION, 0.75);
        rep.recordUtilityExperience(AbilityFamily.SURVIVAL, -0.5);
        store.saveReputation(owner, rep);
        store.flushPendingWrites();
        store = stores.get(); // YAML flush closes its writer; reopening also proves actual disk persistence.
        var loaded = store.loadReputation(owner);
        assertEquals(rep.encodeUtilityExperience(), loaded.encodeUtilityExperience());
        loaded.recordUtilityExperience(AbilityFamily.PRECISION, 0.5);
        assertEquals(1, loaded.getPrecision());
        assertEquals(0, loaded.getKills());
        store.saveReputation(owner, loaded);
        store.flushPendingWrites();
        var reopened = stores.get();
        assertEquals(loaded.encodeUtilityExperience(), reopened.loadReputation(owner).encodeUtilityExperience());
        reopened.flushPendingWrites();
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getDataFolder" -> temp.toFile();
            case "getLogger" -> Logger.getLogger("UtilityExperiencePersistenceTest");
            case "getName" -> "test";
            case "isEnabled" -> true;
            default -> null;
        });
    }
}
