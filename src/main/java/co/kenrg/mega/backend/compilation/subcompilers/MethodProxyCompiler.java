package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static co.kenrg.mega.backend.compilation.util.OpcodeUtils.loadInsn;
import static co.kenrg.mega.backend.compilation.util.OpcodeUtils.storeInsn;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.List;
import java.util.function.BiConsumer;

import co.kenrg.mega.backend.compilation.scope.BindingTypes;
import co.kenrg.mega.backend.compilation.scope.FocusedMethod;
import co.kenrg.mega.backend.compilation.scope.Scope;
import co.kenrg.mega.backend.compilation.util.OpcodeUtils;
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
    static final String PROXY_SUFFIX = "$proxy";

    public static void compileFuncProxy(
        String className,
        ClassWriter cw,
        FunctionType fnType,
        String methodName,
        String proxiedMethodName,
        Scope scope,
        BiConsumer<Node, Scope> compileNode
    ) {
        FunctionType funcProxyType = getMethodProxyType(fnType);
        String methodProxyName = methodName + PROXY_SUFFIX;
        String funcProxyDesc = jvmMethodDescriptor(funcProxyType, false);

        int access = ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC;
        MethodVisitor proxyWriter = cw.visitMethod(access, methodProxyName, funcProxyDesc, null, null);
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
                proxyWriter.visitTypeInsn(CHECKCAST, getInternalName(parameter.getType()));
            }

            MegaType paramType = parameter.getType();
            assert paramType != null;
            proxyWriter.visitVarInsn(storeInsn(paramType), i);
            proxyWriter.visitLabel(l0);
            proxyWriter.visitFrame(F_SAME, 0, null, 0, null);
        }

        for (int i = 0; i < fnType.arity(); i++) {
            Parameter parameter = fnType.parameters.get(i);
            MegaType paramType = parameter.getType();
            assert paramType != null;
            proxyWriter.visitVarInsn(loadInsn(paramType), i);
        }

        String fnDesc = jvmMethodDescriptor(fnType, false);
        proxyWriter.visitMethodInsn(INVOKESTATIC, className, proxiedMethodName, fnDesc, false);

        MegaType returnType = fnType.returnType;
        proxyWriter.visitInsn(OpcodeUtils.returnInsn(returnType));

        proxyWriter.visitMaxs(-1, -1);
        proxyWriter.visitEnd();

        scope.addBinding(methodProxyName, funcProxyType, className, BindingTypes.METHOD, false);
    }

    static FunctionType getMethodProxyType(FunctionType arrowFnType) {
        List<MegaType> paramTypes = Lists.newArrayList(arrowFnType.paramTypes);
        paramTypes.add(PrimitiveTypes.INTEGER);

        return FunctionType.ofSignature(paramTypes, arrowFnType.returnType);
    }
}
