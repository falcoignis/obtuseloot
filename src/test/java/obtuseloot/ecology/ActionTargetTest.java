package obtuseloot.ecology;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class ActionTargetTest {
    @Test void readsTheMeasuredBlockForEachEventKind() {
        var player = api(Player.class, null);
        assertEquals("stone", ActionTarget.of(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null,
                api(Block.class, Material.STONE), BlockFace.UP, EquipmentSlot.HAND)));
        assertEquals(ActionTarget.NONE, ActionTarget.of(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, null,
                null, BlockFace.UP, EquipmentSlot.HAND)));
        // The broken block is already air when drops fire; its captured state is the harvested crop.
        assertEquals("wheat", ActionTarget.of(new BlockDropItemEvent(api(Block.class, Material.AIR),
                api(BlockState.class, Material.WHEAT), player, new ArrayList<>())));
        assertEquals("deepslate_iron_ore", ActionTarget.of(new BlockBreakEvent(api(Block.class, Material.DEEPSLATE_IRON_ORE), player)));
        assertEquals(ActionTarget.NONE, ActionTarget.of(null));
        assertTrue(ActionTarget.valid("deepslate_iron_ore")); assertTrue(ActionTarget.valid(ActionTarget.NONE));
        assertFalse(ActionTarget.valid("Stone")); assertFalse(ActionTarget.valid("")); assertFalse(ActionTarget.valid(null));
    }
    private <T> T api(Class<T> type, Material material) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "getType" -> material;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + " fixture";
            default -> throw new AssertionError("Unexpected API: " + method.getName());
        }));
    }
}
