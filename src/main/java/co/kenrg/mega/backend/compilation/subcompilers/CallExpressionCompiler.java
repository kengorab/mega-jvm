package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileBoxPrimitiveType;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileUnboxPrimitiveType;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.NEW;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import co.kenrg.mega.backend.compilation.Scope;
import co.kenrg.mega.backend.compilation.Scope.Binding;
import co.kenrg.mega.backend.compilation.Scope.BindingTypes;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.CallExpression.NamedArgs;
import co.kenrg.mega.frontend.ast.expression.CallExpression.UnnamedArgs;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Pair;

public class CallExpressionCompiler {
    public static void compileInvocation(CallExpression node, Scope scope, String className, Consumer<Node> compileNode) {
        Node target;
        List<Expression> arguments;
        if (node instanceof CallExpression.UnnamedArgs) {
            UnnamedArgs callExpr = (UnnamedArgs) node;
            target = callExpr.target;
            arguments = callExpr.arguments;
        } else if (node instanceof CallExpression.NamedArgs) {
            NamedArgs callExpr = (NamedArgs) node;
            target = callExpr.target;

            FunctionType fnType = (FunctionType) target.getType();
            assert fnType != null;
            Map<String, Expression> namedArgs = callExpr.namedParamArguments.stream()
                .collect(toMap(param -> param.getKey().value, Pair::getValue));
            arguments = fnType.parameters.stream()
                .map(param -> param.ident.value)
                .map(namedArgs::get)
                .collect(toList());
        } else {
            throw new IllegalStateException("No other possible subclass of CallExpression: " + node.getClass());
        }

        compileInvocation(target, arguments, scope, className, compileNode);
    }

    public static void compileInvocation(Node target, List<Expression> arguments, Scope scope, String className, Consumer<Node> compileNode) {
        FunctionType fnType = (FunctionType) target.getType();
        assert fnType != null; // Should be populated in typechecking pass

        if (target instanceof Identifier) {
            if (fnType.isConstructor) {
                MegaType classType = fnType.returnType;
                assert classType != null; // Should be populated in typechecking pass

                scope.focusedMethod.writer.visitTypeInsn(NEW, classType.className());
                scope.focusedMethod.writer.visitInsn(DUP);
                pushArguments(arguments, scope, compileNode, false);

                String jvmDesc = jvmMethodDescriptor(fnType, false, true);
                scope.focusedMethod.writer.visitMethodInsn(INVOKESPECIAL, classType.className(), "<init>", jvmDesc, false);
                return;
            }

            String name = ((Identifier) target).value;
            Binding binding = scope.getBinding(name);
            assert binding != null; // If binding is null, then typechecking must have failed (probably)
            if (binding.bindingType == BindingTypes.METHOD) {
                pushArguments(arguments, scope, compileNode, false);

                String jvmDesc = jvmMethodDescriptor(fnType, false);
                scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, className, name, jvmDesc, false); // TODO: Don't assume all methods are static
                return;
            } else {
                String jvmDesc = jvmDescriptor(fnType, true);
                if (binding.bindingType == BindingTypes.STATIC) {
                    scope.focusedMethod.writer.visitFieldInsn(GETSTATIC, className, name, jvmDesc);
                } else {
                    scope.focusedMethod.writer.visitVarInsn(ALOAD, binding.index);
                }
            }
        } else {
            compileNode.accept(target);
        }

        scope.focusedMethod.writer.visitTypeInsn(CHECKCAST, getInternalName(fnType.typeClass()));

        pushArguments(arguments, scope, compileNode, true);

        String invokeDesc = String.format("(%s)Ljava/lang/Object;", Strings.repeat("Ljava/lang/Object;", fnType.arity()));
        scope.focusedMethod.writer.visitMethodInsn(INVOKEINTERFACE, getInternalName(fnType.typeClass()), "invoke", invokeDesc, true);

        assert fnType.returnType != null;
        scope.focusedMethod.writer.visitTypeInsn(CHECKCAST, jvmDescriptor(fnType.returnType, true));
        if (isPrimitive(fnType.returnType)) {
            compileUnboxPrimitiveType(fnType.returnType, scope.focusedMethod.writer);
        }
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
