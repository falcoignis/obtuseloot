package obtuseloot.dashboard;

import java.time.Instant;
import java.util.List;

public record ReportGenerationMetadata(
        Instant generatedAt,
        String reportType,
        String outputPath,
        List<DashboardDataSourceDescriptor> dataSources
) {
}
