package obtuseloot.servertest;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.GenomeRevision;
import obtuseloot.artifacts.*;
import obtuseloot.ecology.WorldPosition;
import obtuseloot.persistence.ArtifactProfileCodec;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;

/** Controlled API observations in an empty standalone server; never a native gameplay claim. */
public final class RevisionFitnessProbe extends JavaPlugin {
    private static final String KEY = "artifact:00000000-0000-0000-0000-000000000416";
    private static final UUID OWNER = new UUID(0, 416), BUYER = new UUID(0, 417);

    @Override public void onEnable() {
        getServer().getScheduler().runTaskLater(this, () -> {
            try { runPhase(); }
            catch (Throwable failure) {
                failure.printStackTrace();
                try { Files.createDirectories(getDataFolder().toPath()); Files.writeString(getDataFolder().toPath().resolve("failure.txt"), failure.toString()); }
                catch (Exception writeFailure) { writeFailure.printStackTrace(); }
            } finally { getServer().shutdown(); }
        }, 20);
    }

    private void runPhase() throws Exception {
        var core = ObtuseLoot.get();
        check(core != null && core.isEnabled(), "Standalone core must enable");
        for (String optional : new String[]{"PlaceholderAPI", "LuckPerms", "AuraSkills", "PacketEvents"})
            check(!getServer().getPluginManager().isPluginEnabled(optional), "Unexpected optional plugin " + optional);
        Path root = getDataFolder().toPath(); Files.createDirectories(root);
        int phase = Files.exists(root.resolve("phase")) ? Integer.parseInt(Files.readString(root.resolve("phase"))) : 0;
        check(phase < 3, "Probe already completed");
        var manager = core.getArtifactManager();
        var store = core.getPlayerStateStore();
        var ecosystem = core.getLocalEcosystem();
        var position = new WorldPosition(getServer().getWorlds().getFirst().getUID(), 0, 0);
        Artifact item;
        if (phase == 0) {
            check(store.loadArtifactProfile(KEY) == null, "Probe requires an empty server");
            item = new Artifact(OWNER, "elytra"); item.setArtifactStorageKey(KEY);
            new ArtifactSeedFactory().applySeedProfile(item, -416L);
            manager.registerItem(item); manager.activateItem(OWNER, item);
            var deferred = context(item, position);
            observe(core, deferred, "before");
            item.getItemReputation().setMobility(obtuseloot.config.RuntimeSettings.get().temperedThreshold());
            core.getEvolutionEngine().evaluate(item, item.getItemReputation());
            check(item.getGenomeRevision() == 1, "Real evolution must commit a new revision");
            observe(core, deferred, "delayed-before");
            check(item.getGenomeFitnessHistory().profile(new GenomeRevision(-416, 0)).validatedUtilityScore() > 0, "Old outcomes lost");
            check(item.getGenomeFitnessHistory().profile(GenomeRevision.capture(item)).validatedUtilityScore() == 0, "Delayed result credited new genes");
            manager.saveItem(item);
            check(ecosystem.createEquipment(OWNER, EquipmentArchetype.ELYTRA, position, new Random(11)).getParentStorageKey() == null,
                    "An unproven revision inherited old fitness");
            observe(core, context(item, position), "current");
            var child = ecosystem.createEquipment(OWNER, EquipmentArchetype.ELYTRA, position, new Random(12));
            check(KEY.equals(child.getParentStorageKey()) && child.getParentGenomeRevision() == 1 && child.getParentGenomeSeed() == -416, "Current revision cannot reproduce");
            check(child.getGenomeFitnessHistory().revisions().isEmpty(), "Child inherited parental fitness");
            Files.writeString(root.resolve("child-key"), child.getArtifactStorageKey());
            var prediction = ArtifactProfileCodec.decode(KEY, ArtifactProfileCodec.encode(item));
            var ctx = context(prediction, position); var def = definition(); var result = result(def, ctx, "current");
            new obtuseloot.evolution.ArtifactUsageTracker().trackAbilityExecution(prediction, ctx, result, def);
            Files.writeString(root.resolve("next.yml"), ArtifactProfileCodec.encode(prediction));
        } else {
            item = manager.resolveByStorageKey(KEY);
            var expected = ArtifactProfileCodec.decode(KEY, Files.readString(root.resolve("expected.yml")));
            equalFitness(item, expected);
            check(item.getGenomeRevision() == 1 && item.getGenome().traits().equals(expected.getGenome().traits()), "Genes changed on restart");
            check(item.getOwnerId().equals(phase == 1 ? OWNER : BUYER), "Owner did not persist");
            var savedChild = store.loadArtifactProfile(Files.readString(root.resolve("child-key")));
            check(savedChild != null && savedChild.getParentGenomeRevision() == 1 && savedChild.getParentGenomeSeed() == -416, "Ancestry did not persist");
            if (phase == 1) {
                observe(core, context(item, position), "current");
                equalFitness(item, ArtifactProfileCodec.decode(KEY, Files.readString(root.resolve("next.yml"))));
                manager.activateItem(BUYER, item);
            }
            var child = ecosystem.createEquipment(OWNER, EquipmentArchetype.ELYTRA, position, new Random(12 + phase));
            check(KEY.equals(child.getParentStorageKey()) && child.getParentGenomeRevision() == 1, "Restart/trade lost eligible parent");
        }
        manager.saveItem(item); ecosystem.save(); store.flushPendingWrites();
        Files.writeString(root.resolve("expected.yml"), ArtifactProfileCodec.encode(item));
        Files.writeString(root.resolve("phase"), Integer.toString(phase + 1));
        if (phase == 2) Files.writeString(root.resolve("result.json"), "{\"standalone_boot\":true,\"captured_delayed_revision\":true,\"unproven_revision_refused\":true,\"current_parent_revision\":true,\"next_penalty_preserved\":true,\"two_restarts\":true,\"trade\":true}");
        getLogger().info("REVISION_FITNESS_PHASE_" + (phase + 1) + "_PASS");
    }

    private AbilityEventContext context(Artifact item, WorldPosition position) {
        return new AbilityEventContext(definition().trigger(), item, item.getItemReputation(), 1, "controlled-api-inspect",
                AbilityRuntimeContext.intentional(AbilitySource.BLOCK_INSPECT), null, true).withPosition(position);
    }
    private AbilityDefinition definition() {
        var t = new AbilityRegistry().templateById("precision.material_insight");
        return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }
    private AbilityExecutionResult result(AbilityDefinition def, AbilityEventContext ctx, String key) {
        return AbilityExecutionResult.observed(def, ctx, AbilityOutcomeType.INFORMATION, 1, null, "controlled API result").withObservationKey(key);
    }
    private void observe(ObtuseLoot core, AbilityEventContext ctx, String key) {
        var def = definition(); core.getItemAbilityManager().observeDeferred(def, ctx, result(def, ctx, key));
    }
    private void equalFitness(Artifact item, Artifact expected) {
        check(item.getGenomeFitnessHistory().revisions().equals(expected.getGenomeFitnessHistory().revisions()), "Fitness revisions changed");
        for (var revision : expected.getGenomeFitnessHistory().revisions())
            check(item.getGenomeFitnessHistory().signals(revision).equals(expected.getGenomeFitnessHistory().signals(revision)), "Fitness changed for " + revision);
    }
    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
