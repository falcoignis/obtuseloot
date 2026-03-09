package obtuseloot.analytics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InteractionHeatmapExporter {
    public record ExportResult(Path heatmapPath, Path matrixCsvPath) {}

    public ExportResult export(TraitCorrelationMatrix matrix,
                               Path heatmapPath,
                               Path matrixCsvPath) throws IOException {
        writeCsv(matrix, matrixCsvPath);
        writeHeatmap(matrix, heatmapPath);
        return new ExportResult(heatmapPath, matrixCsvPath);
    }

    private void writeCsv(TraitCorrelationMatrix matrix, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        List<String> traits = new ArrayList<>(matrix.traits());
        StringBuilder sb = new StringBuilder("trait");
        for (String trait : traits) {
            sb.append(',').append(trait);
        }
        sb.append('\n');
        for (String row : traits) {
            sb.append(row);
            for (String col : traits) {
                sb.append(',').append(matrix.get(row, col));
            }
            sb.append('\n');
        }
        Files.writeString(output, sb.toString());
    }

    private void writeHeatmap(TraitCorrelationMatrix matrix, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Set<String> traitSet = matrix.traits();
        List<String> traits = new ArrayList<>(traitSet);
        int n = Math.max(1, traits.size());
        int cell = 24;
        int left = 280;
        int top = 110;
        int width = left + (n * cell) + 80;
        int height = top + (n * cell) + 80;
        int max = Math.max(1, matrix.maxFrequency());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(35, 35, 35));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Trait Interaction Heatmap", 24, 36);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Color intensity represents pair frequency", 24, 58);

        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                String rowTrait = traits.get(row);
                String colTrait = traits.get(col);
                int count = matrix.get(rowTrait, colTrait);
                double ratio = count / (double) max;
                Color color = new Color((int) (20 + 200 * ratio), (int) (40 + 50 * (1 - ratio)), (int) (240 - 170 * ratio));
                int x = left + (col * cell);
                int y = top + (row * cell);
                g.setColor(color);
                g.fillRect(x, y, cell, cell);
                g.setColor(new Color(245, 245, 245, 180));
                g.drawRect(x, y, cell, cell);
            }
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(65, 65, 65));
        for (int i = 0; i < n; i++) {
            int y = top + (i * cell) + 16;
            g.drawString(traits.get(i), 12, y);

            Graphics2D gx = (Graphics2D) g.create();
            gx.translate(left + (i * cell) + 15, top - 8);
            gx.rotate(-Math.PI / 3);
            gx.drawString(traits.get(i), 0, 0);
            gx.dispose();
        }

        ImageIO.write(image, "png", output.toFile());
        g.dispose();
    }
}
