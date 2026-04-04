package obtuseloot.telemetry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcosystemHistoryArchive {
    private final Path archivePath;

    public EcosystemHistoryArchive(Path archivePath) {
        this.archivePath = archivePath;
    }

    public synchronized void append(List<EcosystemTelemetryEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(archivePath.getParent());
            try (Writer writer = Files.newBufferedWriter(archivePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                for (int i = 0; i < events.size(); i++) {
                    EcosystemTelemetryEvent event = events.get(i);
                    try {
                        TelemetryFieldContract.validateEvent(event);
                    } catch (RuntimeException ex) {
                        throw new IllegalArgumentException("Bad telemetry event input at batch index " + i + ": " + ex.getMessage(), ex);
                    }
                    writer.write(encode(event));
                    writer.write('\n');
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to append ecosystem telemetry", ex);
        }
    }

    public synchronized List<EcosystemTelemetryEvent> readAll() {
        if (!Files.exists(archivePath)) {
            return List.of();
        }
        try {
            List<EcosystemTelemetryEvent> events = new ArrayList<>();
            try (var reader = Files.newBufferedReader(archivePath, StandardCharsets.UTF_8)) {
                int lineNumber = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (!line.isBlank()) {
                        events.add(decode(line, lineNumber));
                    }
                }
            }
            return List.copyOf(events);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read ecosystem telemetry", ex);
        } catch (UncheckedIOException ex) {
            throw new IllegalStateException("Unable to read ecosystem telemetry", ex.getCause());
        }
    }

    public synchronized List<EcosystemTelemetryEvent> readRecent(int maxEvents) {
        if (maxEvents <= 0) {
            return List.of();
        }
        if (!Files.exists(archivePath)) {
            return List.of();
        }
        try {
            ArrayDeque<EcosystemTelemetryEvent> ring = new ArrayDeque<>(maxEvents);
            try (var reader = Files.newBufferedReader(archivePath, StandardCharsets.UTF_8)) {
                int lineNumber = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) {
                        continue;
                    }
                    if (ring.size() == maxEvents) {
                        ring.removeFirst();
                    }
                    ring.addLast(decode(line, lineNumber));
                }
            }
            return List.copyOf(ring);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read recent ecosystem telemetry", ex);
        } catch (UncheckedIOException ex) {
            throw new IllegalStateException("Unable to read recent ecosystem telemetry", ex.getCause());
        }
    }

    public synchronized void copyTo(Path outputPath) {
        try {
            Files.createDirectories(outputPath.getParent());
            Path normalizedArchive = archivePath.toAbsolutePath().normalize();
            Path normalizedOutput = outputPath.toAbsolutePath().normalize();
            if (normalizedArchive.equals(normalizedOutput)) {
                if (!Files.exists(archivePath)) {
                    Files.writeString(archivePath, "", StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                }
                return;
            }
            if (!Files.exists(archivePath)) {
                Files.writeString(outputPath, "", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                return;
            }
            Files.copy(archivePath, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to copy ecosystem telemetry archive", ex);
        }
    }

    private String encode(EcosystemTelemetryEvent event) {
        StringBuilder payload = new StringBuilder();
        event.attributes().forEach((key, value) -> {
            if (!payload.isEmpty()) {
                payload.append(';');
            }
            payload.append(escape(key)).append('=').append(escape(value));
        });
        return event.timestampMs() + "|" + event.type().name() + "|" + event.artifactSeed() + "|"
                + escape(event.lineageId() == null ? "" : event.lineageId()) + "|"
                + escape(event.niche() == null ? "" : event.niche()) + "|" + payload;
    }

    private EcosystemTelemetryEvent decode(String line, int lineNumber) {
        String[] parts = line.split("\\|", 6);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Bad persisted telemetry event at line " + lineNumber
                    + ": expected 6 pipe-delimited fields");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        if (!parts[5].isBlank()) {
            for (String pair : parts[5].split(";")) {
                if (pair.isBlank()) {
                    continue;
                }
                String[] kv = pair.split("=", 2);
                String key = unescape(kv[0]);
                String value = kv.length > 1 ? unescape(kv[1]) : "";
                attributes.put(key, value);
            }
        }
        try {
            EcosystemTelemetryEventType type = EcosystemTelemetryEventType.valueOf(parts[1]);
            Map<String, String> normalized = TelemetryFieldContract.normalize(type, attributes);
            EcosystemTelemetryEvent event = new EcosystemTelemetryEvent(
                    Long.parseLong(parts[0]),
                    type,
                    Long.parseLong(parts[2]),
                    unescape(parts[3]),
                    unescape(parts[4]),
                    normalized
            );
            TelemetryFieldContract.validateEvent(event);
            return event;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Bad persisted telemetry event at line " + lineNumber + ": " + ex.getMessage(), ex);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\p").replace(";", "\\s").replace("=", "\\e");
    }

    private String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (char c : value.toCharArray()) {
            if (escaped) {
                out.append(switch (c) {
                    case 'p' -> '|';
                    case 's' -> ';';
                    case 'e' -> '=';
                    case '\\' -> '\\';
                    default -> c;
                });
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
