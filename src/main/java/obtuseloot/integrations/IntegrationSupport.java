package obtuseloot.integrations;

import obtuseloot.ObtuseLoot;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/** LuckPerms is supported through its server permission bridge; no grants/groups are written. */
public final class IntegrationSupport {
    private IntegrationSupport() { }
    public static Runnable enable(ObtuseLoot plugin) {
        return enable(plugin.getServer().getPluginManager(), plugin.getEquipmentAbilities().views(),
                plugin.getItemAbilityManager().registry(), plugin.getDescription().getVersion(), plugin.getLogger());
    }
    public static Runnable enable(org.bukkit.plugin.PluginManager manager, obtuseloot.equipment.EquipmentViews views,
                                  obtuseloot.abilities.AbilityRegistry catalog, String version, java.util.logging.Logger logger) {
        var created = new java.util.ArrayList<Permission>();
        for (var registration : catalog.registrations()) {
            if (manager.getPermission(registration.permission()) == null) {
                var permission = new Permission(registration.permission(), PermissionDefault.TRUE);
                manager.addPermission(permission); created.add(permission);
            }
        }
        boolean aura = manager.isPluginEnabled("AuraSkills");
        boolean luck = manager.isPluginEnabled("LuckPerms");
        boolean papi = manager.isPluginEnabled("PlaceholderAPI");
        views.integrations(java.util.Map.of(
                "auraskills", Boolean.toString(aura), "luckperms", Boolean.toString(luck), "placeholderapi", Boolean.toString(papi)));
        if (aura) logger.info("AuraSkills detected: native skill XP and mana handling remain owned by AuraSkills; no XP events are cancelled or replaced.");
        Runnable unregister = () -> { };
        if (papi) {
            try { unregister = registerExpansion(views, version); }
            catch (RuntimeException | LinkageError exception) {
                logger.log(java.util.logging.Level.SEVERE, "PlaceholderAPI integration unavailable; core plugin remains enabled", exception);
            }
        }
        Runnable expansionShutdown = unregister;
        return () -> {
            try { expansionShutdown.run(); }
            finally { for (var permission : created) if (manager.getPermission(permission.getName()) == permission) manager.removePermission(permission); }
        };
    }
    private static Runnable registerExpansion(obtuseloot.equipment.EquipmentViews views, String version) {
        var expansion = new ObtuseLootExpansion(views, version);
        if (!expansion.register()) throw new IllegalStateException("Could not register the ObtuseLoot PlaceholderAPI expansion");
        return expansion::unregister;
    }
}
