package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;

import java.util.function.Consumer;

import co.kenrg.mega.backend.compilation.scope.Scope;
import co.kenrg.mega.backend.compilation.StdLib;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;

public class StringInfixExpressionCompiler {
    public static void compileStringConcatenation(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        scope.focusedMethod.writer.visitTypeInsn(NEW, "java/lang/StringBuilder");
        scope.focusedMethod.writer.visitInsn(DUP);
        scope.focusedMethod.writer.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        appendStringsRec(node, scope, compileNode);

        scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    }

    private static void appendStringsRec(Node node, Scope scope, Consumer<Node> compileNode) {
        MegaType type = node.getType();
        assert type != null; // Populated by typechecker pass

        if (node instanceof InfixExpression && ((InfixExpression) node).operator.equals("+") && type == PrimitiveTypes.STRING) {
            appendStringsRec(((InfixExpression) node).left, scope, compileNode);
            appendStringsRec(((InfixExpression) node).right, scope, compileNode);
            return;
        }

        compileNode.accept(node);
        if (new ArrayType(PrimitiveTypes.ANY).isEquivalentTo(type)) {
            String signature = "([Ljava/lang/Object;)Ljava/lang/String;";
            scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, StdLib.Collections.Arrays, "toString", signature, false);
            scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        } else {
            String jvmDescriptor = jvmDescriptor(type, false);
            String signature = String.format("(%s)Ljava/lang/StringBuilder;", jvmDescriptor);
            scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", signature, false);
        }
    }

    public static void compileStringRepetition(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        Expression strExpr;
        Expression intExpr;
        if (node.left.getType() == PrimitiveTypes.INTEGER) {
            intExpr = node.left;
            strExpr = node.right;
        } else {
            intExpr = node.right;
            strExpr = node.left;
        }
        compileNode.accept(strExpr);
        compileNode.accept(intExpr);
        String signature = "(Ljava/lang/String;I)Ljava/lang/String;";
        scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, StdLib.Strings, "repeat", signature, false);
    }
}
