package co.kenrg.mega.backend.compilation.subcompilers;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.getInternalName;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static org.objectweb.asm.Opcodes.FCMPG;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.util.function.Consumer;

import co.kenrg.mega.backend.compilation.scope.Scope;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import org.objectweb.asm.Label;

/**
 * Represents a "sub-compiler" which is specialized to generate JVM bytecode for Boolean Infix Expressions
 * (such as && and ||) as well as Comparison Expressions (e.g. <, <=, >, >=, ==, and !=).
 */
public class BooleanInfixExpressionCompiler {

    public static void compileConditionalAndExpression(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        Label condEndLabel = new Label();
        Label condFalseLabel = new Label();

        compileNode.accept(node.left);
        scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        compileNode.accept(node.right);
        scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        scope.focusedMethod.writer.visitInsn(ICONST_1);
        scope.focusedMethod.writer.visitJumpInsn(GOTO, condEndLabel);

        scope.focusedMethod.writer.visitLabel(condFalseLabel);
        scope.focusedMethod.writer.visitInsn(ICONST_0);

        scope.focusedMethod.writer.visitLabel(condEndLabel);
    }

    public static void compileConditionalOrExpression(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        Label condEndLabel = new Label();
        Label condTrueLabel = new Label();
        Label condFalseLabel = new Label();

        compileNode.accept(node.left);
        scope.focusedMethod.writer.visitJumpInsn(IFNE, condTrueLabel);

        compileNode.accept(node.right);
        scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        scope.focusedMethod.writer.visitLabel(condTrueLabel);
        scope.focusedMethod.writer.visitInsn(ICONST_1);
        scope.focusedMethod.writer.visitJumpInsn(GOTO, condEndLabel);

        scope.focusedMethod.writer.visitLabel(condFalseLabel);
        scope.focusedMethod.writer.visitInsn(ICONST_0);

        scope.focusedMethod.writer.visitLabel(condEndLabel);
    }

    public static void compileComparisonExpression(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        MegaType leftType = node.left.getType();
        assert leftType != null;
        MegaType rightType = node.right.getType();
        assert rightType != null;

        if (PrimitiveTypes.NUMBER.isEquivalentTo(leftType) && PrimitiveTypes.NUMBER.isEquivalentTo(rightType)) {
            if (leftType == PrimitiveTypes.INTEGER && rightType == PrimitiveTypes.INTEGER) {
                pushIntegerComparison(node, scope, compileNode);
            } else if (leftType == PrimitiveTypes.FLOAT || rightType == PrimitiveTypes.FLOAT) {
                pushFloatComparison(node, scope, compileNode);
            }
        } else if (leftType == PrimitiveTypes.BOOLEAN && rightType == PrimitiveTypes.BOOLEAN) {
            pushIntegerComparison(node, scope, compileNode);
        } else {
            pushComparableComparison(node, scope, compileNode);
        }
    }

    private static void pushIntegerComparison(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        compileNode.accept(node.left);
        compileNode.accept(node.right);

        switch (node.operator) {
            case "<":
                scope.focusedMethod.writer.visitJumpInsn(IF_ICMPLT, trueLabel);
                break;
            case "<=":
                scope.focusedMethod.writer.visitJumpInsn(IF_ICMPLE, trueLabel);
                break;
            case ">":
                scope.focusedMethod.writer.visitJumpInsn(IF_ICMPGT, trueLabel);
                break;
            case ">=":
                scope.focusedMethod.writer.visitJumpInsn(IF_ICMPGE, trueLabel);
                break;
            case "==":
                scope.focusedMethod.writer.visitJumpInsn(IF_ICMPEQ, trueLabel);
                break;
            case "!=":
                scope.focusedMethod.writer.visitJumpInsn(IF_ICMPNE, trueLabel);
                break;
        }

        scope.focusedMethod.writer.visitInsn(ICONST_0);
        scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        scope.focusedMethod.writer.visitLabel(trueLabel);
        scope.focusedMethod.writer.visitInsn(ICONST_1);

        scope.focusedMethod.writer.visitLabel(endLabel);
    }

