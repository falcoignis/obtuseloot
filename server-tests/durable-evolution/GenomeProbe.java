package obtuseloot.servertest;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.*;
import obtuseloot.persistence.ArtifactProfileCodec;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.UUID;

/** Creates synthetic profiles in an empty disposable server and stops it after each phase. */
public final class GenomeProbe extends JavaPlugin {
    private static final String KEY = "artifact:00000000-0000-0000-0000-000000000415";
    private static final UUID OWNER = new UUID(0, 415), BUYER = new UUID(0, 416);

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
            check(!getServer().getPluginManager().isPluginEnabled(optional), "Unexpected optional dependency " + optional);
        Path root = getDataFolder().toPath();
        Files.createDirectories(root);
        var manager = core.getArtifactManager();
        var store = core.getPlayerStateStore();
        int phase = Files.exists(root.resolve("phase")) ? Integer.parseInt(Files.readString(root.resolve("phase"))) : 0;
        Artifact item;
        if (phase == 0) {
            check(store.loadArtifactProfile(KEY) == null, "Probe needs an empty server");
            item = new Artifact(OWNER, "elytra");
            item.setArtifactStorageKey(KEY);
            new ArtifactSeedFactory().applySeedProfile(item, -415L);
            manager.registerItem(item);
            manager.activateItem(OWNER, item);
            item.getItemReputation().setMobility(obtuseloot.config.RuntimeSettings.get().temperedThreshold());
            core.getEvolutionEngine().evaluate(item, item.getItemReputation());
            check(item.getGenomeRevision() == 1, "Earned evolution must commit once");
            assertReadStable(core, item);
            core.getDriftEngine().applyDriftSimulation(item, item.getItemReputation());
            check(item.getGenomeRevision() == 2, "Drift must commit once");
            item.setArchetypePath("strider");
            var transition = core.getAwakeningEngine().forceAwakening(null, item, item.getItemReputation());
            check(transition != null, "Forced awakening fixture must qualify");
            var evolved = transition.replacement().getGenome();
            item = manager.replaceIdentity(OWNER, transition);
            check(item.getGenome() == evolved && item.getGenomeRevision() == 3, "Replacement must retain committed genes");
            var carried = item.getGenome();
            item = manager.reseed(OWNER, -1415L);
            check(item.getGenome().traits().equals(carried.traits()), "Reseeding must retain the physical item's genes");
            assertReadStable(core, item);
            core.getEvolutionEngine().evaluate(item, item.getItemReputation());
            check(item.getGenomeRevision() == 4, "Re-evolution after reseeding must commit once");
            carried = item.getGenome();
            item = manager.recreate(OWNER);
            check(item.getGenome().traits().equals(carried.traits()), "Recreation must retain the physical item's genes");
            assertReadStable(core, item);
            core.getEvolutionEngine().evaluate(item, item.getItemReputation());
            check(item.getGenomeRevision() == 5, "Re-evolution after recreation must commit once");
        } else {
            item = store.loadArtifactProfile(KEY);
            check(item != null, "Saved profile must survive server restart");
            var expected = ArtifactProfileCodec.decode(KEY, Files.readString(root.resolve("expected.yml")));
            check(item.getGenome().traits().equals(expected.getGenome().traits()), "Expressed genes changed on restart");
            check(item.getGenome().latentTraits().equals(expected.getGenome().latentTraits()), "Latent genes changed on restart");
            check(item.getGenome().activatedLatentTraits().equals(expected.getGenome().activatedLatentTraits()), "Activation flags changed on restart");
            check(item.getGenomeRevision() == 5 && item.getGenomeEvolutionState().equals(expected.getGenomeEvolutionState()), "Transition receipt changed on restart");
            check(item.getOwnerId().equals(phase == 1 ? OWNER : BUYER), "Trade ownership did not persist");
            manager.activateItem(BUYER, item);
        }
        assertReadStable(core, item);
        manager.saveItem(item);
        store.flushPendingWrites();
        Files.writeString(root.resolve("expected.yml"), ArtifactProfileCodec.encode(item));
        Files.writeString(root.resolve("phase"), Integer.toString(phase + 1));
        if (phase == 2) Files.writeString(root.resolve("result.json"), "{\"standalone_boot\":true,\"evolution_drift_awakening\":true,\"baseline_resets\":true,\"two_restarts\":true,\"trade\":true,\"read_stability\":true,\"revision\":5}");
        getLogger().info("DURABLE_GENOME_PHASE_" + (phase + 1) + "_PASS");
    }

    private void assertReadStable(ObtuseLoot core, Artifact item) {
        var genes = item.getGenome();
        long revision = item.getGenomeRevision();
        for (int i = 0; i < 10; i++) {
            core.getItemAbilityManager().profileFor(item, item.getItemReputation());
            core.getItemAbilityManager().rebuildSubscriptions(item.getOwnerId(), item, item.getItemReputation(), "server-probe-refresh");
            check(item.getGenome() == genes && item.getGenomeRevision() == revision, "Profile refresh changed genes");
        }
    }

    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
