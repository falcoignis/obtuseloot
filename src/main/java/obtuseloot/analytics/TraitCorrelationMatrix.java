package obtuseloot.analytics;

import java.util.*;

public class TraitCorrelationMatrix {
    private final Map<String, Map<String, Integer>> matrix = new LinkedHashMap<>();
    private int sampleSize;

    public void incrementPair(String traitA, String traitB) {
        if (traitA == null || traitB == null) {
            return;
        }
        String a = normalize(traitA);
        String b = normalize(traitB);
        if (a.isBlank() || b.isBlank()) {
            return;
        }
        increment(a, b);
        if (!a.equals(b)) {
            increment(b, a);
        }
    }

    public void incrementPairs(Collection<String> traits) {
        List<String> list = traits.stream()
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (list.size() >= 2) {
            sampleSize++;
        }
        for (int i = 0; i < list.size(); i++) {
            for (int j = i; j < list.size(); j++) {
                incrementPair(list.get(i), list.get(j));
            }
        }
    }

    public int sampleSize() {
        return sampleSize;
    }

    public int get(String traitA, String traitB) {
        return matrix.getOrDefault(normalize(traitA), Map.of()).getOrDefault(normalize(traitB), 0);
    }

    public Set<String> traits() {
        return new TreeSet<>(matrix.keySet());
    }

    public int maxFrequency() {
        return matrix.values().stream()
                .flatMap(inner -> inner.values().stream())
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    public List<Map.Entry<String, Integer>> topPairs(int limit) {
        return pairFrequencies().entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(limit)
                .toList();
    }

    public List<Map.Entry<String, Integer>> leastFrequentPairs(int limit) {
        return pairFrequencies().entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .limit(limit)
                .toList();
    }

    public Map<String, Map<String, Integer>> asMap() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        for (String a : traits()) {
            Map<String, Integer> row = new LinkedHashMap<>();
            for (String b : traits()) {
                row.put(b, get(a, b));
            }
            copy.put(a, row);
        }
        return copy;
    }

    private Map<String, Integer> pairFrequencies() {
        Map<String, Integer> pairCounts = new LinkedHashMap<>();
        for (String a : traits()) {
            for (String b : traits()) {
                if (a.compareTo(b) <= 0) {
                    int value = get(a, b);
                    if (value > 0) {
                        pairCounts.put(a + " × " + b, value);
                    }
                }
            }
        }
        return pairCounts;
    }

    private void increment(String a, String b) {
        matrix.computeIfAbsent(a, ignored -> new LinkedHashMap<>()).merge(b, 1, Integer::sum);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
