package co.kenrg.mega.frontend.typechecking.types;

import static com.google.common.collect.Streams.zip;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class StructType extends MegaType {
    public final String typeName;
    private final List<Pair<String, MegaType>> properties;

    public StructType(String typeName, List<Pair<String, MegaType>> properties) {
        this.typeName = typeName;
        this.properties = properties;
    }

    @Override
    public String displayName() {
        return this.typeName;
    }

    public List<Pair<String, MegaType>> getProperties() {
        return this.properties;
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        if (other instanceof StructType) {
            List<Pair<String, MegaType>> properties = ((StructType) other).properties;
            return testPropertyEquivalence(properties);
        } else if (other instanceof ObjectType) {
            List<Pair<String, MegaType>> properties = ((ObjectType) other).properties;
            return testPropertyEquivalence(properties);
        } else {
            return false;
        }
    }

    private boolean testPropertyEquivalence(List<Pair<String, MegaType>> properties) {
        if (properties.size() != this.properties.size()) {
            return false;
        }
        Set<String> propNames = this.properties.stream().map(Pair::getKey).collect(toSet());
        Set<String> otherPropNames = properties.stream().map(Pair::getKey).collect(toSet());
        if (!propNames.equals(otherPropNames)) {
            return false;
        }

        return zip(this.properties.stream(), properties.stream(), (prop, otherProp) -> {
            MegaType otherType = otherProp.getValue();
            MegaType thisType = prop.getValue();

            return thisType.isEquivalentTo(otherType);
        }).reduce(true, Boolean::logicalAnd);
    }
}
