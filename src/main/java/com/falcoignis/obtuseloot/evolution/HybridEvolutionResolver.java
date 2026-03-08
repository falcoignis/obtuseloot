package com.falcoignis.obtuseloot.evolution;

import java.util.HashMap;
import java.util.Map;

public final class HybridEvolutionResolver {
    private static final Map<String, String> HYBRIDS = new HashMap<>();

    static {
        HYBRIDS.put(key("Marksman", "Executioner"), "Deadeye");
        HYBRIDS.put(key("Berserker", "Survivor"), "Juggernaut");
        HYBRIDS.put(key("Acrobat", "Marksman"), "Stormblade");
    }

    private HybridEvolutionResolver() {
    }

    public static String resolve(String primary, String previous) {
        String hybrid = HYBRIDS.get(key(primary, previous));
        return hybrid != null ? hybrid : primary;
    }

    private static String key(String a, String b) {
        return a.compareToIgnoreCase(b) <= 0 ? a + "+" + b : b + "+" + a;
    }
}
