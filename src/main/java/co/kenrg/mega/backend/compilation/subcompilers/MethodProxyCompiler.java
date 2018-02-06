package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;

import java.util.List;
import java.util.function.BiConsumer;

import co.kenrg.mega.backend.compilation.FocusedMethod;
import co.kenrg.mega.backend.compilation.Scope;
import co.kenrg.mega.backend.compilation.Scope.BindingTypes;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Lists;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class MethodProxyCompiler {
    private static final String PROXY_SUFFIX = "$proxy";

    public static void compileFuncProxy(
        String className,
        ClassWriter cw,
        FunctionType fnType,
        String methodName,
        Scope scope,
        BiConsumer<Node, Scope> compileNode
    ) {
        FunctionType funcProxyType = getMethodProxyType(fnType);
        String methodProxyName = methodName + MethodProxyCompiler.PROXY_SUFFIX;
        String funcProxyDesc = jvmMethodDescriptor(funcProxyType, false);

        MethodVisitor proxyWriter = cw.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, methodProxyName, funcProxyDesc, null, null);
        proxyWriter.visitCode();

        int idxBitmask = funcProxyType.arity() - 1; // Param which represents which parameters' bitmask

        for (int i = 0; i < fnType.arity(); i++) {
            Parameter parameter = fnType.parameters.get(i);
            if (!parameter.hasDefaultValue()) {
                continue;
            }

            proxyWriter.visitVarInsn(ILOAD, idxBitmask);
            proxyWriter.visitLdcInsn((int) Math.pow(2, i));
            proxyWriter.visitInsn(IAND);

            Label l0 = new Label();
            proxyWriter.visitJumpInsn(IFEQ, l0);

            compileNode.accept(parameter.defaultValue, scope.createChild(new FocusedMethod(proxyWriter, null, null)));
            if (parameter.ident.getType() instanceof FunctionType) {
                proxyWriter.visitTypeInsn(CHECKCAST, getInternalName(parameter.ident.getType().typeClass()));
            }

            MegaType paramType = parameter.ident.getType();
            assert paramType != null;
            if (paramType == PrimitiveTypes.INTEGER) {
                proxyWriter.visitVarInsn(ISTORE, i);
            } else if (paramType == PrimitiveTypes.BOOLEAN) {
                proxyWriter.visitVarInsn(ISTORE, i);
            } else if (paramType == PrimitiveTypes.FLOAT) {
                proxyWriter.visitVarInsn(FSTORE, i);
            } else {
                proxyWriter.visitVarInsn(ASTORE, i);
            }
            proxyWriter.visitLabel(l0);
            proxyWriter.visitFrame(F_SAME, 0, null, 0, null);
        }

        for (int i = 0; i < fnType.arity(); i++) {
            Parameter parameter = fnType.parameters.get(i);
            MegaType paramType = parameter.ident.getType();
            assert paramType != null;
            if (paramType == PrimitiveTypes.INTEGER) {
                proxyWriter.visitVarInsn(ILOAD, i);
            } else if (paramType == PrimitiveTypes.BOOLEAN) {
                proxyWriter.visitVarInsn(ILOAD, i);
            } else if (paramType == PrimitiveTypes.FLOAT) {
                proxyWriter.visitVarInsn(FLOAD, i);
            } else {
                proxyWriter.visitVarInsn(ALOAD, i);
            }
        }

        String fnDesc = jvmMethodDescriptor(fnType, false);
        proxyWriter.visitMethodInsn(INVOKESTATIC, className, methodName, fnDesc, false);

        MegaType returnType = fnType.returnType;
        if (returnType == PrimitiveTypes.INTEGER) {
            proxyWriter.visitInsn(IRETURN);
        } else if (returnType == PrimitiveTypes.BOOLEAN) {
            proxyWriter.visitInsn(IRETURN);
        } else if (returnType == PrimitiveTypes.FLOAT) {
            proxyWriter.visitInsn(FRETURN);
        } else {
            proxyWriter.visitInsn(ARETURN);
        }

        proxyWriter.visitMaxs(-1, -1);
        proxyWriter.visitEnd();

        scope.addBinding(methodProxyName, funcProxyType, BindingTypes.METHOD, false);
    }

    static FunctionType getMethodProxyType(FunctionType arrowFnType) {
        List<MegaType> paramTypes = Lists.newArrayList(arrowFnType.paramTypes);
        paramTypes.add(PrimitiveTypes.INTEGER);

        return FunctionType.ofSignature(paramTypes, arrowFnType.returnType);
    }
}
