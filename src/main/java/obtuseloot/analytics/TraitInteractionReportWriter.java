package obtuseloot.analytics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TraitInteractionReportWriter {
    public void write(Path output, TraitCorrelationMatrix matrix) throws IOException {
        Files.createDirectories(output.getParent());
        List<java.util.Map.Entry<String, Integer>> top = matrix.topPairs(5);
        List<java.util.Map.Entry<String, Integer>> leastFrequent = matrix.leastFrequentPairs(5);
        int max = Math.max(1, matrix.maxFrequency());

        StringBuilder md = new StringBuilder();
        md.append("# Trait Interaction Report\n\n");
        md.append("1. **Sample size:** ").append(matrix.sampleSize()).append("\n");
        md.append("2. **Most common trait pairings:**\n");
        for (var entry : top) {
            md.append("   - ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }
        md.append("3. **Least frequent or absent trait pairings:**\n");
        for (var entry : leastFrequent) {
            md.append("   - ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }

        md.append("\n## Interpretation\n");
        if (!top.isEmpty()) {
            var dominant = top.get(0);
            md.append("- Suspiciously dominant pairing: **").append(dominant.getKey()).append("** at ")
                    .append(dominant.getValue()).append(" counts. This may indicate hidden weighting in ability branch resolution or genome trait interpolation.\n");
        }
        if (!leastFrequent.isEmpty()) {
            var suppressed = leastFrequent.get(0);
            md.append("- Suppressed pairing: **").append(suppressed.getKey()).append("** at ")
                    .append(suppressed.getValue()).append(" counts. This may indicate missing interaction support in mutation, awakening, or lineage inheritance paths.\n");
        }
        md.append("- Likely hidden generator bias appears when one pair exceeds ~70% of observed max intensity (max ")
                .append(max).append(").\n");
        md.append("- Lineage lock-in risk rises when top pairings repeatedly include stability/precision/resonance without offsetting chaos/mobility variation.\n\n");

        md.append("## Recommended review actions\n");
        md.append("- Audit weighted branch selection for dominant trait-pair reinforcement.\n");
        md.append("- Verify mutation pathways for suppressed pairings and ensure interaction hooks exist.\n");
        md.append("- Run world-lab seasonal comparisons and confirm pair distributions drift over seasons.\n");
        md.append("- Trigger `/obtuseloot debug dashboard` after balance changes to verify heatmap movement.\n");

        Files.writeString(output, md.toString());
    }
}
