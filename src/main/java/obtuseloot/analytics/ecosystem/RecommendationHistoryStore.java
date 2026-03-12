package obtuseloot.analytics.ecosystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class RecommendationHistoryStore {
    private final Path storePath;

    public RecommendationHistoryStore(Path storePath) {
        this.storePath = storePath;
    }

    public synchronized TuningRecommendationRecord append(TuningRecommendationRecord record) {
        List<TuningRecommendationRecord> all = new ArrayList<>(readAll());
        all.add(record);
        writeAll(all);
        return record;
    }

    public synchronized List<TuningRecommendationRecord> readAll() {
        if (!Files.exists(storePath)) {
            return List.of();
        }
        try {
            List<TuningRecommendationRecord> out = new ArrayList<>();
            for (String line : Files.readAllLines(storePath, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    out.add(decode(line));
                }
            }
            return List.copyOf(out);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read recommendation history", ex);
        }
    }

    public synchronized Optional<TuningRecommendationRecord> latest() {
        List<TuningRecommendationRecord> all = readAll();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getLast());
    }

    public synchronized Optional<String> compareAgainstLatest(TuningRecommendationRecord candidate) {
        Optional<TuningRecommendationRecord> previous = latest();
        if (previous.isEmpty()) {
            return Optional.of("first recommendation for this governance store");
        }
        Map<String, Double> oldParams = previous.get().recommendation().parameterAdjustments();
        Map<String, Double> newParams = candidate.recommendation().parameterAdjustments();
        List<String> changed = new ArrayList<>();
        for (String key : newParams.keySet()) {
            double oldValue = oldParams.getOrDefault(key, Double.NaN);
            double newValue = newParams.get(key);
            if (Double.isNaN(oldValue) || Math.abs(oldValue - newValue) > 0.0001D) {
                changed.add(key + ": " + oldValue + " -> " + newValue);
            }
        }
        return Optional.of(changed.isEmpty() ? "no parameter changes" : String.join(", ", changed));
    }

    public synchronized Optional<TuningRecommendationRecord> setDecision(String recommendationId,
                                                                         RecommendationDecision decision,
                                                                         String note) {
        List<TuningRecommendationRecord> all = new ArrayList<>(readAll());
        for (int i = 0; i < all.size(); i++) {
            TuningRecommendationRecord current = all.get(i);
            if (current.recommendationId().equals(recommendationId)) {
                TuningRecommendationRecord updated = current.withDecision(decision, note);
                all.set(i, updated);
                writeAll(all);
                return Optional.of(updated);
            }
        }
        return Optional.empty();
    }

    private void writeAll(List<TuningRecommendationRecord> records) {
        try {
            Files.createDirectories(storePath.getParent());
            StringBuilder buffer = new StringBuilder();
            for (TuningRecommendationRecord record : records) {
                buffer.append(encode(record)).append('\n');
            }
            Files.writeString(storePath, buffer.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist recommendation history", ex);
        }
    }

    private String encode(TuningRecommendationRecord record) {
        Properties p = new Properties();
        p.setProperty("id", record.recommendationId());
        p.setProperty("generatedAtMs", String.valueOf(record.generatedAtMs()));
        p.setProperty("profileName", record.recommendation().profileName());
        p.setProperty("rationale", record.recommendation().rationale());
        p.setProperty("decision", record.decision().name());
        p.setProperty("decisionNote", record.decisionNote() == null ? "" : record.decisionNote());
        p.setProperty("jobId", record.governanceMetadata().sourceAnalysisJobId());
        p.setProperty("window", record.governanceMetadata().analysisWindow());
        p.setProperty("sourceKind", record.governanceMetadata().sourceKind());
        p.setProperty("metadataGeneratedAtMs", String.valueOf(record.governanceMetadata().generatedAtMs()));
        record.recommendation().parameterAdjustments().forEach((k, v) -> p.setProperty("param." + k, String.valueOf(v)));
        StringBuilder out = new StringBuilder();
        p.stringPropertyNames().stream().sorted().forEach(k -> {
            if (!out.isEmpty()) {
                out.append(';');
            }
            out.append(escape(k)).append('=').append(escape(p.getProperty(k, "")));
        });
        return out.toString();
    }

    private TuningRecommendationRecord decode(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : line.split(";")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            map.put(unescape(kv[0]), kv.length > 1 ? unescape(kv[1]) : "");
        }
        Map<String, Double> params = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (k.startsWith("param.")) {
                params.put(k.substring("param.".length()), Double.parseDouble(v));
            }
        });
        TuningProfileRecommendation recommendation = new TuningProfileRecommendation(
                map.getOrDefault("profileName", "phase6-ecosystem-stabilization"),
                Map.copyOf(params),
                map.getOrDefault("rationale", ""));
        GovernanceMetadata metadata = new GovernanceMetadata(
                map.getOrDefault("jobId", "unknown"),
                Long.parseLong(map.getOrDefault("metadataGeneratedAtMs", "0")),
                map.getOrDefault("window", "scenario"),
                map.getOrDefault("sourceKind", "offline"));
        return new TuningRecommendationRecord(
                map.getOrDefault("id", "rec-unknown"),
                Long.parseLong(map.getOrDefault("generatedAtMs", "0")),
                recommendation,
                metadata,
                RecommendationDecision.valueOf(map.getOrDefault("decision", RecommendationDecision.PROPOSED.name())),
                map.getOrDefault("decisionNote", ""));
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\s").replace("=", "\\e");
    }

    private String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (char c : value.toCharArray()) {
            if (escaped) {
                out.append(switch (c) {
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
