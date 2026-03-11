package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.List;

public record LatentActivationResult(
        ArtifactGenome genome,
        List<GenomeTrait> activatedTraits,
        double activationRate
) {
}
