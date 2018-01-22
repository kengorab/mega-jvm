package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.getCompiler;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.getInvokeMethodDesc;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeClinitMethod;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeIfaceInvokeMethod;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeInitMethod;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;

import java.util.List;

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.backend.compilation.FocusedMethod;
import co.kenrg.mega.backend.compilation.Scope.Binding;
import co.kenrg.mega.backend.compilation.Scope.BindingTypes;
import co.kenrg.mega.backend.compilation.Scope.Context;
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
        Context context
    ) {
        FunctionType methodType = (FunctionType) binding.type;
        assert methodType != null; // Should be populated in typechecking pass

        Compiler compiler = getCompiler(innerClassName, methodType, typeEnv, context);
        compiler.cw.visitInnerClass(innerClassName, outerClassName, lambdaName, ACC_FINAL | ACC_STATIC);

        writeClinitMethod(compiler, innerClassName);
        writeInitMethod(compiler, methodType);
        writeIfaceInvokeMethod(compiler, innerClassName, methodType);
        writeActualInvokeMethod(compiler, methodType, outerClassName, binding.name);

        compiler.cw.visitEnd();
        return compiler.results();
    }

    private static void writeActualInvokeMethod(Compiler compiler, FunctionType methodType, String outerClassName, String name) {
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

            if (binding.type == PrimitiveTypes.INTEGER) {
                invokeMethodWriter.visitVarInsn(ILOAD, binding.index);
            } else if (binding.type == PrimitiveTypes.BOOLEAN) {
                invokeMethodWriter.visitVarInsn(ILOAD, binding.index);
            } else if (binding.type == PrimitiveTypes.FLOAT) {
                invokeMethodWriter.visitVarInsn(FLOAD, binding.index);
            } else {
                invokeMethodWriter.visitVarInsn(ALOAD, binding.index);
            }
        }

        String methodDesc = jvmMethodDescriptor(methodType, false);
        invokeMethodWriter.visitMethodInsn(INVOKESTATIC, outerClassName, name, methodDesc, false);

        if (methodType.returnType == PrimitiveTypes.INTEGER) {
            invokeMethodWriter.visitInsn(IRETURN);
        } else if (methodType.returnType == PrimitiveTypes.BOOLEAN) {
            invokeMethodWriter.visitInsn(IRETURN);
        } else if (methodType.returnType == PrimitiveTypes.FLOAT) {
            invokeMethodWriter.visitInsn(FRETURN);
        } else {
            invokeMethodWriter.visitInsn(ARETURN);
        }

        invokeMethodWriter.visitMaxs(2, 2);
        invokeMethodWriter.visitEnd();
    }
}
