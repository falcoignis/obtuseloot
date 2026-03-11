package obtuseloot.events;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerWorkCoalescer {
    private final Plugin plugin;
    private final Map<String, BukkitTask> scheduled = new ConcurrentHashMap<>();

    public TriggerWorkCoalescer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void coalesce(String key, long delayTicks, Runnable work) {
        BukkitTask existing = scheduled.remove(key);
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            scheduled.remove(key);
            work.run();
        }, Math.max(1L, delayTicks));
        scheduled.put(key, task);
    }

    public void cancelByPrefix(String prefix) {
        scheduled.entrySet().removeIf(entry -> {
            if (!entry.getKey().startsWith(prefix)) {
                return false;
            }
            BukkitTask task = entry.getValue();
            if (task != null) {
                task.cancel();
            }
            return true;
        });
    }
}
