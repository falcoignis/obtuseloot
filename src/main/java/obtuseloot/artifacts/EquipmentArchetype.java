package obtuseloot.artifacts;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum EquipmentArchetype {
    WOODEN_SWORD("wooden_sword", EquipmentRole.WEAPON, EquipmentRole.MELEE_WEAPON),
    STONE_SWORD("stone_sword", EquipmentRole.WEAPON, EquipmentRole.MELEE_WEAPON),
    IRON_SWORD("iron_sword", EquipmentRole.WEAPON, EquipmentRole.MELEE_WEAPON),
    GOLDEN_SWORD("golden_sword", EquipmentRole.WEAPON, EquipmentRole.MELEE_WEAPON),
    DIAMOND_SWORD("diamond_sword", EquipmentRole.WEAPON, EquipmentRole.MELEE_WEAPON),
    NETHERITE_SWORD("netherite_sword", EquipmentRole.WEAPON, EquipmentRole.MELEE_WEAPON),

    WOODEN_AXE("wooden_axe", EquipmentRole.WEAPON, EquipmentRole.TOOL, EquipmentRole.TOOL_WEAPON_HYBRID),
    STONE_AXE("stone_axe", EquipmentRole.WEAPON, EquipmentRole.TOOL, EquipmentRole.TOOL_WEAPON_HYBRID),
    IRON_AXE("iron_axe", EquipmentRole.WEAPON, EquipmentRole.TOOL, EquipmentRole.TOOL_WEAPON_HYBRID),
    GOLDEN_AXE("golden_axe", EquipmentRole.WEAPON, EquipmentRole.TOOL, EquipmentRole.TOOL_WEAPON_HYBRID),
    DIAMOND_AXE("diamond_axe", EquipmentRole.WEAPON, EquipmentRole.TOOL, EquipmentRole.TOOL_WEAPON_HYBRID),
    NETHERITE_AXE("netherite_axe", EquipmentRole.WEAPON, EquipmentRole.TOOL, EquipmentRole.TOOL_WEAPON_HYBRID),

    TRIDENT("trident", EquipmentRole.WEAPON, EquipmentRole.SPEAR),
    BOW("bow", EquipmentRole.WEAPON, EquipmentRole.RANGED_WEAPON),
    CROSSBOW("crossbow", EquipmentRole.WEAPON, EquipmentRole.RANGED_WEAPON),

    LEATHER_HELMET("leather_helmet", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.HELMET),
    LEATHER_CHESTPLATE("leather_chestplate", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.CHESTPLATE),
    LEATHER_LEGGINGS("leather_leggings", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.LEGGINGS),
    LEATHER_BOOTS("leather_boots", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.BOOTS),
    CHAINMAIL_HELMET("chainmail_helmet", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.HELMET),
    CHAINMAIL_CHESTPLATE("chainmail_chestplate", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.CHESTPLATE),
    CHAINMAIL_LEGGINGS("chainmail_leggings", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.LEGGINGS),
    CHAINMAIL_BOOTS("chainmail_boots", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.BOOTS),
    IRON_HELMET("iron_helmet", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.HELMET),
    IRON_CHESTPLATE("iron_chestplate", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.CHESTPLATE),
    IRON_LEGGINGS("iron_leggings", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.LEGGINGS),
    IRON_BOOTS("iron_boots", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.BOOTS),
    GOLDEN_HELMET("golden_helmet", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.HELMET),
    GOLDEN_CHESTPLATE("golden_chestplate", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.CHESTPLATE),
    GOLDEN_LEGGINGS("golden_leggings", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.LEGGINGS),
    GOLDEN_BOOTS("golden_boots", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.BOOTS),
    DIAMOND_HELMET("diamond_helmet", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.HELMET),
    DIAMOND_CHESTPLATE("diamond_chestplate", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.CHESTPLATE),
    DIAMOND_LEGGINGS("diamond_leggings", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.LEGGINGS),
    DIAMOND_BOOTS("diamond_boots", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.BOOTS),
    NETHERITE_HELMET("netherite_helmet", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.HELMET),
    NETHERITE_CHESTPLATE("netherite_chestplate", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.CHESTPLATE),
    NETHERITE_LEGGINGS("netherite_leggings", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.LEGGINGS),
    NETHERITE_BOOTS("netherite_boots", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.BOOTS),
    TURTLE_HELMET("turtle_helmet", EquipmentRole.ARMOR, EquipmentRole.DEFENSIVE_ARMOR, EquipmentRole.HELMET),
    ELYTRA("elytra", EquipmentRole.ARMOR, EquipmentRole.MOBILITY, EquipmentRole.TRAVERSAL);

    private static final List<EquipmentArchetype> ALL = List.of(values());
    private static final Map<String, EquipmentArchetype> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(EquipmentArchetype::id, value -> value));

    private final String id;
    private final Set<EquipmentRole> roles;

    EquipmentArchetype(String id, EquipmentRole... roles) {
        this.id = id;
        this.roles = Set.copyOf(Arrays.asList(roles));
    }

    public String id() {
        return id;
    }

    public boolean hasRole(EquipmentRole role) {
        return roles.contains(role);
    }

    public Set<EquipmentRole> roles() {
        return roles;
    }

    public String rootForm() {
        if (hasRole(EquipmentRole.MOBILITY) && hasRole(EquipmentRole.TRAVERSAL)) return "Wings";
        if (hasRole(EquipmentRole.HELMET)) return "Helm";
        if (hasRole(EquipmentRole.CHESTPLATE)) return "Cuirass";
        if (hasRole(EquipmentRole.LEGGINGS)) return "Greaves";
        if (hasRole(EquipmentRole.BOOTS)) return "Boots";
        if (hasRole(EquipmentRole.SPEAR)) return "Spear";
        if (id.endsWith("_sword")) return "Blade";
        if (id.endsWith("_axe")) return "Axe";
        if (id.endsWith("bow")) return "Bow";
        return "Artifact";
    }

    public static EquipmentArchetype fromId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Equipment archetype id cannot be blank");
        }
        EquipmentArchetype archetype = BY_ID.get(id.toLowerCase(Locale.ROOT));
        if (archetype == null) {
            throw new IllegalArgumentException("Unknown equipment archetype: " + id);
        }
        return archetype;
    }

    public static boolean isEquipment(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return BY_ID.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public static List<String> allIds() {
        return ALL.stream().map(EquipmentArchetype::id).toList();
    }

    public static List<String> idsMatching(Predicate<EquipmentArchetype> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return ALL.stream().filter(predicate).map(EquipmentArchetype::id).toList();
    }
}
