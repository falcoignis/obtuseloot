package obtuseloot.evolution.params;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.concurrent.atomic.AtomicReference;

public class EvolutionParameterRegistry {
    private final AtomicReference<EcosystemTuningProfile> activeProfile = new AtomicReference<>(EcosystemTuningProfile.defaults());

    public void load(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        EcosystemTuningProfile defaults = EcosystemTuningProfile.defaults();
        EcosystemTuningProfile profile = new EcosystemTuningProfile(
                config.getDouble("ecosystem.parameters.nicheSaturationSensitivity", defaults.nicheSaturationSensitivity()),
                config.getDouble("ecosystem.parameters.lineageMomentumInfluence", defaults.lineageMomentumInfluence()),
                config.getDouble("ecosystem.parameters.mutationAmplitudeMin", defaults.mutationAmplitudeMin()),
                config.getDouble("ecosystem.parameters.mutationAmplitudeMax", defaults.mutationAmplitudeMax()),
                config.getInt("ecosystem.parameters.driftWindowDurationTicks", defaults.driftWindowDurationTicks()),
                config.getDouble("ecosystem.parameters.competitionReinforcementCurve", defaults.competitionReinforcementCurve())
        );
        activeProfile.set(profile);
    }

    public EcosystemTuningProfile profile() {
        return activeProfile.get();
    }
}
