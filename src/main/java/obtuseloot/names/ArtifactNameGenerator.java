package obtuseloot.names;

import obtuseloot.artifacts.Artifact;
import obtuseloot.config.RuntimeSettings;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Identity-driven naming generator with personality, voice and implication controls.
 */
public final class ArtifactNameGenerator {
    private static final List<String> SOFT = List.of("Hush", "Velvet", "Tender", "Close", "Veil", "Bloom", "Mercy", "Murmur", "Favor", "Devotion", "Vow", "Linger", "Secret", "Cradle", "Keep");
    private static final List<String> PREDATORY = List.of("Hunger", "Tooth", "Thorn", "Mouth", "Hollow", "Claim", "Bind", "Taking", "Dusk", "Swallow", "Waiting", "Ache", "Mark", "Wake");
    private static final List<String> RITUAL = List.of("Sacrament", "Reliquary", "Vesper", "Vigil", "Anoint", "Cinder", "Choir", "Chapel", "Litany", "Witness", "Oath", "Altar", "Pale");
    private static final List<String> MELANCHOLY = List.of("Missing", "Absence", "After", "Remains", "Echo", "Kept", "Promise", "Grief", "Borrowed", "Stayed", "Return", "Unsaid", "Near", "Almost");

    private static final ArtifactPersonalityResolver PERSONALITY_RESOLVER = new ArtifactPersonalityResolver();
    private static final ArtifactVoiceResolver VOICE_RESOLVER = new ArtifactVoiceResolver();
    private static final ArtifactImplicationScorer IMPLICATION_SCORER = new ArtifactImplicationScorer();
    private static final ArtifactNameToneValidator TONE_VALIDATOR = new ArtifactNameToneValidator();

    private ArtifactNameGenerator() {
    }

    public static String generate(UUID ownerId) {
        RuntimeSettings.Snapshot settings = RuntimeSettings.get();
        Random random = settings.namingUseDeterministicOwnerSeed()
                ? new Random(Math.abs(ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits()))
                : ThreadLocalRandom.current();
        return generateWithRandom(random, settings);
    }

    public static String generateFromSeed(long artifactSeed) {
        ArtifactGeneratedIdentity identity = generateIdentity(ArtifactNameContext.minimal(artifactSeed), new Random(artifactSeed ^ 0x9E3779B97F4A7C15L), RuntimeSettings.get());
        return identity.displayName();
    }

    public static String generateForArtifact(Artifact artifact) {
        return generateIdentityForArtifact(artifact).displayName();
    }

    public static ArtifactGeneratedIdentity generateIdentityForArtifact(Artifact artifact) {
        ArtifactNameContext context = new ArtifactNameContext(
                artifact.getArtifactSeed(),
                artifact.getArchetypePath(),
                artifact.getEvolutionPath(),
                artifact.getAwakeningPath(),
                artifact.getFusionPath(),
                artifact.getDriftAlignment(),
                artifact.getItemCategory(),
                List.copyOf(artifact.getNotableEvents()),
                List.copyOf(artifact.getLoreHistory())
        );
        return generateIdentity(context, new Random(artifact.getArtifactSeed() ^ 0x9E3779B97F4A7C15L), RuntimeSettings.get());
    }

    static ArtifactGeneratedIdentity generateIdentity(ArtifactNameContext context, Random random, RuntimeSettings.Snapshot settings) {
        ArtifactPersonalityProfile personality = PERSONALITY_RESOLVER.resolve(context, random);
        ArtifactVoiceProfile voice = VOICE_RESOLVER.resolve(context, personality, random);
        double implication = IMPLICATION_SCORER.score(context, personality, voice, settings);

        String displayName = buildName(random, settings, voice, personality, implication);
        if (settings.namingEnableToneSafetyFilter() || settings.namingEnableCadencePreference()) {
            for (int i = 0; i < 24 && !TONE_VALIDATOR.accept(displayName, voice, implication, settings.namingEnableCadencePreference(), settings.namingEnableToneSafetyFilter()); i++) {
                displayName = buildName(random, settings, voice, personality, implication * 0.9D);
            }
        }

        String trueName = buildTrueName(displayName, voice, personality);
        String epithet = buildEpithet(personality, voice, implication);
        String discovery = "Discovered as " + displayName + "; it answers in a "
                + personality.dominant().name().toLowerCase().replace('_', '-') + " temperament.";
        String awakeningReveal = "When stirred, " + displayName + " becomes " + trueName + ", " + epithet + ".";
        return new ArtifactGeneratedIdentity(displayName, trueName, epithet, discovery, awakeningReveal, implication, personality, voice);
    }

