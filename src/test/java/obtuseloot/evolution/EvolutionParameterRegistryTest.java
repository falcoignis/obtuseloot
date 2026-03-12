package obtuseloot.evolution;

import obtuseloot.evolution.params.EvolutionParameterRegistry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvolutionParameterRegistryTest {

    @Test
    void reloadUpdatesProfileWithoutRestart() {
        EvolutionParameterRegistry registry = new EvolutionParameterRegistry();
        YamlConfiguration config = new YamlConfiguration();
        config.set("ecosystem.parameters.nicheSaturationSensitivity", 1.1D);
        config.set("ecosystem.parameters.mutationAmplitudeMin", 0.8D);
        config.set("ecosystem.parameters.mutationAmplitudeMax", 1.5D);

        registry.load(config);
        assertEquals(1.1D, registry.profile().nicheSaturationSensitivity());

        config.set("ecosystem.parameters.nicheSaturationSensitivity", 0.7D);
        registry.reload(config);
        assertEquals(0.7D, registry.profile().nicheSaturationSensitivity());
    }
}
