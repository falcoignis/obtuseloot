package obtuseloot.abilities;

import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;

/** Seeded probes must not inherit the population history of unrelated tests. */
final class ProbeFixtures {
    private ProbeFixtures() {}

    static ProceduralAbilityGenerator generator(AbilityRegistry registry) {
        return generator(registry, null, null);
    }

    static ProceduralAbilityGenerator generator(AbilityRegistry registry, LineageRegistry lineages,
                                                 LineageInfluenceResolver influence) {
        return new ProceduralAbilityGenerator(registry, null, lineages, influence, null,
                true, ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex());
    }
}
