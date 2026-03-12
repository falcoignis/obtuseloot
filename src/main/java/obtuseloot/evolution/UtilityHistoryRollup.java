package obtuseloot.evolution;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record UtilityHistoryRollup(
        double validatedUtility,
        double utilityDensity,
        double meaningfulRate,
        double noOpRate,
        double budgetEfficiency,
        long attempts,
        Map<String, MechanicUtilitySignal> signalByMechanicTrigger
) {
    private static final String VERSION = "v1";

    public static UtilityHistoryRollup empty() {
        return new UtilityHistoryRollup(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0L, Map.of());
    }

    public static UtilityHistoryRollup fromProfile(ArtifactUsageProfile profile) {
        Map<String, MechanicUtilitySignal> signals = profile.utilitySignalsByMechanic();
        long attempts = signals.values().stream().mapToLong(MechanicUtilitySignal::attempts).sum();
        return new UtilityHistoryRollup(
                profile.validatedUtilityScore(),
                profile.utilityDensity(),
                profile.meaningfulOutcomeRate(),
                profile.averageNoOpRate(),
                profile.utilityBudgetEfficiency(),
                attempts,
                signals
        );
    }

    public static UtilityHistoryRollup parse(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return empty();
        }
        String[] segments = encoded.split("\\|");
        if (segments.length == 0 || !VERSION.equals(segments[0])) {
            return empty();
        }
        Map<String, String> values = new HashMap<>();
        for (int i = 1; i < segments.length; i++) {
            String[] kv = segments[i].split("=", 2);
            if (kv.length == 2) {
                values.put(kv[0], kv[1]);
            }
        }
        Map<String, MechanicUtilitySignal> signals = decodeSignals(values.get("signals"));
        return new UtilityHistoryRollup(
                parseDouble(values.get("vu")),
                parseDouble(values.get("ud")),
                parseDouble(values.get("mr")),
                parseDouble(values.get("nr")),
                parseDouble(values.get("be")),
                parseLong(values.get("at")),
                signals
        );
    }

    public String encode() {
        StringBuilder out = new StringBuilder(VERSION)
                .append("|vu=").append(format(validatedUtility))
                .append("|ud=").append(format(utilityDensity))
                .append("|mr=").append(format(meaningfulRate))
                .append("|nr=").append(format(noOpRate))
                .append("|be=").append(format(budgetEfficiency))
                .append("|at=").append(attempts)
                .append("|signals=");
        boolean first = true;
        for (Map.Entry<String, MechanicUtilitySignal> entry : signalByMechanicTrigger.entrySet()) {
            if (!first) {
                out.append(';');
            }
            first = false;
            MechanicUtilitySignal signal = entry.getValue();
            out.append(entry.getKey()).append(',')
                    .append(format(signal.validatedUtility())).append(',')
                    .append(format(signal.utilityDensity())).append(',')
                    .append(format(signal.noOpRate())).append(',')
                    .append(format(signal.spamPenalty())).append(',')
                    .append(format(signal.redundancyPenalty())).append(',')
                    .append(signal.attempts()).append(',')
                    .append(format(signal.budgetConsumed())).append(',')
                    .append(signal.meaningfulOutcomes()).append(',')
                    .append(format(signal.contextualRelevance()));
        }
        return out.toString();
    }

    public boolean hasUtilityHistory() {
        return attempts >= 3 && !signalByMechanicTrigger.isEmpty();
    }

    public double confidence() {
        return Math.min(1.0D, attempts / 12.0D);
    }

    public double utilityScoreForTemplate(AbilityMechanic mechanic, AbilityTrigger trigger) {
        MechanicUtilitySignal exact = signalByMechanicTrigger.get(mechanic.name() + "@" + trigger.name());
        if (exact != null) {
            return quality(exact);
        }
        double mechanicBest = signalByMechanicTrigger.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(mechanic.name() + "@"))
                .map(Map.Entry::getValue)
                .mapToDouble(this::quality)
                .max()
                .orElse(0.0D);
        double triggerBest = signalByMechanicTrigger.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("@" + trigger.name()))
                .map(Map.Entry::getValue)
                .mapToDouble(this::quality)
                .max()
                .orElse(0.0D);
        return Math.max(mechanicBest, triggerBest);
    }

    public AbilityTrigger preferredTrigger(AbilityTrigger current) {
        if (signalByMechanicTrigger.isEmpty()) {
            return current;
        }
        EnumMap<AbilityTrigger, Double> byTrigger = new EnumMap<>(AbilityTrigger.class);
        for (Map.Entry<String, MechanicUtilitySignal> entry : signalByMechanicTrigger.entrySet()) {
            String[] parts = entry.getKey().split("@", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                AbilityTrigger trigger = AbilityTrigger.valueOf(parts[1]);
                byTrigger.merge(trigger, quality(entry.getValue()), Math::max);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return byTrigger.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(best -> best.getValue() > byTrigger.getOrDefault(current, 0.0D) + 0.12D)
                .map(Map.Entry::getKey)
                .orElse(current);
    }

    public AbilityMechanic preferredMechanic(AbilityMechanic current) {
        if (signalByMechanicTrigger.isEmpty()) {
            return current;
        }
        EnumMap<AbilityMechanic, Double> byMechanic = new EnumMap<>(AbilityMechanic.class);
        for (Map.Entry<String, MechanicUtilitySignal> entry : signalByMechanicTrigger.entrySet()) {
            String[] parts = entry.getKey().split("@", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                AbilityMechanic mechanic = AbilityMechanic.valueOf(parts[0]);
                byMechanic.merge(mechanic, quality(entry.getValue()), Math::max);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return byMechanic.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(best -> best.getValue() > byMechanic.getOrDefault(current, 0.0D) + 0.12D)
                .map(Map.Entry::getKey)
                .orElse(current);
    }

    private double quality(MechanicUtilitySignal signal) {
        return (signal.validatedUtility() * 0.55D)
                + (signal.utilityDensity() * 2.3D)
                + (((double) signal.meaningfulOutcomes() / Math.max(1L, signal.attempts())) * 1.3D)
                - (signal.noOpRate() * 1.6D)
                - (signal.spamPenalty() * 1.4D)
                - (signal.redundancyPenalty() * 1.1D);
    }

    private static Map<String, MechanicUtilitySignal> decodeSignals(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Map.of();
        }
        Map<String, MechanicUtilitySignal> signals = new HashMap<>();
        for (String part : encoded.split(";")) {
            String[] values = part.split(",");
            if (values.length != 10) {
                continue;
            }
            String key = values[0];
            signals.put(key, new MechanicUtilitySignal(
                    key,
                    parseDouble(values[1]),
                    parseDouble(values[2]),
                    parseDouble(values[9]),
                    parseDouble(values[3]),
                    parseDouble(values[4]),
                    parseDouble(values[5]),
                    parseLong(values[6]),
                    parseLong(values[8]),
                    parseDouble(values[7])
            ));
        }
        return Map.copyOf(signals);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
