package obtuseloot.ecology;

import obtuseloot.abilities.AbilityOutcomeObservation;
import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeMutationEngine;
import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.persistence.PlayerStateStore;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/** Reproduction changes future item ancestry; it never removes an owned item. */
public final class LocalEcosystem {
    private final PlayerStateStore store;
    private final ArtifactManager artifacts;
    private final LocalInheritancePool pool;
    private final GenomeMutationEngine mutation = new GenomeMutationEngine();
    public LocalEcosystem(PlayerStateStore store, ArtifactManager artifacts) {
        this(store, artifacts, LocalInheritancePool.decode(store.loadEcologyState()));
    }
    public LocalEcosystem(PlayerStateStore store, ArtifactManager artifacts, LocalInheritancePool pool) {
        this.store = Objects.requireNonNull(store); this.artifacts = Objects.requireNonNull(artifacts);
        this.pool = Objects.requireNonNull(pool);
    }
    public LocalInheritancePool pool() { return pool; }
    public void advanceTick() { pool.advanceTick(RuntimeSettings.get().inheritanceOutcomeWindowTicks()); }
    public void observe(AbilityOutcomeObservation observation) {
        pool.record(observation, RuntimeSettings.get().inheritanceOutcomeWindowTicks());
    }
    public void save() { store.saveEcologyState(pool.encode()); }
    public Artifact createEquipment(UUID initialHolder, EquipmentArchetype archetype, WorldPosition origin) {
        return createEquipment(initialHolder, archetype, origin, ThreadLocalRandom.current());
    }
    public Artifact createEquipment(UUID initialHolder, EquipmentArchetype archetype, WorldPosition origin, RandomGenerator random) {
        Objects.requireNonNull(origin); Objects.requireNonNull(random);
        var settings = RuntimeSettings.get();
        Artifact parent = null;
        while (true) {
            var selected = pool.choose(origin, settings.inheritanceRadiusChunks(), settings.inheritanceOutcomeWindowTicks(), random);
            if (selected == null) break;
            Artifact candidate = artifacts.resolveByStorageKey(selected.storageKey());
            if (candidate == null) throw new IllegalStateException("Missing reproductive parent profile " + selected.storageKey());
            if (candidate.getArtifactSeed() != selected.seed()) {
                pool.forget(selected.storageKey()); // an explicit identity replacement cannot reuse old fitness
                continue;
            }
            parent = candidate;
            break;
        }
        long seed;
        do { seed = random.nextLong(); } while (seed == 0);
        Artifact child = new Artifact(initialHolder, Objects.requireNonNull(archetype));
        child.setArtifactStorageKey(ArtifactStorageKeys.create());
        new ArtifactSeedFactory().applySeedProfile(child, seed);
        if (parent == null) {
            child.setLatentLineage("lineage-" + ArtifactStorageKeys.itemId(child.getArtifactStorageKey()));
            child.getGenome();
        } else {
            var inherited = new ArtifactGenome(seed, parent.getGenome().traits(), parent.getGenome().latentTraits(), parent.getGenome().activatedLatentTraits());
            // Ordinary small mutation uses the existing minimum active mutation stage, not an extra wild-mutation roll.
            child.setGenome(mutation.mutate(inherited, 2));
            child.setLatentLineage(parent.getLatentLineage());
            child.setParentStorageKey(parent.getArtifactStorageKey());
            child.setInheritanceGeneration(Math.addExact(parent.getInheritanceGeneration(), 1));
            child.setSeedPrecisionAffinity(parent.getSeedPrecisionAffinity());
            child.setSeedBrutalityAffinity(parent.getSeedBrutalityAffinity());
            child.setSeedSurvivalAffinity(parent.getSeedSurvivalAffinity());
            child.setSeedMobilityAffinity(parent.getSeedMobilityAffinity());
            child.setSeedChaosAffinity(parent.getSeedChaosAffinity());
            child.setSeedConsistencyAffinity(parent.getSeedConsistencyAffinity());
            child.addLoreHistory("Descended from " + parent.getDisplayName());
        }
        child.setNaming(ArtifactNameResolver.initialize(child));
        artifacts.registerItem(child); // do not expose a stamped item until its profile is durable
        return child;
    }
}
