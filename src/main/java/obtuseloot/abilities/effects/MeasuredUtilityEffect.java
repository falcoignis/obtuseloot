package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.GenomeRevision;
import obtuseloot.config.RuntimeSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Objects;

/** Delivery and owned-item state share one recovery reservation and rollback boundary. */
abstract class MeasuredUtilityEffect implements LiveAbilityEffect {
    private final String tuningKey;
    private final double defaultOutput;
    private final long defaultRecovery;

    protected MeasuredUtilityEffect(String tuningKey, double defaultOutput, long defaultRecovery) {
        this.tuningKey = tuningKey; this.defaultOutput = defaultOutput; this.defaultRecovery = defaultRecovery;
    }
    protected record Report(String text, String key, double benefit, AbilityOutcomeType type,
                            Runnable commit, Runnable rollback) {
        protected Report(String text, String key, double benefit, AbilityOutcomeType type) {
            this(text, key, benefit, type, () -> { }, () -> { });
        }
    }
    protected static Report information(String title, List<String> facts, double output, String key) {
        return information(title, facts, facts, output, key);
    }
    protected static Report information(String title, List<String> facts, List<String> identities, double output, String key) {
        if (facts.size() != identities.size()) throw new IllegalArgumentException("Mismatched information identities");
        long budget = Math.max(1L, (long) Math.floor(output));
        var delivered = facts.stream().limit(budget).toList();
        if (delivered.isEmpty()) return null;
        String text = title + ": " + String.join(" | ", delivered);
        String identity = String.join(";", identities.stream().limit(budget).toList());
        return new Report(text, key + ":" + identity, delivered.size(), AbilityOutcomeType.INFORMATION);
    }
    protected abstract Report prepare(AbilityDefinition definition, AbilityEventContext context, Player player, double output);

    @Override public final AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage) {
        return execute(definition, context, stage, AbilityPerformance.FIXED);
    }
    @Override public final AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context,
                                                        int stage, AbilityPerformance performance) {
        if (!context.liveExecution()) return suppressed(definition, context, "live-context-required");
        if (!Bukkit.isPrimaryThread()) return suppressed(definition, context, "server-thread-required");
        if (!applicable(context)) return suppressed(definition, context, "input-not-applicable");
        Event event = context.liveEvent();
        if (event instanceof Cancellable cancelled && cancelled.isCancelled())
            return suppressed(definition, context, "event-denied");
        if (event instanceof PlayerInteractEvent interaction
                && (interaction.useInteractedBlock() == Event.Result.DENY || interaction.useItemInHand() == Event.Result.DENY))
            return suppressed(definition, context, "interaction-denied");
        EquipmentSlot hand = event instanceof PlayerInteractEvent interaction ? interaction.getHand()
                : event instanceof PlayerInteractEntityEvent interaction ? interaction.getHand() : null;
        if (hand != null && hand != (context.activation() == null ? EquipmentSlot.HAND : context.activation().slot()))
            return suppressed(definition, context, "input-hand-mismatch");
        Player player = event instanceof PlayerEvent playerEvent ? playerEvent.getPlayer()
                : Bukkit.getPlayer(context.artifact().getOwnerId());
        if (player == null || !player.isOnline() || player.isDead()
                || !player.getUniqueId().equals(context.artifact().getOwnerId()))
            return suppressed(definition, context, "holder-unavailable");
        if (context.position() != null && !context.position().worldId().equals(player.getWorld().getUID()))
            return suppressed(definition, context, "world-changed");
        if (!player.hasPermission("obtuseloot.abilities") || !player.hasPermission("obtuseloot.ability." + definition.id()))
            return suppressed(definition, context, "permission-denied");
        var artifact = context.artifact();
        if (context.gameTick() < 0 || !Objects.equals(context.genomeRevision(), GenomeRevision.capture(artifact)))
            return suppressed(definition, context, "stale-or-unclocked-context");
        if (context.gameTick() < artifact.abilitiesReadyAt()) return suppressed(definition, context, "ability-recovering");
        if (!artifact.beginAbilityExecution(definition.id())) return suppressed(definition, context, "ability-in-progress");
        try {
            var settings = RuntimeSettings.get();
            double output = performance.output(settings.utilityOutput(tuningKey, defaultOutput));
            long recovery = performance.recoveryTicks(settings.utilityRecovery(tuningKey, defaultRecovery));
            Report report = prepare(definition, context, player, output);
            if (report == null) return AbilityExecutionResult.observed(definition, context,
                    AbilityOutcomeType.INFORMATION, 0, "utility-unavailable", null);
            long previous = artifact.abilityReadyAt(definition.id());
            long readyAt = Math.addExact(context.gameTick(), recovery);
            var result = AbilityExecutionResult.observed(definition, context, report.type(), report.benefit(), null, report.text())
                    .withObservationKey(report.key()).withPerformance(new AbilityPerformanceMeasurement(report.benefit(), recovery));
            artifact.setAbilityReadyAt(definition.id(), readyAt);
            try {
                report.commit().run();
                player.sendMessage(report.text());
            } catch (RuntimeException | Error failure) {
                try { report.rollback().run(); } catch (RuntimeException | Error rollback) { failure.addSuppressed(rollback); }
                if (artifact.abilityReadyAt(definition.id()) == readyAt) artifact.setAbilityReadyAt(definition.id(), previous);
                throw failure;
            }
            return result;
        } finally { artifact.endAbilityExecution(definition.id()); }
    }
    private AbilityExecutionResult suppressed(AbilityDefinition definition, AbilityEventContext context, String reason) {
        return new AbilityExecutionResult(definition.id(), definition.mechanic(), context.trigger(),
                context.artifact().getArtifactStorageKey(), context.artifact().getOwnerId(), AbilityExecutionStatus.SUPPRESSED,
                AbilityOutcomeType.INFORMATION, false, reason, null);
    }
}
