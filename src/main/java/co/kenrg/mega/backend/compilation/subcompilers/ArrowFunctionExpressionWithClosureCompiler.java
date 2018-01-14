package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getDescriptor;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.getInvokeMethodDesc;
import static co.kenrg.mega.backend.compilation.subcompilers.ArrowFunctionExpressionCompiler.writeIfaceInvokeMethod;
import static java.util.stream.Collectors.joining;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.List;
import java.util.Map.Entry;

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.backend.compilation.FocusedMethod;
import co.kenrg.mega.backend.compilation.Scope.BindingTypes;
import co.kenrg.mega.backend.compilation.Scope.Context;
import co.kenrg.mega.frontend.ast.expression.ArrowFunctionExpression;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment.Binding;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import mega.lang.functions.Invokeable;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.MethodVisitor;

public class ArrowFunctionExpressionWithClosureCompiler {
    public static List<Pair<String, byte[]>> compileArrowFunctionWithClosure(String outerClassName, String lambdaName, String innerClassName, ArrowFunctionExpression node, TypeEnvironment typeEnv, Context context) {
        FunctionType arrowFnType = (FunctionType) node.getType();
        assert arrowFnType != null; // Should be populated in typechecking pass
        List<Entry<String, Binding>> capturedBindings = arrowFnType.getCapturedBindings();

        Compiler compiler = getCompiler(innerClassName, arrowFnType, typeEnv, context);
        compiler.cw.visitInnerClass(innerClassName, outerClassName, lambdaName, ACC_FINAL | ACC_STATIC);

        writeClinitMethod(compiler, innerClassName, capturedBindings);
        writeInitMethod(compiler, arrowFnType, innerClassName, capturedBindings);
        writeIfaceInvokeMethod(compiler, innerClassName, arrowFnType);
        writeActualInvokeMethod(compiler, node, arrowFnType, innerClassName, capturedBindings);

        compiler.cw.visitEnd();
        return compiler.results();
    }

    private static Compiler getCompiler(String innerClassName, FunctionType arrowFnType, TypeEnvironment typeEnv, Context context) {
        String paramTypeDescs = arrowFnType.paramTypes.stream()
            .map(type -> jvmDescriptor(type, true))
            .collect(joining(""));
        String returnTypeDesc = jvmDescriptor(arrowFnType.returnType, true);
        String functionDescTypeArgs = String.format("<%s%s>", paramTypeDescs, returnTypeDesc);

        Class fnClass = arrowFnType.typeClass();
        String desc = getDescriptor(fnClass);
        String functionDesc = String.format("%s%s;", desc.substring(0, desc.length() - 1), functionDescTypeArgs);
        String functionIfaceName = getInternalName(fnClass);
        String arrowFnSignature = String.format("%s%s", getDescriptor(Invokeable.class), functionDesc);
        Compiler compiler = new Compiler(innerClassName, arrowFnSignature, getInternalName(Invokeable.class), new String[]{functionIfaceName}, typeEnv);
        compiler.scope.context = context;
        return compiler;
    }

    private static void writeClinitMethod(Compiler compiler, String innerClassName, List<Entry<String, Binding>> capturedBindings) {
//        compiler.clinitWriter.visitTypeInsn(NEW, innerClassName);
//        compiler.clinitWriter.visitInsn(DUP);
//        compiler.clinitWriter.visitMethodInsn(INVOKESPECIAL, innerClassName, "<init>", "()V", false);
//        compiler.cw.visitField(ACC_PUBLIC | ACC_STATIC, "INSTANCE", "L" + innerClassName + ";", null, null);
//        compiler.clinitWriter.visitFieldInsn(PUTSTATIC, innerClassName, "INSTANCE", "L" + innerClassName + ";");
        for (Entry<String, Binding> capturedBinding : capturedBindings) {
            Binding binding = capturedBinding.getValue();
            String fieldName = "$" + capturedBinding.getKey();
            String fieldDesc = jvmDescriptor(binding.type, false);
            compiler.cw.visitField(ACC_FINAL | ACC_SYNTHETIC, fieldName, fieldDesc, null, null);
        }
        compiler.clinitWriter.visitInsn(RETURN);
        compiler.clinitWriter.visitMaxs(-1, -1);
        compiler.clinitWriter.visitEnd();
    }

