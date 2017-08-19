package co.kenrg.mega.frontend.typechecking;

import java.util.List;
import java.util.Optional;

import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.ast.expression.ArrayLiteral;
import co.kenrg.mega.frontend.ast.expression.BooleanLiteral;
import co.kenrg.mega.frontend.ast.expression.FloatLiteral;
import co.kenrg.mega.frontend.ast.expression.IntegerLiteral;
import co.kenrg.mega.frontend.ast.expression.StringLiteral;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.ExpressionStatement;
import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.ast.statement.LetStatement;
import co.kenrg.mega.frontend.ast.statement.VarStatement;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.frontend.typechecking.errors.TypeMismatchError;
import co.kenrg.mega.frontend.typechecking.errors.UnknownTypeError;
import co.kenrg.mega.frontend.typechecking.types.ArrayType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Lists;

public class TypeChecker {
    private static final MegaType unknownType = new MegaType() {
        @Override
        public String displayName() {
            return "<unknown>";
        }
    };

    private final List<TypeCheckerError> errors;

    public TypeChecker() {
        this.errors = Lists.newArrayList();
    }

    public <T extends Node> TypeCheckResult<T> typecheck(T node, TypeEnvironment env) {
        TypedNode<T> typedNode = typecheckNode(node, env);
        return new TypeCheckResult<>(typedNode, this.errors);
    }

    private <T extends Node> TypedNode<T> typecheckNode(T node, TypeEnvironment env) {
        MegaType type = PrimitiveTypes.UNIT;

        // Statements
        if (node instanceof Module) {
            this.typecheckStatements(((Module) node).statements, env);
        } else if (node instanceof ExpressionStatement) {
            TypedNode<Expression> typedNode = typecheckNode(((ExpressionStatement) node).expression, env);
            return new TypedNode<>(node, typedNode.type);
        } else if (node instanceof LetStatement) {
            this.typecheckLetStatement((LetStatement) node, env);
        } else if (node instanceof VarStatement) {
            this.typecheckVarStatement((VarStatement) node, env);
        }

        if (node instanceof IntegerLiteral) {
            type = PrimitiveTypes.INTEGER;
        } else if (node instanceof FloatLiteral) {
            type = PrimitiveTypes.FLOAT;
        } else if (node instanceof BooleanLiteral) {
            type = PrimitiveTypes.BOOLEAN;
        } else if (node instanceof StringLiteral) {
            type = PrimitiveTypes.STRING;
        } else if (node instanceof ArrayLiteral) {
            type = this.typecheckArrayLiteral((ArrayLiteral) node, env);
        }

        return new TypedNode<>(node, type);
    }

    private void typecheckStatements(List<Statement> statements, TypeEnvironment env) {
        for (Statement statement : statements) {
            //TODO: Record errors...
            this.typecheckNode(statement, env);
        }
    }

    private void typecheckLetStatement(LetStatement statement, TypeEnvironment env) {
        TypedNode<Expression> valueTypeResult = this.typecheckNode(statement.value, env);

        String name = statement.name.value;
        MegaType type = unknownType;

        if (statement.name.typeAnnotation != null) {
            Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(statement.name.typeAnnotation);
            if (declaredTypeOpt.isPresent()) {
                MegaType declaredType = declaredTypeOpt.get();
                if (declaredType.equals(valueTypeResult.type)) {
                    type = declaredType;
                } else {
                    this.errors.add(new TypeMismatchError(declaredType, valueTypeResult.type));
                }
            } else {
                this.errors.add(new UnknownTypeError(statement.name.typeAnnotation));
            }
        } else {
            type = valueTypeResult.type;
        }

        env.add(name, type, true);
    }

    private void typecheckVarStatement(VarStatement statement, TypeEnvironment env) {
        TypedNode<Expression> valueTypeResult = this.typecheckNode(statement.value, env);

        String name = statement.name.value;
        MegaType type = unknownType;

        if (statement.name.typeAnnotation != null) {
            Optional<MegaType> declaredTypeOpt = PrimitiveTypes.byDisplayName(statement.name.typeAnnotation);
            if (declaredTypeOpt.isPresent()) {
                MegaType declaredType = declaredTypeOpt.get();
                if (declaredType.equals(valueTypeResult.type)) {
                    type = declaredType;
                } else {
                    this.errors.add(new TypeMismatchError(declaredType, valueTypeResult.type));
                }
            } else {
                this.errors.add(new UnknownTypeError(statement.name.typeAnnotation));
            }
        } else {
            type = valueTypeResult.type;
        }

        env.add(name, type, false);
    }

    private MegaType typecheckArrayLiteral(ArrayLiteral array, TypeEnvironment env) {
        if (array.elements.isEmpty()) {
            return new ArrayType(PrimitiveTypes.NOTHING);
        }
        List<Expression> elements = array.elements;

        MegaType type = null;
        MegaType firstMismatch = null;
        for (Expression elem : elements) {
            TypedNode<Expression> elemTypeResult = typecheckNode(elem, env);
            if (type == null) {
                type = elemTypeResult.type;
            }
            if (!elemTypeResult.type.equals(type)) {
                firstMismatch = elemTypeResult.type;
            }
        }

        if (firstMismatch != null) {
            this.errors.add(new TypeMismatchError(type, firstMismatch));
        }

        return new ArrayType(type);
    }
}
