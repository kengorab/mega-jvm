package co.kenrg.mega.frontend.typechecking.types;

import java.util.Map;

public class ObjectType extends MegaType {
    public final Map<String, MegaType> properties;

    public ObjectType(Map<String, MegaType> properties) {
        this.properties = properties;
    }

    @Override
    public String displayName() {
        return "Object";
    }
}
