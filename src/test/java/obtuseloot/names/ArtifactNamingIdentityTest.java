package obtuseloot.names;

import obtuseloot.config.RuntimeSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactNamingIdentityTest {
    private final RuntimeSettings.Snapshot settings = RuntimeSettings.get();

    @Test
    void personalityDerivesFromIdentitySignals() {
        ArtifactPersonalityResolver resolver = new ArtifactPersonalityResolver();
        ArtifactPersonalityProfile profile = resolver.resolve(ctx("brutality", "mythic", "warded", "none", "chaos", "artifact", 5), new Random(9L));
        assertNotNull(profile.dominant());
        assertNotNull(profile.secondary());
        assertTrue(profile.intensity() >= 0.45D);
    }

    @Test
    void voiceDerivesIndependentlyFromPersonality() {
        ArtifactVoiceResolver resolver = new ArtifactVoiceResolver();
        ArtifactVoiceProfile voice = resolver.resolve(ctx("mobility", "base", "dormant", "none", "stable", "artifact", 0),
                new ArtifactPersonalityProfile(ArtifactPersonalityTrait.PREDATORY, ArtifactPersonalityTrait.PLAYFUL, 0.6D),
                new Random(2L));
        assertNotNull(voice.primary());
        assertNotNull(voice.secondary());
    }

    @Test
    void implicationScoreIncreasesForFittingIdentity() {
        ArtifactImplicationScorer scorer = new ArtifactImplicationScorer();
        ArtifactNameContext fitting = ctx("brutality", "mythic", "awakened", "fused", "chaos", "artifact", 7);
        ArtifactNameContext plain = ctx("precision", "base", "dormant", "none", "stable", "generic", 0);
        ArtifactPersonalityProfile intimate = new ArtifactPersonalityProfile(ArtifactPersonalityTrait.INTIMATE, ArtifactPersonalityTrait.PREDATORY, 0.8D);
        ArtifactVoiceProfile whisper = new ArtifactVoiceProfile(ArtifactVoiceRegister.WHISPERING, ArtifactVoiceRegister.COURTLY, false, 0.8D);
        ArtifactPersonalityProfile plainProfile = new ArtifactPersonalityProfile(ArtifactPersonalityTrait.COLD, ArtifactPersonalityTrait.PATIENT, 0.4D);
        ArtifactVoiceProfile martial = new ArtifactVoiceProfile(ArtifactVoiceRegister.MARTIAL, ArtifactVoiceRegister.SEVERE, false, 0.2D);

        assertTrue(scorer.score(fitting, intimate, whisper, settings) > scorer.score(plain, plainProfile, martial, settings));
    }

    @Test
    void loadedNamesStayNonCrass() {
        ArtifactGeneratedIdentity identity = ArtifactNameGenerator.generateIdentity(
                ctx("brutality", "mythic", "awakened", "fused", "chaos", "artifact", 8), new Random(4L), settings);
        String out = (identity.displayName() + " " + identity.trueName() + " " + identity.epithet()).toLowerCase();
        for (String forbidden : List.of("fuck", "shit", "cunt", "pussy", "penis", "vagina")) {
            assertFalse(out.contains(forbidden));
        }
    }

    @Test
    void safetyFilterBlocksBluntOrMemeLikePatterns() {
        ArtifactNameSafetyFilter filter = new ArtifactNameSafetyFilter();
        assertFalse(filter.isSafe("Velvet Mouth Hunger Caress", 0.9D));
        assertFalse(filter.isSafe("lol 69 relic", 0.1D));
        assertTrue(filter.isSafe("Mercy of Cinders", 0.5D));
    }

    @Test
    void trueNameIsStableForSameSeedAndIdentity() {
        ArtifactNameContext context = ctx("survival", "mythic", "warded", "none", "stable", "artifact", 4);
        ArtifactGeneratedIdentity first = ArtifactNameGenerator.generateIdentity(context, new Random(77L), settings);
        ArtifactGeneratedIdentity second = ArtifactNameGenerator.generateIdentity(context, new Random(77L), settings);
        assertEquals(first.trueName(), second.trueName());
    }

    @Test
    void suggestiveOutputAppearsOnlyWhenSupported() {
        ArtifactGeneratedIdentity rich = ArtifactNameGenerator.generateIdentity(ctx("brutality", "mythic", "awakened", "fused", "chaos", "artifact", 10), new Random(15L), settings);
        ArtifactGeneratedIdentity plain = ArtifactNameGenerator.generateIdentity(ctx("precision", "base", "dormant", "none", "stable", "generic", 0), new Random(16L), settings);
        assertTrue(rich.implicationScore() >= plain.implicationScore());
    }

    @Test
    void supportAndRitualArchetypesStillGetStrongVoicefulNames() {
        ArtifactGeneratedIdentity support = ArtifactNameGenerator.generateIdentity(ctx("survival", "tempered", "warded", "none", "stable", "artifact", 5), new Random(21L), settings);
        assertNotNull(support.discoveryLine());
        assertTrue(support.displayName().length() >= 4);
    }

    @Test
    void cadenceHeuristicsPrefersBalancedNames() {
        ArtifactCadenceHeuristics heuristics = new ArtifactCadenceHeuristics();
        ArtifactVoiceProfile soft = new ArtifactVoiceProfile(ArtifactVoiceRegister.WHISPERING, ArtifactVoiceRegister.ELEGIAC, false, 0.85D);
        assertTrue(heuristics.score("Velvet Mercy", soft) > heuristics.score("Krkptx Grd", soft));
    }

    @Test
    void goldenExamplesCoverCoreTonalProfiles() {
        List<ArtifactGeneratedIdentity> examples = List.of(
                ArtifactNameGenerator.generateIdentity(ctx("consistency", "tempered", "warded", "none", "stable", "artifact", 6), new Random(31L), settings),
                ArtifactNameGenerator.generateIdentity(ctx("survival", "mythic", "awakened", "none", "stable", "artifact", 9), new Random(32L), settings),
                ArtifactNameGenerator.generateIdentity(ctx("brutality", "mythic", "awakened", "fused", "chaos", "artifact", 12), new Random(33L), settings),
                ArtifactNameGenerator.generateIdentity(ctx("precision", "tempered", "warded", "none", "stable", "artifact", 11), new Random(34L), settings),
                ArtifactNameGenerator.generateIdentity(ctx("survival", "mythic", "awakened", "fused", "stable", "artifact", 13), new Random(35L), settings),
                ArtifactNameGenerator.generateIdentity(ctx("chaos", "mythic", "awakened", "fused", "chaos", "artifact", 14), new Random(36L), settings)
        );
        assertEquals(6, examples.size());
        examples.forEach(example -> assertTrue(new ArtifactNameSafetyFilter().isSafe(example.displayName(), example.implicationScore())));
    }

    private static ArtifactNameContext ctx(String archetype, String evolution, String awakening, String fusion, String drift, String category, int events) {
        return new ArtifactNameContext(123L, archetype, evolution, awakening, fusion, drift, category,
                events <= 0 ? List.of() : java.util.Collections.nCopies(events, "event"),
                events <= 0 ? List.of() : java.util.Collections.nCopies(events, "lore"));
    }
}
