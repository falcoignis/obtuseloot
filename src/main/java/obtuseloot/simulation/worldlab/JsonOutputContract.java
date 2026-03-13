package obtuseloot.simulation.worldlab;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class JsonOutputContract {
    private JsonOutputContract() {
    }

    public static String toJson(Object value) {
        return writeJson(normalize(value), 0);
    }

    public static void writeJson(Writer writer, Object value) {
        try {
            writeJsonValue(writer, normalize(value), 0);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Object normalize(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> e) {
            return e.name().toLowerCase(Locale.ROOT);
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(JsonOutputContract::normalize).orElse(null);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .forEach(entry -> out.put(String.valueOf(entry.getKey()), normalize(entry.getValue())));
            return out;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> out = new ArrayList<>();
            collection.forEach(item -> out.add(normalize(item)));
            return out;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> out = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                out.add(normalize(Array.get(value, i)));
            }
            return out;
        }
        if (value.getClass().isRecord()) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (RecordComponent component : value.getClass().getRecordComponents()) {
                try {
                    out.put(component.getName(), normalize(component.getAccessor().invoke(value)));
                } catch (ReflectiveOperationException ex) {
                    out.put(component.getName(), null);
                }
            }
            return out;
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static String writeJson(Object value, int indent) {
        String pad = "  ".repeat(indent);
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{\n");
            var entries = ((Map<String, Object>) map).entrySet().iterator();
            while (entries.hasNext()) {
                var entry = entries.next();
                sb.append(pad)
                        .append("  ")
                        .append(quote(entry.getKey()))
                        .append(": ")
                        .append(writeJson(entry.getValue(), indent + 1));
                if (entries.hasNext()) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            return sb.append(pad).append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(writeJson(list.get(i), indent + 1));
            }
            return sb.append(']').toString();
        }
        if (value instanceof String s) {
            return quote(s);
        }
        if (value == null) {
            return "null";
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static void writeJsonValue(Writer writer, Object value, int indent) throws IOException {
        String pad = "  ".repeat(indent);
        if (value instanceof Map<?, ?> map) {
            writer.write("{\n");
            var entries = ((Map<String, Object>) map).entrySet().iterator();
            while (entries.hasNext()) {
                var entry = entries.next();
                writer.write(pad);
                writer.write("  ");
                writer.write(quote(entry.getKey()));
                writer.write(": ");
                writeJsonValue(writer, entry.getValue(), indent + 1);
                if (entries.hasNext()) {
                    writer.write(',');
                }
                writer.write('\n');
            }
            writer.write(pad);
            writer.write('}');
            return;
        }
        if (value instanceof List<?> list) {
            writer.write('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    writer.write(',');
                }
                writeJsonValue(writer, list.get(i), indent + 1);
            }
            writer.write(']');
            return;
        }
        if (value instanceof String s) {
            writer.write(quote(s));
            return;
        }
        if (value == null) {
            writer.write("null");
            return;
        }
        writer.write(String.valueOf(value));
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
