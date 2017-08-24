package co.kenrg.mega.frontend.typechecking.types;

import java.util.Map;

public class StructType extends MegaType {
    public final String typeName;
    public final Map<String, MegaType> properties;

    public StructType(String typeName, Map<String, MegaType> properties) {
        this.typeName = typeName;
        this.properties = properties;
    }

    @Override
    public String displayName() {
        return this.typeName;
    }

    @Override
    public boolean isEquivalentTo(MegaType other) {
        if (other instanceof StructType) {
            Map<String, MegaType> properties = ((StructType) other).properties;
            return testPropertyEquivalence(properties);
        } else if (other instanceof ObjectType) {
            Map<String, MegaType> properties = ((ObjectType) other).properties;
            return testPropertyEquivalence(properties);
        } else {
            return false;
        }
    }

    private boolean testPropertyEquivalence(Map<String, MegaType> properties) {
        if (properties.size() != this.properties.size()) {
            return false;
        }
        if (!properties.keySet().equals(this.properties.keySet())) {
            return false;
        }

        return properties.entrySet().stream()
            .map(entry -> {
                MegaType otherType = entry.getValue();
                MegaType thisType = this.properties.get(entry.getKey());

                return thisType.isEquivalentTo(otherType);
            })
            .reduce(true, Boolean::logicalAnd);
    }
}
