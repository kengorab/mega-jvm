package co.kenrg.mega.backend.compilation;

import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;

public class TypesAndSignatures {
    private static String descriptorForClass(String className) {
        return String.format("L%s;", className);
    }

    public static String jvmDescriptor(MegaType type, boolean boxPrimitives) {
        if (type == PrimitiveTypes.INTEGER) {
            return boxPrimitives ? descriptorForClass(PrimitiveTypes.INTEGER.className()) : "I";
        } else if (type == PrimitiveTypes.FLOAT) {
            return boxPrimitives ? descriptorForClass(PrimitiveTypes.FLOAT.className()) : "F";
        } else if (type == PrimitiveTypes.BOOLEAN) {
            return boxPrimitives ? descriptorForClass(PrimitiveTypes.BOOLEAN.className()) : "Z";
        } else if (type == PrimitiveTypes.STRING) {
            return descriptorForClass(PrimitiveTypes.STRING.className());
        } else if (type instanceof ArrayType) {
            String elemDescriptor = jvmDescriptor(((ArrayType) type).typeArg, true);
            return "[" + elemDescriptor;
        }

        return descriptorForClass(type.className());
    }

    public static boolean isPrimitive(MegaType type) {
        return type == PrimitiveTypes.INTEGER || type == PrimitiveTypes.FLOAT || type == PrimitiveTypes.BOOLEAN;
    }
}
