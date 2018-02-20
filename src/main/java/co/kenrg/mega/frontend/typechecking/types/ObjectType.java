package co.kenrg.mega.frontend.typechecking.types;

import static java.util.stream.Collectors.joining;

import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;

public class ObjectType extends MegaType {
    public final LinkedHashMultimap<String, MegaType> properties;

    public ObjectType(LinkedHashMultimap<String, MegaType> properties) {
        this.properties = properties;
    }

    @Override
    public String displayName() {
        return this.properties.entries().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue().signature())
            .collect(joining(", ", "{ ", " }"));
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        return other instanceof ObjectType && this.properties.equals(((ObjectType) other).properties);
    }

    @Override
    public LinkedHashMultimap<String, MegaType> getProperties() {
        return this.properties;
    }

    @Override
    public Set<MegaType> getPropertiesByName(String propName) {
        return this.properties.get(propName);
    }
}
