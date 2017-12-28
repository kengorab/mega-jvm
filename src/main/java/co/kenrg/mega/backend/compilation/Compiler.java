package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.JvmTypesAndSignatures.jvmDescriptor;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCMPG;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IDIV;
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
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class Compiler {
    private List<String> errors = Lists.newArrayList();
    private String className;
    private ClassWriter cw;
    private MethodVisitor clinitWriter;
    private Scope scope;

    public Compiler(String className) {
        this.className = className;

        this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.cw.visit(V1_6, ACC_PUBLIC, className, null, "java/lang/Object", null);

        this.clinitWriter = this.cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        this.scope = new Scope(new FocusedMethod(this.clinitWriter, null, null));

        // Initialize <clinit> method writer for class. This method writer will be used to initialize all the static
        // values for the class (namespace).
        clinitWriter.visitCode();
    }

    public <T extends Node> List<Pair<String, byte[]>> compile(T node) {
        compileNode(node);
        this.clinitWriter.visitInsn(RETURN);
        this.clinitWriter.visitMaxs(-1, -1);
        this.clinitWriter.visitEnd();

        this.cw.visitEnd();
        return Lists.newArrayList(
            Pair.of(this.className, this.cw.toByteArray())
        );
    }

    private <T extends Node> void compileNode(T node) {

        // Statements
        if (node instanceof Module) {
            this.compileStatements(((Module) node).statements);
        } else if (node instanceof ExpressionStatement) {
            this.compileNode(((ExpressionStatement) node).expression);
        } else if (node instanceof ValStatement) {
            this.compileValStatement((ValStatement) node);
        } else if (node instanceof VarStatement) {
            this.compileVarStatement((VarStatement) node);
        }

        // Expressions
        if (node instanceof IntegerLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof FloatLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof BooleanLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof StringLiteral) {
            this.compileLiteral(node);
        } else if (node instanceof PrefixExpression) {
            this.compilePrefixExpression((PrefixExpression) node);
        } else if (node instanceof InfixExpression) {
            this.compileInfixExpression((InfixExpression) node);
        }
    }

    private void compileStatements(List<Statement> statements) {
        for (Statement statement : statements) {
            this.compileNode(statement);
        }
    }

    private void compileValStatement(ValStatement stmt) {
        compileBinding(stmt.name.value, stmt.value, false);
    }

    private void compileVarStatement(VarStatement stmt) {
        compileBinding(stmt.name.value, stmt.value, true);
    }

    private void compileBinding(String bindingName, Expression valExpr, boolean isMutable) {
        MegaType type = valExpr.getType();
        assert type != null; // Should have been set during typechecking pass
        String jvmDescriptor = jvmDescriptor(type);

        // Since we're at the root scope, declare val as static val in the class
        cw.visitField(ACC_PUBLIC | ACC_STATIC, bindingName, jvmDescriptor, null, null);
        compileNode(valExpr);
        clinitWriter.visitFieldInsn(PUTSTATIC, className, bindingName, jvmDescriptor);

        boolean isStatic = this.scope.isRoot();
        this.scope.addBinding(bindingName, valExpr, isStatic, isMutable);
    }

    private void compileLiteral(Node node) {
        if (node instanceof IntegerLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((IntegerLiteral) node).value);
        } else if (node instanceof FloatLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((FloatLiteral) node).value);
        } else if (node instanceof BooleanLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((BooleanLiteral) node).value);
        } else if (node instanceof StringLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((StringLiteral) node).value);
        }
    }

    private void compilePrefixExpression(PrefixExpression node) {
        switch (node.operator) {
            case "-": {
                compileNode(node.expression);
                if (node.expression.getType() == PrimitiveTypes.INTEGER) {
                    this.scope.focusedMethod.writer.visitInsn(INEG);
                } else if (node.expression.getType() == PrimitiveTypes.FLOAT) {
                    this.scope.focusedMethod.writer.visitInsn(FNEG);
                } else {
                    this.errors.add("Cannot apply numeric negation to type: " + node.expression.getType());
                }
                return;
            }
            case "!": {
                Label negationEndLabel = new Label();
                Label negationFalseLabel = new Label();

                compileNode(node.expression);
                this.scope.focusedMethod.writer.visitJumpInsn(IFNE, negationFalseLabel);
                this.scope.focusedMethod.writer.visitInsn(ICONST_1);
                this.scope.focusedMethod.writer.visitJumpInsn(GOTO, negationEndLabel);

                this.scope.focusedMethod.writer.visitLabel(negationFalseLabel);
                this.scope.focusedMethod.writer.visitInsn(ICONST_0);

                this.scope.focusedMethod.writer.visitLabel(negationEndLabel);
                return;
            }
            default:
                this.errors.add("Unrecognized unary operator: " + node.operator);
        }
    }

    private Map<String, Map<MegaType, Integer>> infixOperatorOpcodes = ImmutableMap.of(
        "+", ImmutableMap.of(
            PrimitiveTypes.INTEGER, IADD,
            PrimitiveTypes.FLOAT, FADD
        ),
        "-", ImmutableMap.of(
            PrimitiveTypes.INTEGER, ISUB,
            PrimitiveTypes.FLOAT, FSUB
        ),
        "*", ImmutableMap.of(
            PrimitiveTypes.INTEGER, IMUL,
            PrimitiveTypes.FLOAT, FMUL
        ),
        "/", ImmutableMap.of(
            PrimitiveTypes.INTEGER, IDIV,
            PrimitiveTypes.FLOAT, FDIV
        )
    );

    private void compileInfixExpression(InfixExpression node) {
        MegaType type = node.getType();
        assert type != null; // Should have been populated during typechecking pass

        if (type.isEquivalentTo(PrimitiveTypes.STRING)) {
            // Handle string operators
            return;
        }

        if (type == PrimitiveTypes.INTEGER) {
            compileNode(node.left);
            compileNode(node.right);

            Integer opcode = infixOperatorOpcodes.get(node.operator).get(type);
            this.scope.focusedMethod.writer.visitInsn(opcode);
        } else if (type == PrimitiveTypes.FLOAT) {
            assert node.left.getType() != null;
            compileNode(node.left);
            if (!node.left.getType().isEquivalentTo(PrimitiveTypes.FLOAT)) {
                this.scope.focusedMethod.writer.visitInsn(I2F);
            }

            assert node.right.getType() != null;
            compileNode(node.right);
            if (!node.right.getType().isEquivalentTo(PrimitiveTypes.FLOAT)) {
                this.scope.focusedMethod.writer.visitInsn(I2F);
            }

            Integer opcode = infixOperatorOpcodes.get(node.operator).get(type);
            this.scope.focusedMethod.writer.visitInsn(opcode);
        } else if (type == PrimitiveTypes.BOOLEAN) {
            switch (node.operator) {
                case "&&":
                    pushCondAnd(node);
                    break;
                case "||":
                    pushCondOr(node);
                    break;
                default:
                    pushComparison(node);
                    break;
            }
        }
    }

    private void pushCondAnd(InfixExpression node) {
        Label condEndLabel = new Label();
        Label condFalseLabel = new Label();

        compileNode(node.left);
        this.scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        compileNode(node.right);
        this.scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        this.scope.focusedMethod.writer.visitInsn(ICONST_1);
        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, condEndLabel);

        this.scope.focusedMethod.writer.visitLabel(condFalseLabel);
        this.scope.focusedMethod.writer.visitInsn(ICONST_0);

        this.scope.focusedMethod.writer.visitLabel(condEndLabel);
    }

    private void pushCondOr(InfixExpression node) {
        Label condEndLabel = new Label();
        Label condTrueLabel = new Label();
        Label condFalseLabel = new Label();

        compileNode(node.left);
        this.scope.focusedMethod.writer.visitJumpInsn(IFNE, condTrueLabel);

        compileNode(node.right);
        this.scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        this.scope.focusedMethod.writer.visitLabel(condTrueLabel);
        this.scope.focusedMethod.writer.visitInsn(ICONST_1);
        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, condEndLabel);

        this.scope.focusedMethod.writer.visitLabel(condFalseLabel);
        this.scope.focusedMethod.writer.visitInsn(ICONST_0);

        this.scope.focusedMethod.writer.visitLabel(condEndLabel);
    }

    private void pushComparison(InfixExpression node) {
        MegaType leftType = node.left.getType();
        assert leftType != null;
        MegaType rightType = node.right.getType();
        assert rightType != null;

        if (PrimitiveTypes.NUMBER.isEquivalentTo(leftType) && PrimitiveTypes.NUMBER.isEquivalentTo(rightType)) {
            if (leftType == PrimitiveTypes.INTEGER && rightType == PrimitiveTypes.INTEGER) {
                pushIntegerComparison(node);
            } else if (leftType == PrimitiveTypes.FLOAT || rightType == PrimitiveTypes.FLOAT) {
                pushFloatComparison(node);
            }
        } else if (leftType == PrimitiveTypes.BOOLEAN && rightType == PrimitiveTypes.BOOLEAN) {
            pushIntegerComparison(node);
        } else {
            pushComparableComparison(node);
        }
    }

    private void pushIntegerComparison(InfixExpression node) {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        compileNode(node.left);
        compileNode(node.right);

        switch (node.operator) {
            case "<":
                this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPLT, trueLabel);
                break;
            case "<=":
                this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPLE, trueLabel);
                break;
            case ">":
                this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPGT, trueLabel);
                break;
            case ">=":
                this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPGE, trueLabel);
                break;
            case "==":
                this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPEQ, trueLabel);
                break;
            case "!=":
                this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPNE, trueLabel);
                break;
        }

        this.scope.focusedMethod.writer.visitInsn(ICONST_0);
        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        this.scope.focusedMethod.writer.visitLabel(trueLabel);
        this.scope.focusedMethod.writer.visitInsn(ICONST_1);

        this.scope.focusedMethod.writer.visitLabel(endLabel);
    }

    private void pushFloatComparison(InfixExpression node) {
        MegaType leftType = node.left.getType();
        assert leftType != null;
        compileNode(node.left);
        if (!leftType.isEquivalentTo(PrimitiveTypes.FLOAT)) {
            this.scope.focusedMethod.writer.visitInsn(I2F);
        }

        MegaType rightType = node.right.getType();
        assert rightType != null;
        compileNode(node.right);
        if (!rightType.isEquivalentTo(PrimitiveTypes.FLOAT)) {
            this.scope.focusedMethod.writer.visitInsn(I2F);
        }

        Label trueLabel = new Label();
        Label endLabel = new Label();

        switch (node.operator) {
            case "<":
                this.scope.focusedMethod.writer.visitInsn(FCMPL);
                this.scope.focusedMethod.writer.visitJumpInsn(IFLT, trueLabel);
                break;
            case "<=":
                this.scope.focusedMethod.writer.visitInsn(FCMPL);
                this.scope.focusedMethod.writer.visitJumpInsn(IFLE, trueLabel);
                break;
            case ">":
                this.scope.focusedMethod.writer.visitInsn(FCMPG);
                this.scope.focusedMethod.writer.visitJumpInsn(IFGT, trueLabel);
                break;
            case ">=":
                this.scope.focusedMethod.writer.visitInsn(FCMPG);
                this.scope.focusedMethod.writer.visitJumpInsn(IFGE, trueLabel);
                break;
            case "==":
                this.scope.focusedMethod.writer.visitInsn(FCMPL);
                this.scope.focusedMethod.writer.visitJumpInsn(IFEQ, trueLabel);
                break;
            case "!=":
                this.scope.focusedMethod.writer.visitInsn(FCMPL);
                this.scope.focusedMethod.writer.visitJumpInsn(IFNE, trueLabel);
                break;
        }

        this.scope.focusedMethod.writer.visitInsn(ICONST_0);
        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        this.scope.focusedMethod.writer.visitLabel(trueLabel);
        this.scope.focusedMethod.writer.visitInsn(ICONST_1);

        this.scope.focusedMethod.writer.visitLabel(endLabel);
    }

    private void pushComparableComparison(InfixExpression node) {
        compileNode(node.left);
        compileNode(node.right);

        MegaType leftType = node.left.getType(); // Left type chosen arbitrarily
        assert leftType != null;

        String jvmDescriptor = jvmDescriptor(leftType);
        String signature = String.format("(%s)I", jvmDescriptor);
        String className = leftType.className();
        if (className == null) {
            this.errors.add("Expected type " + leftType + " to have a class name");
            className = PrimitiveTypes.ANY.className();
        }

        Label trueLabel = new Label();
        Label endLabel = new Label();

        switch (node.operator) {
            case "<":
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                this.scope.focusedMethod.writer.visitJumpInsn(IFLT, trueLabel);
                break;
            case "<=":
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                this.scope.focusedMethod.writer.visitJumpInsn(IFLE, trueLabel);
                break;
            case ">":
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                this.scope.focusedMethod.writer.visitJumpInsn(IFGT, trueLabel);
                break;
            case ">=":
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "compareTo", signature, false);
                this.scope.focusedMethod.writer.visitJumpInsn(IFGE, trueLabel);
                break;
            case "==":
                // The `equals` method places a boolean on the top of the stack; call the method and return early
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "equals", "(Ljava/lang/Object;)Z", false);
                return;
            case "!=":
                // If the comparison is (!=), call the `equals` method. If `equals` returns 0 (false), negate
                // by jumping to the true label.
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, className, "equals", "(Ljava/lang/Object;)Z", false);
                this.scope.focusedMethod.writer.visitJumpInsn(IFEQ, trueLabel);
                break;
        }

        this.scope.focusedMethod.writer.visitInsn(ICONST_0);
        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        this.scope.focusedMethod.writer.visitLabel(trueLabel);
        this.scope.focusedMethod.writer.visitInsn(ICONST_1);

        this.scope.focusedMethod.writer.visitLabel(endLabel);
    }

//    private void pushBooleanComparison(InfixExpression node) {
//        this.scope.focusedMethod.writer.visitInsn(ICONST_0);
//        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);
//
//        this.scope.focusedMethod.writer.visitLabel(trueLabel);
//        this.scope.focusedMethod.writer.visitInsn(ICONST_1);
//
//        this.scope.focusedMethod.writer.visitLabel(endLabel);
//    }
}
