package co.kenrg.mega.frontend.typechecking.types;

import static com.google.common.collect.Streams.zip;

import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;

public class StructType extends MegaType {
    public final String typeName;
    private final LinkedHashMultimap<String, MegaType> properties;

    private String className;

    public StructType(String typeName, LinkedHashMultimap<String, MegaType> properties) {
        this.typeName = typeName;
        this.properties = properties;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String className() {
        return this.className;
    }

    @Override
    public String displayName() {
        return this.typeName;
    }

    @Override
    public LinkedHashMultimap<String, MegaType> getProperties() {
        return this.properties;
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        if (other instanceof StructType || other instanceof ObjectType) {
            LinkedHashMultimap<String, MegaType> properties = other.getProperties();
            return testPropertyEquivalence(properties);
        } else {
            return false;
        }
    }

    private boolean testPropertyEquivalence(LinkedHashMultimap<String, MegaType> properties) {
        if (properties.size() != this.properties.size()) {
            return false;
        }
        Set<String> propNames = this.properties.keySet();//.stream().map(Pair::getKey).collect(toSet());
        Set<String> otherPropNames = properties.keySet();//.stream().map(Pair::getKey).collect(toSet());
        if (!propNames.equals(otherPropNames)) {
            return false;
        }

        return zip(this.properties.entries().stream(), properties.entries().stream(), (prop, otherProp) -> {
            MegaType otherType = otherProp.getValue();
            MegaType thisType = prop.getValue();

            return thisType.isEquivalentTo(otherType);
        }).reduce(true, Boolean::logicalAnd);
    }
}
