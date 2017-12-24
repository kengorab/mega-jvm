package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.JvmTypesAndSignatures.jvmDescriptor;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.List;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
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
    private FocusedMethod focusedMethod;

    public Compiler(String className) {
        this.className = className;

        this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        this.cw.visit(V1_6, ACC_PUBLIC, className, null, "java/lang/Object", null);

        this.clinitWriter = this.cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        this.focusedMethod = new FocusedMethod(this.clinitWriter, null, null);

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
        }
    }

    private void compileStatements(List<Statement> statements) {
        for (Statement statement : statements) {
            this.compileNode(statement);
        }
    }

    private void compileValStatement(ValStatement stmt) {
        String valName = stmt.name.value;

        MegaType type = stmt.value.getType();
        assert type != null; // Should have been set during typechecking pass
        String jvmDescriptor = jvmDescriptor(type);

        // Since we're at the root scope, declare val as static val in the class
        cw.visitField(ACC_PUBLIC | ACC_STATIC, valName, jvmDescriptor, null, null);
        compileNode(stmt.value);
        clinitWriter.visitFieldInsn(PUTSTATIC, className, valName, jvmDescriptor);

        // TODO: Register bindings
//        data.vals.put(valName, ValBinding.Static(valName, valDeclExprType))
    }

    private void compileLiteral(Node node) {
        if (node instanceof IntegerLiteral) {
            this.focusedMethod.writer.visitLdcInsn(((IntegerLiteral) node).value);
        } else if (node instanceof FloatLiteral) {
            this.focusedMethod.writer.visitLdcInsn(((FloatLiteral) node).value);
        } else if (node instanceof BooleanLiteral) {
            this.focusedMethod.writer.visitLdcInsn(((BooleanLiteral) node).value);
        } else if (node instanceof StringLiteral) {
            this.focusedMethod.writer.visitLdcInsn(((StringLiteral) node).value);
        }
    }

    private void compilePrefixExpression(PrefixExpression node) {
        switch (node.operator) {
            case "-": {
                compileNode(node.expression);
                if (node.expression.getType() == PrimitiveTypes.INTEGER) {
                    this.focusedMethod.writer.visitInsn(INEG);
                } else if (node.expression.getType() == PrimitiveTypes.FLOAT) {
                    this.focusedMethod.writer.visitInsn(FNEG);
                } else {
                    this.errors.add("Cannot apply numeric negation to type: " + node.expression.getType());
                }
                return;
            }
            case "!": {
                Label negationEndLabel = new Label();
                Label negationFalseLabel = new Label();

                compileNode(node.expression);
                this.focusedMethod.writer.visitJumpInsn(IFNE, negationFalseLabel);
                this.focusedMethod.writer.visitInsn(ICONST_1);
                this.focusedMethod.writer.visitJumpInsn(GOTO, negationEndLabel);

                this.focusedMethod.writer.visitLabel(negationFalseLabel);
                this.focusedMethod.writer.visitInsn(ICONST_0);

                this.focusedMethod.writer.visitLabel(negationEndLabel);
                return;
            }
            default:
                this.errors.add("Unrecognized unary operator: " + node.operator);
        }
    }
}
