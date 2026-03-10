package obtuseloot.names;

import obtuseloot.config.RuntimeSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArtifactLexemeRegistry {
    private ArtifactLexemeRegistry() {
    }

    public static List<String> lexemesFor(List<String> identityTags, long seed) {
        Map<String, List<String>> pools = RuntimeSettings.get().namingLexemePools();
        List<String> picked = new ArrayList<>();
        for (String tag : identityTags) {
            List<String> options = pools.getOrDefault(tag, List.of());
            if (!options.isEmpty()) {
                int index = (int) Math.floorMod(seed ^ tag.hashCode(), options.size());
                picked.add(options.get(index));
            }
        }
        return picked;
    }

    public static Map<String, List<String>> defaultPools() {
        Map<String, List<String>> pools = new LinkedHashMap<>();
        pools.put("defensive", List.of("ward", "bastion", "vigil"));
        pools.put("chaotic", List.of("ruin", "hollow", "mire"));
        pools.put("precision", List.of("oath", "glass", "warden"));
        pools.put("aggression", List.of("hunger", "thorn", "harrow"));
        pools.put("memory", List.of("echo", "reliquary", "vesper"));
        pools.put("mobility", List.of("storm", "dusk", "choir"));
        pools.put("ritual", List.of("pyre", "cairn", "vesper"));
        pools.put("support", List.of("mercy", "bloom", "ward"));
        pools.put("control", List.of("warden", "vigil", "glass"));
        return pools;
    }
}
