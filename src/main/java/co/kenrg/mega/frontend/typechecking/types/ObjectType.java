package co.kenrg.mega.frontend.typechecking.types;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class ObjectType extends MegaType {
    public final List<Pair<String, MegaType>> properties;

    public ObjectType(List<Pair<String, MegaType>> properties) {
        this.properties = properties;
    }

    @Override
    public String displayName() {
        return this.properties.stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue().signature())
            .collect(joining(", ", "{ ", " }"));
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        return other instanceof ObjectType && this.properties.equals(((ObjectType) other).properties);
    }
}
