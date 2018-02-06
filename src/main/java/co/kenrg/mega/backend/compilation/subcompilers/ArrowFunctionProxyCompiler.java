package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.getCompiler;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeClinitMethod;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeInitMethod;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileBoxPrimitiveType;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileUnboxPrimitiveType;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.ISTORE;

import java.util.List;

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.backend.compilation.scope.FocusedMethod;
import co.kenrg.mega.backend.compilation.scope.Scope;
import co.kenrg.mega.backend.compilation.scope.Context;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.Parameter;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ArrowFunctionProxyCompiler {
    public static final String PROXY_SUFFIX = "$proxy";

    public static List<Pair<String, byte[]>> compileArrowFunctionProxy(
        String outerClassName,
        String lambdaName,
        String innerClassName,
        ArrowFunctionExpression node,
        TypeEnvironment typeEnv,
        Context context
    ) {
        FunctionType arrowFnType = (FunctionType) node.getType();
        assert arrowFnType != null; // Should be populated in typechecking pass
        FunctionType arrowFnProxyType = getArrowFnProxyType(arrowFnType);

        Compiler compiler = getCompiler(innerClassName, arrowFnProxyType, typeEnv, context);
        compiler.cw.visitInnerClass(innerClassName, outerClassName, lambdaName, ACC_FINAL | ACC_STATIC);

        writeClinitMethod(compiler, innerClassName);
        writeInitMethod(compiler, arrowFnProxyType.arity());
        writeIfaceInvokeMethod(compiler, arrowFnProxyType, arrowFnType);

        compiler.cw.visitEnd();
        return compiler.results();
    }

    public static FunctionType getArrowFnProxyType(FunctionType arrowFnType) {
        List<MegaType> paramTypes = Lists.newArrayList(arrowFnType.paramTypes);
        paramTypes.add(arrowFnType);
        paramTypes.add(PrimitiveTypes.INTEGER);

        return FunctionType.ofSignature(paramTypes, arrowFnType.returnType);
    }

    private static void writeIfaceInvokeMethod(Compiler compiler, FunctionType arrowFnProxyType, FunctionType proxiedArrowFnType) {
        String ifaceInvokeDesc = String.format("(%s)Ljava/lang/Object;", Strings.repeat("Ljava/lang/Object;", arrowFnProxyType.arity()));
        MethodVisitor ifaceInvokeWriter = compiler.cw.visitMethod(ACC_PUBLIC, "invoke", ifaceInvokeDesc, null, null);
        ifaceInvokeWriter.visitCode();

        int idxBitmask = arrowFnProxyType.arity();
        ifaceInvokeWriter.visitVarInsn(ALOAD, idxBitmask); // Param which represents which parameters bitmask
        ifaceInvokeWriter.visitTypeInsn(CHECKCAST, getInternalName(Integer.class));
        compileUnboxPrimitiveType(PrimitiveTypes.INTEGER, ifaceInvokeWriter);
        idxBitmask = arrowFnProxyType.arity() + 1; // Store tmp var in next available slot
        ifaceInvokeWriter.visitVarInsn(ISTORE, idxBitmask);

        for (int i = 1; i <= proxiedArrowFnType.arity(); i++) { // Start at 1 because idx 0 is `this`
            ifaceInvokeWriter.visitVarInsn(ILOAD, idxBitmask);
            ifaceInvokeWriter.visitLdcInsn((int) Math.pow(2, i - 1));
            ifaceInvokeWriter.visitInsn(IAND);
            Label l0 = new Label();
            ifaceInvokeWriter.visitJumpInsn(IFEQ, l0);

            Scope origScope = compiler.scope;
            compiler.scope = compiler.scope.createChild(new FocusedMethod(ifaceInvokeWriter, null, null)); // TODO: Fix this, it's a little awkward...
            Parameter param = proxiedArrowFnType.parameters.get(i - 1);
            compiler.compileNode(param.defaultValue);
            compiler.scope = origScope;

            MegaType paramType = param.ident.getType();
            assert paramType != null;
            if (isPrimitive(paramType)) {
                compileBoxPrimitiveType(paramType, ifaceInvokeWriter);
            }
            ifaceInvokeWriter.visitVarInsn(ASTORE, i);

            ifaceInvokeWriter.visitLabel(l0);
            ifaceInvokeWriter.visitFrame(F_SAME, 0, null, 0, null);
        }

        int idxProxiedFn = arrowFnProxyType.arity() - 1;
        ifaceInvokeWriter.visitVarInsn(ALOAD, idxProxiedFn);

        for (int i = 1; i <= proxiedArrowFnType.arity(); i++) {
            ifaceInvokeWriter.visitVarInsn(ALOAD, i);
        }

        String invokeDesc = String.format("(%s)Ljava/lang/Object;", Strings.repeat("Ljava/lang/Object;", proxiedArrowFnType.arity()));
        ifaceInvokeWriter.visitMethodInsn(INVOKEINTERFACE, getInternalName(proxiedArrowFnType.typeClass()), "invoke", invokeDesc, true);

        ifaceInvokeWriter.visitInsn(ARETURN);
        ifaceInvokeWriter.visitMaxs(-1, -1);
        ifaceInvokeWriter.visitEnd();
    }
}
