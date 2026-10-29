package obtuseloot.names;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamePoolNamingTest {
    @Test void operatorPoolReplacementInvalidatesCachedNameParts() throws Exception {
        var prefixes = NamePoolManager.class.getDeclaredField("prefixes");
        var suffixes = NamePoolManager.class.getDeclaredField("suffixes");
        prefixes.setAccessible(true);
        suffixes.setAccessible(true);
        Object oldPrefixes = prefixes.get(null), oldSuffixes = suffixes.get(null);
        try {
            prefixes.set(null, List.of("Amber"));
            suffixes.set(null, List.of("of Dawn"));
            assertEquals("Amber Blade of Dawn", ArtifactNameGenerator.generateFromSeed(-19L, "iron_sword"));
            prefixes.set(null, List.of("Frost"));
            suffixes.set(null, List.of("of Dusk"));
            assertEquals("Frost Blade of Dusk", ArtifactNameGenerator.generateFromSeed(-19L, "iron_sword"));
        } finally {
            prefixes.set(null, oldPrefixes);
            suffixes.set(null, oldSuffixes);
        }
    }

    @Test void longPoolEntriesAreOmittedWholeInsteadOfTruncatedIntoAnotherName() throws Exception {
        var prefixes = NamePoolManager.class.getDeclaredField("prefixes");
        var suffixes = NamePoolManager.class.getDeclaredField("suffixes");
        prefixes.setAccessible(true);
        suffixes.setAccessible(true);
        Object oldPrefixes = prefixes.get(null), oldSuffixes = suffixes.get(null);
        try {
            prefixes.set(null, List.of("A".repeat(49), "Amber"));
            suffixes.set(null, List.of("of the Very Long Unfinished Phrase", "of Dawn"));
            assertEquals("Amber Blade of Dawn", ArtifactNameGenerator.generateFromSeed(-19L, "iron_sword"));
        } finally {
            prefixes.set(null, oldPrefixes);
            suffixes.set(null, oldSuffixes);
        }
    }
}
