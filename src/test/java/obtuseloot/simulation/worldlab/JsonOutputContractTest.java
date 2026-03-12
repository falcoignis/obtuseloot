package obtuseloot.simulation.worldlab;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonOutputContractTest {

    private record SampleRecord(String name, int count) {}

    @Test
    void serializesStrictJsonForNestedWorldArtifacts() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("line", "hello\nworld");
        payload.put("quote", "\"quoted\"");
        payload.put("record", new SampleRecord("abc", 7));
        payload.put("timeline", List.of(Map.of("window", 1, "value", 0.3D)));

        String json = JsonOutputContract.toJson(payload);

        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\\"quoted\\\""));
        assertDoesNotThrow(() -> {
            if (!(json.startsWith("{") && json.endsWith("}"))) {
                throw new IllegalStateException("invalid json envelope");
            }
        });
    }
}
