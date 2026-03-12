package obtuseloot.analytics;

import obtuseloot.ObtuseLoot;
import obtuseloot.evolution.ArtifactFitnessEvaluator;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.abilities.ItemAbilityManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

public final class TriggerSubscriptionIndexReporter {

    public void writeReport(Path path, ItemAbilityManager manager) throws IOException {
        ItemAbilityManager.TriggerSubscriptionIndexStats stats = manager.indexStats();
        Map<AbilityTrigger, Double> avgByTrigger = manager.averageSubscribersPerTrigger();
        StringBuilder out = new StringBuilder();
        out.append("# Trigger Subscription Index Report\n\n");
        out.append("Generated: ").append(Instant.now()).append("\n\n");
        out.append("## 1) What was indexed\n");
        out.append("- Ability trigger subscriptions are indexed per player into trigger -> binding lists.\n");
        out.append("- Indexed players: ").append(stats.indexedPlayers()).append("\n");
        out.append("- Index enabled: ").append(stats.enabled()).append("\n\n");

        out.append("## 2) Event types benefiting most\n");
        out.append("Event routing now dispatches to trigger subscribers instead of scanning the entire active ability profile.\n\n");

        out.append("## 3) Average subscriber counts\n");
        for (AbilityTrigger trigger : AbilityTrigger.values()) {
            out.append("- ").append(trigger.name()).append(": ")
                    .append(String.format(Locale.ROOT, "%.3f", avgByTrigger.getOrDefault(trigger, 0.0D)))
                    .append(" avg subscribers/dispatch\n");
        }
        out.append('\n');

        out.append("## 4) Rebuild behavior\n");
        out.append("- Rebuild count: ").append(stats.rebuildCount()).append("\n");
        out.append("- Average rebuild time: ")
                .append(String.format(Locale.ROOT, "%.3f", stats.averageRebuildMicros()))
                .append(" µs\n\n");

        out.append("## 5) Estimated runtime savings\n");
        out.append("- Dispatch calls: ").append(stats.dispatchCalls()).append("\n");
        out.append("- Indexed dispatches: ").append(stats.indexedDispatchCalls()).append("\n");
        out.append("- Fallback full scans: ").append(stats.fallbackFullScanCalls()).append("\n");
        out.append("- Avg indexed subscribers/dispatch: ")
                .append(String.format(Locale.ROOT, "%.3f", stats.averageIndexedSubscribers()))
                .append("\n");
        out.append("- Estimated savings are highest when ability profiles are large and event triggers are sparse per profile.\n\n");

        out.append("## 6) Trigger budget pressure\n");
        out.append("- Suppression reasons (aggregate): ").append(manager.suppressionReasonCounts()).append("\n");
        out.append("- Budget consumption by trigger (x100 units): ").append(manager.triggerBudgetConsumptionByTrigger()).append("\n");
        out.append("- Budget consumption by mechanic-id (x100 units): ").append(manager.triggerBudgetConsumptionByAbility()).append("\n");
        out.append("- Execution status by mechanic@trigger: ").append(manager.executionStatusByMechanicTrigger()).append("\n");
        out.append("- Meaningful outcomes by mechanic@trigger: ").append(manager.meaningfulOutcomeByMechanicTrigger()).append("\n");
        out.append("- No-op outcomes by mechanic@trigger: ").append(manager.noOpByMechanicTrigger()).append("\n");
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin != null && plugin.getArtifactUsageTracker() != null) {
            out.append("- Utility density by mechanic@trigger: ").append(plugin.getArtifactUsageTracker().utilitySignalRollup()).append("\n");
            out.append("- High-volume low-value mechanics: ").append(plugin.getArtifactUsageTracker().highVolumeLowValueSignals()).append("\n");
            out.append("- Utility-first decision hierarchy: ").append(new ArtifactFitnessEvaluator().decisionHierarchy()).append("\n");
        }
        out.append("\n");

        out.append("## 7) Remaining hot-path concerns\n");
        out.append("- Ensure subscription rebuild hooks are triggered after state-changing debug, evolution, drift, awakening, and fusion flows.\n");
        out.append("- Keep fallback scans disabled in production by keeping runtime.triggerSubscriptionIndexing=true.\n");

        Files.createDirectories(path.getParent());
        Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
    }
}