    public static String getInitMethodDesc(List<Entry<String, Binding>> capturedBindings) {
        StringBuilder initDescBuilder = new StringBuilder("(");
        for (Entry<String, Binding> capturedBinding : capturedBindings) {
            String bindingDesc = jvmDescriptor(capturedBinding.getValue().type, false);
            initDescBuilder.append(bindingDesc);
        }
        initDescBuilder.append(")V");
        return initDescBuilder.toString();
    }

    private static void writeInitMethod(Compiler compiler, FunctionType arrowFnType, String innerClassName, List<Entry<String, Binding>> capturedBindings) {
        String initDesc = getInitMethodDesc(capturedBindings);
        MethodVisitor initWriter = compiler.cw.visitMethod(ACC_PUBLIC, "<init>", initDesc, null, null);
        initWriter.visitCode();

        int index = 1;
        for (Entry<String, Binding> capturedBinding : capturedBindings) {
            initWriter.visitVarInsn(ALOAD, 0);

            Binding binding = capturedBinding.getValue();
            if (binding.type == PrimitiveTypes.INTEGER) {
                initWriter.visitVarInsn(ILOAD, index);
            } else if (binding.type == PrimitiveTypes.BOOLEAN) {
                initWriter.visitVarInsn(ILOAD, index);
            } else if (binding.type == PrimitiveTypes.FLOAT) {
                initWriter.visitVarInsn(FLOAD, index);
            } else {
                initWriter.visitVarInsn(ALOAD, index);
            }
            index++;
            String fieldName = "$" + capturedBinding.getKey();
            String fieldDesc = jvmDescriptor(binding.type, false);
            initWriter.visitFieldInsn(PUTFIELD, innerClassName, fieldName, fieldDesc);
        }

        initWriter.visitVarInsn(ALOAD, 0);

        int arity = arrowFnType.arity();
        if (0 <= arity && arity <= 5) {
            initWriter.visitInsn(arity + ICONST_0);
        } else {
            initWriter.visitLdcInsn(arity);
        }
        initWriter.visitMethodInsn(INVOKESPECIAL, "mega/lang/functions/Invokeable", "<init>", "(I)V", false);
        initWriter.visitInsn(RETURN);
        initWriter.visitMaxs(2, 1);
        initWriter.visitEnd();
    }

    private static void writeActualInvokeMethod(Compiler compiler, ArrowFunctionExpression node, FunctionType arrowFnType, String innerClassName, List<Entry<String, Binding>> capturedBindings) {
        MethodVisitor invokeMethodWriter = compiler.cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "invoke", getInvokeMethodDesc(arrowFnType), null, null);
        invokeMethodWriter.visitCode();

        compiler.scope = compiler.scope.createChild(new FocusedMethod(invokeMethodWriter, null, null)); // TODO: Fix this, it's a little awkward...
        compiler.scope.addBinding("this", PrimitiveTypes.ANY, BindingTypes.LOCAL, false); // TODO: Fix this; this is terrible

        for (Identifier parameter : node.parameters) {
            compiler.scope.addBinding(parameter.value, parameter.getType(), BindingTypes.LOCAL, false);
        }

        for (Entry<String, Binding> capturedBinding : capturedBindings) {
            Binding binding = capturedBinding.getValue();
            invokeMethodWriter.visitVarInsn(ALOAD, 0);
            String fieldName = "$" + capturedBinding.getKey();
            String fieldDesc = jvmDescriptor(binding.type, false);
            invokeMethodWriter.visitFieldInsn(GETFIELD, innerClassName, fieldName, fieldDesc);

            int index = compiler.scope.nextLocalVariableIndex();
            if (binding.type == PrimitiveTypes.INTEGER) {
                invokeMethodWriter.visitVarInsn(ISTORE, index);
            } else if (binding.type == PrimitiveTypes.BOOLEAN) {
                invokeMethodWriter.visitVarInsn(ISTORE, index);
            } else if (binding.type == PrimitiveTypes.FLOAT) {
                invokeMethodWriter.visitVarInsn(FSTORE, index);
            } else {
                invokeMethodWriter.visitVarInsn(ASTORE, index);
            }
            compiler.scope.addBinding(capturedBinding.getKey(), binding.type, BindingTypes.LOCAL, binding.isImmutable);
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
