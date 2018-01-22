package co.kenrg.mega.backend.evaluation.object.iface;

import java.util.Arrays;

public enum ObjectType {
    INTEGER("Int"),
    FLOAT("Float"),
    BOOLEAN("Bool"),
    STRING("String"),

    FUNCTION("Func"),
    ARRAY("Array"),
    OBJECT("Object"),

    NULL("Null"),
    UNIT("Unit"),
    EVAL_ERROR("Error");

    public final String displayName;

    ObjectType(String displayName) {
        this.displayName = displayName;
    }

    public static ObjectType byDisplayName(String displayName) {
        return Arrays.stream(ObjectType.values())
            .filter(t -> t.displayName.equals(displayName))
            .findFirst()
            .orElse(null);
    }

    public boolean isNumeric() {
        return this == INTEGER || this == FLOAT;
    }
}
