package obtuseloot.text;

import obtuseloot.artifacts.Artifact;
import obtuseloot.names.ArtifactNameResolver;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactTextResolverTest {
    private final ArtifactTextResolver resolver = new ArtifactTextResolver();

    @Test
    void textIdentityResolvesAcrossChannelsWithLengthBounds() {
        Artifact artifact = seeded(99L, 0.9, 0.8, 0.2, 0.4, 0.8, 0.3);
        artifact.setDriftLevel(2);
        artifact.setDriftAlignment("volatile");
        artifact.addLoreHistory("forge.echo");
        Map<ArtifactTextChannel, String> values = renderAll(artifact);

        assertFalse(values.get(ArtifactTextChannel.NAME).isBlank());
        assertTrue(wordCount(values.get(ArtifactTextChannel.NAME)) <= 4);
        assertTrue(wordCount(values.get(ArtifactTextChannel.DRIFT)) <= 10);
        assertTrue(wordCount(values.get(ArtifactTextChannel.LORE)) <= 14);
        assertTrue(values.values().stream().allMatch(v -> !v.toLowerCase().contains("fuck")));
        assertTrue(values.values().stream().allMatch(this::hasAtMostTwoSentences));
    }

    @Test
    void motifContinuityAppearsAcrossNameLoreAwakeningAndMemory() {
        Artifact artifact = seeded(77L, 0.2, 0.2, 0.2, 0.9, 0.9, 0.3);
        artifact.setAwakeningPath("Voidwake Covenant");
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));

        String name = resolver.compose(artifact, ArtifactTextChannel.NAME, artifact.getNaming().getRootForm()).toLowerCase();
        String lore = resolver.compose(artifact, ArtifactTextChannel.LORE, "").toLowerCase();
        String awaken = resolver.compose(artifact, ArtifactTextChannel.AWAKENING, artifact.getAwakeningPath()).toLowerCase();
        String memory = resolver.compose(artifact, ArtifactTextChannel.MEMORY, "boss_kill").toLowerCase();

        assertTrue(sharedMotif(name, lore, awaken, memory));
    }

    @Test
    void contrastingPersonalitiesProduceDifferentiatedSnapshots() {
        Artifact liturgical = seeded(1L, 0.1, 0.1, 0.9, 0.2, 0.4, 0.9);
        liturgical.setAwakeningPath("Crown of Equilibrium");
        liturgical.setNaming(ArtifactNameResolver.initialize(liturgical));

        Artifact predatory = seeded(2L, 0.2, 0.95, 0.1, 0.3, 0.5, 0.2);
        predatory.setAwakeningPath("Executioner's Oath");
        predatory.setNaming(ArtifactNameResolver.initialize(predatory));

        Artifact intimate = seeded(3L, 0.3, 0.3, 0.4, 0.2, 0.8, 0.4);
        intimate.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
        intimate.setNaming(ArtifactNameResolver.initialize(intimate));

        assertNotEquals(resolver.identityFor(liturgical).personality(),
                resolver.identityFor(predatory).personality());
        assertNotEquals(resolver.identityFor(intimate).voice(),
                resolver.identityFor(predatory).voice());
        assertNotEquals(resolver.compose(intimate, ArtifactTextChannel.MEMORY, "close_call"),
                resolver.compose(predatory, ArtifactTextChannel.MEMORY, "close_call"));
    }

    @Test
    void signalDrivenInnuendoRemainsImplicitAndDeterministic() {
        Artifact artifact = seeded(404L, 0.6, 0.91, 0.2, 0.82, 0.3, 0.7);
        artifact.setConvergencePath("braided lineage");
        artifact.setAwakeningPath("unsealed vow");
        artifact.setDriftLevel(3);
        artifact.setDriftAlignment("slipping");
        artifact.addLoreHistory("old.impact");
        artifact.getMemory().record(obtuseloot.memory.ArtifactMemoryEvent.FIRST_BOSS_KILL);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));

        String firstLore = resolver.compose(artifact, ArtifactTextChannel.LORE, "");
        String secondLore = resolver.compose(artifact, ArtifactTextChannel.LORE, "");
        String name = resolver.compose(artifact, ArtifactTextChannel.NAME, artifact.getNaming().getRootForm()).toLowerCase();

        assertEquals(firstLore, secondLore);
        assertTrue(firstLore.contains(" "));
        assertTrue(containsAny(firstLore.toLowerCase(), "hunger", "nearer", "presses", "slips", "closeness", "linger"));
        assertFalse(containsAny(firstLore.toLowerCase(), "sex", "sexy", "nude", "kiss", "moan"));
        assertTrue(wordCount(name) <= 4);
    }

    @Test
    void safetyFilterBlocksCrudeLexemes() {
        ArtifactExpressionSafetyFilter filter = new ArtifactExpressionSafetyFilter();
        assertFalse(filter.sanitize("a sexy meme lol").contains("sexy"));
        assertFalse(filter.sanitize("a sexy meme lol").contains("lol"));
    }

    private Map<ArtifactTextChannel, String> renderAll(Artifact artifact) {
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        Map<ArtifactTextChannel, String> values = new EnumMap<>(ArtifactTextChannel.class);
        for (ArtifactTextChannel channel : ArtifactTextChannel.values()) {
            values.put(channel, resolver.compose(artifact, channel, "convergence"));
        }
        return values;
    }

    private Artifact seeded(long seed, double precision, double brutality, double survival,
                            double mobility, double chaos, double consistency) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(precision);
        artifact.setSeedBrutalityAffinity(brutality);
        artifact.setSeedSurvivalAffinity(survival);
        artifact.setSeedMobilityAffinity(mobility);
        artifact.setSeedChaosAffinity(chaos);
        artifact.setSeedConsistencyAffinity(consistency);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact;
    }

    private boolean sharedMotif(String... channels) {
        String[] motifs = {"echo", "vigil", "claim", "devotion", "patience", "fracture", "afterimage", "release", "inheritance"};
        for (String motif : motifs) {
            int hits = 0;
            for (String channel : channels) {
                if (channel.contains(motif)) hits++;
            }
            if (hits >= 2) return true;
        }
        return false;
    }

    private boolean hasAtMostTwoSentences(String text) {
        long sentences = text.chars().filter(ch -> ch == '.').count();
        return sentences <= 2;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private int wordCount(String text) {
        return text.trim().split("\\s+").length;
    }
}
