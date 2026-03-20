package obtuseloot.significance;

import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.MechanicUtilitySignal;
import obtuseloot.evolution.UtilityHistoryRollup;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactSignificanceResolverTest {
    private final ArtifactSignificanceResolver resolver = new ArtifactSignificanceResolver();

    @Test
    void significanceProfileSeparatesLineageStateDistinctnessAndAge() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_sword");
        artifact.setLatentLineage("lineage-ember");
        artifact.setSpeciesId("ember_guard_species");
        artifact.setAwakeningLineageTrace("lineage:ember-vow:pressure=6");
        artifact.setAwakeningExpressionTrace("ember arc");
        artifact.setAwakeningPath("ember");
        artifact.getAwakeningTraits().add("flare");
        artifact.getAwakeningTraits().add("guard");
        artifact.getNotableEvents().add("Held the bridge");
        artifact.setPersistenceOriginTimestamp(System.currentTimeMillis() - (3L * 24L * 60L * 60L * 1000L));
        artifact.setIdentityBirthTimestamp(System.currentTimeMillis() - (3L * 24L * 60L * 60L * 1000L));

        ArtifactSignificanceProfile profile = resolver.resolve(artifact);

        assertTrue(profile.lineage().contains("Ember"));
        assertTrue(profile.lineage().endsWith(" line"));
        assertEquals("Ember Arc close blade", profile.functionalIdentity());
        assertEquals("awakened", profile.state());
        assertEquals("open field trait-marked", profile.distinctness());
        assertEquals("carried 3d", profile.age());
        assertTrue(profile.format().contains("— awakened, open field trait-marked, carried 3d"));
    }

    @Test
    void distinctnessUsesBehavioralFitInsteadOfSignalCountLadder() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "elytra");
        artifact.setLatentLineage("skyline");
        artifact.setLastUtilityHistory(new UtilityHistoryRollup(
                0.72D,
                0.41D,
                0.78D,
                0.08D,
                0.64D,
                9L,
                Map.of(
                        "ECOLOGICAL_PATHING@ON_CHUNK_ENTER", signal("ECOLOGICAL_PATHING@ON_CHUNK_ENTER", 0.62D, 0.48D, 9L, 7L),
                        "SENSE_PING@ON_WORLD_SCAN", signal("SENSE_PING@ON_WORLD_SCAN", 0.51D, 0.35D, 7L, 4L),
                        "GLIDE_VECTOR@ON_JUMP", signal("GLIDE_VECTOR@ON_JUMP", 0.57D, 0.31D, 8L, 4L),
                        "DASH_BURST@ON_SPRINT", signal("DASH_BURST@ON_SPRINT", 0.46D, 0.28D, 6L, 3L)
                )).encode());

        ArtifactSignificanceProfile profile = resolver.resolve(artifact);

        assertEquals("open field broad fit", profile.distinctness());
        assertFalse(profile.distinctness().contains("signal"));
        assertFalse(profile.distinctness().matches(".*\\b[1-9]\\b.*"));
    }

    @Test
    void ageReflectsIdentityReplacementWhilePreservingContinuityAge() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_axe");
        long now = System.currentTimeMillis();
        artifact.setPersistenceOriginTimestamp(now - (5L * 24L * 60L * 60L * 1000L));
        artifact.setIdentityBirthTimestamp(now - 10_000L);
        artifact.setConvergencePath("braid");
        artifact.setConvergenceVariantId("braid-1");
        artifact.setConvergenceIdentityShape("woven cleaver");
        artifact.setConvergenceExpressionTrace("woven breach");

        ArtifactSignificanceProfile profile = resolver.resolve(artifact);

        assertEquals("converged", profile.state());
        assertEquals("newly shaped, carried 5d", profile.age());
        assertEquals("open field unproven fit", profile.distinctness());
    }

    @Test
    void distinctnessAndAgeStayStableAcrossPersistenceReloadData() {
        Artifact artifact = new Artifact(UUID.randomUUID(), "diamond_boots");
        long now = System.currentTimeMillis();
        artifact.setPersistenceOriginTimestamp(now - (26L * 60L * 60L * 1000L));
        artifact.setIdentityBirthTimestamp(now - (26L * 60L * 60L * 1000L));
        artifact.getNotableEvents().add("Crossed the salt flats");
        artifact.getNotableEvents().add("Returned to camp");
        artifact.setSpeciesId("sand_strider_species");

        ArtifactSignificanceProfile profile = resolver.resolve(artifact);

        assertEquals("open field story-marked", profile.distinctness());
        assertEquals("carried 26h", profile.age());
    }

    private static MechanicUtilitySignal signal(String key, double validatedUtility, double utilityDensity, long attempts, long meaningful) {
        return new MechanicUtilitySignal(key, validatedUtility, utilityDensity, 0.5D, 0.1D, 0.05D, 0.02D, attempts, meaningful, 0.5D);
    }
}
