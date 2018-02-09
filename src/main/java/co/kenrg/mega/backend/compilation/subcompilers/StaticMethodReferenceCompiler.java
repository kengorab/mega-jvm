package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.getCompiler;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.getInvokeMethodDesc;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeClinitMethod;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeIfaceInvokeMethod;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeInitMethod;
import static co.kenrg.mega.backend.compilation.util.OpcodeUtils.loadInsn;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.List;

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.backend.compilation.scope.Binding;
import co.kenrg.mega.backend.compilation.scope.BindingTypes;
import co.kenrg.mega.backend.compilation.scope.Context;
import co.kenrg.mega.backend.compilation.scope.FocusedMethod;
import co.kenrg.mega.backend.compilation.util.OpcodeUtils;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.MethodVisitor;

public class StaticMethodReferenceCompiler {
    public static List<Pair<String, byte[]>> compileMethodReference(
        String outerClassName,
        String lambdaName,
        String innerClassName,
        Binding binding,
        TypeEnvironment typeEnv,
        Context context,
        int access
    ) {
        FunctionType methodType = (FunctionType) binding.type;
        assert methodType != null; // Should be populated in typechecking pass

        Compiler compiler = getCompiler(innerClassName, methodType, typeEnv, context);
        compiler.cw.visitInnerClass(innerClassName, outerClassName, lambdaName, access);

        writeClinitMethod(compiler, innerClassName);
        writeInitMethod(compiler, methodType.arity());
        writeIfaceInvokeMethod(compiler, innerClassName, methodType);

        String methodName = binding.isExported ? binding.name : binding.name + "$access";
        writeActualInvokeMethod(compiler, methodType, outerClassName, methodName);

        compiler.cw.visitEnd();
        return compiler.results();
    }

    private static void writeActualInvokeMethod(Compiler compiler, FunctionType methodType, String outerClassName, String methodName) {
        MethodVisitor invokeMethodWriter = compiler.cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "invoke", getInvokeMethodDesc(methodType), null, null);
        invokeMethodWriter.visitCode();

        compiler.scope = compiler.scope.createChild(new FocusedMethod(invokeMethodWriter, null, null)); // TODO: Fix this, it's a little awkward...
        compiler.scope.addBinding("this", PrimitiveTypes.ANY, BindingTypes.LOCAL, false); // TODO: Fix this; this is terrible

        List<MegaType> paramTypes = methodType.paramTypes;
        for (int i = 0; i < paramTypes.size(); i++) {
            MegaType paramType = paramTypes.get(i);
            String paramName = "$p" + i; // The name doesn't matter, just used for tracking bindings during debugging

            compiler.scope.addBinding(paramName, paramType, BindingTypes.LOCAL, false);
            Binding binding = compiler.scope.getBinding(paramName);
            assert binding != null; // It was just added above, I don't think there's a way for it to be null...
            invokeMethodWriter.visitVarInsn(loadInsn(binding.type), binding.index);
        }

        String methodDesc = jvmMethodDescriptor(methodType, false);
        invokeMethodWriter.visitMethodInsn(INVOKESTATIC, outerClassName, methodName, methodDesc, false);
        invokeMethodWriter.visitInsn(OpcodeUtils.returnInsn(methodType.returnType));

        invokeMethodWriter.visitMaxs(2, 2);
        invokeMethodWriter.visitEnd();
    }
}
