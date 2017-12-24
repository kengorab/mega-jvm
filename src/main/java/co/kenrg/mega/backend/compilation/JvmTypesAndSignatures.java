package co.kenrg.mega.backend.compilation;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;

public class JvmTypesAndSignatures {
    public static String jvmDescriptor(MegaType type) {
        if (type.equals(PrimitiveTypes.INTEGER)) {
            return "I";
        }

        // TODO: Other types, including primitives, and classes
        return null;
    }
}
