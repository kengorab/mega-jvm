package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getDescriptor;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileBoxPrimitiveType;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileUnboxPrimitiveType;
import static java.util.stream.Collectors.joining;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.backend.compilation.FocusedMethod;
import co.kenrg.mega.backend.compilation.Scope.BindingTypes;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.base.Strings;
import mega.lang.functions.Invokeable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ArrowFunctionExpressionCompiler {
    public static Compiler compileArrowFunction(String outerClassName, String lambdaName, String innerClassName, ArrowFunctionExpression node, TypeEnvironment typeEnv) {
        FunctionType arrowFnType = (FunctionType) node.getType();
        assert arrowFnType != null; // Should be populated in typechecking pass

        Compiler compiler = getCompiler(innerClassName, arrowFnType, typeEnv);
        compiler.cw.visitInnerClass(innerClassName, outerClassName, lambdaName, ACC_FINAL | ACC_STATIC);

        writeClinitMethod(compiler, innerClassName);
        writeInitMethod(compiler);
        writeIfaceInvokeMethod(compiler, innerClassName, arrowFnType);
        writeActualInvokeMethod(compiler, node, arrowFnType);

        compiler.cw.visitEnd();

        return compiler;
    }

    private static Compiler getCompiler(String innerClassName, FunctionType arrowFnType, TypeEnvironment typeEnv) {
        String paramTypeDescs = arrowFnType.paramTypes.stream()
            .map(type -> jvmDescriptor(type, true))
            .collect(joining(""));
        String returnTypeDesc = jvmDescriptor(arrowFnType.returnType, true);
        String functionDescTypeArgs = String.format("<%s%s>", paramTypeDescs, returnTypeDesc);

        Class fnClass = arrowFnType.typeClass();
        String functionDesc = String.format("%s%s", getDescriptor(fnClass), functionDescTypeArgs);
        String functionIfaceName = getInternalName(fnClass);
        String arrowFnSignature = String.format("%s;%s;", getDescriptor(Invokeable.class), functionDesc);
        return new Compiler(innerClassName, arrowFnSignature, getInternalName(Invokeable.class), new String[]{functionIfaceName}, typeEnv);
    }

    private static void writeClinitMethod(Compiler compiler, String innerClassName) {
        compiler.clinitWriter.visitTypeInsn(NEW, innerClassName);
        compiler.clinitWriter.visitInsn(DUP);
        compiler.clinitWriter.visitMethodInsn(INVOKESPECIAL, innerClassName, "<init>", "()V", false);
        compiler.cw.visitField(ACC_PUBLIC | ACC_STATIC, "INSTANCE", "L" + innerClassName + ";", null, null);
        compiler.clinitWriter.visitFieldInsn(PUTSTATIC, innerClassName, "INSTANCE", "L" + innerClassName + ";");
        compiler.clinitWriter.visitInsn(RETURN);
        compiler.clinitWriter.visitMaxs(-1, -1);
        compiler.clinitWriter.visitEnd();
    }

    private static void writeInitMethod(Compiler compiler) {
        MethodVisitor initWriter = compiler.cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        initWriter.visitCode();
        initWriter.visitVarInsn(ALOAD, 0);
        initWriter.visitInsn(ICONST_0);
        initWriter.visitMethodInsn(INVOKESPECIAL, "mega/lang/functions/Invokeable", "<init>", "(I)V", false);
        initWriter.visitInsn(RETURN);
        initWriter.visitMaxs(2, 1);
        initWriter.visitEnd();
    }

    private static void writeIfaceInvokeMethod(Compiler compiler, String innerClassName, FunctionType arrowFnType) {
        String ifaceInvokeDesc = String.format("(%s)Ljava/lang/Object;", Strings.repeat("Ljava/lang/Object;", arrowFnType.arity()));
        MethodVisitor ifaceInvokeWriter = compiler.cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "invoke", ifaceInvokeDesc, null, null);
        ifaceInvokeWriter.visitCode();

        ifaceInvokeWriter.visitVarInsn(ALOAD, 0); // Load `this` (`this` is 0th local within non-static method)
        for (int i = 0; i < arrowFnType.paramTypes.size(); i++) {
            ifaceInvokeWriter.visitVarInsn(ALOAD, i + 1); // Load subsequent parameters

            MegaType paramType = arrowFnType.paramTypes.get(i);
            if (paramType == PrimitiveTypes.INTEGER) {
                ifaceInvokeWriter.visitTypeInsn(CHECKCAST, Type.getInternalName(Integer.class));
                compileBoxPrimitiveType(paramType, ifaceInvokeWriter);
            } else if (paramType == PrimitiveTypes.BOOLEAN) {
                ifaceInvokeWriter.visitTypeInsn(CHECKCAST, Type.getInternalName(Boolean.class));
                compileBoxPrimitiveType(paramType, ifaceInvokeWriter);
            } else if (paramType == PrimitiveTypes.FLOAT) {
                ifaceInvokeWriter.visitTypeInsn(CHECKCAST, Type.getInternalName(Float.class));
                compileBoxPrimitiveType(paramType, ifaceInvokeWriter);
            } else {
                ifaceInvokeWriter.visitVarInsn(ALOAD, i + 1);
            }
        }
        ifaceInvokeWriter.visitMethodInsn(INVOKEVIRTUAL, innerClassName, "invoke", getInvokeMethodDesc(arrowFnType), false);

        assert arrowFnType.returnType != null; // Should have been populated during typechecking pass
        if (isPrimitive(arrowFnType.returnType)) {
            compileUnboxPrimitiveType(arrowFnType.returnType, ifaceInvokeWriter);
        }

        ifaceInvokeWriter.visitInsn(ARETURN);
        ifaceInvokeWriter.visitMaxs(-1, -1);
        ifaceInvokeWriter.visitEnd();
    }

    private static String getInvokeMethodDesc(FunctionType arrowFnType) {
        String paramTypeDescs = arrowFnType.paramTypes.stream()
            .map(type -> jvmDescriptor(type, false))
            .collect(joining(""));
        String returnTypeDesc = jvmDescriptor(arrowFnType.returnType, false);
        return String.format("(%s)%s", paramTypeDescs, returnTypeDesc);
    }

    private static void writeActualInvokeMethod(Compiler compiler, ArrowFunctionExpression node, FunctionType arrowFnType) {
        MethodVisitor invokeMethodWriter = compiler.cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "invoke", getInvokeMethodDesc(arrowFnType), null, null);
        invokeMethodWriter.visitCode();

        compiler.scope = compiler.scope.createChild(new FocusedMethod(invokeMethodWriter, null, null)); // TODO: Fix this, it's a little awkward...
        compiler.scope.addBinding("this", PrimitiveTypes.ANY, BindingTypes.LOCAL, false); // TODO: Fix this; this is terrible

        for (Identifier parameter : node.parameters) {
            compiler.scope.addBinding(parameter.value, parameter.getType(), BindingTypes.LOCAL, false);
        }
        compiler.compileNode(node.body);
        if (arrowFnType.returnType == PrimitiveTypes.INTEGER) {
            invokeMethodWriter.visitInsn(IRETURN);
        } else if (arrowFnType.returnType == PrimitiveTypes.BOOLEAN) {
            invokeMethodWriter.visitInsn(IRETURN);
        } else if (arrowFnType.returnType == PrimitiveTypes.FLOAT) {
            invokeMethodWriter.visitInsn(FRETURN);
        } else {
            invokeMethodWriter.visitInsn(ARETURN);
        }

        invokeMethodWriter.visitMaxs(2, 2);
        invokeMethodWriter.visitEnd();
    }
}