    private static String generateWithRandom(Random random, RuntimeSettings.Snapshot settings) {
        List<String> prefixes = NamePoolManager.prefixes();
        List<String> suffixes = NamePoolManager.suffixes();
        List<String> generic = NamePoolManager.generic();

        if (prefixes.isEmpty() || suffixes.isEmpty()) {
            if (!generic.isEmpty()) {
                return generic.get(random.nextInt(generic.size()));
            }
            return "Nameless Artifact";
        }

        int prefixSuffixChancePercent = Math.max(0, Math.min(100, settings.namingPrefixSuffixChancePercent()));
        if (random.nextInt(100) < prefixSuffixChancePercent || generic.isEmpty()) {
            return prefixes.get(random.nextInt(prefixes.size())) + " " + suffixes.get(random.nextInt(suffixes.size()));
        }

        return generic.get(random.nextInt(generic.size()));
    }

    private static String buildName(Random random, RuntimeSettings.Snapshot settings, ArtifactVoiceProfile voice,
                                    ArtifactPersonalityProfile personality, double implication) {
        List<String> prefixes = NamePoolManager.prefixes();
        List<String> suffixes = NamePoolManager.suffixes();
        List<String> generic = NamePoolManager.generic();

        boolean loaded = implication >= settings.namingImplicationThreshold() && random.nextDouble() < implication;
        if (!loaded && !generic.isEmpty() && random.nextInt(100) >= settings.namingPrefixSuffixChancePercent()) {
            return generic.get(random.nextInt(generic.size()));
        }

        if (loaded) {
            String a = pickToneLexeme(random, personality, voice, true);
            String b = pickToneLexeme(random, personality, voice, false);
            return formatByVoice(random, voice, a, b);
        }

        if (!prefixes.isEmpty() && !suffixes.isEmpty()) {
            return prefixes.get(random.nextInt(prefixes.size())) + " " + suffixes.get(random.nextInt(suffixes.size()));
        }
        return generic.isEmpty() ? "Nameless Artifact" : generic.get(random.nextInt(generic.size()));
    }

    private static String buildTrueName(String displayName, ArtifactVoiceProfile voice, ArtifactPersonalityProfile personality) {
        return switch (voice.primary()) {
            case LITURGICAL, RITUAL, ARCHAIC -> displayName + ", Reliquary of " + titleCase(personality.dominant());
            case COURTLY, CRUELLY_POLITE -> displayName + ", Keeper of Close Things";
            case WHISPERING, VELVETY, INTIMATE -> displayName + ", What Lingers After";
            case MOCKING_CEREMONIAL -> displayName + ", Witness to Better Intentions";
            default -> displayName + ", the " + titleCase(personality.secondary());
        };
    }

    private static String buildEpithet(ArtifactPersonalityProfile personality, ArtifactVoiceProfile voice, double implication) {
        String style = switch (personality.dominant()) {
            case SOLEMN, REVERENT -> "of the Last Mercy";
            case PREDATORY, HUNGRY -> implication > 0.5D ? "the Kindly Claim" : "the Patient Fang";
            case INTIMATE -> "of Soft Devotion";
            case MOCKING -> "of Polite Ruin";
            case MOURNFUL -> "Kept After Dusk";
            default -> "in " + voice.primary().name().toLowerCase().replace('_', ' ');
        };
        return style;
    }

    private static String pickToneLexeme(Random random, ArtifactPersonalityProfile personality, ArtifactVoiceProfile voice, boolean first) {
        List<String> primaryPool;
        if (personality.isAny(ArtifactPersonalityTrait.INTIMATE) || voice.softness() > 0.7D) {
            primaryPool = SOFT;
        } else if (personality.isAny(ArtifactPersonalityTrait.PREDATORY) || personality.isAny(ArtifactPersonalityTrait.HUNGRY)) {
            primaryPool = PREDATORY;
        } else if (voice.titleHeavy()) {
            primaryPool = RITUAL;
        } else {
            primaryPool = MELANCHOLY;
        }
        if (!first && primaryPool == SOFT && personality.isAny(ArtifactPersonalityTrait.PREDATORY)) {
            primaryPool = PREDATORY;
        }
        return primaryPool.get(random.nextInt(primaryPool.size()));
    }

    private static String formatByVoice(Random random, ArtifactVoiceProfile voice, String first, String second) {
        return switch (voice.primary()) {
            case LITURGICAL, RITUAL -> random.nextBoolean() ? first + " of " + second : "The " + first + " " + second;
            case COURTLY, CRUELLY_POLITE -> first + ", the " + second;
            case WHISPERING, VELVETY, INTIMATE, LULLABY_LIKE -> first + " " + second;
            case PREDATORY, MARTIAL, SEVERE -> first + "-" + second;
            case MOCKING_CEREMONIAL -> "Witness to " + first + " " + second;
            default -> first + " of " + second;
        };
    }

    private static String titleCase(ArtifactPersonalityTrait trait) {
        String lower = trait.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
