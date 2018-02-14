package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmMethodDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.MethodProxyCompiler.PROXY_SUFFIX;
import static co.kenrg.mega.backend.compilation.subcompilers.MethodProxyCompiler.getMethodProxyType;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileBoxPrimitiveType;
import static co.kenrg.mega.backend.compilation.subcompilers.PrimitiveBoxingUnboxingCompiler.compileUnboxPrimitiveType;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.NEW;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import co.kenrg.mega.backend.compilation.scope.Scope;
import co.kenrg.mega.backend.compilation.scope.Binding;
import co.kenrg.mega.backend.compilation.scope.BindingTypes;
import co.kenrg.mega.frontend.ast.expression.CallExpression;
import co.kenrg.mega.frontend.ast.expression.CallExpression.NamedArgs;
import co.kenrg.mega.frontend.ast.expression.CallExpression.UnnamedArgs;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Pair;

public class CallExpressionCompiler {
    public static void compileInvocation(CallExpression node, Scope scope, Consumer<Node> compileNode) {
        Node target = node.getTarget();
        FunctionType fnType = (FunctionType) target.getType();
        assert fnType != null;

        List<Expression> arguments;
        if (node instanceof CallExpression.UnnamedArgs) {
            UnnamedArgs callExpr = (UnnamedArgs) node;
            arguments = IntStream.range(0, fnType.arity())
                .mapToObj(i -> callExpr.arguments.size() > i ? callExpr.arguments.get(i) : null)
                .collect(toList());
        } else if (node instanceof CallExpression.NamedArgs) {
            NamedArgs callExpr = (NamedArgs) node;

            Map<String, Expression> namedArgs = callExpr.namedParamArguments.stream()
                .collect(toMap(param -> param.getKey().value, Pair::getValue));

            arguments = fnType.parameters.stream()
                .map(param -> param.ident.value)
                .map(namedArgs::get)
                .collect(toList());
        } else {
            throw new IllegalStateException("No other possible subclass of CallExpression: " + node.getClass());
        }

        boolean shouldInvokeProxy = arguments.stream().anyMatch(Objects::isNull);
        if (shouldInvokeProxy) {
            compileProxyInvocation(target, arguments, scope, compileNode);
        } else {
            compileNonProxyInvocation(target, arguments, scope, compileNode);
        }
    }

    private static void compileNonProxyInvocation(Node target, List<Expression> arguments, Scope scope, Consumer<Node> compileNode) {
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
                scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, binding.ownerModule, name, jvmDesc, false); // TODO: Don't assume all methods are static
                return;
            } else {
                String jvmDesc = jvmDescriptor(fnType, true);
                if (binding.bindingType == BindingTypes.STATIC) {
                    scope.focusedMethod.writer.visitFieldInsn(GETSTATIC, binding.ownerModule, name, jvmDesc);
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

    private static void compileProxyInvocation(Node target, List<Expression> arguments, Scope scope, Consumer<Node> compileNode) {
        FunctionType fnType = (FunctionType) target.getType();
        assert fnType != null; // Should be populated in typechecking pass

        if (target instanceof Identifier) {
            if (fnType.isConstructor) {
                throw new IllegalStateException("Cannot (yet) invoke constructors with default params");
            }

            String name = ((Identifier) target).value;
            Binding binding = scope.getBinding(name);
            assert binding != null; // If binding is null, then typechecking must have failed (probably)

            String proxyName = name + PROXY_SUFFIX;
            Binding proxyBinding = scope.getBinding(proxyName);
            assert proxyBinding != null; // If proxyBinding is null, then typechecking must have failed (probably)

            if (binding.bindingType == BindingTypes.METHOD) {
                FunctionType funcProxyType = getMethodProxyType(fnType);
                pushArgumentsForProxiedCall(funcProxyType, arguments, scope, compileNode);
                int argBitmask = getArgumentBitmask(arguments);
                scope.focusedMethod.writer.visitLdcInsn(argBitmask);

                String jvmDesc = jvmMethodDescriptor(funcProxyType, false);
                scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, binding.ownerModule, proxyName, jvmDesc, false); // TODO: Don't assume all methods are static
            } else {
                throw new IllegalStateException("Cannot invoke arrow functions with default params");
            }
        }
    }

    private static int getArgumentBitmask(List<Expression> arguments) {
        int argBitmask = 0;
        for (int i = 0; i < arguments.size(); i++) {
            Expression arg = arguments.get(i);
            if (arg == null) {
                argBitmask = argBitmask | (int) Math.pow(2, i);
            }
        }
        return argBitmask;
    }

    private static void pushArgumentsForProxiedCall(FunctionType arrowFnProxyType, List<Expression> arguments, Scope scope, Consumer<Node> compileNode) {
        for (int i = 0; i < arguments.size(); i++) {
            Expression arg = arguments.get(i);
            if (arg == null) {
                MegaType paramType = arrowFnProxyType.paramTypes.get(i);
                if (paramType == PrimitiveTypes.INTEGER) {
                    scope.focusedMethod.writer.visitInsn(ICONST_0);
                } else if (paramType == PrimitiveTypes.BOOLEAN) {
                    scope.focusedMethod.writer.visitInsn(ICONST_0);
                } else if (paramType == PrimitiveTypes.FLOAT) {
                    scope.focusedMethod.writer.visitInsn(FCONST_0);
                } else {
                    scope.focusedMethod.writer.visitInsn(ACONST_NULL);
                }
                continue;
            }

            MegaType argType = arg.getType();
            assert argType != null; // Should be populated in typechecking pass

            compileNode.accept(arg);
        }
    }
}
