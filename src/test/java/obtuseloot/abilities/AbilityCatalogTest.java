package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AbilityCatalogTest {
    @Test
    void livePoolContainsOnlyImplementedEntriesAndRejectsUnsupportedMutations() {
        var catalog = new AbilityRegistry().implementedOnly();
        assertFalse(catalog.templates().isEmpty());
        assertTrue(catalog.registrations().stream().allMatch(AbilityRegistration::implemented));
        var t = catalog.templates().getFirst();
        var original = definition(t);
        var variant = new AbilityDefinition(original.id(), original.name(), original.family(), AbilityTrigger.ON_MEMORY_EVENT,
                AbilityMechanic.PULSE, "", "", "", "", "", "", List.of(), List.of(), original.metadata(), "1", "2", "3", "4", "5");
        assertSame(original, catalog.executableVariant(variant, original));
        assertSame(original, catalog.executableVariant(original, original));
        assertTrue(new AbilityRegistry().templates().size() > catalog.templates().size(), "Offline descriptions remain available");
    }

    @Test
    void realLiveResolverAlwaysBuildsExecutableProfiles() {
        var registry = new AbilityRegistry().implementedOnly();
        var lineage = new obtuseloot.lineage.LineageRegistry();
        var resolver = new SeededAbilityResolver(registry, new obtuseloot.memory.ArtifactMemoryEngine(),
                new obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine(), lineage,
                new obtuseloot.lineage.LineageInfluenceResolver(), null);
        var artifact = artifact();
        artifact.setTotalDrifts(3);
        var profile = resolver.resolve(artifact, new ArtifactReputation());
        assertFalse(profile.abilities().isEmpty());
        for (var definition : profile.abilities()) {
            assertTrue(registry.registrationById(definition.id()).supports(definition), definition.id());
        }
    }

    @Test
    void handlersDoNotChangeTemplateEqualityOrHashesAcrossCatalogs() {
        var first = new AbilityRegistry();
        var second = new AbilityRegistry();
        assertEquals(first.templates(), second.templates());
        assertEquals(first.templates().hashCode(), second.templates().hashCode());
        assertThrows(UnsupportedOperationException.class, () -> first.registrations().clear());
    }

    @Test
    void oneRegistrationAddsSelectionMetadataAndLiveExecution() {
        var template = template();
        var catalog = new AbilityRegistry(List.of(new AbilityRegistration(template,
                (definition, context, stage) -> AbilityExecutionResult.observed(definition, context,
                        AbilityOutcomeType.INFORMATION, 1, null, "custom report"))));
        var result = new AbilityExecutor(catalog).execute(definition(template), context(template), 1);
        assertEquals(List.of(template), catalog.templates());
        assertEquals(AbilityExecutionStatus.SUCCESS, result.status());
        assertEquals("custom report", result.outputText());
    }

    @Test
    void removingRegistrationRemovesMetadataAndPreventsStaleDefinitionExecution() {
        var removed = new AbilityRegistry(List.of());
        var template = template();
        assertTrue(removed.templates().isEmpty());
        assertNull(removed.registrationById(template.id()));
        var result = new AbilityExecutor(removed).execute(definition(template), context(template), 1);
        assertEquals(AbilityExecutionStatus.NO_OP, result.status());
        assertNull(result.observedAmount());
        var profile = new ProceduralAbilityGenerator(removed).generate(artifact(), 1,
                new ArtifactMemoryProfile(0, 0, 0, 0, 0, 0, 0, 0));
        assertTrue(profile.abilities().isEmpty());
    }

    @Test
    void duplicateIdsFailAtCatalogConstruction() {
        var registration = new AbilityRegistration(template(), null);
        assertThrows(IllegalArgumentException.class, () -> new AbilityRegistry(List.of(registration, registration)));
    }

    private AbilityTemplate template() {
        var t = new AbilityRegistry().templateById("precision.material_insight");
        return new AbilityTemplate("test.custom_ability", t.name(), t.category(), t.family(), t.trigger(), t.mechanic(),
                t.effectPattern(), t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.convergenceVariant(),
                t.memoryVariant(), t.supportModifiers(), t.metadata());
    }

    private AbilityDefinition definition(AbilityTemplate t) {
        return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), t.effectPattern(),
                t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.convergenceVariant(), t.memoryVariant(),
                t.supportModifiers(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }

    private AbilityEventContext context(AbilityTemplate t) {
        return new AbilityEventContext(t.trigger(), artifact(), new ArtifactReputation(), 1, "test",
                AbilityRuntimeContext.intentional(AbilitySource.OTHER), null, true);
    }

    private Artifact artifact() {
        var artifact = new Artifact(new UUID(0, 1), "wooden_sword");
        artifact.setArtifactSeed(1);
        return artifact;
    }
}
