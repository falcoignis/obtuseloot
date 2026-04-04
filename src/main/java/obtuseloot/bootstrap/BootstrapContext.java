package obtuseloot.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class BootstrapContext {
    private final Map<Class<?>, Object> components = new LinkedHashMap<>();

    public <T> T register(Class<T> componentType, T component) {
        Objects.requireNonNull(componentType, "componentType");
        Objects.requireNonNull(component, "component");
        if (components.containsKey(componentType)) {
            throw new IllegalStateException("Component already registered: " + componentType.getName());
        }
        components.put(componentType, component);
        return component;
    }

    public <T> Optional<T> find(Class<T> componentType) {
        Objects.requireNonNull(componentType, "componentType");
        return Optional.ofNullable(componentType.cast(components.get(componentType)));
    }

    public <T> T require(Class<T> componentType) {
        return find(componentType).orElseThrow(() -> new IllegalStateException(
                "Missing component: " + componentType.getName() + ". Registered components: " + describeComponents()));
    }

    private String describeComponents() {
        if (components.isEmpty()) {
            return "<none>";
        }
        return components.keySet().stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
    }
}
