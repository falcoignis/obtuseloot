package obtuseloot.telemetry;

import java.io.IOException;
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
            for (EcosystemTelemetryEvent event : events) {
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
            try (var lines = Files.lines(archivePath, StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                if (!line.isBlank()) {
                    events.add(decode(line));
                }
                });
            }
            return List.copyOf(events);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read ecosystem telemetry", ex);
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
            try (var lines = Files.lines(archivePath, StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                    if (line.isBlank()) {
                        return;
                    }
                    if (ring.size() == maxEvents) {
                        ring.removeFirst();
                    }
                    ring.addLast(decode(line));
                });
            }
            return List.copyOf(ring);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read recent ecosystem telemetry", ex);
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

    private EcosystemTelemetryEvent decode(String line) {
        String[] parts = line.split("\\|", 6);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Malformed telemetry event line: " + line);
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
        return new EcosystemTelemetryEvent(
                Long.parseLong(parts[0]),
                EcosystemTelemetryEventType.valueOf(parts[1]),
                Long.parseLong(parts[2]),
                unescape(parts[3]),
                unescape(parts[4]),
                Map.copyOf(attributes)
        );
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
