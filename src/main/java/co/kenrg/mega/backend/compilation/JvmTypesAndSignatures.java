package co.kenrg.mega.backend.compilation;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;

public class JvmTypesAndSignatures {
    public static String jvmDescriptor(MegaType type) {
        if (type.equals(PrimitiveTypes.INTEGER)) {
            return "I";
        } else if (type.equals(PrimitiveTypes.FLOAT)) {
            return "F";
        } else if (type.equals(PrimitiveTypes.BOOLEAN)) {
            return "Z";
        } else if (type.equals(PrimitiveTypes.STRING)) {
            return "Ljava/lang/String;";
        }

        // TODO: Other types, including primitives, and classes
        return null;
    }
}
