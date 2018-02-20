package co.kenrg.mega.backend.compilation;

import static java.util.stream.Collectors.joining;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.FunctionType.Kind;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Lists;
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

    public static String getInternalName(MegaType type) {
        if (type.typeClass() != null) {
            return getInternalName(type.typeClass());
        }
        return type.className();
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
        } else if (type instanceof ObjectType) {
            return getDescriptor(Map.class);
        }

        return descriptorForClass(type.className());
    }

    public static boolean isPrimitive(MegaType type) {
        return type == PrimitiveTypes.INTEGER || type == PrimitiveTypes.FLOAT || type == PrimitiveTypes.BOOLEAN;
    }

    public static MegaType typeForClass(Class c) {
        if (c.equals(Integer.class) || c.getName().equals("int")) {
            return PrimitiveTypes.INTEGER;
        } else if (c.equals(Float.class) || c.getName().equals("float")) {
            return PrimitiveTypes.FLOAT;
        } else if (c.equals(Boolean.class) || c.getName().equals("boolean")) {
            return PrimitiveTypes.BOOLEAN;
        } else if (c.equals(String.class)) {
            return PrimitiveTypes.STRING;
        } else if (c.equals(Object.class)) {
            return PrimitiveTypes.ANY;
        } else if (c.equals(Void.class) || c.getName().equals("void")) {
            return PrimitiveTypes.UNIT;
        } else if (c.isArray()) {
            Class componentType = c.getComponentType();
            if (componentType == null) {
                return null;
            }
            return new ArrayType(typeForClass(componentType));
        } else {
            System.out.println("Warning: Cannot find MegaType for class: " + c);
            return null;
        }
    }

    public static FunctionType typeForMethod(Method method) {
        List<Parameter> params = Lists.newArrayList();
        for (java.lang.reflect.Parameter param : method.getParameters()) {
            String paramName = param.getName();
            MegaType type = typeForClass(param.getType());
            if (type == null) {
                System.out.println("Warning: Skipping method: " + method + " due to unsupported type: " + param.getType());
                continue;
            }

            params.add(new Parameter(
                new Identifier(
                    Token.ident(paramName, Position.at(-1, -1)),
                    paramName,
                    null,
                    type
                )
            ));
        }

        MegaType returnType = typeForClass(method.getReturnType());

        return new FunctionType(params, returnType, Kind.METHOD);
    }
}
