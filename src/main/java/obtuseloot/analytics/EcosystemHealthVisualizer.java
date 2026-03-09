package obtuseloot.analytics;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EcosystemHealthVisualizer {
    public void createRankAbundanceChart(String title,
                                         Map<String, Integer> rankedAbundance,
                                         Path outputFile) throws IOException {
        int width = 1200;
        int height = 700;
        int marginLeft = 90;
        int marginRight = 50;
        int marginTop = 80;
        int marginBottom = 140;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int chartX = marginLeft;
        int chartY = marginTop;
        int chartW = width - marginLeft - marginRight;
        int chartH = height - marginTop - marginBottom;

        g.setColor(new Color(230, 232, 235));
        g.fillRect(chartX, chartY, chartW, chartH);

        List<Integer> values = new ArrayList<>(rankedAbundance.values());
        int max = values.stream().mapToInt(Integer::intValue).max().orElse(1);
        int n = Math.max(values.size(), 1);

        g.setColor(new Color(60, 63, 65));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);
        g.drawLine(chartX, chartY, chartX, chartY + chartH);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(title, marginLeft, 45);

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Rank", chartX + chartW / 2 - 18, chartY + chartH + 48);

        g.rotate(-Math.PI / 2);
        g.drawString("Abundance", -(chartY + chartH / 2 + 40), 35);
        g.rotate(Math.PI / 2);

        if (!values.isEmpty()) {
            g.setColor(new Color(63, 121, 191));
            g.setStroke(new BasicStroke(3f));
            int prevX = -1;
            int prevY = -1;
            for (int i = 0; i < values.size(); i++) {
                int x = chartX + (int) ((i / (double) (n - 1 == 0 ? 1 : n - 1)) * chartW);
                int y = chartY + chartH - (int) ((values.get(i) / (double) max) * chartH);
                if (prevX >= 0) {
                    g.drawLine(prevX, prevY, x, y);
                }
                g.fillOval(x - 4, y - 4, 8, 8);
                prevX = x;
                prevY = y;
            }

            g.setColor(new Color(80, 80, 80));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int rank = 1;
            for (Map.Entry<String, Integer> entry : rankedAbundance.entrySet()) {
                int x = chartX + (int) (((rank - 1) / (double) (n - 1 == 0 ? 1 : n - 1)) * chartW);
                g.drawString(String.valueOf(rank), x - 4, chartY + chartH + 18);
                g.drawString(entry.getKey(), x - 24, chartY + chartH + 36);
                rank++;
            }
        }

        Files.createDirectories(outputFile.getParent());
        ImageIO.write(image, "png", outputFile.toFile());
        g.dispose();
    }
}
