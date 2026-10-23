package obtuseloot.abilities;

import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import java.lang.reflect.*;
import java.util.*;

/** Stateful Bukkit API doubles, not native gameplay. Shared by effect and mixed-generation tests. */
public final class InformationFixture implements AutoCloseable {
    public static final UUID OWNER = new UUID(0, 912), WORLD = new UUID(0, 913);
    public UUID holder = OWNER;
    public boolean rich = true, failDelivery;
    public int x = 100;
    public Runnable onMessage = () -> { };
    public final List<String> messages = new ArrayList<>();
    private final Server previous;
    private final RuntimeSettings.Snapshot settings;
    private final Field serverField, settingsField;
    public final Player player;
    public final Block block;
    public final AbilityExecutor executor = new AbilityExecutor();
    public final AbilityDefinition definition;

    public InformationFixture() throws Exception {
        settings = RuntimeSettings.get(); RuntimeSettings.load(new org.bukkit.configuration.file.YamlConfiguration());
        settingsField = RuntimeSettings.class.getDeclaredField("current"); settingsField.setAccessible(true);
        var world = api(World.class, (name, args) -> switch (name) {
            case "getUID" -> WORLD;
            case "isChunkLoaded" -> false;
            case "getMinHeight" -> -64;
            default -> unexpected(name);
        });
        var crop = api(Ageable.class, (name, args) -> switch (name) {
            case "getAge" -> 4;
            case "getMaximumAge" -> 7;
            default -> unexpected(name);
        });
        var farmland = api(Farmland.class, (name, args) -> switch (name) {
            case "getMoisture" -> 7;
            case "getMaximumMoisture" -> 7;
            default -> unexpected(name);
        });
        var plain = api(BlockData.class, (name, args) -> unexpected(name));
        var soil = api(Block.class, (name, args) -> switch (name) {
            case "getType" -> Material.FARMLAND;
            case "getBlockData" -> farmland;
            default -> unexpected(name);
        });
        block = api(Block.class, (name, args) -> switch (name) {
            case "getType" -> rich ? Material.WHEAT : Material.STONE;
            case "getLightLevel" -> (byte) 12;
            case "getBlockData" -> rich ? crop : plain;
            case "getRelative" -> { if (args[0] != BlockFace.DOWN) throw new AssertionError("Unexpected neighbor"); yield soil; }
            case "getWorld" -> world;
            case "getX" -> x;
            case "getY" -> 64;
            case "getZ" -> 0;
            default -> unexpected(name);
        });
        player = api(Player.class, (name, args) -> switch (name) {
            case "getUniqueId" -> holder;
            case "isOnline" -> true;
            case "sendMessage" -> {
                if (failDelivery) throw new IllegalStateException("fixture delivery failed");
                onMessage.run(); messages.add((String) args[0]); yield null;
            }
            default -> unexpected(name);
        });
        var server = api(Server.class, (name, args) -> switch (name) {
            case "isPrimaryThread" -> true;
            default -> unexpected(name);
        });
        serverField = Bukkit.class.getDeclaredField("server"); serverField.setAccessible(true);
        previous = (Server) serverField.get(null); serverField.set(null, server);
        var template = new AbilityRegistry().templateById("precision.material_insight");
        definition = new AbilityDefinition(template.id(), template.name(), template.family(), template.trigger(), template.mechanic(),
                "", "", "", "", "", "", List.of(), List.of(), template.metadata(), "1", "2", "3", "4", "5");
    }
    public Artifact item(long seed, double precision) {
        var item = new Artifact(holder, EquipmentArchetype.DIAMOND_AXE); item.setArtifactStorageKey(ArtifactStorageKeys.create()); item.setArtifactSeed(seed);
        var traits = new EnumMap<>(item.getGenome().traits()); traits.put(obtuseloot.abilities.genome.GenomeTrait.PRECISION_AFFINITY, precision);
        item.setGenome(new obtuseloot.abilities.genome.ArtifactGenome(seed, traits)); return item;
    }
    public PlayerInteractEvent event() { return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND); }
    public AbilityEventContext context(Artifact item, long tick, PlayerInteractEvent event) {
        return new AbilityEventContext(definition.trigger(), item, item.getItemReputation(), 1, "modeled-information",
                AbilityRuntimeContext.intentional(AbilitySource.OTHER), event).withGameTick(tick);
    }
    public AbilityExecutionResult inspect(Artifact item, long tick) { return executor.execute(definition, context(item, tick, event()), 1); }
    @Override public void close() throws Exception { serverField.set(null, previous); settingsField.set(null, settings); }
    private interface Calls { Object call(String name, Object[] args); }
    private <T> T api(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + " fixture";
            default -> calls.call(method.getName(), args);
        }));
    }
    private Object unexpected(String name) { throw new AssertionError("Unexpected API: " + name); }
}
