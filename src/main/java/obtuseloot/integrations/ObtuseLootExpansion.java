package obtuseloot.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import obtuseloot.equipment.EquipmentViews;
import org.bukkit.OfflinePlayer;

/** Registered only with PlaceholderAPI present. Requests read immutable published snapshots. */
public final class ObtuseLootExpansion extends PlaceholderExpansion {
    private final EquipmentViews views;
    private final String version;
    public ObtuseLootExpansion(EquipmentViews views, String version) { this.views = views; this.version = version; }
    @Override public String getIdentifier() { return "obtuseloot"; }
    @Override public String getAuthor() { return "falcoignis"; }
    @Override public String getVersion() { return version; }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player, String params) {
        return views.value(player == null ? null : player.getUniqueId(), params);
    }
}
