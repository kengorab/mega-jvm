package co.kenrg.mega.frontend.typechecking.types;

import static java.util.stream.Collectors.joining;

import java.util.Map;

public class ObjectType extends MegaType {
    public final Map<String, MegaType> properties;

    public ObjectType(Map<String, MegaType> properties) {
        this.properties = properties;
    }

    @Override
    public String displayName() {
        return this.properties.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue().signature())
            .collect(joining(", ", "{ ", " }"));
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        return other instanceof ObjectType && this.properties.equals(((ObjectType) other).properties);
    }
}
