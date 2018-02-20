package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.util.OpcodeUtils.loadInsn;
import static java.util.stream.Collectors.joining;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.List;
import java.util.Map.Entry;

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.backend.compilation.util.OpcodeUtils;
import co.kenrg.mega.frontend.ast.statement.TypeDeclarationStatement;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import co.kenrg.mega.frontend.typechecking.types.StructType;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import mega.lang.collections.Arrays;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class TypeDeclarationStatementCompiler {
    public static List<Pair<String, byte[]>> compileTypeDeclaration(
        String outerClassName,
        String innerClassName,
        TypeDeclarationStatement node,
        TypeEnvironment typeEnv,
        int access
    ) {
        String typeClassName = node.typeName.value;
        MegaType type = typeEnv.getTypeByName(typeClassName);
        if (!(type instanceof StructType)) {
            // Don't generate classes for alias types
            return Lists.newArrayList();
        }
        Compiler compiler = new Compiler(innerClassName, null, getInternalName(Object.class), null, typeEnv);
        compiler.cw.visitInnerClass(innerClassName, outerClassName, typeClassName, access);

        StructType structType = (StructType) type;
        structType.setClassName(innerClassName);
        LinkedHashMultimap<String, MegaType> properties = structType.getProperties();

        writeClinitMethod(compiler, properties);
        writeInitMethod(innerClassName, compiler, properties);
        writeGetterMethods(innerClassName, compiler, properties);
        writeEqualsMethod(innerClassName, compiler, properties);
        writeHashCodeMethod(innerClassName, compiler, properties);
        writeToStringMethod(typeClassName, innerClassName, compiler, properties);

        compiler.cw.visitEnd();
        return compiler.results();
    }

    private static void writeClinitMethod(Compiler compiler, LinkedHashMultimap<String, MegaType> typeProperties) {
        for (Entry<String, MegaType> prop : typeProperties.entries()) {
            String propName = prop.getKey();
            MegaType propType = prop.getValue();
            compiler.cw.visitField(ACC_PRIVATE | ACC_FINAL, propName, jvmDescriptor(propType, false), null, null);
        }

        compiler.clinitWriter.visitInsn(RETURN);
        compiler.clinitWriter.visitMaxs(-1, -1);
        compiler.clinitWriter.visitEnd();
    }

    private static void writeInitMethod(String innerClassName, Compiler compiler, LinkedHashMultimap<String, MegaType> typeProperties) {
        String initDesc = typeProperties.entries().stream()
            .map(Entry::getValue)
            .map(type -> jvmDescriptor(type, false))
            .collect(joining("", "(", ")V"));
        MethodVisitor initWriter = compiler.cw.visitMethod(ACC_PUBLIC, "<init>", initDesc, null, null);
        initWriter.visitCode();
        initWriter.visitVarInsn(ALOAD, 0);
        initWriter.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        int index = 1;
        for (Entry<String, MegaType> prop : typeProperties.entries()) {
            String propName = prop.getKey();
            MegaType propType = prop.getValue();

            initWriter.visitVarInsn(ALOAD, 0);
            initWriter.visitVarInsn(loadInsn(propType), index);
            initWriter.visitFieldInsn(PUTFIELD, innerClassName, propName, jvmDescriptor(propType, false));
            index++;
        }

        initWriter.visitInsn(RETURN);
        initWriter.visitMaxs(2, 1);
        initWriter.visitEnd();
    }

    private static void writeGetterMethods(String innerClassName, Compiler compiler, LinkedHashMultimap<String, MegaType> typeProperties) {
        for (Entry<String, MegaType> prop : typeProperties.entries()) {
            String propName = prop.getKey();
            MegaType propType = prop.getValue();
            String propDesc = jvmDescriptor(propType, false);

            String methodName = "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
            String methodDesc = String.format("()%s", propDesc);
            MethodVisitor getterWriter = compiler.cw.visitMethod(ACC_PUBLIC | ACC_FINAL, methodName, methodDesc, null, null);
            getterWriter.visitVarInsn(ALOAD, 0);
            getterWriter.visitFieldInsn(GETFIELD, innerClassName, propName, propDesc);

            getterWriter.visitInsn(OpcodeUtils.returnInsn(propType));
            getterWriter.visitMaxs(1, 1);
            getterWriter.visitEnd();
        }
    }

    private static void writeEqualsMethod(String innerClassName, Compiler compiler, LinkedHashMultimap<String, MegaType> typeProperties) {
        MethodVisitor equalsWriter = compiler.cw.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
        equalsWriter.visitCode();

        Label trueLabel = new Label();
        Label falseLabel = new Label();

        equalsWriter.visitVarInsn(ALOAD, 0);
        equalsWriter.visitVarInsn(ALOAD, 1);
        equalsWriter.visitJumpInsn(IF_ACMPEQ, trueLabel);

        equalsWriter.visitVarInsn(ALOAD, 1);
        equalsWriter.visitTypeInsn(INSTANCEOF, innerClassName);
        equalsWriter.visitJumpInsn(IFEQ, falseLabel);

        equalsWriter.visitVarInsn(ALOAD, 1);
        equalsWriter.visitTypeInsn(CHECKCAST, innerClassName);
        equalsWriter.visitVarInsn(ASTORE, 2);

        for (Entry<String, MegaType> prop : typeProperties.entries()) {
            String propName = prop.getKey();
            MegaType propType = prop.getValue();
            String propDesc = jvmDescriptor(propType, false);

            equalsWriter.visitVarInsn(ALOAD, 0);
            equalsWriter.visitFieldInsn(GETFIELD, innerClassName, propName, propDesc);

            equalsWriter.visitVarInsn(ALOAD, 2);
            equalsWriter.visitFieldInsn(GETFIELD, innerClassName, propName, propDesc);

            if (propType == PrimitiveTypes.INTEGER) {
                equalsWriter.visitJumpInsn(IF_ICMPNE, falseLabel);
            } else if (propType == PrimitiveTypes.BOOLEAN) {
                equalsWriter.visitJumpInsn(IF_ICMPNE, falseLabel);
            } else if (propType == PrimitiveTypes.FLOAT) {
                equalsWriter.visitInsn(FCMPL);
                equalsWriter.visitJumpInsn(IFNE, falseLabel);
            } else if (propType instanceof ArrayType) {
                equalsWriter.visitMethodInsn(INVOKESTATIC, getInternalName(java.util.Arrays.class), "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
                equalsWriter.visitJumpInsn(IFEQ, falseLabel);
            } else {
                equalsWriter.visitMethodInsn(INVOKEVIRTUAL, propDesc, "equals", "(Ljava/lang/Object;)Z", false);
                equalsWriter.visitJumpInsn(IFEQ, falseLabel);
            }
        }

        equalsWriter.visitLabel(trueLabel);
        equalsWriter.visitInsn(ICONST_1);
        equalsWriter.visitInsn(IRETURN);

        equalsWriter.visitLabel(falseLabel);
        equalsWriter.visitInsn(ICONST_0);
        equalsWriter.visitInsn(IRETURN);

        equalsWriter.visitMaxs(2, 3);
        equalsWriter.visitEnd();
    }

    private static void writeHashCodeMethod(String innerClassName, Compiler compiler, LinkedHashMultimap<String, MegaType> typeProperties) {
        MethodVisitor hashCodeWriter = compiler.cw.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        hashCodeWriter.visitCode();

        List<Entry<String, MegaType>> entries = Lists.newArrayList(typeProperties.entries());
        for (int i = 0; i < typeProperties.size(); i++) {
            Entry<String, MegaType> prop = entries.get(i);
            String propName = prop.getKey();
            MegaType propType = prop.getValue();
            String propDesc = jvmDescriptor(propType, false);

            if (i > 0) {
                hashCodeWriter.visitVarInsn(ILOAD, 1);
                hashCodeWriter.visitIntInsn(BIPUSH, 23);
                hashCodeWriter.visitInsn(IMUL);
            }

            hashCodeWriter.visitVarInsn(ALOAD, 0);
            hashCodeWriter.visitFieldInsn(GETFIELD, innerClassName, propName, propDesc);
            if (isPrimitive(propType)) {
                String desc = String.format("(%s)I", jvmDescriptor(propType, false));
                hashCodeWriter.visitMethodInsn(INVOKESTATIC, getInternalName(propType), "hashCode", desc, false);
            } else if (propType instanceof ArrayType) {
                hashCodeWriter.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I", false);
            } else {
                hashCodeWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);
            }
            if (i > 0) {
                hashCodeWriter.visitInsn(IADD);
            }
            hashCodeWriter.visitVarInsn(ISTORE, 1);
        }

        hashCodeWriter.visitVarInsn(ILOAD, 1);
        hashCodeWriter.visitInsn(IRETURN);
        hashCodeWriter.visitMaxs(2, 2);
        hashCodeWriter.visitEnd();
    }

    private static void writeToStringMethod(String typeClassName, String innerClassName, Compiler compiler, LinkedHashMultimap<String, MegaType> typeProperties) {
        MethodVisitor toStringWriter = compiler.cw.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        toStringWriter.visitCode();
        toStringWriter.visitTypeInsn(NEW, "java/lang/StringBuilder");
        toStringWriter.visitInsn(DUP);
        toStringWriter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        toStringWriter.visitLdcInsn(typeClassName + " { ");
        toStringWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        List<Entry<String, MegaType>> entries = Lists.newArrayList(typeProperties.entries());
        for (int i = 0; i < typeProperties.size(); i++) {
            Entry<String, MegaType> prop = entries.get(i);
            String propName = prop.getKey();
            MegaType propType = prop.getValue();
            String propDesc = jvmDescriptor(propType, false);

            toStringWriter.visitLdcInsn(propName + ": ");
            toStringWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

            if (propType == PrimitiveTypes.STRING) {
                toStringWriter.visitLdcInsn("\"");
                toStringWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            }
            toStringWriter.visitVarInsn(ALOAD, 0);
            toStringWriter.visitFieldInsn(GETFIELD, innerClassName, propName, propDesc);
            String appendDesc;
            if (isPrimitive(propType)) {
                appendDesc = String.format("(%s)Ljava/lang/StringBuilder;", propDesc);
            } else if (propType instanceof ArrayType) {
                if (((ArrayType) propType).typeArg == PrimitiveTypes.STRING) {
                    toStringWriter.visitMethodInsn(INVOKESTATIC, getInternalName(Arrays.class), "strArrayToString", "([Ljava/lang/String;)Ljava/lang/String;", false);
                } else {
                    toStringWriter.visitMethodInsn(INVOKESTATIC, getInternalName(Arrays.class), "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
                }
                appendDesc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
            } else {
                appendDesc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
            }
            toStringWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", appendDesc, false);
            if (propType == PrimitiveTypes.STRING) {
                toStringWriter.visitLdcInsn("\"");
                toStringWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            }

            if (i == typeProperties.size() - 1) {
                toStringWriter.visitLdcInsn(" }");
            } else {
                toStringWriter.visitLdcInsn(", ");
            }
            toStringWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        }
        toStringWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        toStringWriter.visitInsn(ARETURN);
        toStringWriter.visitMaxs(2, 1);
        toStringWriter.visitEnd();
    }
}
