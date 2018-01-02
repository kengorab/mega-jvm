package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;
import static co.kenrg.mega.backend.compilation.subcompilers.BooleanInfixExpressionCompiler.compileComparisonExpression;
import static co.kenrg.mega.backend.compilation.subcompilers.BooleanInfixExpressionCompiler.compileConditionalAndExpression;
import static co.kenrg.mega.backend.compilation.subcompilers.BooleanInfixExpressionCompiler.compileConditionalOrExpression;
import static co.kenrg.mega.backend.compilation.subcompilers.StringInfixExpressionCompiler.compileStringConcatenation;
import static co.kenrg.mega.backend.compilation.subcompilers.StringInfixExpressionCompiler.compileStringRepetition;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.F_CHOP;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.List;
import java.util.Map;

import co.kenrg.mega.backend.compilation.Scope.Binding;
import co.kenrg.mega.backend.compilation.Scope.BindingTypes;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.AssignmentExpression;
import co.kenrg.mega.frontend.ast.expression.BlockExpression;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.expression.IfExpression;
import co.kenrg.mega.frontend.ast.expression.IndexExpression;
import co.kenrg.mega.frontend.ast.expression.InfixExpression;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.PrefixExpression;
import co.kenrg.mega.frontend.ast.expression.RangeExpression;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.ForLoopStatement;
import co.kenrg.mega.frontend.ast.statement.ValStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
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

        Label clinitStart = new Label();
        Label clinitEnd = new Label();
        this.clinitWriter = this.cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        this.scope = new Scope(new FocusedMethod(this.clinitWriter, clinitStart, clinitEnd));

        // Initialize <clinit> method writer for class. This method writer will be used to initialize all the static
        // values for the class (namespace).
        clinitWriter.visitLabel(clinitStart);
        clinitWriter.visitCode();
        clinitWriter.visitLabel(clinitEnd);
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
        } else if (node instanceof ForLoopStatement) {
            this.compileForLoopStatement((ForLoopStatement) node);
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
        } else if (node instanceof ArrayLiteral) {
            this.compileArrayLiteral((ArrayLiteral) node);
        } else if (node instanceof PrefixExpression) {
            this.compilePrefixExpression((PrefixExpression) node);
        } else if (node instanceof InfixExpression) {
            this.compileInfixExpression((InfixExpression) node);
        } else if (node instanceof IfExpression) {
            this.compileIfExpression((IfExpression) node);
        } else if (node instanceof Identifier) {
            this.compileIdentifier((Identifier) node);
        } else if (node instanceof AssignmentExpression) {
            this.compileAssignmentExpression((AssignmentExpression) node);
        } else if (node instanceof RangeExpression) {
            this.compileRangeExpression((RangeExpression) node);
        } else if (node instanceof IndexExpression) {
            this.compileIndexExpression((IndexExpression) node);
        }
    }

    private void compileStatements(List<Statement> statements) {
        for (Statement statement : statements) {
            this.compileNode(statement);
        }
    }

    //***************************************************************
    //************              Statements               ************
    //***************************************************************

    private void compileValStatement(ValStatement stmt) {
        compileBinding(stmt.name.value, stmt.value, false);
    }

    private void compileVarStatement(VarStatement stmt) {
        compileBinding(stmt.name.value, stmt.value, true);
    }

    private void compileBinding(String bindingName, Expression bindingExpr, boolean isMutable) {
        MegaType type = bindingExpr.getType();
        assert type != null; // Should have been set during typechecking pass
        String jvmDescriptor = jvmDescriptor(type, false);

        if (this.scope.isRoot()) {
            int access = ACC_PUBLIC | ACC_STATIC; // At root scope, declare binding as static in the class
            if (!isMutable) {
                access = access | ACC_FINAL;
            }
            cw.visitField(access, bindingName, jvmDescriptor, null, null);
            compileNode(bindingExpr);
            clinitWriter.visitFieldInsn(PUTSTATIC, className, bindingName, jvmDescriptor);

            this.scope.addBinding(bindingName, bindingExpr.getType(), BindingTypes.STATIC, isMutable);
            return;
        }

        int index = this.scope.nextLocalVariableIndex();
        compileNode(bindingExpr);
        if (type == PrimitiveTypes.INTEGER) {
            this.scope.focusedMethod.writer.visitVarInsn(ISTORE, index);
        } else if (type == PrimitiveTypes.BOOLEAN) {
            this.scope.focusedMethod.writer.visitVarInsn(ISTORE, index);
        } else if (type == PrimitiveTypes.FLOAT) {
            this.scope.focusedMethod.writer.visitVarInsn(FSTORE, index);
        } else {
            this.scope.focusedMethod.writer.visitVarInsn(ASTORE, index);
        }
        this.scope.addBinding(bindingName, bindingExpr.getType(), BindingTypes.LOCAL, isMutable);
    }

    private void compileForLoopStatement(ForLoopStatement node) {
        String tag = RandomStringUtils.randomAlphanumeric(6); // Tag to uniquely id synthesized loop variables

        compileNode(node.iteratee);
        int iterateeIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitVarInsn(ASTORE, iterateeIndex);
        this.scope.focusedMethod.writer.visitLocalVariable("for_loop_iteratee_" + tag, jvmDescriptor(node.iteratee.getType(), true), null, this.scope.focusedMethod.start, this.scope.focusedMethod.end, iterateeIndex);
        this.scope.addBinding("$$for_loop_iteratee_" + tag, node.iteratee.getType(), BindingTypes.LOCAL, false);

        int iterateeLengthIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitVarInsn(ALOAD, iterateeIndex);
        this.scope.focusedMethod.writer.visitInsn(ARRAYLENGTH);
        this.scope.focusedMethod.writer.visitVarInsn(ISTORE, iterateeLengthIndex);
        this.scope.focusedMethod.writer.visitLocalVariable("for_loop_iteratee_length_" + tag, "I", null, this.scope.focusedMethod.start, this.scope.focusedMethod.end, iterateeLengthIndex);
        this.scope.addBinding("$$for_loop_iteratee_length_" + tag, PrimitiveTypes.INTEGER, BindingTypes.LOCAL, false);

        int iteratorIndexIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitInsn(ICONST_0);
        this.scope.focusedMethod.writer.visitVarInsn(ISTORE, iteratorIndexIndex);
        this.scope.focusedMethod.writer.visitLocalVariable("for_loop_iterator_idx" + tag, "I", null, this.scope.focusedMethod.start, this.scope.focusedMethod.end, iteratorIndexIndex);
        this.scope.addBinding("$$for_loop_iterator_idx_" + tag, PrimitiveTypes.INTEGER, BindingTypes.LOCAL, false);

        Label loopStart = new Label();
        Label loopEnd = new Label();

        this.scope.focusedMethod.writer.visitLabel(loopStart);
        Object[] localsSignatures = this.scope.getLocalsSignatures();
        this.scope.focusedMethod.writer.visitFrame(F_FULL, localsSignatures.length, localsSignatures, 0, null);
        this.scope.focusedMethod.writer.visitVarInsn(ILOAD, iteratorIndexIndex);
        this.scope.focusedMethod.writer.visitVarInsn(ILOAD, iterateeLengthIndex);
        this.scope.focusedMethod.writer.visitJumpInsn(IF_ICMPGE, loopEnd);

        String iteratorName = node.iterator.value;
        int iteratorIndex = this.scope.nextLocalVariableIndex();
        this.scope.focusedMethod.writer.visitVarInsn(ALOAD, iterateeIndex);
        this.scope.focusedMethod.writer.visitVarInsn(ILOAD, iteratorIndexIndex);
        ArrayType iterateeType = (ArrayType) node.iteratee.getType();
        assert iterateeType != null; // Should be populated by typechecking pass
        if (iterateeType.typeArg == PrimitiveTypes.INTEGER) {
            this.scope.focusedMethod.writer.visitInsn(AALOAD);
            this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, PrimitiveTypes.INTEGER.className(), "intValue", "()I", false);
            this.scope.focusedMethod.writer.visitVarInsn(ISTORE, iteratorIndex);
        }

        this.scope.focusedMethod.writer.visitLocalVariable(iteratorName, jvmDescriptor(iterateeType.typeArg, true), null, this.scope.focusedMethod.start, this.scope.focusedMethod.end, iteratorIndex);
        this.scope.addBinding(iteratorName, node.iterator.getType(), BindingTypes.LOCAL, false);

        compileBlockExpression(node.block);

        this.scope.focusedMethod.writer.visitIincInsn(iteratorIndexIndex, 1);

        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, loopStart);

        this.scope.focusedMethod.writer.visitLabel(loopEnd);
        this.scope.focusedMethod.writer.visitFrame(F_CHOP, 3, null, 0, null);
    }

    //***************************************************************
    //************              Expressions              ************
    //***************************************************************

    private void compileLiteral(Node node) {
        if (node instanceof IntegerLiteral) {
            int value = ((IntegerLiteral) node).value;
            if (0 <= value && value <= 5) {
                this.scope.focusedMethod.writer.visitInsn(value + ICONST_0);
            } else {
                this.scope.focusedMethod.writer.visitLdcInsn(value);
            }
        } else if (node instanceof FloatLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((FloatLiteral) node).value);
        } else if (node instanceof BooleanLiteral) {
            int value = ((BooleanLiteral) node).value ? ICONST_1 : ICONST_0;
            this.scope.focusedMethod.writer.visitInsn(value);
        } else if (node instanceof StringLiteral) {
            this.scope.focusedMethod.writer.visitLdcInsn(((StringLiteral) node).value);
        }
    }

    private void compileArrayLiteral(ArrayLiteral node) {
        ArrayType type = (ArrayType) node.getType();
        assert type != null; // Should have been populated in typechecking pass
        MegaType elType = type.typeArg;
        String elTypeDescriptor = jvmDescriptor(elType, true);

        this.scope.focusedMethod.writer.visitLdcInsn(node.elements.size());
        this.scope.focusedMethod.writer.visitTypeInsn(ANEWARRAY, elTypeDescriptor);
        for (int i = 0; i < node.elements.size(); i++) {
            this.scope.focusedMethod.writer.visitInsn(DUP);
            this.scope.focusedMethod.writer.visitLdcInsn(i);
            Expression element = node.elements.get(i);
            compileNode(element);

            if (isPrimitive(elType)) {
                String elTypeClass = elType.className();
                String signature = String.format("(%s)L%s;", jvmDescriptor(elType, false), elTypeClass);
                this.scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, elTypeClass, "valueOf", signature, false);
            }
            this.scope.focusedMethod.writer.visitInsn(AASTORE);
        }
    }

    private void compileIndexExpression(IndexExpression node) {
        compileNode(node.target);
        compileNode(node.index);

        ArrayType arrayType = (ArrayType) node.target.getType();
        assert arrayType != null; // Should have been populated in typechecking pass
        MegaType arrayElType = arrayType.typeArg;
        assert arrayElType != null; // Should have been populated in typechecking pass

        this.scope.focusedMethod.writer.visitInsn(AALOAD);

        if (arrayElType == PrimitiveTypes.INTEGER) {
            this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, PrimitiveTypes.INTEGER.className(), "intValue", "()I", false);
        } else if (arrayElType == PrimitiveTypes.BOOLEAN) {
            this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, PrimitiveTypes.BOOLEAN.className(), "booleanValue", "()Z", false);
        } else if (arrayElType == PrimitiveTypes.FLOAT) {
            this.scope.focusedMethod.writer.visitMethodInsn(INVOKEVIRTUAL, PrimitiveTypes.FLOAT.className(), "floatValue", "()F", false);
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
            switch (node.operator) {
                case "+": {
                    compileStringConcatenation(node, this.scope, this::compileNode);
                    return;
                }
                case "*": {
                    compileStringRepetition(node, this.scope, this::compileNode);
                    return;
                }
            }
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
                    compileConditionalAndExpression(node, this.scope, this::compileNode);
                    break;
                case "||":
                    compileConditionalOrExpression(node, this.scope, this::compileNode);
                    break;
                default:
                    compileComparisonExpression(node, this.scope, this::compileNode);
                    break;
            }
        }
    }

    private void compileIfExpression(IfExpression node) {
        Label elseBlockLabel = new Label();
        Label endLabel = new Label();

        compileNode(node.condition);

        boolean hasElse = node.elseExpr != null;
        Label condFalseLabel = hasElse ? elseBlockLabel : endLabel;
        this.scope.focusedMethod.writer.visitJumpInsn(IFEQ, condFalseLabel);

        compileBlockExpression(node.thenExpr);
        this.scope.focusedMethod.writer.visitJumpInsn(GOTO, endLabel);

        if (hasElse) {
            this.scope.focusedMethod.writer.visitLabel(elseBlockLabel);
            Object[] localsSignatures = this.scope.getLocalsSignatures();
            this.scope.focusedMethod.writer.visitFrame(F_FULL, localsSignatures.length, localsSignatures, 0, null);

            compileBlockExpression(node.elseExpr);
        }

        this.scope.focusedMethod.writer.visitLabel(endLabel);
        this.scope.focusedMethod.writer.visitFrame(F_SAME, 0, null, 0, null);
    }

    private void compileBlockExpression(BlockExpression node) {
        Scope origScope = this.scope;
        this.scope = this.scope.createChild();

        compileStatements(node.statements);
        this.scope = origScope;
    }

    private void compileIdentifier(Identifier node) {
        String identName = node.value;
        Binding binding = this.scope.getBinding(identName);
        if (binding == null) {
            System.out.printf("Expected identifier %s to be present, but it wasn't", identName);
            return;
        }

        MegaType type = binding.type;
        assert type != null; // Should be filled in by the typechecking pass

        if (binding.bindingType == BindingTypes.STATIC) {
            this.scope.focusedMethod.writer.visitFieldInsn(GETSTATIC, this.className, identName, jvmDescriptor(type, false));
            return;
        }

        if (type == PrimitiveTypes.INTEGER) {
            this.scope.focusedMethod.writer.visitVarInsn(ILOAD, binding.index);
        } else if (type == PrimitiveTypes.BOOLEAN) {
            this.scope.focusedMethod.writer.visitVarInsn(ILOAD, binding.index);
        } else if (type == PrimitiveTypes.FLOAT) {
            this.scope.focusedMethod.writer.visitVarInsn(FLOAD, binding.index);
        } else {
            this.scope.focusedMethod.writer.visitVarInsn(ALOAD, binding.index);
        }
    }

    private void compileAssignmentExpression(AssignmentExpression node) {
        String identName = node.name.value;
        Binding binding = this.scope.getBinding(identName);
        if (binding == null) {
            System.out.printf("Expected identifier %s to be present, but it wasn't", identName);
            return;
        }
        if (!binding.isMutable) {
            System.out.printf("Expected identifier %s to be mutable, but it wasn't", identName);
            return;
        }

        compileNode(node.right);

        MegaType type = binding.type;
        assert type != null; // Should be filled in by the typechecking pass
        if (type == PrimitiveTypes.INTEGER) {
            this.scope.focusedMethod.writer.visitVarInsn(ISTORE, binding.index);
        } else if (type == PrimitiveTypes.BOOLEAN) {
            this.scope.focusedMethod.writer.visitVarInsn(ISTORE, binding.index);
        } else if (type == PrimitiveTypes.FLOAT) {
            this.scope.focusedMethod.writer.visitVarInsn(FSTORE, binding.index);
        } else {
            this.scope.focusedMethod.writer.visitVarInsn(ASTORE, binding.index);
        }
    }

    private void compileRangeExpression(RangeExpression node) {
        compileNode(node.leftBound);
        compileNode(node.rightBound);
        this.scope.focusedMethod.writer.visitMethodInsn(INVOKESTATIC, StdLib.Ranges, "of", "(II)[Ljava/lang/Integer;", false);
    }
}
