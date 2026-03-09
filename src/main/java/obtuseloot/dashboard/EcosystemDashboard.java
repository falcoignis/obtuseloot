package obtuseloot.dashboard;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class EcosystemDashboard {
    private final DashboardDataAssembler assembler = new DashboardDataAssembler();
    private final DashboardRenderer renderer = new DashboardRenderer();
    private final DashboardSummaryBuilder summaryBuilder = new DashboardSummaryBuilder();

    public Path generate(Path analyticsRoot, DashboardMetrics metrics, Path output) throws IOException {
        Map<String, Object> data = assembler.assemble(analyticsRoot, metrics);
        String summary = summaryBuilder.build(metrics);
        return renderer.render(output, data, summary);
    }
}
