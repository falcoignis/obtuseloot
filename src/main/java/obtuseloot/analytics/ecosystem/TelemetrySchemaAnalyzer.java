package obtuseloot.analytics.ecosystem;

import obtuseloot.telemetry.EcosystemTelemetryEventType;
import obtuseloot.telemetry.TelemetryFieldContract;

import java.util.LinkedHashMap;
import java.util.Map;

public class TelemetrySchemaAnalyzer {

    public Map<EcosystemTelemetryEventType, SchemaHealth> analyzeContracts() {
        Map<EcosystemTelemetryEventType, SchemaHealth> report = new LinkedHashMap<>();
        for (EcosystemTelemetryEventType type : EcosystemTelemetryEventType.values()) {
            TelemetryFieldContract.SchemaContract contract = TelemetryFieldContract.contractFor(type);
            int required = contract == null ? 0 : contract.required().size();
            int optional = contract == null ? 0 : contract.optional().size();
            int absent = contract == null ? 0 : contract.intentionallyAbsent().size();
            report.put(type, new SchemaHealth(required, optional, absent,
                    contract != null && required >= 2 && (required + optional) > 4));
        }
        return Map.copyOf(report);
    }

    public record SchemaHealth(int requiredFieldCount,
                               int optionalFieldCount,
                               int intentionallyAbsentFieldCount,
                               boolean contractBalanced) {
    }
}
