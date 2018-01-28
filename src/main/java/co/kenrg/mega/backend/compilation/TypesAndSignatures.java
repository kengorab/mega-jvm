package co.kenrg.mega.backend.compilation;

import static java.util.stream.Collectors.joining;

import java.util.Map;

import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Maps;
import org.objectweb.asm.Type;

public class TypesAndSignatures {
    private static Map<Class, String> classDescriptors = Maps.newHashMap();

    public static String getDescriptor(Class clazz) {
        if (classDescriptors.containsKey(clazz)) {
            return classDescriptors.get(clazz);
        }
        String desc = Type.getDescriptor(clazz);
        classDescriptors.put(clazz, desc);
        return desc;
    }

    private static Map<Class, String> classInternalNames = Maps.newHashMap();

    public static String getInternalName(Class clazz) {
        if (classInternalNames.containsKey(clazz)) {
            return classInternalNames.get(clazz);
        }
        String name = Type.getInternalName(clazz);
        classInternalNames.put(clazz, name);
        return name;
    }

    private static String descriptorForClass(String className) {
        return String.format("L%s;", className);
    }

    public static String jvmMethodDescriptor(FunctionType methodType, boolean boxPrimitives) {
        return jvmMethodDescriptor(methodType, boxPrimitives, false);
    }

    public static String jvmMethodDescriptor(FunctionType methodType, boolean boxPrimitives, boolean returnsVoid) {
        String paramTypeDescs = methodType.paramTypes.stream()
            .map(paramType -> jvmDescriptor(paramType, boxPrimitives))
            .collect(joining(""));
        String returnTypeDesc = returnsVoid ? "V" : jvmDescriptor(methodType.returnType, boxPrimitives);
        return String.format("(%s)%s", paramTypeDescs, returnTypeDesc);
    }

    public static String jvmDescriptor(MegaType type, boolean boxPrimitives) {
        if (type == PrimitiveTypes.INTEGER) {
            return boxPrimitives ? getDescriptor(PrimitiveTypes.INTEGER.typeClass()) : "I";
        } else if (type == PrimitiveTypes.FLOAT) {
            return boxPrimitives ? getDescriptor(PrimitiveTypes.FLOAT.typeClass()) : "F";
        } else if (type == PrimitiveTypes.BOOLEAN) {
            return boxPrimitives ? getDescriptor(PrimitiveTypes.BOOLEAN.typeClass()) : "Z";
        } else if (type == PrimitiveTypes.STRING) {
            return getDescriptor(PrimitiveTypes.STRING.typeClass());
        } else if (type instanceof ArrayType) {
            String elemDescriptor = jvmDescriptor(((ArrayType) type).typeArg, true);
            return "[" + elemDescriptor;
        } else if (type instanceof FunctionType) {
            return getDescriptor(type.typeClass());
        }

        return descriptorForClass(type.className());
    }

    public static boolean isPrimitive(MegaType type) {
        return type == PrimitiveTypes.INTEGER || type == PrimitiveTypes.FLOAT || type == PrimitiveTypes.BOOLEAN;
    }
}
