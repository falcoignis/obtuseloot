package obtuseloot.analytics;

import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.ecosystem.EnvironmentalEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class EnvironmentalPressureReporter {
    public Path writeReport(Path output, EnvironmentPressureEngine engine) throws IOException {
        Files.createDirectories(output.getParent());
        EnvironmentalEvent event = engine.currentEvent();
        Map<GenomeTrait, Double> modifiers = engine.currentModifiers();

        StringBuilder lines = new StringBuilder();
        lines.append("# Environment Pressure Report\n\n");
        lines.append("- Current event: `").append(event.name()).append("`\n");
        lines.append("- Remaining duration: `").append(event.remainingSeasons()).append("` seasons\n");
        lines.append("- Elapsed seasons tracked: `").append(engine.elapsedSeasons()).append("`\n\n");
        lines.append("## Active Fitness Landscape Modifiers\n\n");
        lines.append("| Genome Trait | Multiplier |\n");
        lines.append("| --- | ---: |\n");
        for (GenomeTrait trait : GenomeTrait.values()) {
            lines.append("| ")
                    .append(trait.name().toLowerCase(Locale.ROOT))
                    .append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f", modifiers.getOrDefault(trait, 1.0D)))
                    .append(" |\n");
        }

        Files.writeString(output, lines.toString());
        return output;
    }
}
