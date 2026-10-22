package obtuseloot.equipment;

import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.integrations.IntegrationSupport;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class StandaloneIntegrationTest {
    @Test void optionalApiClassesCanBeCompletelyAbsentAndOwnedPermissionsAreCleaned() throws Exception {
        boot(false);
    }
    @Test void brokenOptionalPluginDoesNotDisableCoreOrChangeAnotherPluginsPermissions() throws Exception {
        boot(true);
    }
    private void boot(boolean brokenPapi) throws Exception {
        Map<String, Permission> permissions = new HashMap<>();
        var existing = new Permission("obtuseloot.ability.mobility.fleetfoot");
        permissions.put(existing.getName(), existing);
        var manager = (PluginManager) Proxy.newProxyInstance(PluginManager.class.getClassLoader(), new Class<?>[]{PluginManager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPermission" -> permissions.get(args[0]);
                    case "addPermission" -> { var p = (Permission) args[0]; assertNull(permissions.putIfAbsent(p.getName(), p)); yield null; }
                    case "removePermission" -> { permissions.remove(((Permission) args[0]).getName()); yield null; }
                    case "isPluginEnabled" -> args[0].equals("AuraSkills") || args[0].equals("PlaceholderAPI") && brokenPapi;
                    default -> throw new AssertionError(method.getName());
                });
        var location = IntegrationSupport.class.getProtectionDomain().getCodeSource().getLocation();
        try (var isolated = new URLClassLoader(new java.net.URL[]{location}, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("me.clip.placeholderapi.") || name.startsWith("dev.aurelium.") || name.startsWith("net.luckperms."))
                    throw new ClassNotFoundException("Optional API deliberately absent");
                if (name.startsWith("obtuseloot.integrations.")) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) type = findClass(name);
                    if (resolve) resolveClass(type);
                    return type;
                }
                return super.loadClass(name, resolve);
            }
        }) {
            var integration = Class.forName("obtuseloot.integrations.IntegrationSupport", true, isolated);
            var views = new EquipmentViews();
            var stop = (Runnable) integration.getMethod("enable", PluginManager.class, EquipmentViews.class,
                    AbilityRegistry.class, String.class, Logger.class).invoke(null, manager, views,
                    new AbilityRegistry().implementedOnly(), "test", Logger.getLogger("StandaloneIntegrationTest"));
            assertTrue(permissions.size() > 1);
            assertEquals("true", views.value(null, "integration_auraskills"));
            assertEquals("false", views.value(null, "integration_luckperms"));
            stop.run();
            assertEquals(Map.of(existing.getName(), existing), permissions);
        }
    }
}
