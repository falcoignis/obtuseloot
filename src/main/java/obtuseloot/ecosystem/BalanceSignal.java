package obtuseloot.ecosystem;

public record BalanceSignal(String metric, String key, double observedRate, double pressure) {
}
