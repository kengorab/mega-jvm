package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileBoxPrimitiveType;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileUnboxPrimitiveType;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.Collection;
import java.util.function.Consumer;

import co.kenrg.mega.backend.compilation.Scope;
import co.kenrg.mega.backend.compilation.Scope.Binding;
import co.kenrg.mega.backend.compilation.Scope.BindingTypes;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.CallExpression.NamedArgs;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import com.google.common.base.Strings;

public class CallExpressionCompiler {
    public static void compileInvocation(CallExpression node, Scope scope, String className, Consumer<Node> compileNode) {
        if (node instanceof CallExpression.UnnamedArgs) {
            compileUnnamedArgsInvocation((CallExpression.UnnamedArgs) node, scope, className, compileNode);
        } else if (node instanceof CallExpression.NamedArgs) {
            compileNamedArgsInvocation((CallExpression.NamedArgs) node, scope, className, compileNode);
        } else {
            throw new IllegalStateException("No other possible subclass of CallExpression: " + node.getClass());
        }
    }

    public static void compileUnnamedArgsInvocation(CallExpression.UnnamedArgs node, Scope scope, String className, Consumer<Node> compileNode) {
        FunctionType fnType;

        if (node.target instanceof Identifier) {
            String name = ((Identifier) node.target).value;
            Binding binding = scope.getBinding(name);
            assert binding != null; // If binding is null, then typechecking must have failed (probably)

            fnType = (FunctionType) binding.type;
            if (binding.bindingType == BindingTypes.METHOD) {
                pushArguments(node.arguments, scope, compileNode, false);

                String jvmDesc = jvmMethodDescriptor(fnType, false);
                scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, className, name, jvmDesc, false); // TODO: Don't assume all methods are static
                return;
            }

            String jvmDesc = jvmDescriptor(fnType, true);
            if (binding.bindingType == BindingTypes.STATIC) {
                scope.focusedMethod.writer.visitFieldInsn(GETSTATIC, className, name, jvmDesc);
            } else {
                scope.focusedMethod.writer.visitVarInsn(ALOAD, binding.index);
            }
        } else {
            fnType = (FunctionType) node.target.getType();
            assert fnType != null; // Should be populated in typechecking pass

            compileNode.accept(node.target);
        }

        scope.focusedMethod.writer.visitTypeInsn(CHECKCAST, getInternalName(fnType.typeClass()));

        pushArguments(node.arguments, scope, compileNode, true);

        String invokeDesc = String.format("(%s)Ljava/lang/Object;", Strings.repeat("Ljava/lang/Object;", fnType.arity()));
        scope.focusedMethod.writer.visitMethodInsn(INVOKEINTERFACE, getInternalName(fnType.typeClass()), "invoke", invokeDesc, true);

        assert fnType.returnType != null;
        scope.focusedMethod.writer.visitTypeInsn(CHECKCAST, jvmDescriptor(fnType.returnType, true));
        if (isPrimitive(fnType.returnType)) {
            compileUnboxPrimitiveType(fnType.returnType, scope.focusedMethod.writer);
        }
    }

    private static void compileNamedArgsInvocation(NamedArgs node, Scope scope, String className, Consumer<Node> compileNode) {

    }

    private static void pushArguments(Collection<Expression> arguments, Scope scope, Consumer<Node> compileNode, boolean shouldBox) {
        for (Expression arg : arguments) {
            MegaType argType = arg.getType();
            assert argType != null; // Should be populated in typechecking pass

            compileNode.accept(arg);
            if (shouldBox && isPrimitive(argType)) {
                compileBoxPrimitiveType(argType, scope.focusedMethod.writer);
            }
        }
    }
}
