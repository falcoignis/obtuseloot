package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.ArrayList;
import java.util.List;

public class LatentTraitActivationResolver {
    public LatentActivationResult resolve(ArtifactGenome genome, LatentActivationContext context) {
        ArtifactGenome activatedGenome = genome;
        List<GenomeTrait> activated = new ArrayList<>();
        for (GenomeTrait trait : GenomeTrait.values()) {
            if (activatedGenome.latentActivated(trait)) {
                continue;
            }
            double latent = activatedGenome.latentTrait(trait);
            if (latent < 0.25D) {
                continue;
            }
            double pathwaySignal = (context.environmentalExposure() * 0.28D)
                    + (context.interferenceSignal() * 0.27D)
                    + (context.experienceSignal() * 0.21D)
                    + (context.lineageDriftSignal() * 0.14D)
                    + (context.repeatedNicheExposure() * 0.10D)
                    + (latent * 0.35D);
            double threshold = 0.76D + ((trait.ordinal() % 4) * 0.04D);
            if (pathwaySignal >= threshold) {
                double promotionStrength = 0.25D + (context.interferenceSignal() * 0.20D) + (context.environmentalExposure() * 0.10D);
                activatedGenome = activatedGenome.activateLatentTrait(trait, promotionStrength);
                activated.add(trait);
            }
        }
        double rate = GenomeTrait.values().length == 0 ? 0.0D : activated.size() / (double) GenomeTrait.values().length;
        return new LatentActivationResult(activatedGenome, List.copyOf(activated), rate);
    }
}