    private static void pushFloatComparison(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        MegaType leftType = node.left.getType();
        assert leftType != null;
        compileNode.accept(node.left);
        if (!leftType.isEquivalentTo(PrimitiveTypes.FLOAT)) {
            scope.focusedMethod.writer.visitInsn(I2F);
        }

        MegaType rightType = node.right.getType();
        assert rightType != null;
        compileNode.accept(node.right);
        if (!rightType.isEquivalentTo(PrimitiveTypes.FLOAT)) {
            scope.focusedMethod.writer.visitInsn(I2F);
        }

        Label trueLabel = new Label();
        Label endLabel = new Label();

        switch (node.operator) {
            case "<":
                scope.focusedMethod.writer.visitInsn(FCMPL);
                scope.focusedMethod.writer.visitJumpInsn(IFLT, trueLabel);
                break;
            case "<=":
                scope.focusedMethod.writer.visitInsn(FCMPL);
                scope.focusedMethod.writer.visitJumpInsn(IFLE, trueLabel);
                break;
            case ">":
                scope.focusedMethod.writer.visitInsn(FCMPG);
                scope.focusedMethod.writer.visitJumpInsn(IFGT, trueLabel);
                break;
            case ">=":
                scope.focusedMethod.writer.visitInsn(FCMPG);
                scope.focusedMethod.writer.visitJumpInsn(IFGE, trueLabel);
                break;
            case "==":
                scope.focusedMethod.writer.visitInsn(FCMPL);
                scope.focusedMethod.writer.visitJumpInsn(IFEQ, trueLabel);
                break;
            case "!=":
                scope.focusedMethod.writer.visitInsn(FCMPL);
                scope.focusedMethod.writer.visitJumpInsn(IFNE, trueLabel);
                break;
        }

        scope.focusedMethod.writer.visitInsn(ICONST_0);
        scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        scope.focusedMethod.writer.visitLabel(trueLabel);
        scope.focusedMethod.writer.visitInsn(ICONST_1);

        scope.focusedMethod.writer.visitLabel(endLabel);
    }

    private static void pushComparableComparison(InfixExpression node, Scope scope, Consumer<Node> compileNode) {
        compileNode.accept(node.left);
        compileNode.accept(node.right);

        MegaType leftType = node.left.getType(); // Left type chosen arbitrarily
        assert leftType != null;

        String jvmDescriptor = jvmDescriptor(leftType, false);
        String signature = String.format("(%s)I", jvmDescriptor);
        String className = getInternalName(leftType.typeClass());
        if (className == null) {
            System.out.printf("Expected type %s to have a class name\n", leftType);
            className = getInternalName(PrimitiveTypes.ANY.typeClass());
        }

        Label trueLabel = new Label();
        Label endLabel = new Label();

        switch (node.operator) {
            case "<":
                scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                scope.focusedMethod.writer.visitJumpInsn(IFLT, trueLabel);
                break;
            case "<=":
                scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                scope.focusedMethod.writer.visitJumpInsn(IFLE, trueLabel);
                break;
            case ">":
                scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                scope.focusedMethod.writer.visitJumpInsn(IFGT, trueLabel);
                break;
            case ">=":
                scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                scope.focusedMethod.writer.visitJumpInsn(IFGE, trueLabel);
                break;
            case "==":
                // The `equals` method places a boolean on the top of the stack; call the method and return early
                scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "equals", "(Ljava/lang/Object;)Z", false);
                return;
            case "!=":
                // If the comparison is (!=), call the `equals` method. If `equals` returns 0 (false), negate
                // by jumping to the true label.
                scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "equals", "(Ljava/lang/Object;)Z", false);
                scope.focusedMethod.writer.visitJumpInsn(IFEQ, trueLabel);
                break;
        }

        scope.focusedMethod.writer.visitInsn(ICONST_0);
        scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        scope.focusedMethod.writer.visitLabel(trueLabel);
        scope.focusedMethod.writer.visitInsn(ICONST_1);

        scope.focusedMethod.writer.visitLabel(endLabel);
    }
}
