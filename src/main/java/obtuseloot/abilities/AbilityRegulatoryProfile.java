package obtuseloot.abilities;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class AbilityRegulatoryProfile {
    private final EnumMap<RegulatoryGate, RegulatoryGateState> gates;

    public AbilityRegulatoryProfile(Map<RegulatoryGate, RegulatoryGateState> gates) {
        this.gates = new EnumMap<>(RegulatoryGate.class);
        for (RegulatoryGate gate : RegulatoryGate.values()) {
            this.gates.put(gate, gates.getOrDefault(gate, RegulatoryGateState.closed(0.0D)));
        }
    }

    public boolean isOpen(RegulatoryGate gate) {
        return gates.getOrDefault(gate, RegulatoryGateState.closed(0.0D)).open();
    }

    public double strength(RegulatoryGate gate) {
        return gates.getOrDefault(gate, RegulatoryGateState.closed(0.0D)).strength();
    }

    public Map<RegulatoryGate, RegulatoryGateState> gates() {
        return Map.copyOf(gates);
    }

    public String profileKey() {
        return gates.entrySet().stream()
                .filter(entry -> entry.getValue().open())
                .map(entry -> entry.getKey().id())
                .sorted()
                .collect(Collectors.joining("+", "[", "]"));
    }

    public String openGatesCsv() {
        return gates.entrySet().stream()
                .filter(entry -> entry.getValue().open())
                .map(entry -> entry.getKey().id())
                .sorted()
                .collect(Collectors.joining(","));
    }

    public String summary() {
        List<String> values = gates.entrySet().stream()
                .map(entry -> entry.getKey().id() + "=" + (entry.getValue().open() ? "open" : "closed")
                        + "(" + String.format(Locale.ROOT, "%.2f", entry.getValue().strength()) + ")")
                .toList();
        return String.join(", ", values);
    }
}
